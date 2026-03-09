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
     * Append a user message and stream the assistant's response.
     */
    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        if (_isGenerating.value) return

        // Append user message to history
        val userMsg = ChatMessage(role = Role.USER, text = userText.trim())
        var currentList = _messages.value + userMsg
        _messages.value = currentList

        // Add placeholder assistant message that will be updated as tokens stream in
        var assistantMsg = ChatMessage(
            role = Role.ASSISTANT,
            text = "",
            isStreaming = true,
        )
        currentList = currentList + assistantMsg
        _messages.value = currentList

        _isGenerating.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = AIService.buildPrompt(currentList.dropLast(1))
                val accumulated = java.lang.StringBuilder()

                android.util.Log.d("ChatViewModel", "Calling AIService.sendMessage...")
                AIService.sendMessage(prompt) { partial ->
                    accumulated.append(partial)
                    
                    // Cleanup common markdown artifacts immediately before checking logic
                    val currentText = accumulated.toString().trim()
                    
                    // Hide output from UI if we detect it's building a JSON object or markdown block
                    val isHiddenJSON = currentText.startsWith("{") 
                                    || currentText.startsWith("```json") 
                                    || currentText.startsWith("```")
                    
                    _messages.value = currentList.map { msg ->
                        if (msg.id == assistantMsg.id) {
                            if (isHiddenJSON) {
                                msg.copy(text = "Thinking...", isStreaming = true)
                            } else {
                                msg.copy(text = currentText, isStreaming = true)
                            }
                        } else msg
                    }
                }

                android.util.Log.d("ChatViewModel", "AIService.sendMessage returned. Accumulated length=${accumulated.length}")
                android.util.Log.d("ChatViewModel", "Raw response: ${accumulated.toString().take(300)}")

                // Clean up the final text by stripping markdown JSON wrappers if the model hallucinated them
                var firstPassText = accumulated.toString().trim()
                if (firstPassText.startsWith("```json")) {
                    firstPassText = firstPassText.removePrefix("```json").removeSuffix("```").trim()
                } else if (firstPassText.startsWith("```")) {
                    firstPassText = firstPassText.removePrefix("```").removeSuffix("```").trim()
                }
                
                // Tool Call Detection & Execution
                if (firstPassText.startsWith("{") && firstPassText.endsWith("}")) {
                    try {
                        val element = com.google.gson.JsonParser.parseString(firstPassText).asJsonObject
                        if (element.has("type") && element.get("type").asString == "tool_call") {
                            val modelFuncName = element.get("name").asString
                            val argsElement = element.get("arguments")
                            val modelArgsMap: Map<String, Any> = if (argsElement != null && argsElement.isJsonObject) {
                                com.google.gson.Gson().fromJson<Map<String, Any>>(argsElement, object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type) ?: emptyMap()
                            } else {
                                emptyMap()
                            }
                            val resolvedCall = ToolCallRouter.resolve(
                                userQuestion = userText.trim(),
                                modelFunctionName = modelFuncName,
                                modelArguments = modelArgsMap
                            )
                            val funcName = resolvedCall.functionName
                            val argsMap = resolvedCall.arguments
                            if (modelFuncName != funcName) {
                                android.util.Log.w(
                                    "ChatViewModel",
                                    "Tool call corrected: model=$modelFuncName -> resolved=$funcName, user='${userText.trim()}'"
                                )
                            }

                            if (funcName.startsWith("navigate")) {
                                // UI Navigation Task
                                _messages.value = currentList.map { msg ->
                                    if (msg.id == assistantMsg.id) {
                                        msg.copy(text = "Navigating to ${funcName.removePrefix("navigateTo")}...", isStreaming = false)
                                    } else msg
                                }
                                _navigationEvent.emit(funcName)
                                _isGenerating.value = false
                                return@launch
                            }

                            // Data Query Task
                            _messages.value = currentList.map { msg ->
                                if (msg.id == assistantMsg.id) {
                                    msg.copy(text = "Fetching data via $funcName...", isStreaming = true)
                                } else msg
                            }

                            val funcResult = AppFunctions.executeFunction(funcName, argsMap)
                            android.util.Log.d("ChatViewModel", "Function $funcName returned: $funcResult")

                            // 2nd Pass: Build a clean prompt WITHOUT tool-calling instructions
                            val finalPrompt = AIService.buildSecondPassPrompt(
                                userQuestion = userText.trim(),
                                funcName = funcName,
                                funcResult = funcResult
                            )
                            
                            assistantMsg = ChatMessage(role = Role.ASSISTANT, text = "", isStreaming = true)
                            currentList = currentList.dropLast(1) + assistantMsg
                            _messages.value = currentList

                            val finalAccumulated = java.lang.StringBuilder()
                            
                            AIService.sendMessage(finalPrompt) { partial ->
                                finalAccumulated.append(partial)
                                _messages.value = currentList.map { msg ->
                                    if (msg.id == assistantMsg.id) {
                                        msg.copy(text = finalAccumulated.toString().trim(), isStreaming = true)
                                    } else msg
                                }
                            }

                            // Clean up the second pass response (strip backticks if model wraps them)
                            var secondPassText = finalAccumulated.toString().trim()
                            if (secondPassText.startsWith("```")) {
                                secondPassText = secondPassText.removePrefix("```json")
                                    .removePrefix("```").removeSuffix("```").trim()
                            }
                            android.util.Log.d("ChatViewModel", "Second pass final text: $secondPassText")

                            _messages.value = currentList.map { msg ->
                                if (msg.id == assistantMsg.id) {
                                    msg.copy(text = secondPassText, isStreaming = false)
                                } else msg
                            }
                            _isGenerating.value = false
                            return@launch
                        }
                    } catch (e: Exception) {
                        android.util.Log.d("ChatViewModel", "JSON parse failed, treating as normal text", e)
                    }
                }

                // Fallback Intent (Not JSON, output standard response)
                _messages.value = currentList.map { msg ->
                    if (msg.id == assistantMsg.id) {
                        msg.copy(
                            text = firstPassText,
                            isStreaming = false
                        )
                    } else msg
                }
            } catch (e: Exception) {
                // Replace placeholder with error message
                _messages.value = currentList.map { msg ->
                    if (msg.id == assistantMsg.id) {
                        msg.copy(
                            text = "Error: ${e.message ?: "Inference failed"}",
                            isStreaming = false
                        )
                    } else msg
                }
            } finally {
                _isGenerating.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        AIService.close()
    }
}
