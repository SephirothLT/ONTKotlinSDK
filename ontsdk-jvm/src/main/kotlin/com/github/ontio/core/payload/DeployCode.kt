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

package com.github.ontio.core.payload

import java.io.IOException

import com.github.ontio.common.Address
import com.github.ontio.core.transaction.TransactionType
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter


class DeployCode : Transaction(TransactionType.DeployCode) {
    var code: ByteArray
    var needStorage: Boolean = false
    var name: String
    var version: String
    var author: String
    var email: String
    var description: String

    val addressU160ForVerifying: Array<Address>?
        get() = null

    @Throws(IOException::class)
    protected fun deserializeExclusiveData(reader: BinaryReader) {
        try {
            code = reader.readVarBytes()
            needStorage = reader.readBoolean()
            name = reader.readVarString()
            version = reader.readVarString()
            author = reader.readVarString()
            email = reader.readVarString()
            description = reader.readVarString()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    protected fun serializeExclusiveData(writer: BinaryWriter) {
        writer.writeVarBytes(code)
        writer.writeBoolean(needStorage)
        writer.writeVarString(name)
        writer.writeVarString(version)
        writer.writeVarString(author)
        writer.writeVarString(email)
        writer.writeVarString(description)
    }
}
