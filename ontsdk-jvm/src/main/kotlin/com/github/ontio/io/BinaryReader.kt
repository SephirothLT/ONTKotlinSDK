/*
 * Copyright (C) 2018 The ontology Authors
 * This file is part of The ontology library.
 *
 *  The ontology is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The ontology is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with The ontology.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.github.ontio.io

import java.io.*
import java.lang.reflect.Array
import java.nio.*

import com.github.ontio.common.ErrorCode
import com.github.ontio.core.scripts.ScriptOp
import org.bouncycastle.math.ec.ECPoint

import com.github.ontio.crypto.ECC

/*
 ******************************************************************************
 * public func for outside calling
 ******************************************************************************
 * 1. WriteVarUint func, depend on the inpute number's Actual number size,
 *    serialize to bytes.
 *      uint8  =>  (LittleEndian)num in 1 byte                 = 1bytes
 *      uint16 =>  0xfd(1 byte) + (LittleEndian)num in 2 bytes = 3bytes
 *      uint32 =>  0xfe(1 byte) + (LittleEndian)num in 4 bytes = 5bytes
 *      uint64 =>  0xff(1 byte) + (LittleEndian)num in 8 bytes = 9bytes
 * 2. ReadVarUint  func, this func will read the first byte to determined
 *    the num length to read.and retrun the uint64
 *      first byte = 0xfd, read the next 2 bytes as uint16
 *      first byte = 0xfe, read the next 4 bytes as uint32
 *      first byte = 0xff, read the next 8 bytes as uint64
 *      other else,        read this byte as uint8
 * 3. WriteVarBytes func, this func will output two item as serialization.
 *      length of bytes (uint8/uint16/uint32/uint64)  +  bytes
 * 4. WriteString func, this func will output two item as serialization.
 *      length of string(uint8/uint16/uint32/uint64)  +  bytes(string)
 * 5. ReadVarBytes func, this func will first read a uint to identify the
 *    length of bytes, and use it to get the next length's bytes to return.
 * 6. ReadString func, this func will first read a uint to identify the
 *    length of string, and use it to get the next bytes as a string.
 * 7. GetVarUintSize func, this func will return the length of a uint when it
 *    serialized by the WriteVarUint func.
 * 8. ReadBytes func, this func will read the specify lenth's bytes and retun.
 * 9. ReadUint8,16,32,64 read uint with fixed length
 * 10.WriteUint8,16,32,64 Write uint with fixed length
 * 11.ToArray SerializableData to ToArray() func.
 ******************************************************************************
 */
