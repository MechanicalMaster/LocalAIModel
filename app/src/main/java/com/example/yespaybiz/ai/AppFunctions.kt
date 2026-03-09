package com.example.yespaybiz.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Realistic dummy dataset: 40 transactions across the last 10 days.
 *
 * Business rules modelled:
 *  - Each day has 3–5 transactions via UPI / CARD / NETBANKING
 *  - ~1 in 20 transactions is placed on HOLD (not settled for 30 days)
 *  - Settled days (T+1): yesterday and older, excluding hold days
 *  - Today's collection is PENDING settlement
 *  - Settlement arrives same-day at 11:30 PM for previous day's batch
 */
object AppFunctions {

    private val gson = Gson()

    // ── Data model ────────────────────────────────────────────────────────────────

    data class Transaction(
        val id: String,
        val date: LocalDate,       // actual calendar date
        val amount: Double,
        val status: String,        // SUCCESS | FAILED
        val settlementStatus: String, // SETTLED | PENDING | HOLD
        val method: String,        // UPI | CARD | NETBANKING
        val time: String,          // display time e.g. "10:30 AM"
        val holdReason: String? = null,
        val holdReleaseDate: LocalDate? = null
    )

    // ── Master dataset: 40 transactions over last 10 days ─────────────────────────
    // Generated relative to "today" so the data is always current.

    private val TODAY: LocalDate = LocalDate.now()

    private fun daysAgo(n: Long) = TODAY.minusDays(n)

