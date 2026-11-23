package com.kodraliu.localrock.shared.protocol

import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

private fun cryptoRandom(size: Int): Uint8Array =
    js("crypto.getRandomValues(new Uint8Array(size))")

internal actual fun secureRandomBytes(size: Int): ByteArray {
    if (size == 0) return ByteArray(0)
    val random = cryptoRandom(size)
    return ByteArray(size) { random[it] }
}

/** The browser has no file system to drop debug blobs into. */
actual fun saveDebugBlob(name: String, bytes: ByteArray): String? = null
