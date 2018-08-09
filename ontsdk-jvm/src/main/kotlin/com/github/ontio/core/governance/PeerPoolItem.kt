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

package com.github.ontio.core.governance

import com.github.ontio.common.Address
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable

import java.io.IOException
import java.util.HashMap

class PeerPoolItem : Serializable {
    var index: Int = 0
    var peerPubkey: String
    var address: Address
    var status: Int = 0
    var initPos: Long = 0
    var totalPos: Long = 0

    constructor() {}
    constructor(index: Int, peerPubkey: String, address: Address, status: Int, initPos: Long, totalPos: Long) {
        this.index = index
        this.peerPubkey = peerPubkey
        this.address = address
        this.status = status
        this.initPos = initPos
        this.totalPos = totalPos
    }

    @Throws(IOException::class)
    fun deserialize(reader: BinaryReader) {
        this.index = reader.readInt()
        this.peerPubkey = reader.readVarString()
        try {
            this.address = reader.readSerializable(Address::class.java)
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

        this.status = reader.readByte()
        this.initPos = reader.readLong()
        this.totalPos = reader.readLong()
    }

    @Throws(IOException::class)
    fun serialize(writer: BinaryWriter) {
        writer.writeInt(index)
        writer.writeVarString(peerPubkey)
        writer.writeSerializable(address)
        writer.writeByte(status.toByte())
        writer.writeLong(initPos)
        writer.writeLong(totalPos)
    }

    fun Json(): Any {
        val map = HashMap()
        map.put("index", index)
        map.put("peerPubkey", peerPubkey)
        map.put("address", address.toBase58())
        map.put("status", status)
        map.put("initPos", initPos)
        map.put("totalPos", totalPos)
        return map
    }
}
