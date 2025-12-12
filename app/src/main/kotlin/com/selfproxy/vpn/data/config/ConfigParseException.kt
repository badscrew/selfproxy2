package com.selfproxy.vpn.data.config

/**
 * Exception thrown when configuration parsing fails.
 */
class ConfigParseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