    val ALL_TRANSACTIONS: List<Transaction> = listOf(

        // ── Day 0 (Today) — 4 txns, all PENDING ────────────────────────────────
        Transaction("TXN2040", TODAY,  1_250.00, "SUCCESS", "PENDING",  "UPI",        "09:15 AM"),
        Transaction("TXN2041", TODAY,  3_400.00, "SUCCESS", "PENDING",  "CARD",       "11:02 AM"),
        Transaction("TXN2042", TODAY,    450.00, "FAILED",  "PENDING",  "UPI",        "12:30 PM"),
        Transaction("TXN2043", TODAY,  2_100.00, "SUCCESS", "PENDING",  "NETBANKING", "02:45 PM"),

        // ── Day 1 (Yesterday) — 5 txns, SETTLED (T+1) ──────────────────────────
        Transaction("TXN2035", daysAgo(1),  1_800.00, "SUCCESS", "SETTLED",  "UPI",        "08:55 AM"),
        Transaction("TXN2036", daysAgo(1),  5_200.50, "SUCCESS", "SETTLED",  "CARD",       "10:30 AM"),
        Transaction("TXN2037", daysAgo(1),    320.00, "SUCCESS", "SETTLED",  "UPI",        "01:15 PM"),
        Transaction("TXN2038", daysAgo(1),  2_750.00, "SUCCESS", "SETTLED",  "NETBANKING", "03:40 PM"),
        Transaction("TXN2039", daysAgo(1),    980.75, "FAILED",  "SETTLED",  "UPI",        "06:20 PM"),

        // ── Day 2 — 4 txns; TXN2031 on HOLD (suspicious amount) ────────────────
        Transaction("TXN2031", daysAgo(2), 15_000.00, "SUCCESS", "HOLD",     "NETBANKING", "09:10 AM",
            holdReason     = "High-value transaction under review",
            holdReleaseDate = daysAgo(2).plusDays(30)),
        Transaction("TXN2032", daysAgo(2),  1_100.00, "SUCCESS", "SETTLED",  "UPI",        "11:25 AM"),
        Transaction("TXN2033", daysAgo(2),  3_600.00, "SUCCESS", "SETTLED",  "CARD",       "02:00 PM"),
        Transaction("TXN2034", daysAgo(2),    650.00, "SUCCESS", "SETTLED",  "UPI",        "05:30 PM"),

        // ── Day 3 — 4 txns, all SETTLED ────────────────────────────────────────
        Transaction("TXN2027", daysAgo(3),  2_200.00, "SUCCESS", "SETTLED",  "UPI",        "10:05 AM"),
        Transaction("TXN2028", daysAgo(3),  4_800.00, "SUCCESS", "SETTLED",  "CARD",       "12:45 PM"),
        Transaction("TXN2029", daysAgo(3),    780.50, "SUCCESS", "SETTLED",  "UPI",        "03:10 PM"),
        Transaction("TXN2030", daysAgo(3),  1_350.00, "FAILED",  "SETTLED",  "NETBANKING", "07:00 PM"),

        // ── Day 4 — 4 txns; TXN2023 on HOLD (chargeback risk) ──────────────────
        Transaction("TXN2023", daysAgo(4), 12_500.00, "SUCCESS", "HOLD",     "CARD",       "08:30 AM",
            holdReason     = "Chargeback risk — pending customer dispute",
            holdReleaseDate = daysAgo(4).plusDays(30)),
        Transaction("TXN2024", daysAgo(4),  2_900.00, "SUCCESS", "SETTLED",  "UPI",        "11:00 AM"),
        Transaction("TXN2025", daysAgo(4),  1_450.00, "SUCCESS", "SETTLED",  "CARD",       "01:30 PM"),
        Transaction("TXN2026", daysAgo(4),    560.00, "SUCCESS", "SETTLED",  "UPI",        "04:15 PM"),

        // ── Day 5 — 4 txns, all SETTLED ────────────────────────────────────────
        Transaction("TXN2019", daysAgo(5),  3_100.00, "SUCCESS", "SETTLED",  "UPI",        "09:40 AM"),
        Transaction("TXN2020", daysAgo(5),  6_750.00, "SUCCESS", "SETTLED",  "CARD",       "11:55 AM"),
        Transaction("TXN2021", daysAgo(5),    420.00, "FAILED",  "SETTLED",  "UPI",        "02:20 PM"),
        Transaction("TXN2022", daysAgo(5),  2_050.00, "SUCCESS", "SETTLED",  "NETBANKING", "05:45 PM"),

        // ── Day 6 — 4 txns; TXN2016 on HOLD (velocity flag) ────────────────────
        Transaction("TXN2016", daysAgo(6),  8_800.00, "SUCCESS", "HOLD",     "UPI",        "07:50 AM",
            holdReason     = "Velocity flag — multiple high-value UPI payments",
            holdReleaseDate = daysAgo(6).plusDays(30)),
        Transaction("TXN2017", daysAgo(6),  1_700.00, "SUCCESS", "SETTLED",  "CARD",       "10:20 AM"),
        Transaction("TXN2018", daysAgo(6),  3_300.00, "SUCCESS", "SETTLED",  "UPI",        "01:00 PM"),
        Transaction("TXN2015", daysAgo(6),    890.00, "SUCCESS", "SETTLED",  "NETBANKING", "04:50 PM"),

        // ── Day 7 — 4 txns, all SETTLED ────────────────────────────────────────
        Transaction("TXN2011", daysAgo(7),  2_500.00, "SUCCESS", "SETTLED",  "UPI",        "09:00 AM"),
        Transaction("TXN2012", daysAgo(7),  4_100.00, "SUCCESS", "SETTLED",  "CARD",       "12:10 PM"),
        Transaction("TXN2013", daysAgo(7),    670.00, "FAILED",  "SETTLED",  "UPI",        "02:35 PM"),
        Transaction("TXN2014", daysAgo(7),  1_950.00, "SUCCESS", "SETTLED",  "NETBANKING", "06:00 PM"),

        // ── Day 8 — 4 txns; TXN2008 on HOLD (KYC mismatch) ─────────────────────
        Transaction("TXN2008", daysAgo(8), 18_000.00, "SUCCESS", "HOLD",     "NETBANKING", "08:15 AM",
            holdReason     = "KYC mismatch — beneficiary details under verification",
            holdReleaseDate = daysAgo(8).plusDays(30)),
        Transaction("TXN2009", daysAgo(8),  2_300.00, "SUCCESS", "SETTLED",  "UPI",        "11:30 AM"),
        Transaction("TXN2010", daysAgo(8),  5_600.00, "SUCCESS", "SETTLED",  "CARD",       "02:00 PM"),
        Transaction("TXN2007", daysAgo(8),    730.00, "SUCCESS", "SETTLED",  "UPI",        "05:15 PM"),

        // ── Day 9 — 4 txns, all SETTLED ────────────────────────────────────────
        Transaction("TXN2003", daysAgo(9),  1_600.00, "SUCCESS", "SETTLED",  "UPI",        "09:25 AM"),
        Transaction("TXN2004", daysAgo(9),  3_850.00, "SUCCESS", "SETTLED",  "CARD",       "12:00 PM"),
        Transaction("TXN2005", daysAgo(9),    510.00, "FAILED",  "SETTLED",  "UPI",        "03:30 PM"),
        Transaction("TXN2006", daysAgo(9),  2_200.00, "SUCCESS", "SETTLED",  "NETBANKING", "06:45 PM")
    )

    // ── Derived aggregates ────────────────────────────────────────────────────────

    private fun txnsForDay(date: LocalDate) =
        ALL_TRANSACTIONS.filter { it.date == date }

