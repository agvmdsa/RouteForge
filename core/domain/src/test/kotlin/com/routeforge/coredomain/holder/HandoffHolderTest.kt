package com.routeforge.coredomain.holder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HandoffHolderTest {
    @Test
    fun `starts with the initial value`() {
        assertNull(HandoffHolder<String?>(initial = null).value.value)
        assertEquals("start", HandoffHolder("start").value.value)
    }

    @Test
    fun `set replaces the current value`() {
        val holder = HandoffHolder("start")

        holder.set("replaced")

        assertEquals("replaced", holder.value.value)
    }
}
