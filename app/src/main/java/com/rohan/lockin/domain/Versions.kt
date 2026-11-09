package com.rohan.lockin.domain

/** Release tags are "v1.1.<CI run number>"; the run number is also the APK versionCode. */
fun versionCodeFromTag(tag: String): Int? {
    if (!tag.startsWith("v")) return null
    val parts = tag.removePrefix("v").split('.')
    if (parts.size != 3 || parts.any { p -> p.isEmpty() || !p.all(Char::isDigit) }) return null
    return parts.last().toIntOrNull()
}

/** Only GitHub-hosted HTTPS downloads are trusted for the APK. */
fun isTrustedDownloadUrl(url: String): Boolean {
    val host = url.removePrefix("https://").substringBefore('/').lowercase()
    return url.startsWith("https://") && (host == "github.com" || host.endsWith(".githubusercontent.com"))
}
