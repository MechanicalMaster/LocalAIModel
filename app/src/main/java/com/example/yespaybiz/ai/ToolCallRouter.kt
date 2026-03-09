package com.example.yespaybiz.ai

import java.util.Locale

/**
 * Two responsibilities:
 *
 * 1. [classify] — fast keyword pre-check before any LLM call. Returns one of:
 *      KnownIntent    — high-confidence match; LLM still called for arg extraction
 *      LowConfidence  — weak signal; LLM called, but if it fails we ask for clarification
 *      UnknownIntent  — nothing matched; canned refusal, no LLM call
 *
 * 2. [resolve] — deterministic guardrail applied AFTER the LLM emits a tool call.
 *    Corrects any mismatch between what the model said and what the user meant.
 *
 * 3. [suggestedFollowUps] — returns contextual chip suggestions for a given function.
 */
object ToolCallRouter {

    // ── Public types ──────────────────────────────────────────────────────────────

    sealed class Intent
    data class KnownIntent(val functionName: String, val arguments: Map<String, Any>) : Intent()
    data class LowConfidence(val likelyFunctionName: String, val arguments: Map<String, Any>) : Intent()
    object UnknownIntent : Intent()

    data class ResolvedToolCall(
        val functionName: String,
        val arguments: Map<String, Any>
    )

    // ── Step 1: classify before LLM ──────────────────────────────────────────────

    fun classify(userQuestion: String): Intent {
        val q = userQuestion.lowercase(Locale.ROOT).trim()

        // ── High-confidence navigation ────────────────────────────────────────────
        if (containsAny(q, listOf("open transaction", "go to transaction",
                "transaction screen", "transactions screen")))
            return KnownIntent("navigateToTransactions", emptyMap())

        if (containsAny(q, listOf("open qr", "show qr", "qr code", "scan qr")))
            return KnownIntent("navigateToQR", emptyMap())

        if (containsAny(q, listOf("open settlement", "go to settlement", "settlement screen")))
            return KnownIntent("navigateToSettlement", emptyMap())

        // ── High-confidence data intents ──────────────────────────────────────────
        if (isHoldIntent(q))
            return KnownIntent("getHoldTransactions", emptyMap())

        if (isSettlementIntent(q))
            return KnownIntent("getSettlementStatus", emptyMap())

        if (isCollectionsIntent(q)) {
            val dateRange = extractDateRange(q) ?: "today"
            return KnownIntent("getCollections", mapOf("dateRange" to dateRange))
        }

        if (isTransactionsIntent(q)) {
            val dateRange = extractDateRange(q) ?: "today"
            val limit = extractLimit(q)
            val args: Map<String, Any> = if (limit != null)
                mapOf("dateRange" to dateRange, "limit" to limit)
            else
                mapOf("dateRange" to dateRange)
            return KnownIntent("getTransactions", args)
        }

        // ── Low-confidence: query has a merchant flavour but no clear keyword match ─
        // Examples: "what's my status?", "how am I doing today?", "any issues?"
        if (hasMerchantFlavour(q)) {
            // Best guess: if there's a date signal lean toward collections, else settlement
            val dateRange = extractDateRange(q)
            return if (dateRange != null)
                LowConfidence("getCollections", mapOf("dateRange" to dateRange))
            else
                LowConfidence("getSettlementStatus", emptyMap())
        }

        return UnknownIntent
    }

    // ── Step 2: guardrail after LLM ──────────────────────────────────────────────

    fun resolve(
        userQuestion: String,
        modelFunctionName: String,
        modelArguments: Map<String, Any>
    ): ResolvedToolCall {
        val q = userQuestion.lowercase(Locale.ROOT)

        if (modelFunctionName.startsWith("navigate"))
            return ResolvedToolCall(modelFunctionName, modelArguments)

        val correctedName = when {
            isHoldIntent(q)         -> "getHoldTransactions"
            isSettlementIntent(q)   -> "getSettlementStatus"
            isTransactionsIntent(q) -> "getTransactions"
            isCollectionsIntent(q)  -> "getCollections"
            else                    -> modelFunctionName
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

    // ── Step 3: follow-up chip suggestions ───────────────────────────────────────

    /**
     * Returns contextual follow-up query chips to show after an assistant response.
     * Chips are tailored to what was just answered so they feel like natural next steps.
     */
    fun suggestedFollowUps(functionName: String, args: Map<String, Any>): List<String> {
        val dateRange = args["dateRange"] as? String ?: "today"
        return when (functionName) {
            "getCollections" -> when (dateRange) {
                "today"     -> listOf("Show today's transactions", "Settlement status", "Any holds?")
                "yesterday" -> listOf("Show yesterday's transactions", "This week's collection", "Settlement status")
                "this week" -> listOf("Show this week's transactions", "Any holds?", "Settlement status")
                else        -> listOf("Show transactions", "Settlement status")
            }
            "getTransactions" -> listOf(
                "Total collection ${if (dateRange == "today") "today" else dateRange}",
                "Settlement status",
                "Any holds?"
            )
            "getSettlementStatus" -> listOf(
                "Today's collection",
                "Yesterday's collection",
                "Show hold transactions"
            )
            "getHoldTransactions" -> listOf(
                "Today's collection",
                "Settlement status",
                "Show today's transactions"
            )
            else -> emptyList()
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private fun isCollectionsIntent(q: String) = containsAny(
        q, listOf("collect", "collected", "collection", "collections",
            "sales", "revenue", "earn", "earned", "income",
            "kitna mila", "kitne mila", "kitna aaya", "kitne aaye",
            "how much did i", "how much have i", "total amount")
    )

    private fun isTransactionsIntent(q: String) = containsAny(
        q, listOf("transaction", "transactions", "txn", "txns",
            "payment", "payments", "transfer", "transfers",
            "last ", "recent ", "show me")
    ) || extractLimit(q) != null

    private fun isSettlementIntent(q: String) = containsAny(
        q, listOf("settlement", "payout", "settled", "bank transfer",
            "kitna settled", "when will", "credited to bank", "bank credit")
    )

    private fun isHoldIntent(q: String) = containsAny(
        q, listOf("hold transaction", "on hold", "held", "hold amount",
            "transactions on hold", "hold txn", "held back",
            "stuck", "not credited", "money stuck", "why not settled")
    )

    // Merchant-flavoured but ambiguous — low confidence zone
    private fun hasMerchantFlavour(q: String) = containsAny(
        q, listOf("status", "today", "yesterday", "this week",
            "how much", "how many", "balance", "amount",
            "doing today", "any issue", "summary", "report",
            "aaj", "kal", "kitna", "batao", "dikhao")
    )

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any { text.contains(it) }

    private fun extractDateRange(q: String): String? = when {
        q.contains("today") || q.contains("aaj")   -> "today"
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
