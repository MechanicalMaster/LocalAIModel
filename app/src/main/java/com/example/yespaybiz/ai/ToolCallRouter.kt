package com.example.yespaybiz.ai

import java.util.Locale

/**
 * Two responsibilities:
 *
 * 1. [classify] — fast keyword pre-check before any LLM call.
 *    Returns KnownIntent (with function + args already resolved) or UnknownIntent.
 *    UnknownIntent means we skip the LLM entirely and return a canned refusal.
 *
 * 2. [resolve] — deterministic guardrail applied AFTER the LLM emits a tool call.
 *    Corrects any mismatch between what the model said and what the user actually meant.
 */
object ToolCallRouter {

    // ── Public types ─────────────────────────────────────────────────────────────

    sealed class Intent
    data class KnownIntent(val functionName: String, val arguments: Map<String, Any>) : Intent()
    object UnknownIntent : Intent()

    data class ResolvedToolCall(
        val functionName: String,
        val arguments: Map<String, Any>
    )

    // ── Step 1: classify before LLM ──────────────────────────────────────────────

    /**
     * Keyword-based intent classifier. Runs before any LLM call.
     *
     * - Returns [KnownIntent] with the resolved function and arguments if the query
     *   matches a known merchant domain (transactions, collections, settlement, navigation).
     * - Returns [UnknownIntent] for anything outside merchant business context.
     *   The caller should respond with a canned refusal — no LLM call needed.
     */
    fun classify(userQuestion: String): Intent {
        val q = userQuestion.lowercase(Locale.ROOT).trim()

        // Navigation intents — match first, most unambiguous
        if (containsAny(q, listOf("open transaction", "go to transaction", "show transaction screen",
                "transaction screen", "transactions screen")))
            return KnownIntent("navigateToTransactions", emptyMap())

        if (containsAny(q, listOf("open qr", "show qr", "qr code", "scan qr")))
            return KnownIntent("navigateToQR", emptyMap())

        if (containsAny(q, listOf("open settlement", "show settlement", "go to settlement",
                "settlement screen")))
            return KnownIntent("navigateToSettlement", emptyMap())

        // Hold transactions intent — check before settlement (both share "hold" keyword)
        if (isHoldIntent(q))
            return KnownIntent("getHoldTransactions", emptyMap())

        // Settlement / payout intent
        if (isSettlementIntent(q))
            return KnownIntent("getSettlementStatus", emptyMap())

        // Collections intent
        if (isCollectionsIntent(q)) {
            val dateRange = extractDateRange(q) ?: "today"
            return KnownIntent("getCollections", mapOf("dateRange" to dateRange))
        }

        // Transactions intent (also catches "last N transactions" etc.)
        if (isTransactionsIntent(q)) {
            val dateRange = extractDateRange(q) ?: "today"
            val limit = extractLimit(q)
            val args: Map<String, Any> = if (limit != null)
                mapOf("dateRange" to dateRange, "limit" to limit)
            else
                mapOf("dateRange" to dateRange)
            return KnownIntent("getTransactions", args)
        }

        // Nothing matched — outside merchant domain
        return UnknownIntent
    }

    // ── Step 2: guardrail after LLM ──────────────────────────────────────────────

    /**
     * Corrects the model's tool call output against the known user intent.
     * Called only when the LLM successfully emits a JSON tool call.
     */
    fun resolve(
        userQuestion: String,
        modelFunctionName: String,
        modelArguments: Map<String, Any>
    ): ResolvedToolCall {
        val q = userQuestion.lowercase(Locale.ROOT)

        if (modelFunctionName.startsWith("navigate")) {
            return ResolvedToolCall(modelFunctionName, modelArguments)
        }

        val correctedName = when {
            isHoldIntent(q)         -> "getHoldTransactions"
            isSettlementIntent(q)   -> "getSettlementStatus"
            isTransactionsIntent(q) -> "getTransactions"
            isCollectionsIntent(q)  -> "getCollections"
            else -> modelFunctionName
        }

        val dateRange = extractDateRange(q)
            ?: (modelArguments["dateRange"] as? String)
            ?: "today"
        val limit = extractLimit(q) ?: (modelArguments["limit"] as? Number)?.toInt()

        val finalArgs: Map<String, Any> = when (correctedName) {
            "getCollections"      -> mapOf("dateRange" to dateRange)
            "getTransactions"     -> if (limit != null && limit > 0)
                mapOf("dateRange" to dateRange, "limit" to limit)
            else
                mapOf("dateRange" to dateRange)
            "getSettlementStatus" -> emptyMap()
            "getHoldTransactions" -> emptyMap()
            else                  -> modelArguments
        }

        return ResolvedToolCall(correctedName, finalArgs)
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private fun isCollectionsIntent(q: String) = containsAny(
        q, listOf("collect", "collected", "collection", "collections",
            "sales", "revenue", "earn", "earned", "income",
            "kitna mila", "kitne mila", "kitna aaya", "kitne aaye")
    )

    private fun isTransactionsIntent(q: String) = containsAny(
        q, listOf("transaction", "transactions", "txn", "txns",
            "payment", "payments", "transfer", "transfers",
            "last ", "recent ")
    ) || extractLimit(q) != null

    private fun isHoldIntent(q: String) = containsAny(
        q, listOf("hold transaction", "on hold", "held", "hold amount",
            "transactions on hold", "hold txn", "hold txns", "held back")
    )

    private fun isSettlementIntent(q: String) = containsAny(
        q, listOf("settlement", "payout", "settled", "bank transfer",
            "kitna settled", "settle")
    )

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any { text.contains(it) }

    private fun extractDateRange(q: String): String? = when {
        q.contains("today") || q.contains("aaj") -> "today"
        q.contains("yesterday") || q.contains("kal") -> "yesterday"
        q.contains("this week") || q.contains("week") || q.contains("hafte") -> "this week"
        else -> null
    }

    private fun extractLimit(q: String): Int? {
        val patterns = listOf(
            Regex("""(?:last|recent)\s+(\d{1,2})"""),
            Regex("""(\d{1,2})\s+(?:transactions?|txns?|payments?)""")
        )
        for (pattern in patterns) {
            val value = pattern.find(q)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (value != null && value > 0) return value
        }
        return null
    }
}
