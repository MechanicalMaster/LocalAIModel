package com.example.yespaybiz.ai

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // Shared flow that carries a File to share when the user requests log export
    private val _exportEvent = MutableSharedFlow<File>()
    val exportEvent = _exportEvent.asSharedFlow()

    /**
     * Conversation context: remembers the last successfully executed function call
     * so follow-up questions (e.g. "which one failed?") can reference it.
     */
    private var lastFunctionName: String = ""
    private var lastFunctionArgs: Map<String, Any> = emptyMap()
    private var lastFunctionResult: String = ""

    init {
        AIService.initialize(application)
        ChatLogger.init(application)
    }

    // ── Model import ──────────────────────────────────────────────────────────────

    fun importModelFromUri(context: android.content.Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
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
                AIService.initialize(context)
            } catch (e: Exception) {
                val field = AIService::class.java.getDeclaredField("_modelState")
                field.isAccessible = true
                val stateFlow = field.get(AIService) as MutableStateFlow<AIService.ModelState>
                stateFlow.value = AIService.ModelState.Error("Failed to import model: ${e.message}")
            }
        }
    }

    // ── Log export ────────────────────────────────────────────────────────────────

    fun exportLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = ChatLogger.exportToFile(getApplication())
            if (file != null) _exportEvent.emit(file)
        }
    }

    // ── Main send flow ────────────────────────────────────────────────────────────

    /**
     * Hybrid flow:
     *
     * classify() →
     *   UnknownIntent   → instant canned refusal (no LLM)
     *   LowConfidence   → LLM called; if JSON parse fails → ask for clarification
     *   KnownIntent     → LLM called for arg extraction → guardrail → tool → Kotlin formatter
     *
     * Conversation context is injected into the classifier and LLM prompt so
     * follow-up questions ("which one failed?") resolve correctly.
     */
    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isGenerating.value) return
        val trimmed = userText.trim()
        val startMs = System.currentTimeMillis()

        // ── Append user bubble ────────────────────────────────────────────────────
        val userMsg = ChatMessage(role = Role.USER, text = trimmed)
        var currentList = _messages.value + userMsg
        _messages.value = currentList

        // ── Placeholder assistant bubble ──────────────────────────────────────────
        var assistantMsg = ChatMessage(role = Role.ASSISTANT, text = "", isStreaming = true)
        currentList = currentList + assistantMsg
        _messages.value = currentList
        _isGenerating.value = true

        viewModelScope.launch(Dispatchers.IO) {

            // Mutable log fields — filled in as the pipeline progresses
            var classifierStr   = ""
            var llmCalled       = false
            var llmRaw          = ""
            var llmParseOk      = false
            var guardrailFixed  = false
            var functionCalled  = ""
            var finalAnswer     = ""
            var errorStr: String? = null

            try {
                // ── Step 1: Kotlin classifier ─────────────────────────────────────
                val intent = ToolCallRouter.classify(resolveWithContext(trimmed))
                classifierStr = intent.describe()

                // ── UnknownIntent: refuse, no LLM ────────────────────────────────
                if (intent is ToolCallRouter.UnknownIntent) {
                    finalAnswer = "I can only help with your YesPayBiz business data.\n" +
                                  "Try asking about your transactions, collections, settlement, or hold transactions."
                    functionCalled = "REFUSED"
                    setFinal(currentList, assistantMsg, finalAnswer, emptyList())
                    return@launch
                }

                // ── Navigation: no LLM, no data needed ───────────────────────────
                val classified = intent as? ToolCallRouter.KnownIntent
                if (classified?.functionName?.startsWith("navigate") == true) {
                    finalAnswer = "Opening ${classified.functionName.removePrefix("navigateTo")}…"
                    functionCalled = classified.functionName
                    setFinal(currentList, assistantMsg, finalAnswer, emptyList())
                    _navigationEvent.emit(classified.functionName)
                    return@launch
                }

                // ── Step 2: LLM — intent + arg extraction ─────────────────────────
                update(currentList, assistantMsg, "Thinking…")
                llmCalled = true

                val contextHint = buildContextHint()
                val prompt = AIService.buildPrompt(trimmed, contextHint)
                val accumulated = StringBuilder()

                AIService.sendMessage(prompt) { partial ->
                    accumulated.append(partial)
                    // Don't stream raw JSON to the user
                }
                llmRaw = accumulated.toString()

                // ── Step 3: Parse + guardrail ─────────────────────────────────────
                val resolved = parseLlmToolCall(llmRaw, trimmed)
                llmParseOk = resolved != null

                val finalCall: ToolCallRouter.ResolvedToolCall = when {
                    resolved != null -> {
                        // Check if guardrail corrected the model's choice
                        val modelName = extractModelFunctionName(llmRaw)
                        guardrailFixed = modelName != null && modelName != resolved.functionName
                        resolved
                    }
                    intent is ToolCallRouter.KnownIntent -> {
                        // LLM failed JSON but classifier was confident — use classifier result
                        android.util.Log.w("ChatViewModel", "LLM parse failed, using classifier fallback")
                        ToolCallRouter.ResolvedToolCall(intent.functionName, intent.arguments)
                    }
                    intent is ToolCallRouter.LowConfidence -> {
                        // LLM failed AND classifier was uncertain — ask for clarification
                        finalAnswer = "I'm not sure what you mean. Could you rephrase?\n" +
                                      "For example: \"today's collection\", \"show transactions\", or \"settlement status\"."
                        functionCalled = "CLARIFICATION"
                        setFinal(currentList, assistantMsg, finalAnswer, emptyList())
                        return@launch
                    }
                    else -> {
                        finalAnswer = "Something went wrong. Please try again."
                        setFinal(currentList, assistantMsg, finalAnswer, emptyList())
                        return@launch
                    }
                }

                // ── Step 4: Execute tool ──────────────────────────────────────────
                functionCalled = finalCall.functionName
                update(currentList, assistantMsg, "Fetching data…")
                val funcResult = AppFunctions.executeFunction(finalCall.functionName, finalCall.arguments)

                // Check for error JSON from AppFunctions
                if (funcResult.contains("\"error\"")) {
                    finalAnswer = "Couldn't fetch the data right now. Please try again in a moment."
                    errorStr = funcResult
                    setFinal(currentList, assistantMsg, finalAnswer, emptyList())
                    return@launch
                }

                // ── Step 5: Kotlin formatter — no 2nd LLM pass ───────────────────
                finalAnswer = AppFunctions.formatAnswer(finalCall.functionName, funcResult)

                // Update conversation context for follow-up questions
                lastFunctionName   = finalCall.functionName
                lastFunctionArgs   = finalCall.arguments
                lastFunctionResult = funcResult

                val chips = ToolCallRouter.suggestedFollowUps(finalCall.functionName, finalCall.arguments)
                setFinal(currentList, assistantMsg, finalAnswer, chips)

            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error in sendMessage", e)
                errorStr = e.message
                finalAnswer = "Something went wrong. Please try again."
                setFinal(currentList, assistantMsg, finalAnswer, emptyList())
            } finally {
                val latency = System.currentTimeMillis() - startMs
                ChatLogger.log(
                    ChatLogger.TurnLog(
                        userInput       = trimmed,
                        classifierResult = classifierStr,
                        llmCalled       = llmCalled,
                        llmRawOutput    = llmRaw,
                        llmParseOk      = llmParseOk,
                        guardrailFixed  = guardrailFixed,
                        functionCalled  = functionCalled,
                        finalAnswer     = finalAnswer,
                        latencyMs       = latency,
                        modelVersion    = AIService.MODEL_VERSION,
                        promptVersion   = AIService.PROMPT_VERSION,
                        error           = errorStr
                    )
                )
                _isGenerating.value = false
            }
        }
    }

    // ── Conversation context ──────────────────────────────────────────────────────

    /**
     * If the user asks a follow-up like "which one failed?" or "show more",
     * append the last function context so the classifier gets a richer signal.
     */
    private fun resolveWithContext(q: String): String {
        if (lastFunctionName.isEmpty()) return q
        val isFollowUp = isFollowUpQuery(q)
        return if (isFollowUp)
            "$q [context: previous query was $lastFunctionName with args $lastFunctionArgs]"
        else q
    }

    private fun buildContextHint(): String {
        if (lastFunctionName.isEmpty()) return ""
        return "\n[Context: the previous query used $lastFunctionName, args=$lastFunctionArgs, result summary: ${lastFunctionResult.take(200)}]"
    }

    private fun isFollowUpQuery(q: String): Boolean {
        val followUpSignals = listOf(
            "which one", "show more", "what about", "and the",
            "how about", "the failed", "the success", "that one",
            "tell me more", "more details", "previous", "same"
        )
        return followUpSignals.any { q.lowercase().contains(it) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private fun update(list: List<ChatMessage>, target: ChatMessage, text: String) {
        _messages.value = list.map { if (it.id == target.id) it.copy(text = text, isStreaming = true) else it }
    }

    private fun setFinal(
        list: List<ChatMessage>,
        target: ChatMessage,
        text: String,
        chips: List<String>
    ) {
        _messages.value = list.map {
            if (it.id == target.id) it.copy(text = text, isStreaming = false, suggestedFollowUps = chips) else it
        }
        _isGenerating.value = false
    }

    private fun ToolCallRouter.Intent.describe(): String = when (this) {
        is ToolCallRouter.KnownIntent    -> "KnownIntent(${functionName},${arguments["dateRange"] ?: ""})"
        is ToolCallRouter.LowConfidence  -> "LowConfidence(${likelyFunctionName})"
        is ToolCallRouter.UnknownIntent  -> "UnknownIntent"
    }

    private fun parseLlmToolCall(raw: String, userQuestion: String): ToolCallRouter.ResolvedToolCall? {
        var text = raw.trim()
        if (text.startsWith("```")) {
            text = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }
        val start = text.indexOf('{')
        val end   = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null

        return try {
            val jsonStr = text.substring(start, end + 1)
            val element = com.google.gson.JsonParser.parseString(jsonStr).asJsonObject
            if (!element.has("type") || element.get("type").asString != "tool_call") return null

            val modelFuncName = element.get("name")?.asString ?: return null
            val argsElement   = element.get("arguments")
            val modelArgsMap: Map<String, Any> = if (argsElement != null && argsElement.isJsonObject)
                com.google.gson.Gson().fromJson(
                    argsElement,
                    object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                ) ?: emptyMap()
            else emptyMap()

            ToolCallRouter.resolve(
                userQuestion        = userQuestion,
                modelFunctionName   = modelFuncName,
                modelArguments      = modelArgsMap
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractModelFunctionName(raw: String): String? {
        return try {
            val start = raw.indexOf('{'); val end = raw.lastIndexOf('}')
            if (start == -1 || end <= start) return null
            com.google.gson.JsonParser.parseString(raw.substring(start, end + 1))
                .asJsonObject.get("name")?.asString
        } catch (e: Exception) { null }
    }

    override fun onCleared() {
        super.onCleared()
        AIService.close()
    }
}
