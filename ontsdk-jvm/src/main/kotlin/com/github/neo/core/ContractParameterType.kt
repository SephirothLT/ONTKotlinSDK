package com.github.neo.core

/**
 *
 */
enum class ContractParameterType private constructor(v: Int) {
    /**
     *
     */
    Signature(0x00),
    /**
     *
     */
    Boolean(0x01),
    /**
     *
     */
    Integer(0x02),
    /**
     *
     */
    Hash160(0x03),
    /**
     *
     */
    Hash256(0x04),
    /**
     *
     */
    ByteArray(0x05),
    PublicKey(0x06),
    String(0x07),
    Array(0x10),
    InteropInterface(0xf0),
    Void(0xff);

    private val value: Byte

    init {
        value = v.toByte()
    }
}