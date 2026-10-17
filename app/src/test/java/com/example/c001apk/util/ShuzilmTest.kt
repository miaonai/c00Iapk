package com.example.c001apk.util

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream

class ShuzilmTest {
    @Test fun decodesCompressedAndPlainResponses() {
        val plain = "{\"n_a\":\"DUvalid_test_123456\"}".toByteArray()
        val compressed = ByteArrayOutputStream().also { output ->
            DeflaterOutputStream(output).use { it.write(plain) }
        }.toByteArray()
        assertEquals("DUvalid_test_123456", Shuzilm.decode(Shuzilm.xor(plain)))
        assertEquals("DUvalid_test_123456", Shuzilm.decode(Shuzilm.xor(compressed)))
    }

    @Test fun rejectsInvalidIdsAndOversizedResponses() {
        for (id in listOf("", "000000000000000000000000000000000000", "DUbad\r\nheader")) {
            assertFalse(Shuzilm.validId(id))
        }
        assertThrows(IllegalArgumentException::class.java) { Shuzilm.decode(ByteArray(65537)) }
        assertThrows(IllegalArgumentException::class.java) {
            Shuzilm.decode(Shuzilm.xor("{\"n_a\":\"invalid\"}".toByteArray()))
        }
    }
}
