package com.github.neo.core

/**
 *
 */
enum class TransactionAttributeUsage private constructor(v: Int) {

    Nonce(0x00),
    /**
     *
     */
    Script(0x20),

    DescriptionUrl(0x81),
    Description(0x90);

    private val value: Byte

    init {
        value = v.toByte()
    }

    fun value(): Byte {
        return value
    }

    companion object {

        fun valueOf(v: Byte): TransactionAttributeUsage {
            for (e in TransactionAttributeUsage.values()) {
                if (e.value == v) {
                    return e
                }
            }
            throw IllegalArgumentException()
        }
    }
}
