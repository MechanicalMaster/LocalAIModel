package com.example.yespaybiz.ai

import com.google.gson.Gson

/**
 * Defines the available tools (functions) that the AI can call,
 * along with mock data responses.
 */
object AppFunctions {

    private val gson = Gson()

    /**
     * Executes a tool based on its name and arguments.
     * Returns a JSON string representing the result, or an error message.
     */
    fun executeFunction(functionName: String, args: Map<String, Any>): String {
        return try {
            when (functionName) {
                "getCollections" -> {
                    val dateRange = args["dateRange"] as? String ?: "today"
                    getCollections(dateRange)
                }
                "getTransactions" -> {
                    val dateRange = args["dateRange"] as? String ?: "today"
                    getTransactions(dateRange)
                }
                "getSettlementStatus" -> {
                    getSettlementStatus()
                }
                else -> {
                    "{\"error\": \"Unknown function: $functionName\"}"
                }
            }
        } catch (e: Exception) {
            "{\"error\": \"Failed to execute $functionName: ${e.message}\"}"
        }
    }

    // --- Mock Implementations ---

    private fun getCollections(dateRange: String): String {
        val amount = when (dateRange.lowercase()) {
            "today" -> 14500.50
            "yesterday" -> 12300.75
            "this week" -> 85400.00
            else -> 0.0
        }
        val response = mapOf(
            "dateRange" to dateRange,
            "totalCollectionsINR" to amount,
            "transactionCount" to if (amount > 0) 42 else 0
        )
        return gson.toJson(response)
    }

    private fun getTransactions(dateRange: String): String {
        // Return a mock list of recent transactions
        val transactions = listOf(
            mapOf("id" to "TXN1001", "amount" to 150.00, "status" to "SUCCESS", "method" to "UPI", "time" to "10:30 AM"),
            mapOf("id" to "TXN1002", "amount" to 850.50, "status" to "SUCCESS", "method" to "CARD", "time" to "11:15 AM"),
            mapOf("id" to "TXN1003", "amount" to 45.00,  "status" to "FAILED",  "method" to "UPI", "time" to "12:05 PM")
        )
        val response = mapOf(
            "dateRange" to dateRange,
            "transactions" to transactions
        )
        return gson.toJson(response)
    }

    private fun getSettlementStatus(): String {
        val response = mapOf(
            "status" to "PENDING",
            "amountPendingINR" to 14500.50,
            "estimatedSettlementTime" to "Today, 11:30 PM",
            "bankAccountEnd" to "4509"
        )
        return gson.toJson(response)
    }
}
