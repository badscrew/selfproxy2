# Sanitized Logger

## Overview

The `SanitizedLogger` is a logging utility that automatically sanitizes sensitive data before logging. This ensures that private keys, UUIDs, passwords, and other sensitive credentials are never exposed in log files or logcat output.

## Features

- **Automatic Sanitization**: Removes or masks sensitive data patterns
- **Multiple Log Levels**: Debug, Info, Warning, and Error
- **Verbose Logging Toggle**: Enable/disable debug logs
- **Log Export**: Export logs to file for troubleshooting
- **Thread-Safe**: Safe to use from multiple threads

## Sensitive Data Patterns

The logger automatically sanitizes:

1. **WireGuard Keys**: Base64-encoded 44-character keys (private keys, preshared keys)
2. **VLESS UUIDs**: RFC 4122 format UUIDs
3. **Labeled Secrets**: Any value following labels like:
   - `privateKey`, `private_key`
   - `presharedKey`, `preshared_key`
   - `secretKey`, `secret_key`
   - `password`
   - `token`
   - `auth`

## Usage

### Basic Logging

```kotlin
import com.selfproxy.vpn.domain.util.SanitizedLogger

class MyClass {
    companion object {
        private const val TAG = "MyClass"
    }
    
    fun myFunction() {
        // Debug log (only shown when verbose logging is enabled)
        SanitizedLogger.d(TAG, "Starting operation")
        
        // Info log
        SanitizedLogger.i(TAG, "Operation completed successfully")
        
        // Warning log
        SanitizedLogger.w(TAG, "Potential issue detected")
        
        // Error log
        SanitizedLogger.e(TAG, "Operation failed", exception)
    }
}
```

### Automatic Sanitization

```kotlin
// This will be sanitized automatically
val privateKey = "abc123def456..."
SanitizedLogger.i(TAG, "Using private key: $privateKey")
// Output: "Using private key: [REDACTED_KEY]"

val uuid = "550e8400-e29b-41d4-a716-446655440000"
SanitizedLogger.i(TAG, "VLESS UUID: $uuid")
// Output: "VLESS UUID: [REDACTED_UUID]"

val config = "privateKey: $privateKey, presharedKey: $psk"
SanitizedLogger.i(TAG, "Config: $config")
// Output: "Config: privateKey: [REDACTED_SECRET], presharedKey: [REDACTED_SECRET]"
```

### Verbose Logging

```kotlin
// Enable verbose logging (shows debug logs)
SanitizedLogger.verboseLoggingEnabled = true

// Debug logs will now be shown
SanitizedLogger.d(TAG, "This will be visible")

// Disable verbose logging
SanitizedLogger.verboseLoggingEnabled = false

// Debug logs will be hidden
SanitizedLogger.d(TAG, "This will NOT be visible")
```

### Log Export

```kotlin
import java.io.File

// Enable log export to file
val logFile = File(context.filesDir, "vpn_logs.txt")
SanitizedLogger.enableLogExport(logFile)

// All logs will now be written to the file
SanitizedLogger.i(TAG, "This will be in the file")

// Get the log file for sharing
val file = SanitizedLogger.getLogFile()
shareLogFile(file)

// Clear the log file
SanitizedLogger.clearLogFile()

// Disable log export
SanitizedLogger.disableLogExport()
```

### Manual Sanitization

If you need to sanitize a string without logging it:

```kotlin
val sensitiveMessage = "privateKey: abc123, uuid: 550e8400-e29b-41d4-a716-446655440000"
val sanitized = SanitizedLogger.sanitize(sensitiveMessage)
// sanitized: "privateKey: [REDACTED_SECRET], uuid: [REDACTED_UUID]"
```

## Best Practices

1. **Always use SanitizedLogger**: Replace all `android.util.Log` calls with `SanitizedLogger`
2. **Use appropriate log levels**:
   - `d()` for debug information (development only)
   - `i()` for informational messages
   - `w()` for warnings
   - `e()` for errors
3. **Enable verbose logging during development**: Set `verboseLoggingEnabled = true` in debug builds
4. **Disable verbose logging in production**: Set `verboseLoggingEnabled = false` in release builds
5. **Export logs for troubleshooting**: Enable log export when users report issues

## Configuration

### Debug Build

In your debug build configuration:

```kotlin
class DebugApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Enable verbose logging in debug builds
        SanitizedLogger.verboseLoggingEnabled = true
        
        // Optionally enable log export
        val logFile = File(filesDir, "debug_logs.txt")
        SanitizedLogger.enableLogExport(logFile)
    }
}
```

### Release Build

In your release build configuration:

```kotlin
class ReleaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Disable verbose logging in release builds
        SanitizedLogger.verboseLoggingEnabled = false
        
        // Only enable log export if user requests it
        // (e.g., through a settings option)
    }
}
```

## Testing

The logger includes comprehensive property-based tests to ensure sensitive data is properly sanitized:

```bash
./gradlew app:testDebugUnitTest --tests "com.selfproxy.vpn.domain.util.LogSanitizationPropertiesTest"
```

## Requirements

This logger satisfies the following requirements:

- **2.6**: Logs do not include private keys, UUIDs, preshared keys, or encryption keys
- **12.8**: Diagnostic information can be logged for troubleshooting
- **12.9**: Verbose logging can be enabled for debugging
- **12.10**: Logs can be exported with sensitive data sanitized

## Security Notes

- The logger uses regex patterns to detect and sanitize sensitive data
- Sanitization happens before any log output (logcat or file)
- Even if verbose logging is disabled, all logs are still sanitized
- Log files are stored in the app's private directory (not accessible to other apps)
- Users should be warned before sharing log files (even though they're sanitized)

## Performance

- Sanitization adds minimal overhead (regex matching)
- Debug logs are completely skipped when verbose logging is disabled
- File I/O is performed asynchronously to avoid blocking
- Log file size should be monitored and rotated if needed
