package com.bibo.android.core.datastore

object SearchHistoryPolicy {
    fun add(current: List<String>, query: String): List<String> {
        val cleaned = query.trim()
        if (cleaned.isBlank()) return current.take(10)
        return (listOf(cleaned) + current.filterNot { it.equals(cleaned, ignoreCase = true) })
            .take(10)
    }
}
