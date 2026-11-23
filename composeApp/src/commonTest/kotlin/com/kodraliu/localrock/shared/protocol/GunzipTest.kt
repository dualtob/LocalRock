package com.kodraliu.localrock.shared.protocol

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

// Inputs made with Python's gzip/zlib modules. The map fixtures cover dynamic Huffman blocks.
@OptIn(ExperimentalEncodingApi::class)
class GunzipTest {

    @Test
    fun inflates_a_fixed_huffman_member() {
        val gz = Base64.decode("H4sIAAAAAAAC/8tIzcnJV8hAIoPyk/JLAIWmx6IXAAAA")
        assertEquals("hello hello hello Robot", gunzip(gz).decodeToString())
    }

    @Test
    fun inflates_a_stored_member_with_a_file_name() {
        val gz = Base64.decode("H4sICAAAAAAA/25hbWUuYmluAAEMAPP/c3RvcmVkIFJvYm90VmXVpwwAAAA=")
        assertEquals("stored Robot", gunzip(gz).decodeToString())
    }

    @Test
    fun rejects_a_corrupted_member() {
        val gz = Base64.decode("H4sIAAAAAAAC/8tIzcnJV8hAIoPyk/JLAIWmx6IXAAAA")
        gz[gz.size - 6] = (gz[gz.size - 6].toInt() xor 0xff).toByte() // break the CRC
        assertFails { gunzip(gz) }
    }
}
