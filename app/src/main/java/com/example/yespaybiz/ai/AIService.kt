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

    // Max new tokens to generate per response
    private const val MAX_TOKENS = 1024

    // Gemma instruction-tuned chat prompt format
    private const val TURN_USER  = "<start_of_turn>user\n"
    private const val TURN_MODEL = "<start_of_turn>model\n"
    private const val TURN_END   = "<end_of_turn>\n"

    // System instruction: with clear function descriptions for accurate tool selection
    private const val SYSTEM_PROMPT = 
        "You are YesPayBiz AI assistant. You can call these tools:\n" +
        "- getTransactions(): get list of individual transactions with IDs, amounts, status\n" +
        "- getCollections(): get total collection summary (total amount, count)\n" +
        "- getSettlementStatus(): get settlement/payout status\n" +
        "- navigateToTransactions(): open transactions screen\n" +
        "- navigateToQR(): open QR code screen\n" +
        "- navigateToSettlement(): open settlement screen\n" +
        "If user asks about specific transactions, use getTransactions. If user asks about total collections, use getCollections.\n" +
<<<<<<< ours
<<<<<<< ours
        "If user asks \"How much did I collect\" or similar, ALWAYS use getCollections (not getSettlementStatus).\n" +
        "If user asks for last/recent N transactions, pass limit=N.\n" +
        "If user asks about today/yesterday/week, pass the matching dateRange.\n" +
=======
>>>>>>> theirs
=======
>>>>>>> theirs
        "For tool calls, output ONLY JSON: {\"type\":\"tool_call\",\"name\":\"FUNCTION_NAME\",\"arguments\":{}}\n" +
        "If no tool is needed, answer normally."

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
     * Build a Gemma-formatted prompt from the conversation history.
     * Injects the system prompt into the first user turn.
     * Clamps history to the last 6 messages to prevent context degradation.
     */
    fun buildPrompt(messages: List<ChatMessage>): String {
        val sb = StringBuilder()
        
        // Truncate memory to prevent 270M model degradation
        val recentMessages = messages.takeLast(6)
        
        for ((index, msg) in recentMessages.withIndex()) {
            if (msg.role == Role.USER || msg.role == Role.SYSTEM) {
                sb.append(TURN_USER)
                if (index == 0) {
                    sb.append(SYSTEM_PROMPT).append("\n\n")
                }
                sb.append(msg.text).append(TURN_END)
            } else {
                sb.append(TURN_MODEL).append(msg.text).append(TURN_END)
            }
        }
        sb.append(TURN_MODEL)
        
        val finalPrompt = sb.toString()
        Log.d(TAG, "Full Prompt being sent to model:\n$finalPrompt")
        return finalPrompt
    }

    /**
     * Build a simple Gemma-formatted prompt for the 2nd pass (after function execution).
     * Does NOT inject the tool-calling system prompt, so the model responds in natural language.
     */
    fun buildSecondPassPrompt(userQuestion: String, funcName: String, funcResult: String): String {
        val sb = StringBuilder()
        
        // Single user turn: give the model the data and ask it to answer naturally
        sb.append(TURN_USER)
        sb.append("Data: $funcResult\n\n")
        sb.append("Using ONLY the data above, answer this question: $userQuestion\n")
        sb.append("Do not make up any information not present in the data. Be concise.")
        sb.append(TURN_END)
        sb.append(TURN_MODEL)
        
        val finalPrompt = sb.toString()
        Log.d(TAG, "Second pass prompt:\n$finalPrompt")
        return finalPrompt
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
