package com.example.yespaybiz.ai

enum class Role {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val role: Role,
    val text: String,
    val isStreaming: Boolean = false
)
