package com.kodraliu.localrock.shared.protocol

import okio.Buffer
import okio.GzipSource
import okio.buffer

internal actual fun gunzip(data: ByteArray): ByteArray {
    val src = Buffer().apply { write(data) }
    return GzipSource(src).buffer().readByteArray()
}
