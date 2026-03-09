package com.example.yespaybiz.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Realistic dummy dataset: 40 transactions across the last 10 days.
 *
 * Business rules:
 *  - Each day has 3–5 transactions via UPI / CARD / NETBANKING
 *  - ~1 in 10 transactions is on HOLD (not settled for 30 days)
 *  - T+1 settlement for yesterday and older (excluding HOLD)
 *  - Today is PENDING
 *  - Settlement batch at 11:30 PM
 */
object AppFunctions {

    private val gson = Gson()

    // ── Data model ────────────────────────────────────────────────────────────────

    data class Transaction(
        val id: String,
        val date: LocalDate,
        val amount: Double,
        val status: String,           // SUCCESS | FAILED
        val settlementStatus: String, // SETTLED | PENDING | HOLD
        val method: String,           // UPI | CARD | NETBANKING
        val time: String,
        val failureReason: String? = null,
        val holdReason: String? = null,
        val holdReleaseDate: LocalDate? = null,
        val customerName: String? = null
    )

    // ── Mock dispute data ─────────────────────────────────────────────────────────

    data class Dispute(
        val txnId: String,
        val amount: Double,
        val filedDate: LocalDate,
        val status: String,       // OPEN | RESOLVED | PENDING_DOCS
        val resolution: String?,
        val refId: String
    )

    private val TODAY: LocalDate = LocalDate.now()
    private fun daysAgo(n: Long) = TODAY.minusDays(n)