    private fun txnsForRange(dateRange: String): List<Transaction> {
        return when (dateRange.lowercase().trim()) {
            "today"     -> txnsForDay(TODAY)
            "yesterday" -> txnsForDay(daysAgo(1))
            "this week" -> ALL_TRANSACTIONS.filter { it.date >= TODAY.minusDays(6) }
            else        -> txnsForDay(TODAY)
        }
    }

    // ── Public: executeFunction ───────────────────────────────────────────────────

    fun executeFunction(functionName: String, args: Map<String, Any>): String {
        return try {
            when (functionName) {
                "getCollections"      -> getCollections(args["dateRange"] as? String ?: "today")
                "getTransactions"     -> getTransactions(
                    dateRange = args["dateRange"] as? String ?: "today",
                    limit     = (args["limit"] as? Number)?.toInt()
                )
                "getSettlementStatus" -> getSettlementStatus()
                "getHoldTransactions" -> getHoldTransactions()
                else -> """{"error":"Unknown function: $functionName"}"""
            }
        } catch (e: Exception) {
            """{"error":"Failed to execute $functionName: ${e.message}"}"""
        }
    }

    // ── Public: formatAnswer ──────────────────────────────────────────────────────

    fun formatAnswer(functionName: String, jsonResult: String): String {
        return try {
            val obj = JsonParser.parseString(jsonResult).asJsonObject
            when (functionName) {
                "getCollections"      -> formatCollections(obj)
                "getTransactions"     -> formatTransactions(obj)
                "getSettlementStatus" -> formatSettlement(obj)
                "getHoldTransactions" -> formatHoldTransactions(obj)
                else -> jsonResult
            }
        } catch (e: Exception) {
            jsonResult
        }
    }

    // ── Private: data functions ───────────────────────────────────────────────────

    private fun getCollections(dateRange: String): String {
        val txns    = txnsForRange(dateRange).filter { it.status == "SUCCESS" }
        val total   = txns.sumOf { it.amount }
        val count   = txns.size
        val held    = txns.filter { it.settlementStatus == "HOLD" }.sumOf { it.amount }
        val settled = txns.filter { it.settlementStatus == "SETTLED" }.sumOf { it.amount }
        val pending = txns.filter { it.settlementStatus == "PENDING" }.sumOf { it.amount }

        val response = mapOf(
            "dateRange"            to dateRange,
            "totalCollectionsINR"  to total,
            "transactionCount"     to count,
            "settledAmountINR"     to settled,
            "pendingAmountINR"     to pending,
            "holdAmountINR"        to held
        )
        return gson.toJson(response)
    }

    private fun getTransactions(dateRange: String, limit: Int?): String {
        // Sort newest-first so "last N" always returns the most recent
        var txns = txnsForRange(dateRange).sortedByDescending { it.date.toString() + it.time }
        if (limit != null && limit > 0) txns = txns.take(limit)

        val response = mapOf(
            "dateRange"    to dateRange,
            "total"        to txns.size,
            "transactions" to txns.map { it.toMap() }
        )
        return gson.toJson(response)
    }

    private fun getSettlementStatus(): String {
        // Pending = today's successful transactions
        val pendingTxns    = txnsForDay(TODAY).filter { it.status == "SUCCESS" }
        val pendingAmount  = pendingTxns.sumOf { it.amount }

        // Last settled batch = yesterday (excluding hold)
        val lastSettledTxns   = txnsForDay(daysAgo(1)).filter {
            it.status == "SUCCESS" && it.settlementStatus == "SETTLED"
        }
        val lastSettledAmount = lastSettledTxns.sumOf { it.amount }

        val response = mapOf(
            "status"                  to "PENDING",
            "pendingAmountINR"        to pendingAmount,
            "pendingTransactionCount" to pendingTxns.size,
            "estimatedSettlementTime" to "Today, 11:30 PM",
            "lastSettledAmountINR"    to lastSettledAmount,
            "lastSettledDate"         to daysAgo(1).format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
            "bankAccountEnd"          to "4509"
        )
        return gson.toJson(response)
    }

    private fun getHoldTransactions(): String {
        val holdTxns = ALL_TRANSACTIONS.filter { it.settlementStatus == "HOLD" }
        val totalHeld = holdTxns.sumOf { it.amount }
        val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")

        val response = mapOf(
            "holdCount"       to holdTxns.size,
            "totalHeldINR"    to totalHeld,
            "transactions"    to holdTxns.map { txn ->
                mapOf(
                    "id"              to txn.id,
                    "date"            to txn.date.format(dateFmt),
                    "amount"          to txn.amount,
                    "method"          to txn.method,
                    "holdReason"      to (txn.holdReason ?: "Under review"),
                    "expectedRelease" to (txn.holdReleaseDate?.format(dateFmt) ?: "TBD")
                )
            }
        )
        return gson.toJson(response)
    }

