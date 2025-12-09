package com.selfproxy.vpn.domain.util

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * A logging utility that automatically sanitizes sensitive data before logging.
 * 
 * This logger removes or masks:
 * - WireGuard private keys (base64 encoded 32-byte keys)
 * - WireGuard preshared keys (base64 encoded 32-byte keys)
 * - VLESS UUIDs (RFC 4122 format)
 * - Private key data in various formats
 * - Authentication tokens
 * 
 * Requirements: 2.6, 12.8, 12.9, 12.10
 */
object SanitizedLogger {
    
    private const val TAG_PREFIX = "SelfProxy"
    
    // Regex patterns for sensitive data
    private val WIREGUARD_KEY_PATTERN = Regex(
        """[A-Za-z0-9+/]{42}[AEIMQUYcgkosw048]="""
    )
    
    private val UUID_PATTERN = Regex(
        """[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"""
    )
    
    private val PRIVATE_KEY_LABEL_PATTERN = Regex(
        """(?i)(private[_\s]?key|preshared[_\s]?key|secret[_\s]?key|password|token|auth)[:\s=]+[^\s,}\]]+""",
        RegexOption.IGNORE_CASE
    )
    
    // Replacement text for sanitized data
    private const val SANITIZED_KEY = "[REDACTED_KEY]"
    private const val SANITIZED_UUID = "[REDACTED_UUID]"
    private const val SANITIZED_SECRET = "[REDACTED_SECRET]"
    
    // Verbose logging toggle
    @Volatile
    var verboseLoggingEnabled: Boolean = false
    
    // Log export settings
    private var logExportEnabled: Boolean = false
    private var logFile: File? = null
    private val logDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    /**
     * Enable log export to file
     */
    fun enableLogExport(file: File) {
        logExportEnabled = true
        logFile = file
        
        // Create parent directories if needed
        file.parentFile?.mkdirs()
        
        // Create or clear the log file
        if (!file.exists()) {
            file.createNewFile()
        }
    }
    
    /**
     * Disable log export
     */
    fun disableLogExport() {
        logExportEnabled = false
        logFile = null
    }
    
    /**
     * Get the current log file for export
     */
    fun getLogFile(): File? = logFile
    
    /**
     * Sanitize a message by removing or masking sensitive data
     */
    fun sanitize(message: String): String {
        var sanitized = message
        
        // Replace WireGuard keys (base64 encoded 44-character strings)
        sanitized = WIREGUARD_KEY_PATTERN.replace(sanitized, SANITIZED_KEY)
        
        // Replace UUIDs
        sanitized = UUID_PATTERN.replace(sanitized, SANITIZED_UUID)
        
        // Replace labeled secrets (e.g., "privateKey: abc123")
        sanitized = PRIVATE_KEY_LABEL_PATTERN.replace(sanitized) { matchResult ->
            val fullMatch = matchResult.value
            val label = fullMatch.split(Regex("[:\\s=]+")).first()
            "$label: $SANITIZED_SECRET"
        }
        
        return sanitized
    }
    
    /**
     * Log a debug message with sanitization
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (verboseLoggingEnabled) {
            val sanitizedMessage = sanitize(message)
            val fullTag = "$TAG_PREFIX:$tag"
            
            if (throwable != null) {
                Log.d(fullTag, sanitizedMessage, throwable)
            } else {
                Log.d(fullTag, sanitizedMessage)
            }
            
            writeToFile("DEBUG", tag, sanitizedMessage, throwable)
        }
    }
    
    /**
     * Log an info message with sanitization
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitize(message)
        val fullTag = "$TAG_PREFIX:$tag"
        
        if (throwable != null) {
            Log.i(fullTag, sanitizedMessage, throwable)
        } else {
            Log.i(fullTag, sanitizedMessage)
        }
        
        writeToFile("INFO", tag, sanitizedMessage, throwable)
    }
    
    /**
     * Log a warning message with sanitization
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitize(message)
        val fullTag = "$TAG_PREFIX:$tag"
        
        if (throwable != null) {
            Log.w(fullTag, sanitizedMessage, throwable)
        } else {
            Log.w(fullTag, sanitizedMessage)
        }
        
        writeToFile("WARN", tag, sanitizedMessage, throwable)
    }
    
    /**
     * Log an error message with sanitization
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitize(message)
        val fullTag = "$TAG_PREFIX:$tag"
        
        if (throwable != null) {
            Log.e(fullTag, sanitizedMessage, throwable)
        } else {
            Log.e(fullTag, sanitizedMessage)
        }
        
        writeToFile("ERROR", tag, sanitizedMessage, throwable)
    }
    
    /**
     * Write log entry to file if export is enabled
     */
    private fun writeToFile(level: String, tag: String, message: String, throwable: Throwable?) {
        if (!logExportEnabled || logFile == null) return
        
        try {
            val timestamp = logDateFormat.format(Date())
            val logEntry = buildString {
                append("$timestamp [$level] $TAG_PREFIX:$tag - $message")
                if (throwable != null) {
                    append("\n")
                    append(throwable.stackTraceToString())
                }
                append("\n")
            }
            
            logFile?.appendText(logEntry)
        } catch (e: Exception) {
            // Fail silently to avoid logging loops
            Log.e("$TAG_PREFIX:Logger", "Failed to write to log file", e)
        }
    }
    
    /**
     * Clear the log file
     */
    fun clearLogFile() {
        logFile?.writeText("")
    }
    
    /**
     * Get the size of the log file in bytes
     */
    fun getLogFileSize(): Long {
        return logFile?.length() ?: 0L
    }
}