    val ALL_TRANSACTIONS: List<Transaction> = listOf(

        // ── Day 0 (Today) — 4 txns, all PENDING ────────────────────────────────
        Transaction("TXN2040", TODAY,  1_250.00, "SUCCESS", "PENDING",  "UPI",        "09:15 AM", customerName = "Ramesh Kumar"),
        Transaction("TXN2041", TODAY,  3_400.00, "SUCCESS", "PENDING",  "CARD",       "11:02 AM", customerName = "Sunita Devi"),
        Transaction("TXN2042", TODAY,    450.00, "FAILED",  "PENDING",  "UPI",        "12:30 PM",
            failureReason = "Insufficient funds at payer's bank", customerName = "Mohan Lal"),
        Transaction("TXN2043", TODAY,  2_100.00, "SUCCESS", "PENDING",  "NETBANKING", "02:45 PM", customerName = "Priya Sharma"),

        // ── Day 1 (Yesterday) — 5 txns, SETTLED ────────────────────────────────
        Transaction("TXN2035", daysAgo(1),  1_800.00, "SUCCESS", "SETTLED",  "UPI",        "08:55 AM", customerName = "Anil Singh"),
        Transaction("TXN2036", daysAgo(1),  5_200.50, "SUCCESS", "SETTLED",  "CARD",       "10:30 AM", customerName = "Kavita Rao"),
        Transaction("TXN2037", daysAgo(1),    320.00, "SUCCESS", "SETTLED",  "UPI",        "01:15 PM", customerName = "Deepak Gupta"),
        Transaction("TXN2038", daysAgo(1),  2_750.00, "SUCCESS", "SETTLED",  "NETBANKING", "03:40 PM", customerName = "Meena Verma"),
        Transaction("TXN2039", daysAgo(1),    980.75, "FAILED",  "SETTLED",  "UPI",        "06:20 PM",
            failureReason = "UPI PIN attempts exceeded — transaction declined by payer's bank", customerName = "Rajesh Patel"),

        // ── Day 2 — HOLD: TXN2031 ───────────────────────────────────────────────
        Transaction("TXN2031", daysAgo(2), 15_000.00, "SUCCESS", "HOLD", "NETBANKING", "09:10 AM",
            holdReason = "High-value transaction under review",
            holdReleaseDate = daysAgo(2).plusDays(30), customerName = "Suresh Mehta"),
        Transaction("TXN2032", daysAgo(2),  1_100.00, "SUCCESS", "SETTLED",  "UPI",        "11:25 AM", customerName = "Pooja Nair"),
        Transaction("TXN2033", daysAgo(2),  3_600.00, "SUCCESS", "SETTLED",  "CARD",       "02:00 PM", customerName = "Vikram Joshi"),
        Transaction("TXN2034", daysAgo(2),    650.00, "SUCCESS", "SETTLED",  "UPI",        "05:30 PM", customerName = "Anita Yadav"),

        // ── Day 3 — all SETTLED ─────────────────────────────────────────────────
        Transaction("TXN2027", daysAgo(3),  2_200.00, "SUCCESS", "SETTLED",  "UPI",        "10:05 AM", customerName = "Ravi Kumar"),
        Transaction("TXN2028", daysAgo(3),  4_800.00, "SUCCESS", "SETTLED",  "CARD",       "12:45 PM", customerName = "Shalini Tiwari"),
        Transaction("TXN2029", daysAgo(3),    780.50, "SUCCESS", "SETTLED",  "UPI",        "03:10 PM", customerName = "Manoj Dubey"),
        Transaction("TXN2030", daysAgo(3),  1_350.00, "FAILED",  "SETTLED",  "NETBANKING", "07:00 PM",
            failureReason = "Netbanking session timed out — customer did not complete payment", customerName = "Geeta Mishra"),

        // ── Day 4 — HOLD: TXN2023 ───────────────────────────────────────────────
        Transaction("TXN2023", daysAgo(4), 12_500.00, "SUCCESS", "HOLD", "CARD", "08:30 AM",
            holdReason = "Chargeback risk — pending customer dispute",
            holdReleaseDate = daysAgo(4).plusDays(30), customerName = "Harish Reddy"),
        Transaction("TXN2024", daysAgo(4),  2_900.00, "SUCCESS", "SETTLED",  "UPI",        "11:00 AM", customerName = "Lakshmi Iyer"),
        Transaction("TXN2025", daysAgo(4),  1_450.00, "SUCCESS", "SETTLED",  "CARD",       "01:30 PM", customerName = "Santosh Pandey"),
        Transaction("TXN2026", daysAgo(4),    560.00, "SUCCESS", "SETTLED",  "UPI",        "04:15 PM", customerName = "Usha Devi"),

        // ── Day 5 — all SETTLED ─────────────────────────────────────────────────
        Transaction("TXN2019", daysAgo(5),  3_100.00, "SUCCESS", "SETTLED",  "UPI",        "09:40 AM", customerName = "Girish Soni"),
        Transaction("TXN2020", daysAgo(5),  6_750.00, "SUCCESS", "SETTLED",  "CARD",       "11:55 AM", customerName = "Rekha Chandra"),
        Transaction("TXN2021", daysAgo(5),    420.00, "FAILED",  "SETTLED",  "UPI",        "02:20 PM",
            failureReason = "Bank server error — transaction could not be processed", customerName = "Dinesh Bhatt"),
        Transaction("TXN2022", daysAgo(5),  2_050.00, "SUCCESS", "SETTLED",  "NETBANKING", "05:45 PM", customerName = "Sarla Jain"),

        // ── Day 6 — HOLD: TXN2016 ───────────────────────────────────────────────
        Transaction("TXN2016", daysAgo(6),  8_800.00, "SUCCESS", "HOLD", "UPI", "07:50 AM",
            holdReason = "Velocity flag — multiple high-value UPI payments",
            holdReleaseDate = daysAgo(6).plusDays(30), customerName = "Prakash Nair"),
        Transaction("TXN2017", daysAgo(6),  1_700.00, "SUCCESS", "SETTLED",  "CARD",       "10:20 AM", customerName = "Sudha Pillai"),
        Transaction("TXN2018", daysAgo(6),  3_300.00, "SUCCESS", "SETTLED",  "UPI",        "01:00 PM", customerName = "Ajay Bose"),
        Transaction("TXN2015", daysAgo(6),    890.00, "SUCCESS", "SETTLED",  "NETBANKING", "04:50 PM", customerName = "Nirmala Seth"),

        // ── Day 7 — all SETTLED ─────────────────────────────────────────────────
        Transaction("TXN2011", daysAgo(7),  2_500.00, "SUCCESS", "SETTLED",  "UPI",        "09:00 AM", customerName = "Bharat Shah"),
        Transaction("TXN2012", daysAgo(7),  4_100.00, "SUCCESS", "SETTLED",  "CARD",       "12:10 PM", customerName = "Kamla Devi"),
        Transaction("TXN2013", daysAgo(7),    670.00, "FAILED",  "SETTLED",  "UPI",        "02:35 PM",
            failureReason = "Payer's account has daily UPI limit exceeded", customerName = "Sanjay Goyal"),
        Transaction("TXN2014", daysAgo(7),  1_950.00, "SUCCESS", "SETTLED",  "NETBANKING", "06:00 PM", customerName = "Pushpa Agarwal"),

        // ── Day 8 — HOLD: TXN2008 ───────────────────────────────────────────────
        Transaction("TXN2008", daysAgo(8), 18_000.00, "SUCCESS", "HOLD", "NETBANKING", "08:15 AM",
            holdReason = "KYC mismatch — beneficiary details under verification",
            holdReleaseDate = daysAgo(8).plusDays(30), customerName = "Vinod Kapoor"),
        Transaction("TXN2009", daysAgo(8),  2_300.00, "SUCCESS", "SETTLED",  "UPI",        "11:30 AM", customerName = "Radha Menon"),
        Transaction("TXN2010", daysAgo(8),  5_600.00, "SUCCESS", "SETTLED",  "CARD",       "02:00 PM", customerName = "Ashok Trivedi"),
        Transaction("TXN2007", daysAgo(8),    730.00, "SUCCESS", "SETTLED",  "UPI",        "05:15 PM", customerName = "Savitri Das"),

        // ── Day 9 — all SETTLED ─────────────────────────────────────────────────
        Transaction("TXN2003", daysAgo(9),  1_600.00, "SUCCESS", "SETTLED",  "UPI",        "09:25 AM", customerName = "Hemant Thakur"),
        Transaction("TXN2004", daysAgo(9),  3_850.00, "SUCCESS", "SETTLED",  "CARD",       "12:00 PM", customerName = "Malti Singh"),
        Transaction("TXN2005", daysAgo(9),    510.00, "FAILED",  "SETTLED",  "UPI",        "03:30 PM",
            failureReason = "Wrong UPI PIN entered — transaction declined", customerName = "Rohan Saxena"),
        Transaction("TXN2006", daysAgo(9),  2_200.00, "SUCCESS", "SETTLED",  "NETBANKING", "06:45 PM", customerName = "Shobha Kulkarni")
    )

    val ALL_DISPUTES: List<Dispute> = listOf(
        Dispute("TXN2031", 15_000.00, daysAgo(2), "OPEN",
            resolution = null,
            refId = "DISP-2031-${TODAY.year}"),
        Dispute("TXN2023", 12_500.00, daysAgo(4), "PENDING_DOCS",
            resolution = null,
            refId = "DISP-2023-${TODAY.year}"),
        Dispute("TXN2008", 18_000.00, daysAgo(8), "OPEN",
            resolution = null,
            refId = "DISP-2008-${TODAY.year}")
    )

    // ── Derived helpers ───────────────────────────────────────────────────────────

    private fun txnsForDay(date: LocalDate) = ALL_TRANSACTIONS.filter { it.date == date }

    private fun txnsForRange(dateRange: String): List<Transaction> = when (dateRange.lowercase().trim()) {
        "today"     -> txnsForDay(TODAY)
        "yesterday" -> txnsForDay(daysAgo(1))
        "this week" -> ALL_TRANSACTIONS.filter { it.date >= TODAY.minusDays(6) }
        else        -> txnsForDay(TODAY)
    }