    // ── Private: formatters ───────────────────────────────────────────────────────

    private fun formatCollections(obj: JsonObject): String {
        val dateRange = obj.get("dateRange")?.asString ?: "today"
        val total     = obj.get("totalCollectionsINR")?.asDouble ?: 0.0
        val count     = obj.get("transactionCount")?.asInt ?: 0
        val settled   = obj.get("settledAmountINR")?.asDouble ?: 0.0
        val pending   = obj.get("pendingAmountINR")?.asDouble ?: 0.0
        val held      = obj.get("holdAmountINR")?.asDouble ?: 0.0

        val period = periodLabel(dateRange)
        if (total == 0.0) return "$period's collection: ₹0 — no transactions found."

        val sb = StringBuilder("$period's collection: ₹${fmt(total)} ($count transactions)\n")
        if (settled > 0) sb.append("  • Settled:  ₹${fmt(settled)}\n")
        if (pending > 0) sb.append("  • Pending:  ₹${fmt(pending)}\n")
        if (held    > 0) sb.append("  • On hold:  ₹${fmt(held)}")
        return sb.toString().trimEnd()
    }

    private fun formatTransactions(obj: JsonObject): String {
        val dateRange = obj.get("dateRange")?.asString ?: "today"
        val txns      = obj.getAsJsonArray("transactions") ?: return "No transactions found."
        val period    = periodLabel(dateRange)

        if (txns.size() == 0) return "$period: No transactions found."

        val sb = StringBuilder("$period's transactions (${txns.size()}):\n")
        txns.forEach { el ->
            val t              = el.asJsonObject
            val id             = t.get("id")?.asString ?: "-"
            val amt            = t.get("amount")?.asDouble ?: 0.0
            val status         = t.get("status")?.asString ?: "-"
            val settlStatus    = t.get("settlementStatus")?.asString ?: ""
            val method         = t.get("method")?.asString ?: "-"
            val time           = t.get("time")?.asString ?: ""

            val txnIcon = if (status == "SUCCESS") "✓" else "✗"
            val holdTag = if (settlStatus == "HOLD") " [HOLD]" else ""

            sb.append("• $txnIcon $id — ₹${fmt(amt)} ($method, $time)$holdTag\n")
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
               "  • Pending:       ₹${fmt(pending)} ($pendingCount txns) — due $eta\n" +
               "  • Last settled:  ₹${fmt(lastAmt)} on $lastDate"
    }

    private fun formatHoldTransactions(obj: JsonObject): String {
        val count     = obj.get("holdCount")?.asInt ?: 0
        val totalHeld = obj.get("totalHeldINR")?.asDouble ?: 0.0
        val txns      = obj.getAsJsonArray("transactions") ?: return "No hold transactions."

        if (count == 0) return "No transactions are currently on hold."

        val sb = StringBuilder("$count transaction(s) on hold — ₹${fmt(totalHeld)} total:\n")
        txns.forEach { el ->
            val t       = el.asJsonObject
            val id      = t.get("id")?.asString ?: "-"
            val date    = t.get("date")?.asString ?: "-"
            val amt     = t.get("amount")?.asDouble ?: 0.0
            val method  = t.get("method")?.asString ?: "-"
            val reason  = t.get("holdReason")?.asString ?: "-"
            val release = t.get("expectedRelease")?.asString ?: "TBD"
            sb.append("• $id — ₹${fmt(amt)} ($method, $date)\n")
            sb.append("  Reason: $reason\n")
            sb.append("  Expected release: $release\n")
        }
        return sb.toString().trimEnd()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private fun periodLabel(dateRange: String) = when (dateRange.lowercase().trim()) {
        "today"     -> "Today"
        "yesterday" -> "Yesterday"
        "this week" -> "This week"
        else        -> dateRange
    }

    private fun fmt(amount: Double): String =
        if (amount == amount.toLong().toDouble()) "%,d".format(amount.toLong())
        else "%,.2f".format(amount)

    private fun Transaction.toMap(): Map<String, Any?> = mapOf(
        "id"               to id,
        "date"             to date.format(DateTimeFormatter.ofPattern("dd MMM")),
        "amount"           to amount,
        "status"           to status,
        "settlementStatus" to settlementStatus,
        "method"           to method,
        "time"             to time
    )
}
