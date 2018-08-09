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
import java.nio.*

import org.bouncycastle.math.ec.ECPoint

class BinaryWriter(stream: OutputStream) : AutoCloseable {
    private val writer: DataOutputStream
    private val array = ByteArray(8)
    private val buffer = ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN)

    init {
        this.writer = DataOutputStream(stream)
    }

    @Throws(IOException::class)
    override fun close() {
        writer.close()
    }

    @Throws(IOException::class)
    fun flush() {
        writer.flush()
    }

    @Throws(IOException::class)
    fun write(buffer: ByteArray) {
        writer.write(buffer)
    }

    @Throws(IOException::class)
    fun write(buffer: ByteArray, index: Int, length: Int) {
        writer.write(buffer, index, length)
    }

    @Throws(IOException::class)
    fun writeBoolean(v: Boolean) {
        writer.writeBoolean(v)
    }

    @Throws(IOException::class)
    fun writeByte(v: Byte) {
        writer.writeByte(v.toInt())
    }

    @Throws(IOException::class)
    fun writeDouble(v: Double) {
        buffer.putDouble(0, v)
        writer.write(array, 0, 8)
    }

    @Throws(IOException::class)
    fun writeECPoint(v: ECPoint) {
        writer.write(v.getEncoded(true))
    }

    @Throws(IOException::class)
    fun writeFixedString(v: String?, length: Int) {
        if (v == null) {
            throw IllegalArgumentException()
        }
        if (v.length > length) {
            throw IllegalArgumentException()
        }
        val bytes = v.toByteArray(charset("UTF-8"))
        if (bytes.size > length) {
            throw IllegalArgumentException()
        }
        writer.write(bytes)
        if (bytes.size < length) {
            writer.write(ByteArray(length - bytes.size))
        }
    }

    @Throws(IOException::class)
    fun writeFloat(v: Float) {
        buffer.putFloat(0, v)
        writer.write(array, 0, 4)
    }

    @Throws(IOException::class)
    fun writeInt(v: Int) {
        buffer.putInt(0, v)
        writer.write(array, 0, 4)
    }

    @Throws(IOException::class)
    fun writeLong(v: Long) {
        buffer.putLong(0, v)
        writer.write(array, 0, 8)
    }

    @Throws(IOException::class)
    fun writeSerializable(v: Serializable) {
        v.serialize(this)
    }

    @Throws(IOException::class)
    fun writeSerializableArray(v: Array<Serializable>) {
        writeVarInt(v.size.toLong())
        for (i in v.indices) {
            v[i].serialize(this)
        }
    }

    @Throws(IOException::class)
    fun writeSerializableArray2(v: Array<Serializable>) {
        writeInt(v.size)
        for (i in v.indices) {
            v[i].serialize(this)
        }
    }

    @Throws(IOException::class)
    fun writeShort(v: Short) {
        buffer.putShort(0, v)
        writer.write(array, 0, 2)
    }

    @Throws(IOException::class)
    fun writeVarBytes(v: ByteArray) {
        writeVarInt(v.size.toLong())
        writer.write(v)
    }

    @Throws(IOException::class)
    fun writeVarInt(v: Long) {
        if (v < 0) {
            throw IllegalArgumentException()
        }
        if (v < 0xFD) {
            writeByte(v.toByte())
        } else if (v <= 0xFFFF) {
            writeByte(0xFD.toByte())
            writeShort(v.toShort())
        } else if (v <= 0xFFFFFFFFL) {
            writeByte(0xFE.toByte())
            writeInt(v.toInt())
        } else {
            writeByte(0xFF.toByte())
            writeLong(v)
        }
    }

    @Throws(IOException::class)
    fun writeVarString(v: String) {
        writeVarBytes(v.toByteArray(charset("UTF-8")))
    }
}
