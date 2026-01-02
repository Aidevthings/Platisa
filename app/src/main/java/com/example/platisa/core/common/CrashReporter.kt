package com.example.platisa.core.common

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashReporter {

    private const val TAG = "CrashReporter"
    private const val FILE_NAME = "crash_log.txt"

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(TAG, "Uncaught Exception detected in thread ${thread.name}", throwable)
                saveCrashLog(context, thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save crash log", e)
            } finally {
                // Pass control to the default handler to let the app crash "normally" or exit
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun saveCrashLog(context: Context, thread: Thread, throwable: Throwable) {
        val stackTrace = StringWriter()
        throwable.printStackTrace(PrintWriter(stackTrace))

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logContent = """
            
            ------------------------------------
            CRASH DETECTED AT $timestamp
            Thread: ${thread.name}
            ------------------------------------
            $stackTrace
            ------------------------------------
        """.trimIndent()

        try {
            val file = File(context.filesDir, FILE_NAME)
            // Append to existing file so we don't lose previous crashes if multiple happen rapidly
            file.appendText(logContent)
            Log.d(TAG, "Crash log saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error writing to crash log file", e)
        }
    }
}
