package com.cgens67.avidtune.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class LogLevel(val label: String, val priority: Int) {
    DEBUG("DEBUG", Log.DEBUG),
    INFO("INFO", Log.INFO),
    WARN("WARN", Log.WARN),
    ERROR("ERROR", Log.ERROR)
}

data class LogEntry(
    val id: Long,
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val stackTrace: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))

    val exceptionName: String?
        get() = throwable?.javaClass?.simpleName ?: if (!stackTrace.isNullOrBlank()) {
            stackTrace.lineSequence().firstOrNull()?.substringBefore(":")?.trim()
        } else null
}

object LogManager {
    private const val MAX_LOGS = 500
    private val idCounter = AtomicLong(0)

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    fun log(priority: Int, tag: String?, message: String, t: Throwable? = null) {
        val level = when (priority) {
            Log.ERROR, Log.ASSERT -> LogLevel.ERROR
            Log.WARN -> LogLevel.WARN
            Log.INFO -> LogLevel.INFO
            else -> LogLevel.DEBUG
        }

        val stackTrace = t?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        }

        val entry = LogEntry(
            id = idCounter.incrementAndGet(),
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag ?: "AvidTune",
            message = message,
            throwable = t,
            stackTrace = stackTrace
        )

        _logs.update { current ->
            (listOf(entry) + current).take(MAX_LOGS)
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }

    fun exportLogsAsText(): String {
        return _logs.value.joinToString("\n\n" + "=".repeat(60) + "\n\n") { entry ->
            buildString {
                append("[${entry.formattedTime}] [${entry.level.label}] [${entry.tag}]\n")
                append("Message: ${entry.message}\n")
                if (!entry.stackTrace.isNullOrBlank()) {
                    append("StackTrace:\n${entry.stackTrace}")
                }
            }
        }
    }
}

class MemoryLogTree : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
        LogManager.log(priority, tag, message, t)
    }
}
