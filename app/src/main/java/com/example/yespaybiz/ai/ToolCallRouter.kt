package com.example.yespaybiz.ai

import java.util.Locale

object ToolCallRouter {

    sealed class Intent
    data class KnownIntent(val functionName: String, val arguments: Map<String, Any>) : Intent()
    data class LowConfidence(val likelyFunctionName: String, val arguments: Map<String, Any>) : Intent()
    object UnknownIntent : Intent()

    data class ResolvedToolCall(val functionName: String, val arguments: Map<String, Any>)

    // ── Classify (before LLM) ─────────────────────────────────────────────────────

    fun classify(userQuestion: String): Intent {
        val q = userQuestion.lowercase(Locale.ROOT).trim()

        // Help
        if (containsAny(q, listOf("help", "kya kar sakte", "what can you do", "kya karoge", "capabilities")))
            return KnownIntent("getHelp", emptyMap())

        // Navigation
        if (containsAny(q, listOf("open transaction", "go to transaction", "transaction screen", "transactions screen")))
            return KnownIntent("navigateToTransactions", emptyMap())
        if (containsAny(q, listOf("open qr", "show qr", "qr code", "scan qr")))
            return KnownIntent("navigateToQR", emptyMap())
        if (containsAny(q, listOf("open settlement", "go to settlement", "settlement screen")))
            return KnownIntent("navigateToSettlement", emptyMap())

        // Daily summary
        if (containsAny(q, listOf("summary", "hisaab", "aaj ka", "daily report", "overview",
                "kitna hua aaj", "how did i do", "daily", "aaj ka hisaab")))
            return KnownIntent("getDailySummary", emptyMap())

        // Comparison
        val cmpPeriods = extractComparisonPeriods(q)
        if (cmpPeriods != null)
            return KnownIntent("getComparison", mapOf("period1" to cmpPeriods.first, "period2" to cmpPeriods.second))

        // Dispute / refund status
        if (containsAny(q, listOf("dispute", "refund", "chargeback", "kab aayega refund",
                "refund status", "money refund", "paisa wapas", "complaint")))
            return KnownIntent("getDisputeStatus", emptyMap())

        // Hold deep-dive — must check before generic hold so specific ID matched first
        val txnId = extractTxnId(q)
        if (txnId != null && containsAny(q, listOf("explain", "why", "kyun", "kyu", "detail", "hold", "stuck")))
            return KnownIntent("explainHold", mapOf("txnId" to txnId))
        if (txnId != null)
            return KnownIntent("explainHold", mapOf("txnId" to txnId))

        // Hold list
        if (isHoldIntent(q))
            return KnownIntent("getHoldTransactions", emptyMap())

        // Settlement / payout
        if (isSettlementIntent(q))
            return KnownIntent("getSettlementStatus", emptyMap())

        // Failed transactions
        if (containsAny(q, listOf("failed", "fail", "unsuccessful", "decline", "declined",
                "nahi aaya", "reject", "rejected", "failure", "failed payment", "not received"))) {
            val dateRange = extractDateRange(q) ?: "today"
            return KnownIntent("getFailedTransactions", mapOf("dateRange" to dateRange))
        }

        // Weekly trend
        if (containsAny(q, listOf("weekly trend", "week trend", "weekly report", "daily breakdown",
                "week breakdown", "7 days", "last 7", "trend", "graph", "chart", "bar")))
            return KnownIntent("getWeeklyTrend", emptyMap())

        // Amount search
        val amount = extractAmount(q)
        if (amount != null)
            return KnownIntent("searchByAmount", mapOf("amount" to amount))

        // Collections
        if (isCollectionsIntent(q)) {
            val dateRange = extractDateRange(q) ?: "today"
            return KnownIntent("getCollections", mapOf("dateRange" to dateRange))
        }

        // Transactions
        if (isTransactionsIntent(q)) {
            val dateRange = extractDateRange(q) ?: "today"
            val limit     = extractLimit(q)
            val args: Map<String, Any> = if (limit != null)
                mapOf("dateRange" to dateRange, "limit" to limit)
            else mapOf("dateRange" to dateRange)
            return KnownIntent("getTransactions", args)
        }

        // Low confidence
        if (hasMerchantFlavour(q)) {
            val dateRange = extractDateRange(q)
            return if (dateRange != null)
                LowConfidence("getCollections", mapOf("dateRange" to dateRange))
            else
                LowConfidence("getSettlementStatus", emptyMap())
        }

        return UnknownIntent
    }

