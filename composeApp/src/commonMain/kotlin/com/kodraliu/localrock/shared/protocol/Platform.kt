package com.kodraliu.localrock.shared.protocol


internal expect fun secureRandomBytes(size: Int): ByteArray


expect fun saveDebugBlob(name: String, bytes: ByteArray): String?

/** Okio's gzip on Android and iOS (`appMain`); Okio has none for wasm, so the Web App has its own. */
internal expect fun gunzip(data: ByteArray): ByteArray
