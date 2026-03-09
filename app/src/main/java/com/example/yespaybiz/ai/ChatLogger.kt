package com.example.yespaybiz.ai

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.util.Log
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * On-device SQLite logger for every AI conversation turn.
 *
 * Schema (one row per turn):
 *   id               INTEGER PK AUTOINCREMENT
 *   ts               TEXT    ISO-8601 timestamp
 *   user_input       TEXT
 *   classifier       TEXT    e.g. "KnownIntent(getCollections,yesterday)" | "UnknownIntent" | "LowConfidence"
 *   llm_called       INTEGER 0/1
 *   llm_raw          TEXT    raw LLM output (first 500 chars)
 *   llm_parse_ok     INTEGER 0/1
 *   guardrail_fixed  INTEGER 0/1  — model name != resolved name
 *   function_called  TEXT
 *   final_answer     TEXT    (first 300 chars)
 *   latency_ms       INTEGER
 *   model_version    TEXT
 *   prompt_version   TEXT
 *   error            TEXT    null if successful
 */
object ChatLogger {

    private const val TAG = "ChatLogger"
    private const val DB_NAME = "ai_chat_log.db"
    private const val DB_VERSION = 1
    private const val TABLE = "turns"

    private var db: SQLiteDatabase? = null

    private val ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        .withZone(ZoneId.of("UTC"))

    // ── Initialise ────────────────────────────────────────────────────────────────

    fun init(context: Context) {
        try {
            db = DbHelper(context).writableDatabase
            Log.i(TAG, "ChatLogger initialised")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open log DB", e)
        }
    }

    // ── Log a turn ────────────────────────────────────────────────────────────────

    data class TurnLog(
        val userInput: String,
        val classifierResult: String,       // "KnownIntent(fn,args)" | "UnknownIntent" | "LowConfidence(fn)"
        val llmCalled: Boolean,
        val llmRawOutput: String,
        val llmParseOk: Boolean,
        val guardrailFixed: Boolean,
        val functionCalled: String,
        val finalAnswer: String,
        val latencyMs: Long,
        val modelVersion: String,
        val promptVersion: String,
        val error: String? = null
    )

    fun log(turn: TurnLog) {
        val database = db ?: return
        try {
            val cv = ContentValues().apply {
                put("ts",              ISO.format(Instant.now()))
                put("user_input",      turn.userInput.take(500))
                put("classifier",      turn.classifierResult)
                put("llm_called",      if (turn.llmCalled) 1 else 0)
                put("llm_raw",         turn.llmRawOutput.take(500))
                put("llm_parse_ok",    if (turn.llmParseOk) 1 else 0)
                put("guardrail_fixed", if (turn.guardrailFixed) 1 else 0)
                put("function_called", turn.functionCalled)
                put("final_answer",    turn.finalAnswer.take(300))
                put("latency_ms",      turn.latencyMs)
                put("model_version",   turn.modelVersion)
                put("prompt_version",  turn.promptVersion)
                put("error",           turn.error)
            }
            database.insert(TABLE, null, cv)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log turn", e)
        }
    }

    // ── Export to a .txt file ─────────────────────────────────────────────────────

    /**
     * Exports all logged turns to a human-readable text file in the app's cache dir.
     * Returns the [File] on success, null on failure.
     */
    fun exportToFile(context: Context): File? {
        val database = db ?: return null
        return try {
            val cursor = database.query(
                TABLE, null, null, null, null, null, "id ASC"
            )
            val file = File(context.cacheDir, "yespaybiz_ai_logs.txt")
            file.bufferedWriter().use { writer ->
                writer.write("YesPayBiz AI Chat Logs\n")
                writer.write("Exported: ${ISO.format(Instant.now())}\n")
                writer.write("Model: ${AIService.MODEL_VERSION}  |  Prompt: ${AIService.PROMPT_VERSION}\n")
                writer.write("=" .repeat(80) + "\n\n")

                var count = 0
                while (cursor.moveToNext()) {
                    count++
                    writer.write("Turn #$count\n")
                    writer.write("  Time:            ${cursor.string("ts")}\n")
                    writer.write("  User:            ${cursor.string("user_input")}\n")
                    writer.write("  Classifier:      ${cursor.string("classifier")}\n")
                    writer.write("  LLM called:      ${cursor.int("llm_called") == 1}\n")
                    if (cursor.int("llm_called") == 1) {
                        writer.write("  LLM raw:         ${cursor.string("llm_raw")}\n")
                        writer.write("  LLM parse OK:    ${cursor.int("llm_parse_ok") == 1}\n")
                        writer.write("  Guardrail fixed: ${cursor.int("guardrail_fixed") == 1}\n")
                    }
                    writer.write("  Function:        ${cursor.string("function_called")}\n")
                    writer.write("  Answer:          ${cursor.string("final_answer")}\n")
                    writer.write("  Latency:         ${cursor.int("latency_ms")} ms\n")
                    writer.write("  Model version:   ${cursor.string("model_version")}\n")
                    writer.write("  Prompt version:  ${cursor.string("prompt_version")}\n")
                    val err = cursor.string("error")
                    if (!err.isNullOrEmpty()) writer.write("  ERROR:           $err\n")
                    writer.write("\n")
                }
                writer.write("─".repeat(80) + "\n")
                writer.write("Total turns: $count\n")
                cursor.close()
            }
            Log.i(TAG, "Exported $file")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Export failed", e)
            null
        }
    }

    // ── SQLite helpers ────────────────────────────────────────────────────────────

    private class DbHelper(context: Context) :
        SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    ts               TEXT,
                    user_input       TEXT,
                    classifier       TEXT,
                    llm_called       INTEGER,
                    llm_raw          TEXT,
                    llm_parse_ok     INTEGER,
                    guardrail_fixed  INTEGER,
                    function_called  TEXT,
                    final_answer     TEXT,
                    latency_ms       INTEGER,
                    model_version    TEXT,
                    prompt_version   TEXT,
                    error            TEXT
                )
            """.trimIndent())
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE")
            onCreate(db)
        }
    }

    private fun android.database.Cursor.string(col: String): String? =
        getColumnIndex(col).takeIf { it >= 0 }?.let { getString(it) }

    private fun android.database.Cursor.int(col: String): Int =
        getColumnIndex(col).takeIf { it >= 0 }?.let { getInt(it) } ?: 0
}
