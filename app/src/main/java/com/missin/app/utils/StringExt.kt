package com.missin.app.utils

/**
 * Redacts a full name for privacy.
 * Example: "Ajay Kumar" becomes "Aj** Ku***"
 * Example: "John Doe" becomes "Jo** Do*"
 * Single names or edge cases handle degradation gracefully.
 */
fun String.redactName(): String {
    val parts = this.trim().split("\\s+".toRegex())
    return parts.joinToString(" ") { part ->
        if (part.length <= 2) {
            part // Too short, just show it
        } else {
            part.take(2) + "*".repeat(part.length - 2)
        }
    }
}