    // ── executeFunction ───────────────────────────────────────────────────────────

    fun executeFunction(functionName: String, args: Map<String, Any>): String {
        return try {
            when (functionName) {
                "getCollections"        -> getCollections(args["dateRange"] as? String ?: "today")
                "getTransactions"       -> getTransactions(
                    dateRange = args["dateRange"] as? String ?: "today",
                    limit     = (args["limit"] as? Number)?.toInt()
                )
                "getSettlementStatus"   -> getSettlementStatus()
                "getHoldTransactions"   -> getHoldTransactions()
                "getDailySummary"       -> getDailySummary()
                "getComparison"         -> getComparison(
                    period1 = args["period1"] as? String ?: "today",
                    period2 = args["period2"] as? String ?: "yesterday"
                )
                "getFailedTransactions" -> getFailedTransactions(args["dateRange"] as? String ?: "today")
                "getWeeklyTrend"        -> getWeeklyTrend()
                "explainHold"           -> explainHold(args["txnId"] as? String ?: "")
                "getDisputeStatus"      -> getDisputeStatus()
                "searchByAmount"        -> searchByAmount((args["amount"] as? Number)?.toDouble() ?: 0.0)
                "getHelp"               -> getHelp()
                else -> """{"error":"Unknown function: $functionName"}"""
            }
        } catch (e: Exception) {
            """{"error":"Failed to execute $functionName: ${e.message}"}"""
        }
    }

    // ── formatAnswer ──────────────────────────────────────────────────────────────

    fun formatAnswer(functionName: String, jsonResult: String, hinglish: Boolean = false): String {
        return try {
            val obj = JsonParser.parseString(jsonResult).asJsonObject
            val answer = when (functionName) {
                "getCollections"        -> formatCollections(obj)
                "getTransactions"       -> formatTransactions(obj)
                "getSettlementStatus"   -> formatSettlement(obj)
                "getHoldTransactions"   -> formatHoldTransactions(obj)
                "getDailySummary"       -> formatDailySummary(obj)
                "getComparison"         -> formatComparison(obj)
                "getFailedTransactions" -> formatFailedTransactions(obj)
                "getWeeklyTrend"        -> formatWeeklyTrend(obj)
                "explainHold"           -> formatExplainHold(obj)
                "getDisputeStatus"      -> formatDisputeStatus(obj)
                "searchByAmount"        -> formatSearchByAmount(obj)
                "getHelp"               -> formatHelp()
                else -> jsonResult
            }
            if (hinglish) toHinglish(functionName, answer, obj) else answer
        } catch (e: Exception) {
            jsonResult
        }
    }

    // ── Data functions ────────────────────────────────────────────────────────────

    private fun getCollections(dateRange: String): String {
        val txns    = txnsForRange(dateRange).filter { it.status == "SUCCESS" }
        val total   = txns.sumOf { it.amount }
        val count   = txns.size
        val held    = txns.filter { it.settlementStatus == "HOLD" }.sumOf { it.amount }
        val settled = txns.filter { it.settlementStatus == "SETTLED" }.sumOf { it.amount }
        val pending = txns.filter { it.settlementStatus == "PENDING" }.sumOf { it.amount }
        return gson.toJson(mapOf(
            "dateRange"           to dateRange,
            "totalCollectionsINR" to total,
            "transactionCount"    to count,
            "settledAmountINR"    to settled,
            "pendingAmountINR"    to pending,
            "holdAmountINR"       to held
        ))
    }

    private fun getTransactions(dateRange: String, limit: Int?): String {
        var txns = txnsForRange(dateRange).sortedByDescending { it.date.toString() + it.time }
        if (limit != null && limit > 0) txns = txns.take(limit)
        return gson.toJson(mapOf(
            "dateRange"    to dateRange,
            "total"        to txns.size,
            "transactions" to txns.map { it.toMap() }
        ))
    }

    private fun getSettlementStatus(): String {
        val pendingTxns       = txnsForDay(TODAY).filter { it.status == "SUCCESS" }
        val pendingAmount     = pendingTxns.sumOf { it.amount }
        val lastSettledTxns   = txnsForDay(daysAgo(1)).filter { it.status == "SUCCESS" && it.settlementStatus == "SETTLED" }
        val lastSettledAmount = lastSettledTxns.sumOf { it.amount }
        return gson.toJson(mapOf(
            "status"                  to "PENDING",
            "pendingAmountINR"        to pendingAmount,
            "pendingTransactionCount" to pendingTxns.size,
            "estimatedSettlementTime" to "Today, 11:30 PM",
            "lastSettledAmountINR"    to lastSettledAmount,
            "lastSettledDate"         to daysAgo(1).format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
            "bankAccountEnd"          to "4509"
        ))
    }

    private fun getHoldTransactions(): String {
        val holdTxns  = ALL_TRANSACTIONS.filter { it.settlementStatus == "HOLD" }
        val totalHeld = holdTxns.sumOf { it.amount }
        val dateFmt   = DateTimeFormatter.ofPattern("dd MMM yyyy")
        return gson.toJson(mapOf(
            "holdCount"    to holdTxns.size,
            "totalHeldINR" to totalHeld,
            "transactions" to holdTxns.map { txn ->
                mapOf(
                    "id"              to txn.id,
                    "date"            to txn.date.format(dateFmt),
                    "amount"          to txn.amount,
                    "method"          to txn.method,
                    "holdReason"      to (txn.holdReason ?: "Under review"),
                    "expectedRelease" to (txn.holdReleaseDate?.format(dateFmt) ?: "TBD")
                )
            }
        ))
    }

