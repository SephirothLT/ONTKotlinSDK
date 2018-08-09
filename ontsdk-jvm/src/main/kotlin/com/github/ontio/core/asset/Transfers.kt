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
import com.github.ontio.crypto.Digest
import com.github.ontio.io.*

import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

/**
 *
 */
class Transfers : Serializable {
    var states: Array<State>

    constructor() {

    }

    constructor(states: Array<State>) {
        this.states = states
    }

    @Throws(IOException::class)
    fun deserialize(reader: BinaryReader) {
        val len = reader.readVarInt() as Int
        states = arrayOfNulls(len)
        for (i in 0 until len) {
            try {
                states[i] = reader.readSerializable(State::class.java)
            } catch (e: InstantiationException) {
                e.printStackTrace()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }

        }
    }

    @Throws(IOException::class)
    fun serialize(writer: BinaryWriter) {
        writer.writeSerializableArray(states)
    }

    fun json(): Any {
        val json = HashMap<Any, Any>()
        val list = ArrayList<Any>()
        for (i in states.indices) {
            list.add(states[i].json())
        }
        json["States"] = list
        return json
    }

    companion object {

        @Throws(IOException::class)
        fun deserializeFrom(value: ByteArray): Transfers {
            try {
                val offset = 0
                val ms = ByteArrayInputStream(value, offset, value.size - offset)
                val reader = BinaryReader(ms)
                val transfers = Transfers()
                transfers.deserialize(reader)
                return transfers
            } catch (ex: IOException) {
                throw IOException(ex)
            }

        }
    }

}
