package com.example.yespaybiz.ai

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Role {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val role: Role,
    val text: String,
    val timestamp: String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
    val isStreaming: Boolean = false,
    // Suggested follow-up queries shown as chips below an assistant reply.
    // Empty list = show no chips.
    val suggestedFollowUps: List<String> = emptyList()
)