    private fun getDailySummary(): String {
        val todayTxns   = txnsForDay(TODAY)
        val success     = todayTxns.filter { it.status == "SUCCESS" }
        val failed      = todayTxns.filter { it.status == "FAILED" }
        val holds       = todayTxns.filter { it.settlementStatus == "HOLD" }
        val collected   = success.sumOf { it.amount }

        val yestSettled = txnsForDay(daysAgo(1))
            .filter { it.status == "SUCCESS" && it.settlementStatus == "SETTLED" }
            .sumOf { it.amount }

        val holdCount   = ALL_TRANSACTIONS.filter { it.settlementStatus == "HOLD" }.size
        val holdTotal   = ALL_TRANSACTIONS.filter { it.settlementStatus == "HOLD" }.sumOf { it.amount }

        val upiAmt      = success.filter { it.method == "UPI" }.sumOf { it.amount }
        val cardAmt     = success.filter { it.method == "CARD" }.sumOf { it.amount }
        val nbAmt       = success.filter { it.method == "NETBANKING" }.sumOf { it.amount }
        val topMethod   = mapOf("UPI" to upiAmt, "CARD" to cardAmt, "NETBANKING" to nbAmt)
            .maxByOrNull { it.value }?.key ?: "UPI"

        return gson.toJson(mapOf(
            "date"                    to TODAY.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
            "collectedINR"            to collected,
            "successCount"            to success.size,
            "failedCount"             to failed.size,
            "holdsTodayCount"         to holds.size,
            "allHoldsCount"           to holdCount,
            "allHoldsTotalINR"        to holdTotal,
            "pendingSettlementINR"    to collected,
            "settlementETA"           to "Tonight, 11:30 PM",
            "yesterdaySettledINR"     to yestSettled,
            "topPaymentMethod"        to topMethod
        ))
    }

    private fun getComparison(period1: String, period2: String): String {
        fun periodTotal(p: String) = txnsForRange(p).filter { it.status == "SUCCESS" }.sumOf { it.amount }
        fun periodCount(p: String) = txnsForRange(p).filter { it.status == "SUCCESS" }.size

        val amt1   = periodTotal(period1)
        val amt2   = periodTotal(period2)
        val cnt1   = periodCount(period1)
        val cnt2   = periodCount(period2)
        val diff   = amt1 - amt2
        val pct    = if (amt2 > 0) ((diff / amt2) * 100).roundToInt() else 0

        return gson.toJson(mapOf(
            "period1"       to period1,
            "period2"       to period2,
            "amount1"       to amt1,
            "amount2"       to amt2,
            "count1"        to cnt1,
            "count2"        to cnt2,
            "differenceINR" to diff,
            "changePercent" to pct,
            "period1IsOngoing" to (period1 == "today")
        ))
    }

    private fun getFailedTransactions(dateRange: String): String {
        val failed = txnsForRange(dateRange).filter { it.status == "FAILED" }
        return gson.toJson(mapOf(
            "dateRange"   to dateRange,
            "failedCount" to failed.size,
            "totalLostINR" to failed.sumOf { it.amount },
            "transactions" to failed.map { txn ->
                mapOf(
                    "id"            to txn.id,
                    "amount"        to txn.amount,
                    "method"        to txn.method,
                    "time"          to txn.time,
                    "failureReason" to (txn.failureReason ?: "Transaction declined"),
                    "customerName"  to (txn.customerName ?: "Unknown"),
                    "suggestion"    to failureSuggestion(txn.failureReason)
                )
            }
        ))
    }

    private fun getWeeklyTrend(): String {
        val dateFmt = DateTimeFormatter.ofPattern("EEE dd")
        val days = (6 downTo 0).map { i ->
            val date   = daysAgo(i.toLong())
            val txns   = txnsForDay(date).filter { it.status == "SUCCESS" }
            val amount = txns.sumOf { it.amount }
            val count  = txns.size
            mapOf(
                "date"      to date.format(dateFmt),
                "dayLabel"  to if (i == 0) "Today" else if (i == 1) "Yesterday" else date.format(DateTimeFormatter.ofPattern("EEE")),
                "amountINR" to amount,
                "count"     to count,
                "isToday"   to (i == 0)
            )
        }
        val maxAmt = days.maxOf { (it["amountINR"] as Double) }
        return gson.toJson(mapOf("days" to days, "maxAmountINR" to maxAmt))
    }

    private fun explainHold(txnId: String): String {
        val txn = ALL_TRANSACTIONS.firstOrNull {
            it.id.equals(txnId, ignoreCase = true) && it.settlementStatus == "HOLD"
        }
        if (txn == null) {
            // Try to find any hold if no specific ID matched
            val anyHold = ALL_TRANSACTIONS.firstOrNull { it.settlementStatus == "HOLD" }
                ?: return """{"error":"No hold transactions found"}"""
            return buildHoldExplanation(anyHold)
        }
        return buildHoldExplanation(txn)
    }

    private fun buildHoldExplanation(txn: Transaction): String {
        val dateFmt     = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val actionSteps = holdActionSteps(txn.holdReason)
        return gson.toJson(mapOf(
            "id"              to txn.id,
            "date"            to txn.date.format(dateFmt),
            "amount"          to txn.amount,
            "method"          to txn.method,
            "holdReason"      to (txn.holdReason ?: "Under review"),
            "expectedRelease" to (txn.holdReleaseDate?.format(dateFmt) ?: "TBD"),
            "actionRequired"  to actionSteps.first,
            "steps"           to actionSteps.second,
            "supportRef"      to "HOLD-${txn.id}-${TODAY.year}"
        ))
    }

    private fun getDisputeStatus(): String {
        val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
        return gson.toJson(mapOf(
            "openCount"  to ALL_DISPUTES.count { it.status == "OPEN" },
            "totalINR"   to ALL_DISPUTES.sumOf { it.amount },
            "disputes"   to ALL_DISPUTES.map { d ->
                mapOf(
                    "txnId"           to d.txnId,
                    "amount"          to d.amount,
                    "filedDate"       to d.filedDate.format(dateFmt),
                    "status"          to d.status,
                    "statusLabel"     to disputeStatusLabel(d.status),
                    "resolution"      to (d.resolution ?: "Pending"),
                    "refId"           to d.refId,
                    "expectedDays"    to "15–20 business days"
                )
            }
        ))
    }

