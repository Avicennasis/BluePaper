package com.avicennasis.bluepaper.protocol

class NiimbotPacket(
    val type: Int,
    val data: ByteArray,
) {
    init {
        require(type in 0..255) { "Type must be 0-255, got $type" }
        require(data.size <= 255) { "Data must be 0-255 bytes, got ${data.size}" }
    }

    val checksum: Int
        get() {
            var cs = type xor data.size
            for (b in data) {
                cs = cs xor (b.toInt() and 0xFF)
            }
            return cs and 0xFF
        }

    fun toBytes(): ByteArray {
        val result = ByteArray(data.size + 7)
        result[0] = HEADER
        result[1] = HEADER
        result[2] = type.toByte()
        result[3] = data.size.toByte()
        data.copyInto(result, 4)
        result[4 + data.size] = checksum.toByte()
        result[5 + data.size] = FOOTER
        result[6 + data.size] = FOOTER
        return result
    }

    fun dataToInt(): Long {
        var value = 0L
        for (b in data) {
            value = (value shl 8) or (b.toLong() and 0xFF)
        }
        return value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NiimbotPacket) return false
        return type == other.type && data.contentEquals(other.data)
    }

    override fun hashCode(): Int = 31 * type + data.contentHashCode()

    override fun toString(): String =
        "NiimbotPacket(type=0x${type.toString(16).padStart(2, '0')}, data=[${data.size} bytes])"

    companion object {
        const val HEADER: Byte = 0x55
        val FOOTER: Byte = 0xAA.toByte()

        fun fromBytes(raw: ByteArray): NiimbotPacket {
            require(raw.size >= 7) { "Packet too short: ${raw.size} bytes (minimum 7)" }
            require(raw[0] == HEADER && raw[1] == HEADER) {
                "Invalid header: expected 0x5555, got 0x${(raw[0].toInt() and 0xFF).toString(16)}${(raw[1].toInt() and 0xFF).toString(16)}"
            }

            val type = raw[2].toInt() and 0xFF
            val len = raw[3].toInt() and 0xFF
            val expectedEnd = 4 + len + 3

            require(raw.size >= expectedEnd) {
                "Packet truncated: need $expectedEnd bytes, have ${raw.size}"
            }
            require(raw[expectedEnd - 1] == FOOTER && raw[expectedEnd - 2] == FOOTER) {
                "Invalid footer"
            }

            val data = raw.copyOfRange(4, 4 + len)

            var cs = type xor len
            for (b in data) {
                cs = cs xor (b.toInt() and 0xFF)
            }
            val actualChecksum = raw[expectedEnd - 3].toInt() and 0xFF
            require((cs and 0xFF) == actualChecksum) {
                "Checksum mismatch: expected 0x${(cs and 0xFF).toString(16)}, got 0x${actualChecksum.toString(16)}"
            }

            return NiimbotPacket(type, data)
        }
    }
}
