package com.example.yespaybiz.ai

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalTime

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    val modelState: StateFlow<AIService.ModelState> = AIService.modelState

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    private val _exportEvent = MutableSharedFlow<File>()
    val exportEvent = _exportEvent.asSharedFlow()

    // Conversation context
    private var lastFunctionName: String = ""
    private var lastFunctionArgs: Map<String, Any> = emptyMap()
    private var lastFunctionResult: String = ""

    init {
        AIService.initialize(application)
        ChatLogger.init(application)
        // Show greeting + proactive alerts shortly after the screen opens
        viewModelScope.launch {
            delay(600)
            loadProactiveAlerts()
        }
    }

    // ── Proactive alerts on open ──────────────────────────────────────────────────

    private fun loadProactiveAlerts() {
        val greeting = timeAwareGreeting()
        val greetMsg = ChatMessage(
            role = Role.ASSISTANT,
            text = greeting,
            suggestedFollowUps = listOf("Daily summary", "Settlement status", "Show hold transactions")
        )
        _messages.value = listOf(greetMsg)
    }

    private fun timeAwareGreeting(): String {
        val hour = LocalTime.now().hour
        val salutation = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else      -> "Good evening"
        }
        return "$salutation! I'm your AI Assistant.\nAsk me about your collections, transactions, settlements, or holds."
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
                context.contentResolver.openInputStream(uri)?.use { ins ->
                    FileOutputStream(destFile).use { out -> ins.copyTo(out) }
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

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isGenerating.value) return
        val trimmed = userText.trim()
        val startMs = System.currentTimeMillis()
        val useHinglish = ToolCallRouter.isHinglish(trimmed)

        val userMsg = ChatMessage(role = Role.USER, text = trimmed)
        var currentList = _messages.value + userMsg
        _messages.value = currentList

        var assistantMsg = ChatMessage(role = Role.ASSISTANT, text = "", isStreaming = true)
        currentList = currentList + assistantMsg
        _messages.value = currentList
        _isGenerating.value = true

        viewModelScope.launch(Dispatchers.IO) {
            var classifierStr  = ""
            var llmCalled      = false
            var llmRaw         = ""
            var llmParseOk     = false
            var guardrailFixed = false
            var functionCalled = ""
            var finalAnswer    = ""
            var errorStr: String? = null

            try {
                // Step 1 — classify
                val intent = ToolCallRouter.classify(resolveWithContext(trimmed))
                classifierStr = intent.describe()

                if (intent is ToolCallRouter.UnknownIntent) {
                    finalAnswer = if (useHinglish)
                        "Mujhe sirf AI Biz business data ke baare mein puchho.\nJaise: \"aaj ka collection\", \"transactions dikhao\", ya \"settlement kab aayega\"."
                    else
                        "I can only help with your AI Biz business data.\nTry: collections, transactions, settlement, holds, or disputes."
                    functionCalled = "REFUSED"
                    setFinal(currentList, assistantMsg, finalAnswer, emptyList())
                    return@launch
                }

                // Step 2 — navigation (no LLM)
                val classified = intent as? ToolCallRouter.KnownIntent
                if (classified?.functionName?.startsWith("navigate") == true) {
                    finalAnswer = "Opening ${classified.functionName.removePrefix("navigateTo")}…"
                    functionCalled = classified.functionName
                    setFinal(currentList, assistantMsg, finalAnswer, emptyList())
                    _navigationEvent.emit(classified.functionName)
                    return@launch
                }

                // Step 3 — LLM for arg extraction
                update(currentList, assistantMsg, if (useHinglish) "Soch raha hoon…" else "Thinking…")
                llmCalled = true
                val prompt = AIService.buildPrompt(trimmed, buildContextHint())
                val accumulated = StringBuilder()
                AIService.sendMessage(prompt) { partial -> accumulated.append(partial) }
                llmRaw = accumulated.toString()

                // Step 4 — parse + guardrail
                val resolved   = parseLlmToolCall(llmRaw, trimmed)
                llmParseOk = resolved != null

                val finalCall: ToolCallRouter.ResolvedToolCall = when {
                    resolved != null -> {
                        val modelName  = extractModelFunctionName(llmRaw)
                        guardrailFixed = modelName != null && modelName != resolved.functionName
                        resolved
                    }
                    intent is ToolCallRouter.KnownIntent ->
                        ToolCallRouter.ResolvedToolCall(intent.functionName, intent.arguments)
                    intent is ToolCallRouter.LowConfidence -> {
                        finalAnswer = if (useHinglish)
                            "Samajh nahi aaya — thoda aur batao?\nJaise: \"aaj ka collection\", \"transactions dikhao\"."
                        else
                            "I'm not sure what you mean. Could you rephrase?\nTry: \"today's collection\", \"show transactions\", or \"settlement status\"."
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

                // Step 5 — execute
                functionCalled = finalCall.functionName
                update(currentList, assistantMsg, if (useHinglish) "Data la raha hoon…" else "Fetching data…")
                val funcResult = AppFunctions.executeFunction(finalCall.functionName, finalCall.arguments)

                if (funcResult.contains("\"error\"")) {
                    finalAnswer = if (useHinglish)
                        "Abhi data nahi mil raha. Thodi der baad try karo."
                    else
                        "Couldn't fetch the data right now. Please try again in a moment."
                    errorStr = funcResult
                    setFinal(currentList, assistantMsg, finalAnswer, emptyList())
                    return@launch
                }

                // Step 6 — Kotlin formatter (no 2nd LLM pass)
                finalAnswer = AppFunctions.formatAnswer(finalCall.functionName, funcResult, useHinglish)

                lastFunctionName   = finalCall.functionName
                lastFunctionArgs   = finalCall.arguments
                lastFunctionResult = funcResult

                val chips = ToolCallRouter.suggestedFollowUps(finalCall.functionName, finalCall.arguments)
                setFinal(currentList, assistantMsg, finalAnswer, chips)

            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error in sendMessage", e)
                errorStr    = e.message
                finalAnswer = "Something went wrong. Please try again."
                setFinal(currentList, assistantMsg, finalAnswer, emptyList())
            } finally {
                ChatLogger.log(ChatLogger.TurnLog(
                    userInput        = trimmed,
                    classifierResult = classifierStr,
                    llmCalled        = llmCalled,
                    llmRawOutput     = llmRaw,
                    llmParseOk       = llmParseOk,
                    guardrailFixed   = guardrailFixed,
                    functionCalled   = functionCalled,
                    finalAnswer      = finalAnswer,
                    latencyMs        = System.currentTimeMillis() - startMs,
                    modelVersion     = AIService.MODEL_VERSION,
                    promptVersion    = AIService.PROMPT_VERSION,
                    error            = errorStr
                ))
                _isGenerating.value = false
            }
        }
    }

    // ── Context helpers ───────────────────────────────────────────────────────────

    private fun resolveWithContext(q: String): String {
        if (lastFunctionName.isEmpty()) return q
        return if (isFollowUpQuery(q))
            "$q [context: previous=$lastFunctionName args=$lastFunctionArgs]"
        else q
    }

    private fun buildContextHint(): String {
        if (lastFunctionName.isEmpty()) return ""
        return "\n[Context: previous=$lastFunctionName, args=$lastFunctionArgs, result=${lastFunctionResult.take(200)}]"
    }

    private fun isFollowUpQuery(q: String) = listOf(
        "which one", "show more", "what about", "and the", "how about",
        "the failed", "the success", "that one", "tell me more", "more details", "previous", "same"
    ).any { q.lowercase().contains(it) }

    // ── State helpers ─────────────────────────────────────────────────────────────

    private fun update(list: List<ChatMessage>, target: ChatMessage, text: String) {
        _messages.value = list.map { if (it.id == target.id) it.copy(text = text, isStreaming = true) else it }
    }

    private fun setFinal(list: List<ChatMessage>, target: ChatMessage, text: String, chips: List<String>) {
        _messages.value = list.map {
            if (it.id == target.id) it.copy(text = text, isStreaming = false, suggestedFollowUps = chips) else it
        }
        _isGenerating.value = false
    }

    private fun ToolCallRouter.Intent.describe() = when (this) {
        is ToolCallRouter.KnownIntent   -> "KnownIntent(${functionName},${arguments["dateRange"] ?: ""})"
        is ToolCallRouter.LowConfidence -> "LowConfidence($likelyFunctionName)"
        is ToolCallRouter.UnknownIntent -> "UnknownIntent"
    }

    private fun parseLlmToolCall(raw: String, userQuestion: String): ToolCallRouter.ResolvedToolCall? {
        var text = raw.trim()
        if (text.startsWith("```")) text = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = text.indexOf('{'); val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return try {
            val el = com.google.gson.JsonParser.parseString(text.substring(start, end + 1)).asJsonObject
            if (!el.has("type") || el.get("type").asString != "tool_call") return null
            val modelFn   = el.get("name")?.asString ?: return null
            val argsEl    = el.get("arguments")
            val argsMap: Map<String, Any> = if (argsEl != null && argsEl.isJsonObject)
                com.google.gson.Gson().fromJson(argsEl, object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type) ?: emptyMap()
            else emptyMap()
            ToolCallRouter.resolve(userQuestion, modelFn, argsMap)
        } catch (e: Exception) { null }
    }

    private fun extractModelFunctionName(raw: String): String? = try {
        val s = raw.indexOf('{'); val e = raw.lastIndexOf('}')
        if (s == -1 || e <= s) null
        else com.google.gson.JsonParser.parseString(raw.substring(s, e + 1)).asJsonObject.get("name")?.asString
    } catch (e: Exception) { null }

    override fun onCleared() {
        super.onCleared()
        AIService.close()
    }
}