    private fun searchByAmount(amount: Double): String {
        if (amount <= 0) return """{"error":"Invalid amount"}"""
        val tolerance = amount * 0.05  // ±5%
        val matches = ALL_TRANSACTIONS
            .filter { abs(it.amount - amount) <= tolerance }
            .sortedByDescending { it.date.toString() + it.time }
        val dateFmt = DateTimeFormatter.ofPattern("dd MMM")
        return gson.toJson(mapOf(
            "searchAmount" to amount,
            "matchCount"   to matches.size,
            "transactions" to matches.map { txn ->
                mapOf(
                    "id"               to txn.id,
                    "date"             to txn.date.format(dateFmt),
                    "amount"           to txn.amount,
                    "status"           to txn.status,
                    "settlementStatus" to txn.settlementStatus,
                    "method"           to txn.method,
                    "time"             to txn.time,
                    "customerName"     to (txn.customerName ?: "Unknown")
                )
            }
        ))
    }

    private fun getHelp(): String = gson.toJson(mapOf("help" to true))

    // ── Formatters ────────────────────────────────────────────────────────────────

    private fun formatCollections(obj: JsonObject): String {
        val dateRange = obj.get("dateRange")?.asString ?: "today"
        val total     = obj.get("totalCollectionsINR")?.asDouble ?: 0.0
        val count     = obj.get("transactionCount")?.asInt ?: 0
        val settled   = obj.get("settledAmountINR")?.asDouble ?: 0.0
        val pending   = obj.get("pendingAmountINR")?.asDouble ?: 0.0
        val held      = obj.get("holdAmountINR")?.asDouble ?: 0.0
        val period    = periodLabel(dateRange)
        if (total == 0.0) return "$period's collection: ₹0 — no transactions found."
        val sb = StringBuilder("$period's collection: ${fmtInr(total)} ($count transactions)\n")
        if (settled > 0) sb.append("  • Settled:  ${fmtInr(settled)}\n")
        if (pending > 0) sb.append("  • Pending:  ${fmtInr(pending)}\n")
        if (held    > 0) sb.append("  • On hold:  ${fmtInr(held)}")
        return sb.toString().trimEnd()
    }

    private fun formatTransactions(obj: JsonObject): String {
        val dateRange = obj.get("dateRange")?.asString ?: "today"
        val txns      = obj.getAsJsonArray("transactions") ?: return "No transactions found."
        val period    = periodLabel(dateRange)
        if (txns.size() == 0) return "$period: No transactions found."
        val sb = StringBuilder("$period's transactions (${txns.size()}):\n")
        txns.forEach { el ->
            val t         = el.asJsonObject
            val id        = t.get("id")?.asString ?: "-"
            val amt       = t.get("amount")?.asDouble ?: 0.0
            val status    = t.get("status")?.asString ?: "-"
            val sStatus   = t.get("settlementStatus")?.asString ?: ""
            val method    = t.get("method")?.asString ?: "-"
            val time      = t.get("time")?.asString ?: ""
            val icon      = if (status == "SUCCESS") "✓" else "✗"
            val holdTag   = if (sStatus == "HOLD") " [HOLD]" else ""
            sb.append("• $icon $id — ${fmtInr(amt)} ($method, $time)$holdTag\n")
        }
        return sb.toString().trimEnd()
    }

    private fun formatSettlement(obj: JsonObject): String {
        val pending      = obj.get("pendingAmountINR")?.asDouble ?: 0.0
        val pendingCount = obj.get("pendingTransactionCount")?.asInt ?: 0
        val eta          = obj.get("estimatedSettlementTime")?.asString ?: "N/A"
        val lastAmt      = obj.get("lastSettledAmountINR")?.asDouble ?: 0.0
        val lastDate     = obj.get("lastSettledDate")?.asString ?: "-"
        val bank         = obj.get("bankAccountEnd")?.asString ?: "****"
        return "Settlement to account ****$bank:\n" +
               "  • Pending:       ${fmtInr(pending)} ($pendingCount txns) — due $eta\n" +
               "  • Last settled:  ${fmtInr(lastAmt)} on $lastDate"
    }

    private fun formatHoldTransactions(obj: JsonObject): String {
        val count     = obj.get("holdCount")?.asInt ?: 0
        val totalHeld = obj.get("totalHeldINR")?.asDouble ?: 0.0
        val txns      = obj.getAsJsonArray("transactions") ?: return "No hold transactions."
        if (count == 0) return "No transactions are currently on hold."
        val sb = StringBuilder("$count transaction(s) on hold — ${fmtInr(totalHeld)} total:\n")
        txns.forEach { el ->
            val t       = el.asJsonObject
            val id      = t.get("id")?.asString ?: "-"
            val date    = t.get("date")?.asString ?: "-"
            val amt     = t.get("amount")?.asDouble ?: 0.0
            val method  = t.get("method")?.asString ?: "-"
            val reason  = t.get("holdReason")?.asString ?: "-"
            val release = t.get("expectedRelease")?.asString ?: "TBD"
            sb.append("• $id — ${fmtInr(amt)} ($method, $date)\n")
            sb.append("  Reason: $reason\n")
            sb.append("  Expected release: $release\n")
        }
        return sb.toString().trimEnd()
    }

