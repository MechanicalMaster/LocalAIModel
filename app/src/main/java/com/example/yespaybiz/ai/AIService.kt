package com.example.yespaybiz.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device LLM service using MediaPipe LLM Inference API with Gemma-3 1B.
 *
 * The model file is imported into app internal storage via a file picker.
 */
object AIService {

    private const val TAG = "AIService"

    // Model filename — used across the app for import and initialization
    const val MODEL_FILENAME = "gemma3-1B-it-int4.task"

    // Version stamps written into every log row so eval can be diffed across releases
    const val MODEL_VERSION  = "gemma3-1B-it-int4"
    const val PROMPT_VERSION = "v3"   // bump whenever SYSTEM_PROMPT changes

    // Max new tokens to generate per response
    private const val MAX_TOKENS = 1024

    // Gemma instruction-tuned chat prompt format
    private const val TURN_USER  = "<start_of_turn>user\n"
    private const val TURN_MODEL = "<start_of_turn>model\n"
    private const val TURN_END   = "<end_of_turn>\n"

    // System prompt: ONLY extracts intent and arguments, never generates answers.
    // The LLM's sole job is to output a single JSON tool call — nothing else.
    private const val SYSTEM_PROMPT =
        "You are a merchant payment assistant. Your ONLY job is to output a JSON tool call.\n" +
        "Available tools:\n" +
        "- getTransactions(dateRange, limit): user wants to see individual transactions\n" +
        "- getCollections(dateRange): user wants total collected amount or count\n" +
        "- getSettlementStatus(): user wants settlement or payout info\n" +
        "- getHoldTransactions(): user asks about transactions on hold or held back\n" +
        "- navigateToTransactions(): user wants to go to transactions screen\n" +
        "- navigateToQR(): user wants to open QR code\n" +
        "- navigateToSettlement(): user wants to go to settlement screen\n" +
        "dateRange values: \"today\", \"yesterday\", \"this week\"\n" +
        "Output ONLY a single JSON object, no explanation, no text before or after:\n" +
        "{\"type\":\"tool_call\",\"name\":\"TOOL_NAME\",\"arguments\":{\"dateRange\":\"today\"}}"

    sealed class ModelState {
        object Loading : ModelState()
        object Ready   : ModelState()
        object MissingModel : ModelState()
        object ImportingModel : ModelState()
        data class Error(val message: String) : ModelState()
    }

    private val _modelState = MutableStateFlow<ModelState>(ModelState.Loading)
    val modelState: StateFlow<ModelState> = _modelState

    private var llmInference: LlmInference? = null

    /**
     * Initialize the LlmInference instance. Must be called once before [sendMessage].
     * Runs on [Dispatchers.IO]; safe to call from any thread.
     */
    fun initialize(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // We now load the model from the app's internal filesDir
                val modelFile = java.io.File(context.filesDir, MODEL_FILENAME)
                if (!modelFile.exists()) {
                    _modelState.value = ModelState.MissingModel
                    return@launch
                }
                
                Log.i(TAG, "Initializing LlmInference from: ${modelFile.absolutePath}")
                
                // LlmInferenceOptions is a nested class inside LlmInference
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTopK(64)
                    .setMaxTokens(MAX_TOKENS)
                    .build()
                llmInference = LlmInference.createFromOptions(context, options)
                _modelState.value = ModelState.Ready
                Log.i(TAG, "LlmInference initialized successfully")
            } catch (e: Exception) {
                val msg = "Failed to load model: ${e.message}"
                Log.e(TAG, msg, e)
                _modelState.value = ModelState.Error(msg)
            }
        }
    }

    /**
     * Build a Gemma-formatted prompt for intent + argument extraction.
     * Only sends the current user message — no history — so the model stays focused
     * on emitting a clean JSON tool call for this single query.
     *
     * [contextHint] is an optional one-line summary of the previous function call,
     * injected so the model can resolve follow-up questions correctly.
     */
    fun buildPrompt(userMessage: String, contextHint: String = ""): String {
        val prompt = StringBuilder()
            .append(TURN_USER)
            .append(SYSTEM_PROMPT)
            .apply { if (contextHint.isNotBlank()) append(contextHint) }
            .append("\n\nUser request: ")
            .append(userMessage)
            .append(TURN_END)
            .append(TURN_MODEL)
            .toString()
        Log.d(TAG, "Prompt:\n$prompt")
        return prompt
    }

    /**
     * Send a prompt and stream partial results via [onPartialResult].
     * Suspends until generation is complete; returns the full response string.
     *
     * Must only be called when [modelState] is [ModelState.Ready].
     */
    suspend fun sendMessage(
        prompt: String,
        onPartialResult: (String) -> Unit
    ): String = suspendCancellableCoroutine { continuation ->
        val inference = llmInference
        if (inference == null) {
            Log.e(TAG, "sendMessage called but llmInference is null!")
            continuation.resumeWithException(
                IllegalStateException("Model not initialized")
            )
            return@suspendCancellableCoroutine
        }

        Log.d(TAG, "Starting generateResponseAsync...")

        try {
            val fullResponse = StringBuilder()
            var callbackCount = 0

            // generateResponseAsync streams tokens via the result listener;
            // 'done' is true on the final callback.
            inference.generateResponseAsync(prompt) { partialResult, done ->
                callbackCount++
                Log.d(TAG, "Callback #$callbackCount: partialResult=${partialResult?.take(50)}, done=$done")

                if (partialResult != null && partialResult.isNotEmpty()) {
                    fullResponse.append(partialResult)
                    onPartialResult(partialResult)
                }
                if (done) {
                    Log.d(TAG, "Generation done. Total callbacks=$callbackCount, response length=${fullResponse.length}")
                    Log.d(TAG, "Full response: ${fullResponse.toString().take(200)}")
                    if (continuation.isActive) {
                        val result = fullResponse.toString()
                        if (result.isBlank()) {
                            Log.w(TAG, "Model returned empty response!")
                        }
                        continuation.resume(result)
                    }
                }
            }
            Log.d(TAG, "generateResponseAsync call returned (callback pending)")
        } catch (e: Exception) {
            Log.e(TAG, "Error during inference: ${e.message}", e)
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }

        continuation.invokeOnCancellation {
            Log.d(TAG, "Inference coroutine cancelled")
        }
    }

    /**
     * Release native resources. Call from ViewModel.onCleared().
     */
    fun close() {
        llmInference?.close()
        llmInference = null
        _modelState.value = ModelState.Loading
    }
}
