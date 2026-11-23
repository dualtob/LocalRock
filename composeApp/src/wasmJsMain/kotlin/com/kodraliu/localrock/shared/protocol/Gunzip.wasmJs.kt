package com.kodraliu.localrock.shared.protocol

/*
 * gzip (RFC 1952) and inflate (RFC 1951) for the Web App, since Okio has no gzip on wasm and the
 * browser's DecompressionStream is asynchronous while callers here are not. The inflater follows
 * zlib's reference decoder "puff": canonical Huffman codes decoded a bit at a time.
 */

internal actual fun gunzip(data: ByteArray): ByteArray {
    val input = BitInput(data)
    if (input.byte() != 0x1f || input.byte() != 0x8b) throw GzipException("not gzip data")
    if (input.byte() != 8) throw GzipException("unsupported compression method")
    val flags = input.byte()
    input.skip(6) // MTIME, XFL, OS
    if (flags and 0x04 != 0) input.skip(input.byte() or (input.byte() shl 8)) // FEXTRA
    if (flags and 0x08 != 0) input.skipZeroTerminated() // FNAME
    if (flags and 0x10 != 0) input.skipZeroTerminated() // FCOMMENT
    if (flags and 0x02 != 0) input.skip(2) // FHCRC

    val out = inflate(input)

    input.alignToByte()
    val crc = input.int32()
    val size = input.int32()
    if (crc != crc32(out)) throw GzipException("gzip CRC mismatch")
    if (size != out.size) throw GzipException("gzip size mismatch")
    return out
}

internal class GzipException(message: String) : RuntimeException(message)

private class BitInput(private val data: ByteArray) {
    private var pos = 0
    private var bitBuf = 0
    private var bitCount = 0

    fun byte(): Int {
        if (pos >= data.size) throw GzipException("unexpected end of gzip data")
        return data[pos++].toInt() and 0xff
    }

    fun skip(n: Int) {
        pos += n
    }

    fun skipZeroTerminated() {
        while (byte() != 0) Unit
    }

    fun int32(): Int = byte() or (byte() shl 8) or (byte() shl 16) or (byte() shl 24)

    /** [n] bits, least significant first, as deflate packs them. */
    fun bits(n: Int): Int {
        var buf = bitBuf
        while (bitCount < n) {
            buf = buf or (byte() shl bitCount)
            bitCount += 8
        }
        bitBuf = buf ushr n
        bitCount -= n
        return buf and ((1 shl n) - 1)
    }

    /** Drop the rest of the current byte; whole buffered bytes are not lost since bits() reads lazily. */
    fun alignToByte() {
        bitBuf = 0
        bitCount = 0
    }

    fun copyTo(out: ByteSink, n: Int) {
        if (pos + n > data.size) throw GzipException("unexpected end of gzip data")
        for (i in 0 until n) out.add(data[pos + i])
        pos += n
    }
}

private class ByteSink {
    var bytes = ByteArray(1 shl 16)
    var size = 0

    fun add(b: Byte) {
        if (size == bytes.size) bytes = bytes.copyOf(bytes.size * 2)
        bytes[size++] = b
    }

    fun toByteArray(): ByteArray = bytes.copyOf(size)
}

private const val MAX_BITS = 15

private class Huffman(lengths: IntArray, offset: Int, n: Int) {
    val count = IntArray(MAX_BITS + 1)
    val symbol = IntArray(n)

    init {
        for (i in 0 until n) count[lengths[offset + i]]++
        val offs = IntArray(MAX_BITS + 1)
        for (len in 1 until MAX_BITS) offs[len + 1] = offs[len] + count[len]
        for (sym in 0 until n) {
            val len = lengths[offset + sym]
            if (len != 0) symbol[offs[len]++] = sym
        }
    }

    fun decode(input: BitInput): Int {
        var code = 0
        var first = 0
        var index = 0
        for (len in 1..MAX_BITS) {
            code = code or input.bits(1)
            val c = count[len]
            if (code - c < first) return symbol[index + (code - first)]
            index += c
            first = (first + c) shl 1
            code = code shl 1
        }
        throw GzipException("invalid Huffman code")
    }
}