class BinaryReader(stream: InputStream) : AutoCloseable {
    private val reader: DataInputStream
    private val array = ByteArray(8)
    private val buffer = ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN)

    init {
        this.reader = DataInputStream(stream)
    }

    @Throws(IOException::class)
    override fun close() {
        reader.close()
    }

    @Throws(IOException::class)
    fun read(buffer: ByteArray) {
        reader.readFully(buffer)
    }

    @Throws(IOException::class)
    fun read(buffer: ByteArray, index: Int, length: Int) {
        reader.readFully(buffer, index, length)
    }

    @Throws(IOException::class)
    fun readBoolean(): Boolean {
        return reader.readBoolean()
    }

    @Throws(IOException::class)
    fun readByte(): Byte {
        return reader.readByte()
    }

    @Throws(IOException::class)
    fun readBytes(count: Int): ByteArray {
        val buffer = ByteArray(count)
        reader.readFully(buffer)
        return buffer
    }

    @Throws(IOException::class)
    fun readDouble(): Double {
        reader.readFully(array, 0, 8)
        return buffer.getDouble(0)
    }

    @Throws(IOException::class)
    fun readECPoint(): ECPoint {
        val encoded: ByteArray
        val fb = reader.readByte()
        when (fb) {
            0x00 -> encoded = ByteArray(1)
            0x02, 0x03 -> {
                encoded = ByteArray(33)
                encoded[0] = fb
                reader.readFully(encoded, 1, 32)
            }
            0x04 -> {
                encoded = ByteArray(65)
                encoded[0] = fb
                reader.readFully(encoded, 1, 64)
            }
            else -> throw IOException(ErrorCode.ParamError)
        }
        return ECC.secp256r1.curve.decodePoint(encoded)
    }

    @Throws(IOException::class)
    fun readFixedString(length: Int): String {
        val data = readBytes(length)
        var count = -1
        while (data[++count].toInt() != 0);
        return String(data, 0, count, "UTF-8")
    }

    @Throws(IOException::class)
    fun readFloat(): Float {
        reader.readFully(array, 0, 4)
        return buffer.getFloat(0)
    }

    @Throws(IOException::class)
    fun readInt(): Int {
        reader.readFully(array, 0, 4)
        return buffer.getInt(0)
    }

    @Throws(IOException::class)
    fun readLong(): Long {
        reader.readFully(array, 0, 8)
        return buffer.getLong(0)
    }

    @Throws(InstantiationException::class, IllegalAccessException::class, IOException::class)
    fun <T : Serializable> readSerializable(t: Class<T>): T {
        val obj = t.newInstance()
        obj.deserialize(this)
        return obj
    }

    @Throws(InstantiationException::class, IllegalAccessException::class, IOException::class)
    fun <T : Serializable> readSerializableArray(t: Class<T>): Array<T> {
        val array = Array.newInstance(t, readVarInt(0x10000000).toInt()) as Array<T>
        for (i in array.indices) {
            array[i] = t.newInstance()
            array[i].deserialize(this)
        }
        return array
    }

    @Throws(IOException::class)
    fun readShort(): Short {
        reader.readFully(array, 0, 2)
        return buffer.getShort(0)
    }

    @Throws(IOException::class)
    fun readVarBytes2(): ByteArray {
        return readBytes(readVarInt2(0X7fffffc7).toInt())
    }

    @Throws(IOException::class)
    @JvmOverloads
    fun readVarBytes(max: Int = 0X7fffffc7): ByteArray {
        return readBytes(readVarInt(max.toLong()).toInt())
    }

    @Throws(IOException::class)
    @JvmOverloads
    fun readVarInt(max: Long = java.lang.Long.MAX_VALUE): Long {
        val fb = java.lang.Byte.toUnsignedLong(readByte())
        val value: Long
        if (fb == 0xFDL) {
            value = java.lang.Short.toUnsignedLong(readShort())
        } else if (fb == 0xFEL) {
            value = Integer.toUnsignedLong(readInt())
        } else if (fb == 0xFFL) {
            value = readLong()
        } else {
            value = fb
        }
        if (java.lang.Long.compareUnsigned(value, max) > 0) {
            throw IOException(ErrorCode.ParamError)
        }
        return value
    }

    @Throws(IOException::class)
    fun readVarInt2(max: Long): Long {
        val fb = java.lang.Byte.toUnsignedLong(readByte())
        val value: Long
        if (fb == ScriptOp.OP_PUSHDATA1.byte.toLong()) {
            value = java.lang.Byte.toUnsignedLong(readByte())
        } else if (fb == ScriptOp.OP_PUSHDATA2.byte.toLong()) {
            value = java.lang.Short.toUnsignedLong(readShort())
        } else if (fb == ScriptOp.OP_PUSHDATA4.byte.toLong()) {
            value = Integer.toUnsignedLong(readInt())
        } else {
            value = fb
        }
        if (java.lang.Long.compareUnsigned(value, max) > 0) {
            throw IOException(ErrorCode.ParamError)
        }
        return value
    }

    @Throws(IOException::class)
    fun readVarString(): String {
        return String(readVarBytes(), "UTF-8")
    }

    @Throws(IOException::class)
    fun available(): Int {
        return reader.available()
    }

    @Throws(IOException::class)
    fun Seek(n: Long): Long {
        reader.reset()
        return reader.skip(n)
    }
}