    private fun formatDailySummary(obj: JsonObject): String {
        val date        = obj.get("date")?.asString ?: "-"
        val collected   = obj.get("collectedINR")?.asDouble ?: 0.0
        val sucCount    = obj.get("successCount")?.asInt ?: 0
        val failCount   = obj.get("failedCount")?.asInt ?: 0
        val holdCount   = obj.get("allHoldsCount")?.asInt ?: 0
        val holdTotal   = obj.get("allHoldsTotalINR")?.asDouble ?: 0.0
        val settlETA    = obj.get("settlementETA")?.asString ?: "Tonight, 11:30 PM"
        val yestSettled = obj.get("yesterdaySettledINR")?.asDouble ?: 0.0
        val topMethod   = obj.get("topPaymentMethod")?.asString ?: "UPI"

        val sb = StringBuilder("Today's summary ($date):\n")
        sb.append("  • Collected:    ${fmtInr(collected)} ($sucCount transactions)\n")
        if (failCount > 0) sb.append("  • Failed:       $failCount transaction(s)\n")
        sb.append("  • Settlement:   ${fmtInr(collected)} due $settlETA\n")
        if (holdCount > 0) sb.append("  • On hold:      $holdCount txn(s) — ${fmtInr(holdTotal)} total\n")
        sb.append("  • Top method:   $topMethod\n")
        if (yestSettled > 0) sb.append("\nYesterday settled: ${fmtInr(yestSettled)} → account ****4509")
        return sb.toString().trimEnd()
    }

    private fun formatComparison(obj: JsonObject): String {
        val p1      = obj.get("period1")?.asString ?: "today"
        val p2      = obj.get("period2")?.asString ?: "yesterday"
        val amt1    = obj.get("amount1")?.asDouble ?: 0.0
        val amt2    = obj.get("amount2")?.asDouble ?: 0.0
        val cnt1    = obj.get("count1")?.asInt ?: 0
        val cnt2    = obj.get("count2")?.asInt ?: 0
        val diff    = obj.get("differenceINR")?.asDouble ?: 0.0
        val pct     = obj.get("changePercent")?.asInt ?: 0
        val ongoing = obj.get("period1IsOngoing")?.asBoolean ?: false

        val arrow   = when { pct > 0 -> "↑" ; pct < 0 -> "↓" ; else -> "→" }
        val sign    = if (diff >= 0) "+" else ""
        val l1      = periodLabel(p1)
        val l2      = periodLabel(p2)

        val sb = StringBuilder("$l1 vs $l2:\n")
        sb.append("  • $l1:  ${fmtInr(amt1)} ($cnt1 txns)  $arrow ${kotlin.math.abs(pct)}%\n")
        sb.append("  • $l2: ${fmtInr(amt2)} ($cnt2 txns)\n")
        sb.append("\n${sign}${fmtInr(diff)} compared to $l2")
        if (ongoing) sb.append("\n(Today is still ongoing)")
        return sb.toString()
    }

    private fun formatFailedTransactions(obj: JsonObject): String {
        val dateRange = obj.get("dateRange")?.asString ?: "today"
        val count     = obj.get("failedCount")?.asInt ?: 0
        val lost      = obj.get("totalLostINR")?.asDouble ?: 0.0
        val txns      = obj.getAsJsonArray("transactions") ?: return "No failed transactions found."
        val period    = periodLabel(dateRange)

        if (count == 0) return "Good news! No failed transactions $period."

        val sb = StringBuilder("$count failed transaction(s) $period — ${fmtInr(lost)} not collected:\n\n")
        txns.forEach { el ->
            val t          = el.asJsonObject
            val id         = t.get("id")?.asString ?: "-"
            val amt        = t.get("amount")?.asDouble ?: 0.0
            val method     = t.get("method")?.asString ?: "-"
            val time       = t.get("time")?.asString ?: ""
            val customer   = t.get("customerName")?.asString ?: "Unknown"
            val reason     = t.get("failureReason")?.asString ?: "Transaction declined"
            val suggestion = t.get("suggestion")?.asString ?: ""
            sb.append("• $id — ${fmtInr(amt)} ($method, $time)\n")
            sb.append("  Customer: $customer\n")
            sb.append("  Reason: $reason\n")
            if (suggestion.isNotEmpty()) sb.append("  Tip: $suggestion\n")
            sb.append("\n")
        }
        return sb.toString().trimEnd()
    }

    private fun formatWeeklyTrend(obj: JsonObject): String {
        val days   = obj.getAsJsonArray("days") ?: return "No data available."
        val maxAmt = obj.get("maxAmountINR")?.asDouble ?: 1.0
        val barMax = 12  // characters wide

        val sb = StringBuilder("Weekly collection trend:\n\n")
        days.forEach { el ->
            val d        = el.asJsonObject
            val label    = d.get("dayLabel")?.asString ?: "-"
            val amt      = d.get("amountINR")?.asDouble ?: 0.0
            val count    = d.get("count")?.asInt ?: 0
            val isToday  = d.get("isToday")?.asBoolean ?: false
            val barLen   = if (maxAmt > 0) ((amt / maxAmt) * barMax).roundToInt() else 0
            val bar      = "█".repeat(barLen) + if (isToday) " ←" else ""
            val amtStr   = fmtInr(amt).padStart(11)
            sb.append("  ${label.padEnd(9)} $amtStr  $bar\n")
            if (count == 0) sb.append("  ${" ".repeat(9)} (no transactions)\n")
        }
        return sb.toString().trimEnd()
    }

    private fun formatExplainHold(obj: JsonObject): String {
        val id      = obj.get("id")?.asString ?: "-"
        val date    = obj.get("date")?.asString ?: "-"
        val amt     = obj.get("amount")?.asDouble ?: 0.0
        val method  = obj.get("method")?.asString ?: "-"
        val reason  = obj.get("holdReason")?.asString ?: "Under review"
        val release = obj.get("expectedRelease")?.asString ?: "TBD"
        val actReq  = obj.get("actionRequired")?.asBoolean ?: false
        val ref     = obj.get("supportRef")?.asString ?: "-"

        val stepsArr = obj.getAsJsonArray("steps")
        val steps = buildString {
            stepsArr?.forEachIndexed { i, el -> append("  ${i + 1}. ${el.asString}\n") }
        }.trimEnd()

        val sb = StringBuilder("$id — ${fmtInr(amt)} ($method, $date)\n")
        sb.append("Status: ON HOLD\n\n")
        sb.append("Reason: $reason\n")
        sb.append("Expected release: $release\n\n")
        sb.append(if (actReq) "Action required from you:\n" else "No action needed from your side:\n")
        sb.append("$steps\n\n")
        sb.append("Support reference: $ref")
        return sb.toString()
    }

