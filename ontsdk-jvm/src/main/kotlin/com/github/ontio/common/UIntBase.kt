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

package com.github.ontio.common

import java.io.IOException
import java.nio.*
import java.util.Arrays

import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable
import com.github.ontio.io.*

/**
 * Custom type base abstract class, it defines the storage and the serialization
 * and deserialization of actual data
 */
abstract class UIntBase protected constructor(bytes: Int, value: ByteArray?) : Serializable {
    protected var data_bytes: ByteArray

    init {
        if (value == null) {
            this.data_bytes = ByteArray(bytes)

        }
        if (value!!.size != bytes) {
            throw IllegalArgumentException()
        }
        this.data_bytes = value
    }

    override fun equals(obj: Any?): Boolean {
        if (obj == null) {
            return false
        }
        if (obj === this) {
            return true
        }
        if (obj !is UIntBase) {
            return false
        }
        val other = obj as UIntBase?
        return Arrays.equals(this.data_bytes, other!!.data_bytes)
    }

    override fun hashCode(): Int {
        return ByteBuffer.wrap(data_bytes).order(ByteOrder.LITTLE_ENDIAN).int
    }

    override fun toArray(): ByteArray {
        return data_bytes
    }

    /**
     * 转为16进制字符串
     *
     * @return 返回16进制字符串
     */
    override fun toString(): String {
        // return Helper.toHexString(data_bytes);
        return Helper.toHexString(Helper.reverse(data_bytes))
    }

    override fun toHexString(): String {
        return Helper.reverse(Helper.toHexString(toArray()))
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.write(data_bytes)
    }

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
        reader.read(data_bytes)
    }
}
