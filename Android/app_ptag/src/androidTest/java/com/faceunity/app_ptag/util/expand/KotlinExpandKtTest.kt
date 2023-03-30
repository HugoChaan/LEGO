package com.faceunity.app_ptag.util.expand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 *
 */
class KotlinExpandKtTest {

    @Test
    fun format() {
        assertEquals(1.23.format(1), "1.2")
        assertEquals(1.23456.format(1), "1.2")
        assertEquals(1.0.format(1), "1.0")
        assertNotEquals(1.000.format(1), "1")
        assertEquals(1.23456.format(5), "1.23456")

        assertEquals(1.21.format(1), "1.2")
        assertEquals(1.29.format(1), "1.3")
        assertEquals(0.99.format(1), "1.0")
    }
}