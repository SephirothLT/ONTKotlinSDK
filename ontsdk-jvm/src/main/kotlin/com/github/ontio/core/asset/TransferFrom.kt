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
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable

import java.io.IOException
import java.math.BigInteger
import java.util.HashMap

/**
 *
 */
class TransferFrom(var sender: Address, var from: Address, var to: Address, var value: Long) : Serializable {
    @Throws(IOException::class)
    fun deserialize(reader: BinaryReader) {
        try {
            sender = reader.readSerializable(Address::class.java)
            from = reader.readSerializable(Address::class.java)
            to = reader.readSerializable(Address::class.java)
            value = reader.readVarInt()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    fun serialize(writer: BinaryWriter) {
        writer.writeSerializable(sender)
        writer.writeSerializable(from)
        writer.writeSerializable(to)
        writer.writeVarInt(value)

    }


    fun json(): Any {
        val json = HashMap<Any, Any>()
        json["sender"] = sender.toHexString()
        json["from"] = from.toHexString()
        json["to"] = to.toHexString()
        json["value"] = value
        return json
    }

}
