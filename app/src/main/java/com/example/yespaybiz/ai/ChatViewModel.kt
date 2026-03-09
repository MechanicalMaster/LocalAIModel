package com.example.yespaybiz.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val modelState: StateFlow<AIService.ModelState> = AIService.modelState

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // Pass the Application context
    init {
        AIService.initialize(application)
    }

    /**
     * Imports the model file from the given URI to the app's internal filesDir,
     * so it can be accessed by MediaPipe via an absolute filesystem path.
     */
    fun importModelFromUri(context: android.content.Context, uri: Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Signal to UI that we are importing
                val field = AIService::class.java.getDeclaredField("_modelState")
                field.isAccessible = true
                val stateFlow = field.get(AIService) as MutableStateFlow<AIService.ModelState>
                stateFlow.value = AIService.ModelState.ImportingModel
                
                val destFile = File(context.filesDir, AIService.MODEL_FILENAME)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                // Once copied, re-initialize AIService so it picks up the new file
                AIService.initialize(context)
            } catch (e: Exception) {
                val field = AIService::class.java.getDeclaredField("_modelState")
                field.isAccessible = true
                val stateFlow = field.get(AIService) as MutableStateFlow<AIService.ModelState>
                stateFlow.value = AIService.ModelState.Error("Failed to import model: ${e.message}")
            }
        }
    }

    /**
     * Hybrid flow:
     *
     * 1. Kotlin classifier checks the question first (no LLM cost).
     *    - UnknownIntent → instant canned refusal, done.
     *    - KnownIntent   → proceed to step 2.
     *
     * 2. LLM (Pass 1 only) extracts intent + arguments from natural language.
     *    A deterministic guardrail (ToolCallRouter.resolve) corrects any mismatch.
     *
     * 3. AppFunctions executes the resolved tool call and returns structured data.
     *
     * 4. Kotlin formatter (AppFunctions.formatAnswer) builds the final answer.
     *    No second LLM pass — answers are always accurate.
     */
    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        if (_isGenerating.value) return

        val trimmed = userText.trim()

        // Append user message
        val userMsg = ChatMessage(role = Role.USER, text = trimmed)
        var currentList = _messages.value + userMsg
        _messages.value = currentList

        // Placeholder assistant message
        var assistantMsg = ChatMessage(role = Role.ASSISTANT, text = "", isStreaming = true)
        currentList = currentList + assistantMsg
        _messages.value = currentList

        _isGenerating.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // ── Step 1: Kotlin classifier ────────────────────────────────────
                val intent = ToolCallRouter.classify(trimmed)

                if (intent is ToolCallRouter.UnknownIntent) {
                    // Outside merchant domain — no LLM call, instant refusal
                    android.util.Log.d("ChatViewModel", "UnknownIntent for: $trimmed")
                    setFinalMessage(currentList, assistantMsg,
                        "I can only help with your YesPayBiz business data.\n" +
                        "Try asking about your transactions, collections, or settlement status.")
                    return@launch
                }

                val classified = intent as ToolCallRouter.KnownIntent

                // Handle navigation immediately — no data fetch needed
                if (classified.functionName.startsWith("navigate")) {
                    setFinalMessage(currentList, assistantMsg,
                        "Opening ${classified.functionName.removePrefix("navigateTo")}…")
                    _navigationEvent.emit(classified.functionName)
                    return@launch
                }

                // ── Step 2: LLM — intent + argument extraction only ───────────────
                // Show thinking state while LLM runs
                updateMessage(currentList, assistantMsg, "Thinking…", streaming = true)

                val prompt = AIService.buildPrompt(trimmed)
                val accumulated = StringBuilder()

                AIService.sendMessage(prompt) { partial ->
                    accumulated.append(partial)
                    // Keep "Thinking…" visible — don't stream raw JSON to the user
                }

                android.util.Log.d("ChatViewModel", "LLM raw output: ${accumulated.toString().take(300)}")

                // ── Step 3: Parse LLM output, apply guardrail ─────────────────────
                val resolvedCall = parseLlmToolCall(accumulated.toString(), trimmed)
                    ?: run {
                        // LLM failed to emit valid JSON — fall back to the classifier result
                        android.util.Log.w("ChatViewModel", "LLM did not emit valid JSON, using classifier result")
                        ToolCallRouter.ResolvedToolCall(classified.functionName, classified.arguments)
                    }

                android.util.Log.d("ChatViewModel", "Resolved: ${resolvedCall.functionName} args=${resolvedCall.arguments}")

                // ── Step 4: Execute tool ──────────────────────────────────────────
                updateMessage(currentList, assistantMsg, "Fetching data…", streaming = true)
                val funcResult = AppFunctions.executeFunction(resolvedCall.functionName, resolvedCall.arguments)
                android.util.Log.d("ChatViewModel", "Function result: $funcResult")

                // ── Step 5: Kotlin formatter → final answer (no 2nd LLM pass) ────
                val answer = AppFunctions.formatAnswer(resolvedCall.functionName, funcResult)
                setFinalMessage(currentList, assistantMsg, answer)

            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error in sendMessage", e)
                setFinalMessage(currentList, assistantMsg,
                    "Something went wrong. Please try again.")
            } finally {
                _isGenerating.value = false
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private fun updateMessage(
        list: List<ChatMessage>,
        target: ChatMessage,
        text: String,
        streaming: Boolean
    ) {
        _messages.value = list.map { msg ->
            if (msg.id == target.id) msg.copy(text = text, isStreaming = streaming) else msg
        }
    }

    private fun setFinalMessage(
        list: List<ChatMessage>,
        target: ChatMessage,
        text: String
    ) {
        _messages.value = list.map { msg ->
            if (msg.id == target.id) msg.copy(text = text, isStreaming = false) else msg
        }
        _isGenerating.value = false
    }

    /**
     * Parses the raw LLM output into a [ToolCallRouter.ResolvedToolCall].
     * Strips markdown fences, extracts the JSON object, and applies the
     * deterministic guardrail to correct any model mistakes.
     * Returns null if the output is not a recognisable tool call.
     */
    private fun parseLlmToolCall(
        raw: String,
        userQuestion: String
    ): ToolCallRouter.ResolvedToolCall? {
        var text = raw.trim()
        // Strip markdown code fences if present
        if (text.startsWith("```")) {
            text = text.removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
        }
        // Extract first JSON object
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null

        return try {
            val jsonStr = text.substring(start, end + 1)
            val element = com.google.gson.JsonParser.parseString(jsonStr).asJsonObject
            if (!element.has("type") || element.get("type").asString != "tool_call") return null

            val modelFuncName = element.get("name")?.asString ?: return null
            val argsElement = element.get("arguments")
            val modelArgsMap: Map<String, Any> = if (argsElement != null && argsElement.isJsonObject) {
                com.google.gson.Gson().fromJson(
                    argsElement,
                    object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                ) ?: emptyMap()
            } else {
                emptyMap()
            }

            ToolCallRouter.resolve(
                userQuestion = userQuestion,
                modelFunctionName = modelFuncName,
                modelArguments = modelArgsMap
            )
        } catch (e: Exception) {
            android.util.Log.d("ChatViewModel", "JSON parse failed: ${e.message}")
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        AIService.close()
    }
}