private val LENGTH_BASE = intArrayOf(
    3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
    35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258,
)
private val LENGTH_EXTRA = intArrayOf(
    0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
    3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0,
)
private val DIST_BASE = intArrayOf(
    1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
    257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577,
)
private val DIST_EXTRA = intArrayOf(
    0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
    7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13,
)
private val CODE_LENGTH_ORDER = intArrayOf(16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15)

private val fixedCodes: Pair<Huffman, Huffman> by lazy {
    val lengths = IntArray(288 + 30)
    for (i in 0 until 144) lengths[i] = 8
    for (i in 144 until 256) lengths[i] = 9
    for (i in 256 until 280) lengths[i] = 7
    for (i in 280 until 288) lengths[i] = 8
    for (i in 288 until 318) lengths[i] = 5
    Huffman(lengths, 0, 288) to Huffman(lengths, 288, 30)
}

private fun inflate(input: BitInput): ByteArray {
    val out = ByteSink()
    do {
        val last = input.bits(1) == 1
        when (input.bits(2)) {
            0 -> stored(input, out)
            1 -> fixedCodes.let { (lit, dist) -> codes(input, out, lit, dist) }
            2 -> dynamic(input, out)
            else -> throw GzipException("invalid deflate block type")
        }
    } while (!last)
    return out.toByteArray()
}

private fun stored(input: BitInput, out: ByteSink) {
    input.alignToByte()
    val len = input.byte() or (input.byte() shl 8)
    val nlen = input.byte() or (input.byte() shl 8)
    if (len != (nlen.inv() and 0xffff)) throw GzipException("invalid stored block length")
    input.copyTo(out, len)
}

private fun dynamic(input: BitInput, out: ByteSink) {
    val nlen = input.bits(5) + 257
    val ndist = input.bits(5) + 1
    val ncode = input.bits(4) + 4
    if (nlen > 286 || ndist > 30) throw GzipException("bad deflate code counts")

    val lengths = IntArray(19)
    for (i in 0 until ncode) lengths[CODE_LENGTH_ORDER[i]] = input.bits(3)
    val lencode = Huffman(lengths, 0, 19)

    val all = IntArray(nlen + ndist)
    var index = 0
    while (index < nlen + ndist) {
        val sym = lencode.decode(input)
        if (sym < 16) {
            all[index++] = sym
            continue
        }
        var value = 0
        val repeat = when (sym) {
            16 -> {
                if (index == 0) throw GzipException("repeat with no previous length")
                value = all[index - 1]
                3 + input.bits(2)
            }
            17 -> 3 + input.bits(3)
            else -> 11 + input.bits(7)
        }
        if (index + repeat > nlen + ndist) throw GzipException("too many code lengths")
        repeat(repeat) { all[index++] = value }
    }
    if (all[256] == 0) throw GzipException("no end-of-block code")
    codes(input, out, Huffman(all, 0, nlen), Huffman(all, nlen, ndist))
}

private fun codes(input: BitInput, out: ByteSink, lit: Huffman, dist: Huffman) {
    while (true) {
        val sym = lit.decode(input)
        when {
            sym < 256 -> out.add(sym.toByte())
            sym == 256 -> return
            else -> {
                val l = sym - 257
                if (l >= 29) throw GzipException("invalid length code")
                val len = LENGTH_BASE[l] + input.bits(LENGTH_EXTRA[l])
                val d = dist.decode(input)
                if (d >= 30) throw GzipException("invalid distance code")
                val back = DIST_BASE[d] + input.bits(DIST_EXTRA[d])
                if (back > out.size) throw GzipException("distance too far back")
                var from = out.size - back
                repeat(len) { out.add(out.bytes[from++]) }
            }
        }
    }
}

private val CRC_TABLE = IntArray(256) { n ->
    var c = n
    repeat(8) { c = if (c and 1 != 0) (c ushr 1) xor 0xedb88320.toInt() else c ushr 1 }
    c
}

private fun crc32(data: ByteArray): Int {
    var crc = -1
    for (b in data) crc = CRC_TABLE[(crc xor b.toInt()) and 0xff] xor (crc ushr 8)
    return crc.inv()
}