    private fun formatDisputeStatus(obj: JsonObject): String {
        val openCount = obj.get("openCount")?.asInt ?: 0
        val totalINR  = obj.get("totalINR")?.asDouble ?: 0.0
        val disputes  = obj.getAsJsonArray("disputes") ?: return "No disputes found."

        if (disputes.size() == 0) return "No open disputes. Your account is clear."

        val sb = StringBuilder("$openCount open dispute(s) — ${fmtInr(totalINR)} under review:\n\n")
        disputes.forEach { el ->
            val d      = el.asJsonObject
            val txnId  = d.get("txnId")?.asString ?: "-"
            val amt    = d.get("amount")?.asDouble ?: 0.0
            val filed  = d.get("filedDate")?.asString ?: "-"
            val label  = d.get("statusLabel")?.asString ?: "-"
            val ref    = d.get("refId")?.asString ?: "-"
            val eta    = d.get("expectedDays")?.asString ?: "15–20 business days"
            sb.append("• $txnId — ${fmtInr(amt)} (filed $filed)\n")
            sb.append("  Status: $label\n")
            sb.append("  Ref: $ref\n")
            sb.append("  Resolution expected: $eta\n\n")
        }
        sb.append("Contact YES Bank support for updates: 1800-1080")
        return sb.toString().trimEnd()
    }

    private fun formatSearchByAmount(obj: JsonObject): String {
        val searchAmt = obj.get("searchAmount")?.asDouble ?: 0.0
        val count     = obj.get("matchCount")?.asInt ?: 0
        val txns      = obj.getAsJsonArray("transactions") ?: return "No matches found."
        if (count == 0) return "No transactions found near ${fmtInr(searchAmt)}."
        val sb = StringBuilder("$count match(es) near ${fmtInr(searchAmt)}:\n")
        txns.forEach { el ->
            val t        = el.asJsonObject
            val id       = t.get("id")?.asString ?: "-"
            val amt      = t.get("amount")?.asDouble ?: 0.0
            val status   = t.get("status")?.asString ?: "-"
            val sStatus  = t.get("settlementStatus")?.asString ?: "-"
            val method   = t.get("method")?.asString ?: "-"
            val date     = t.get("date")?.asString ?: "-"
            val time     = t.get("time")?.asString ?: ""
            val customer = t.get("customerName")?.asString ?: "Unknown"
            val icon     = if (status == "SUCCESS") "✓" else "✗"
            sb.append("• $icon $id — ${fmtInr(amt)}\n")
            sb.append("  $customer · $method · $date $time\n")
            sb.append("  Settlement: $sStatus\n")
        }
        return sb.toString().trimEnd()
    }

    private fun formatHelp(): String =
        "I can help you with:\n\n" +
        "💰 Collections\n" +
        "   \"Aaj kitna hua?\"  \"This week's total\"\n\n" +
        "📋 Transactions\n" +
        "   \"Show last 5 payments\"  \"Show failed transactions\"\n\n" +
        "🏦 Settlement\n" +
        "   \"Kab aayega paisa?\"  \"Settlement status\"\n\n" +
        "⚠️ Holds & Disputes\n" +
        "   \"Show hold transactions\"  \"Explain TXN2031\"\n\n" +
        "📊 Reports\n" +
        "   \"Daily summary\"  \"Compare today vs yesterday\"  \"Weekly trend\"\n\n" +
        "🔍 Search\n" +
        "   \"Find ₹5,200 transaction\"  \"Search by amount\""

    // ── Hinglish response layer ───────────────────────────────────────────────────