    // ── Guardrail (after LLM) ─────────────────────────────────────────────────────

    fun resolve(userQuestion: String, modelFunctionName: String, modelArguments: Map<String, Any>): ResolvedToolCall {
        val q = userQuestion.lowercase(Locale.ROOT)
        if (modelFunctionName.startsWith("navigate")) return ResolvedToolCall(modelFunctionName, modelArguments)

        val txnId = extractTxnId(q)
        val correctedName = when {
            txnId != null                   -> "explainHold"
            isHoldIntent(q)                 -> "getHoldTransactions"
            isSettlementIntent(q)           -> "getSettlementStatus"
            isTransactionsIntent(q)         -> "getTransactions"
            isCollectionsIntent(q)          -> "getCollections"
            else                            -> modelFunctionName
        }

        val dateRange = extractDateRange(q) ?: (modelArguments["dateRange"] as? String) ?: "today"
        val limit     = extractLimit(q) ?: (modelArguments["limit"] as? Number)?.toInt()

        val finalArgs: Map<String, Any> = when (correctedName) {
            "getCollections"        -> mapOf("dateRange" to dateRange)
            "getTransactions"       -> if (limit != null && limit > 0)
                mapOf("dateRange" to dateRange, "limit" to limit) else mapOf("dateRange" to dateRange)
            "getSettlementStatus"   -> emptyMap()
            "getHoldTransactions"   -> emptyMap()
            "explainHold"           -> if (txnId != null) mapOf("txnId" to txnId) else emptyMap()
            "getFailedTransactions" -> mapOf("dateRange" to dateRange)
            else                    -> modelArguments
        }
        return ResolvedToolCall(correctedName, finalArgs)
    }

    // ── Follow-up chips ───────────────────────────────────────────────────────────

    fun suggestedFollowUps(functionName: String, args: Map<String, Any>): List<String> {
        val dateRange = args["dateRange"] as? String ?: "today"
        return when (functionName) {
            "getDailySummary"       -> listOf("Show today's transactions", "Settlement status", "Any holds?")
            "getCollections"        -> when (dateRange) {
                "today"     -> listOf("Today's transactions", "Settlement status", "Weekly trend")
                "yesterday" -> listOf("Yesterday's transactions", "This week's collection", "Settlement status")
                else        -> listOf("Show transactions", "Settlement status", "Any holds?")
            }
            "getTransactions"       -> listOf("Total collection $dateRange", "Show failed payments", "Settlement status")
            "getSettlementStatus"   -> listOf("Today's collection", "Yesterday's collection", "Show hold transactions")
            "getHoldTransactions"   -> listOf("Today's collection", "Settlement status", "Dispute status")
            "getFailedTransactions" -> listOf("Show all transactions", "Today's collection", "Settlement status")
            "getComparison"         -> listOf("Daily summary", "Weekly trend", "Show transactions")
            "getWeeklyTrend"        -> listOf("Daily summary", "This week's collection", "Settlement status")
            "explainHold"           -> listOf("All hold transactions", "Dispute status", "Settlement status")
            "getDisputeStatus"      -> listOf("Show hold transactions", "Settlement status", "Today's collection")
            "searchByAmount"        -> listOf("Show all transactions", "Today's collection", "Settlement status")
            else                    -> emptyList()
        }
    }

    // ── Hinglish detection ────────────────────────────────────────────────────────

