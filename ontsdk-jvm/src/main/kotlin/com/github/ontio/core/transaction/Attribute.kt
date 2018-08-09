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

package com.github.ontio.core.transaction

import java.io.IOException
import java.util.Arrays
import java.util.HashMap

import com.github.ontio.common.Helper
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable


class Attribute : Serializable {

    var usage: AttributeUsage
    var data: ByteArray
    var size: Int = 0

    @Throws(IOException::class)
    fun serialize(writer: BinaryWriter) {
        writer.writeByte(usage.value())
        if (usage === AttributeUsage.Script
                || usage === AttributeUsage.DescriptionUrl
                || usage === AttributeUsage.Description
                || usage === AttributeUsage.Nonce) {
            writer.writeVarBytes(data)
        } else {
            throw IOException()
        }
    }

    @Throws(IOException::class)
    fun deserialize(reader: BinaryReader) {
        usage = AttributeUsage.valueOf(reader.readByte())
        if (usage === AttributeUsage.Script
                || usage === AttributeUsage.DescriptionUrl
                || usage === AttributeUsage.Description
                || usage === AttributeUsage.Nonce) {
            data = reader.readVarBytes(255)
        } else {
            throw IOException()
        }
    }

    fun json(): Any {
        val json = HashMap<Any, Any>()
        json["usage"] = usage.value()
        json["data"] = Helper.toHexString(data)
        return json
    }

    override fun toString(): String {
        return ("TransactionAttribute [usage=" + usage + ", data="
                + Arrays.toString(data) + "]")
    }

    //	@Override
    //	public void fromJson(JsonReader reader) {
    //		JObject json = reader.json();
    //		usage = TransactionAttributeUsage.valueOf((byte)json.get("Usage").asNumber());
    //		data = Helper.hexToBytes(json.get("Data").asString());
    //	}
}
