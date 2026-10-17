package com.example.c001apk.util

import org.junit.Assert.*
import org.junit.Test

class CoolapkTokenTest {
    @Test fun matchesIndependentV3Vectors() {
        // Generated independently with Python bcrypt from the v1.36.1 protocol.
        assertEquals("v3JDJ5JDA0JE5qazRNREJsT0RBdk16VXhZemxsTnVOeS96WDhRWDE5NXNheklDN05mbE1ZRy9EaDN4cEhP",
            CoolapkToken.generate("synthetic-test-device", 1770000000))
        assertEquals("v3JDJ5JDA0JE5qazRNREJsT0RFdk1qVmtabVkwTWVrNGhudDRBcWtOMUVGTThzMHhiekdpMjdPWmhnZzRt",
            CoolapkToken.generate("synthetic-test-device", 1770000001))
    }

    @Test fun signatureIsBoundToDeviceAndVersion() {
        val token = CoolapkToken.generate("device-a", 1770000000)
        assertNotEquals(token, CoolapkToken.generate("device-b", 1770000000))
        assertNotEquals(token, CoolapkToken.generate("device-a", 1770000000, 2604202))
    }

    @Test fun allTimeTableEntriesProduceV3Tokens() {
        repeat(100) { second ->
            val token = CoolapkToken.generate("synthetic-test-device", 1770000000L + second)
            assertTrue(token.startsWith("v3"))
            assertFalse(token.contains('='))
        }
    }
}