    fun isHinglish(q: String): Boolean {
        val hindiWords = listOf("kitna", "kitne", "kab", "kya", "aaj", "kal", "hafte",
            "mila", "mera", "paisa", "hisaab", "batao", "dikhao", "kyun", "kyu",
            "wapas", "aaya", "hua", "nahi", "theek", "bhai")
        val lq = q.lowercase(Locale.ROOT)
        return hindiWords.count { lq.contains(it) } >= 2
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private fun isCollectionsIntent(q: String) = containsAny(q, listOf(
        "collect", "collected", "collection", "collections", "sales", "revenue",
        "earn", "earned", "income", "kitna mila", "kitne mila", "kitna aaya",
        "kitne aaye", "how much did i", "how much have i", "total amount", "total collection"))

    private fun isTransactionsIntent(q: String) = containsAny(q, listOf(
        "transaction", "transactions", "txn", "txns", "payment", "payments",
        "transfer", "last ", "recent ", "show me")) || extractLimit(q) != null

    private fun isSettlementIntent(q: String) = containsAny(q, listOf(
        "settlement", "payout", "settled", "bank transfer", "kitna settled",
        "when will", "credited to bank", "bank credit", "kab aayega paisa",
        "paisa kab", "settle kab"))

    private fun isHoldIntent(q: String) = containsAny(q, listOf(
        "hold transaction", "on hold", "held", "hold amount", "transactions on hold",
        "hold txn", "held back", "stuck", "not credited", "money stuck", "why not settled",
        "kyu ruka", "kyun ruka", "paise ruke"))

    private fun hasMerchantFlavour(q: String) = containsAny(q, listOf(
        "status", "today", "yesterday", "this week", "how much", "how many",
        "balance", "amount", "doing today", "any issue", "summary", "report",
        "aaj", "kal", "kitna", "batao", "dikhao"))

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any { text.contains(it) }

    private fun extractDateRange(q: String): String? = when {
        q.contains("today")     || q.contains("aaj")    -> "today"
        q.contains("yesterday") || q.contains("kal")    -> "yesterday"
        q.contains("this week") || q.contains("week")   || q.contains("hafte") -> "this week"
        else -> null
    }

    private fun extractLimit(q: String): Int? {
        listOf(Regex("""(?:last|recent)\s+(\d{1,2})"""), Regex("""(\d{1,2})\s+(?:transactions?|txns?|payments?)"""))
            .forEach { pattern ->
                val v = pattern.find(q)?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (v != null && v > 0) return v
            }
        return null
    }

    private fun extractTxnId(q: String): String? =
        Regex("""txn\s*\d{4}""", RegexOption.IGNORE_CASE).find(q)?.value?.uppercase()?.replace(" ", "")

    private fun extractAmount(q: String): Double? {
        // Match ₹5200, Rs 5200, 5200 rupees, rs5200
        val patterns = listOf(
            Regex("""[₹rs\.]+\s*(\d[\d,]*)""", RegexOption.IGNORE_CASE),
            Regex("""(\d[\d,]*)\s*(?:rupees?|rs\.?)""", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val v = p.find(q)?.groupValues?.getOrNull(1)?.replace(",", "")?.toDoubleOrNull()
            if (v != null && v > 0) return v
        }
        return null
    }

    private fun extractComparisonPeriods(q: String): Pair<String, String>? {
        val hasVs      = q.contains(" vs ") || q.contains(" versus ") || q.contains(" compared to ")
        val hasCompare = containsAny(q, listOf("compare", "comparison", "better than", "more than yesterday",
            "aaj vs kal", "today vs yesterday"))
        if (!hasVs && !hasCompare) return null

        val p1 = when {
            q.contains("this week") -> "this week"
            q.contains("today") || q.contains("aaj") -> "today"
            q.contains("yesterday") -> "yesterday"
            else -> "today"
        }
        val p2 = when {
            q.contains("last week") -> "this week"  // approximate
            q.contains("yesterday") && p1 == "today" -> "yesterday"
            q.contains("today") && p1 != "today" -> "today"
            else -> "yesterday"
        }
        return if (p1 != p2) Pair(p1, p2) else null
    }
}
