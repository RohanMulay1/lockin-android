package com.rohan.lockin.domain

import kotlin.math.abs

/** Indian digit grouping: 400000 -> ₹4,00,000. */
fun rupees(value: Int): String {
    val digits = abs(value.toLong()).toString()
    val grouped = if (digits.length <= 3) {
        digits
    } else {
        val lastThree = digits.takeLast(3)
        val rest = digits.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
        "$rest,$lastThree"
    }
    return (if (value < 0) "-" else "") + "₹" + grouped
}

fun signedRupees(value: Int): String = if (value > 0) "+${rupees(value)}" else rupees(value)

fun mmss(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1_000L
    return "%02d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
