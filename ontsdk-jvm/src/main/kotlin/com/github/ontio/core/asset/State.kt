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

package com.github.ontio.core.asset

import com.github.ontio.common.Address
import com.github.ontio.common.Helper
import com.github.ontio.crypto.Digest
import com.github.ontio.io.*

import java.io.ByteArrayInputStream
import java.io.IOException
import java.math.BigInteger
import java.util.HashMap

/**
 *
 */
class State : Serializable {
    var from: Address
    var to: Address
    var value: Long = 0

    constructor() {}
    constructor(from: Address, to: Address, amount: Long) {
        this.from = from
        this.to = to
        this.value = amount
    }

    @Throws(IOException::class)
    fun deserialize(reader: BinaryReader) {
        from = Address(reader.readVarBytes())
        to = Address(reader.readVarBytes())
        value = Helper.BigIntFromBytes(reader.readVarBytes()).toLong()
    }

    @Throws(IOException::class)
    fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(from.toArray())
        writer.writeVarBytes(to.toArray())
        writer.writeVarBytes(Helper.BigInt2Bytes(BigInteger.valueOf(value)))
    }

    fun json(): Any {
        val json = HashMap<Any, Any>()
        json["from"] = from.toHexString()
        json["to"] = to.toHexString()
        json["value"] = value
        return json
    }

    companion object {

        @Throws(IOException::class)
        fun deserializeFrom(value: ByteArray): State {
            try {
                val offset = 0
                val ms = ByteArrayInputStream(value, offset, value.size - offset)
                val reader = BinaryReader(ms)
                val state = State()
                state.deserialize(reader)
                return state
            } catch (ex: IOException) {
                throw IOException(ex)
            }

        }
    }

}
