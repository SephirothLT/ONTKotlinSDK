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

package com.github.ontio.core.ontid

import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable

import java.io.IOException
import java.io.Serializable

class Attribute :Serializable {
    var key: ByteArray
    var valueType: ByteArray
    var value: ByteArray

    constructor() {}
    constructor(key: ByteArray, valueType: ByteArray, value: ByteArray) {
        this.key = key
        this.valueType = valueType
        this.value = value
    }

    @Throws(IOException::class)
    fun deserialize(reader: BinaryReader) {
        this.key = reader.readVarBytes()
        this.valueType = reader.readVarBytes()
        this.value = reader.readVarBytes()
    }

    @Throws(IOException::class)
    fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(key)
        writer.writeVarBytes(valueType)
        writer.writeVarBytes(value)
    }
}