    /**
     * Wraps key data points in a Hinglish phrasing for merchant-friendly tone.
     * Only applied when the classifier detects Hindi/Hinglish input.
     * We keep the numbers and structure identical — just the framing changes.
     */
    private fun toHinglish(functionName: String, english: String, obj: JsonObject): String {
        return when (functionName) {
            "getDailySummary" -> {
                val collected = obj.get("collectedINR")?.asDouble ?: 0.0
                val count     = obj.get("successCount")?.asInt ?: 0
                val failCount = obj.get("failedCount")?.asInt ?: 0
                val eta       = obj.get("settlementETA")?.asString ?: "aaj raat 11:30 PM"
                val yest      = obj.get("yesterdaySettledINR")?.asDouble ?: 0.0
                buildString {
                    append("Aaj ka hisaab:\n")
                    append("  • Aaya:      ${fmtInr(collected)} ($count transactions)\n")
                    if (failCount > 0) append("  • Failed:    $failCount payment(s)\n")
                    append("  • Settlement: ${fmtInr(collected)} — $eta tak\n")
                    if (yest > 0) append("\nKal ka settled: ${fmtInr(yest)} → account ****4509")
                }
            }
            "getCollections" -> {
                val total  = obj.get("totalCollectionsINR")?.asDouble ?: 0.0
                val count  = obj.get("transactionCount")?.asInt ?: 0
                val dateRange = obj.get("dateRange")?.asString ?: "today"
                val period = when (dateRange) { "today" -> "Aaj" ; "yesterday" -> "Kal" ; else -> "Is hafte" }
                "$period ka collection: ${fmtInr(total)} ($count transactions)\n" + english.lines().drop(1).joinToString("\n")
            }
            "getSettlementStatus" -> {
                val pending = obj.get("pendingAmountINR")?.asDouble ?: 0.0
                val eta     = obj.get("estimatedSettlementTime")?.asString ?: "aaj raat"
                val bank    = obj.get("bankAccountEnd")?.asString ?: "****"
                "Paisa kab aayega?\n" +
                "  • Pending: ${fmtInr(pending)} — $eta tak account ****$bank mein aayega\n" +
                english.lines().drop(2).joinToString("\n")
            }
            "getFailedTransactions" -> {
                val count = obj.get("failedCount")?.asInt ?: 0
                if (count == 0) "Koi bhi payment fail nahi hua! Sab theek hai."
                else "Failed payments ($count):\n" + english.lines().drop(1).joinToString("\n")
            }
            else -> english  // fallback to English for other functions
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    private fun failureSuggestion(reason: String?): String = when {
        reason == null -> "Ask customer to try again"
        reason.contains("insufficient", ignoreCase = true) -> "Ask customer to check their bank balance and retry"
        reason.contains("PIN", ignoreCase = true)          -> "Ask customer to enter correct UPI PIN"
        reason.contains("limit", ignoreCase = true)        -> "Ask customer to use a different payment method (Card/Netbanking)"
        reason.contains("timed out", ignoreCase = true)    -> "Ask customer to complete payment without leaving the app"
        reason.contains("server", ignoreCase = true)       -> "Retry after a few minutes — bank server issue"
        else -> "Ask customer to retry or use a different payment method"
    }

    private fun holdActionSteps(reason: String?): Pair<Boolean, List<String>> = when {
        reason == null -> Pair(false, listOf("NO action needed from your side", "YES Bank compliance team is reviewing", "You'll receive an SMS when released"))
        reason.contains("KYC", ignoreCase = true) -> Pair(true, listOf(
            "Contact YES Bank support: 1800-1080",
            "Keep your KYC documents ready (Aadhaar, PAN)",
            "Verification usually resolves in 3–5 business days"
        ))
        reason.contains("chargeback", ignoreCase = true) || reason.contains("dispute", ignoreCase = true) -> Pair(true, listOf(
            "Keep your bill/invoice for this transaction",
            "You may be asked to submit proof of delivery",
            "Contact support if customer dispute is invalid: 1800-1080"
        ))
        reason.contains("velocity", ignoreCase = true) -> Pair(false, listOf(
            "No action needed from your side",
            "Auto-review by risk team — usually cleared in 2–3 days",
            "Avoid very high-value UPI payments in quick succession"
        ))
        else -> Pair(false, listOf(
            "No action needed from your side",
            "YES Bank compliance team is reviewing",
            "You'll receive an SMS when released"
        ))
    }

    private fun disputeStatusLabel(status: String) = when (status) {
        "OPEN"          -> "Under review by YES Bank"
        "RESOLVED"      -> "Resolved — amount credited"
        "PENDING_DOCS"  -> "Waiting for your documents"
        else            -> status
    }

    // ── Indian number formatter (uses lakh/crore system above 99,999) ────────────

    fun fmtInr(amount: Double): String {
        val prefix = "₹"
        val long = amount.toLong()
        val isWhole = amount == long.toDouble()

        return if (long >= 10_00_000) {
            val cr = amount / 1_00_00_000.0
            if (cr >= 1.0) "$prefix${"%.2f".format(cr)} Cr"
            else {
                val lakh = amount / 1_00_000.0
                "$prefix${"%.2f".format(lakh)} L"
            }
        } else if (long >= 1_00_000) {
            val lakh = amount / 1_00_000.0
            "$prefix${"%.2f".format(lakh)} L"
        } else {
            // Use Indian comma style: ##,##,###
            if (isWhole) "$prefix${indianFormat(long)}"
            else "$prefix${indianFormat(long)}.${"%.2f".format(amount).substringAfter('.')}"
        }
    }

    private fun indianFormat(n: Long): String {
        if (n < 1000) return n.toString()
        val s = n.toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        val rest  = s.dropLast(3)
        val grouped = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$grouped,$last3"
    }

    // ── Transaction serialisation ─────────────────────────────────────────────────

    private val dateFmt = DateTimeFormatter.ofPattern("dd MMM")

    private fun periodLabel(dateRange: String) = when (dateRange.lowercase().trim()) {
        "today"     -> "Today"
        "yesterday" -> "Yesterday"
        "this week" -> "This week"
        else        -> dateRange
    }

    private fun Transaction.toMap(): Map<String, Any?> = mapOf(
        "id"               to id,
        "date"             to date.format(dateFmt),
        "amount"           to amount,
        "status"           to status,
        "settlementStatus" to settlementStatus,
        "method"           to method,
        "time"             to time,
        "customerName"     to customerName
    )

    // Proactive alert data used by ViewModel on chat open
    fun getProactiveAlerts(): List<String> {
        val alerts = mutableListOf<String>()
        val holdTxns  = ALL_TRANSACTIONS.filter { it.settlementStatus == "HOLD" }
        val failToday = txnsForDay(TODAY).filter { it.status == "FAILED" }
        val pending   = txnsForDay(TODAY).filter { it.status == "SUCCESS" }.sumOf { it.amount }
        val openDisp  = ALL_DISPUTES.filter { it.status == "OPEN" || it.status == "PENDING_DOCS" }

        if (holdTxns.isNotEmpty())
            alerts.add("⚠️ ${holdTxns.size} transaction(s) on hold — ${fmtInr(holdTxns.sumOf { it.amount })} not yet settled. Tap to see details.")
        if (failToday.isNotEmpty())
            alerts.add("✗ ${failToday.size} payment(s) failed today (${fmtInr(failToday.sumOf { it.amount })}). Ask customers to retry.")
        if (pending > 0)
            alerts.add("🏦 Today's collection of ${fmtInr(pending)} will be settled tonight at 11:30 PM.")
        if (openDisp.isNotEmpty())
            alerts.add("📋 ${openDisp.size} open dispute(s) under review — ${fmtInr(openDisp.sumOf { it.amount })} total.")
        return alerts
    }
}
