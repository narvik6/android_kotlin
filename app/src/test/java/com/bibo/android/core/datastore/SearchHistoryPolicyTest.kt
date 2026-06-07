package com.bibo.android.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchHistoryPolicyTest {
    @Test
    fun addsNewestQueryFirstAndDeduplicatesIgnoringCase() {
        val result = SearchHistoryPolicy.add(
            current = listOf("медитация", "дневник"),
            query = " Медитация ",
        )

        assertEquals(listOf("Медитация", "дневник"), result)
    }

    @Test
    fun keepsOnlyTenNewestQueries() {
        val current = (1..10).map { "q$it" }

        val result = SearchHistoryPolicy.add(current, "q11")

        assertEquals(10, result.size)
        assertEquals("q11", result.first())
        assertEquals("q9", result.last())
    }

    @Test
    fun ignoresBlankQuery() {
        val current = listOf("дневник")

        assertEquals(current, SearchHistoryPolicy.add(current, " "))
    }
}
