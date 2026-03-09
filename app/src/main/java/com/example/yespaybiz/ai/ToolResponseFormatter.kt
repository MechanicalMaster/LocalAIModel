package com.example.yespaybiz.ai

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.text.NumberFormat
import java.util.Locale

/**
 * Deterministic formatter for tool/function outputs.
 * Converts function JSON into clean natural language without a second LLM pass.
 */
object ToolResponseFormatter {

    fun format(functionName: String, userQuestion: String, functionResult: String): String {
        val parsed = runCatching { JsonParser.parseString(functionResult).asJsonObject }.getOrNull()
        if (parsed == null) {
            return "I fetched the data, but could not parse it correctly."
        }

        if (parsed.has("error")) {
            return parsed.get("error")?.asString ?: "Something went wrong while fetching data."
        }

        return when (functionName) {
            "getCollections" -> formatCollections(parsed)
            "getTransactions" -> formatTransactions(parsed, userQuestion)
            "getSettlementStatus" -> formatSettlement(parsed)
            else -> "I fetched the data, but do not have a formatter for $functionName yet."
        }
    }

    private fun formatCollections(data: JsonObject): String {
        val dateRange = data.get("dateRange")?.asString ?: "the selected period"
        val amount = data.get("totalCollectionsINR")?.asDouble ?: 0.0
        val txnCount = data.get("transactionCount")?.asInt ?: 0
        return "You collected ${formatInr(amount)} from $txnCount transactions for $dateRange."
    }

    private fun formatTransactions(data: JsonObject, userQuestion: String): String {
        val dateRange = data.get("dateRange")?.asString ?: "the selected period"
        val transactionsArray = data.getAsJsonArray("transactions")
        if (transactionsArray == null || transactionsArray.size() == 0) {
            return "No transactions found for $dateRange."
        }

        val requestedLimit = extractRequestedTransactionLimit(userQuestion)
        val transactionObjects = transactionsArray.mapNotNull { element ->
            runCatching { element.asJsonObject }.getOrNull()
        }
        val selectedTransactions = if (requestedLimit != null && requestedLimit > 0) {
            transactionObjects.takeLast(requestedLimit)
        } else {
            transactionObjects
        }

        val lines = selectedTransactions.map { txn ->
            val id = txn.get("id")?.asString ?: "N/A"
            val amount = txn.get("amount")?.asDouble ?: 0.0
            val status = txn.get("status")?.asString ?: "UNKNOWN"
            val method = txn.get("method")?.asString ?: "UNKNOWN"
            val time = txn.get("time")?.asString ?: "Unknown time"
            "$id: ${formatInr(amount)}, status $status, method $method, at $time"
        }

        val header = if (requestedLimit != null && requestedLimit > 0) {
            "Here are your last ${lines.size} transactions for $dateRange:"
        } else {
            "Here are your transactions for $dateRange:"
        }

        return buildString {
            append(header)
            append("\n")
            append(lines.joinToString("\n"))
        }
    }

    private fun formatSettlement(data: JsonObject): String {
        val status = data.get("status")?.asString ?: "UNKNOWN"
        val amount = data.get("amountPendingINR")?.asDouble ?: 0.0
        val eta = data.get("estimatedSettlementTime")?.asString ?: "N/A"
        val bankAccountEnd = data.get("bankAccountEnd")?.asString ?: "N/A"
        val method = data.get("method")?.asString ?: "UNKNOWN"

        return "Settlement status: $status. Pending amount: ${formatInr(amount)}. Method: $method. Expected by: $eta. Bank account ending: $bankAccountEnd."
    }

    private fun formatInr(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        return formatter.format(amount)
    }

    private fun extractRequestedTransactionLimit(userQuestion: String): Int? {
        val normalized = userQuestion.lowercase(Locale.ROOT)
        val patterns = listOf(
            Regex("""(?:last|recent)\s+(\d{1,2})"""),
            Regex("""(\d{1,2})\s+(?:transactions?|txns?)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(normalized) ?: continue
            val value = match.groupValues.getOrNull(1)?.toIntOrNull() ?: continue
            if (value > 0) return value
        }
        return null
    }
}
