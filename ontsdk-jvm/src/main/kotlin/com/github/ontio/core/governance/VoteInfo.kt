package com.github.ontio.core.governance


import java.io.IOException
import java.util.HashMap

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


import com.alibaba.fastjson.JSON
import com.github.ontio.common.Address
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable


class VoteInfo : Serializable {
    var peerPubkey: String
    var address: Address
    var consensusPos: Long = 0
    var freezePos: Long = 0
    var newPos: Long = 0
    var withdrawPos: Long = 0
    var withdrawFreezePos: Long = 0
    var withdrawUnfreezePos: Long = 0

    @Throws(IOException::class)
    fun deserialize(reader: BinaryReader) {
        this.peerPubkey = reader.readVarString()
        try {
            this.address = reader.readSerializable(Address::class.java)
            this.consensusPos = reader.readLong()
            this.freezePos = reader.readLong()
            this.newPos = reader.readLong()
            this.withdrawPos = reader.readLong()
            this.withdrawFreezePos = reader.readLong()
            this.withdrawUnfreezePos = reader.readLong()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    fun serialize(writer: BinaryWriter) {

    }

    fun json(): String {
        val map = HashMap()
        map.put("peerPubkey", peerPubkey)
        map.put("address", address.toBase58())
        map.put("consensusPos", consensusPos)
        map.put("freezePos", freezePos)
        map.put("newPos", newPos)
        map.put("withdrawPos", withdrawPos)
        map.put("withdrawFreezePos", withdrawFreezePos)
        map.put("withdrawUnfreezePos", withdrawUnfreezePos)
        return JSON.toJSONString(map)
    }
}
