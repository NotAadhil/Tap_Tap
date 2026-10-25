package com.tapflow.automation

import java.net.URI

/**
 * Validates action parameters and user inputs.
 */
object ActionValidator {

    /**
     * Validates that the tag display name is non-empty and within length limits.
     * @return null if valid, or an error message string.
     */
    fun validateTagName(name: String?): String? {
        val trimmed = name?.trim() ?: ""
        if (trimmed.isEmpty()) return "Tag name cannot be empty."
        if (trimmed.length > 50) return "Tag name must be 50 characters or fewer."
        return null
    }

    /**
     * Validates an HTTP or HTTPS URL.
     * @return null if valid, or an error message string.
     */
    fun validateUrl(url: String?): String? {
        val trimmed = url?.trim() ?: ""
        if (trimmed.isEmpty()) return "URL cannot be empty."
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            return "URL must start with http:// or https://"
        }
        return try {
            val uri = URI(trimmed)
            if (uri.host.isNullOrBlank()) {
                "URL must have a valid host domain (e.g. https://example.com)."
            } else {
                null
            }
        } catch (_: Exception) {
            "Malformed URL syntax."
        }
    }
}
