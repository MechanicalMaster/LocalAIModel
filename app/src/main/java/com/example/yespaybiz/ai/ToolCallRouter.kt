package com.example.yespaybiz.ai

import java.util.Locale

/**
 * Deterministic guardrail for tool routing.
 * If the model emits a mismatched tool call, this router corrects it from user intent.
 */
object ToolCallRouter {

    data class ResolvedToolCall(
        val functionName: String,
        val arguments: Map<String, Any>
    )

    fun resolve(
        userQuestion: String,
        modelFunctionName: String,
        modelArguments: Map<String, Any>
    ): ResolvedToolCall {
        val normalized = userQuestion.lowercase(Locale.ROOT)

        if (modelFunctionName.startsWith("navigate")) {
            return ResolvedToolCall(modelFunctionName, modelArguments)
        }

        val forcedFunctionName = when {
            isSettlementIntent(normalized) -> "getSettlementStatus"
            isTransactionsIntent(normalized) -> "getTransactions"
            isCollectionsIntent(normalized) -> "getCollections"
            else -> modelFunctionName
        }

        val dateRange = extractDateRange(normalized)
            ?: (modelArguments["dateRange"] as? String)
            ?: "today"
        val limit = extractLimit(normalized) ?: (modelArguments["limit"] as? Number)?.toInt()

        val finalArgs = when (forcedFunctionName) {
            "getCollections" -> mapOf("dateRange" to dateRange)
            "getTransactions" -> {
                if (limit != null && limit > 0) {
                    mapOf("dateRange" to dateRange, "limit" to limit)
                } else {
                    mapOf("dateRange" to dateRange)
                }
            }
            "getSettlementStatus" -> emptyMap()
            else -> modelArguments
        }

        return ResolvedToolCall(
            functionName = forcedFunctionName,
            arguments = finalArgs
        )
    }

    private fun isCollectionsIntent(q: String): Boolean {
        return containsAny(
            q,
            listOf("collect", "collected", "collection", "collections", "sales", "revenue")
        )
    }

    private fun isTransactionsIntent(q: String): Boolean {
        return containsAny(
            q,
            listOf("transaction", "transactions", "txn", "txns", "payment", "payments")
        ) || extractLimit(q) != null
    }

    private fun isSettlementIntent(q: String): Boolean {
        return containsAny(
            q,
            listOf("settlement", "payout", "settled", "bank transfer")
        )
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    private fun extractDateRange(q: String): String? {
        return when {
            q.contains("today") -> "today"
            q.contains("yesterday") -> "yesterday"
            q.contains("this week") || q.contains("week") -> "this week"
            else -> null
        }
    }

    private fun extractLimit(q: String): Int? {
        val patterns = listOf(
            Regex("""(?:last|recent)\s+(\d{1,2})"""),
            Regex("""(\d{1,2})\s+(?:transactions?|txns?)""")
        )
        for (pattern in patterns) {
            val match = pattern.find(q) ?: continue
            val value = match.groupValues.getOrNull(1)?.toIntOrNull() ?: continue
            if (value > 0) return value
        }
        return null
    }
}
