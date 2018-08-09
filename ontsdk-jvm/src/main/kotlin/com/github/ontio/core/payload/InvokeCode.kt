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

import com.github.ontio.common.Helper
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.core.transaction.TransactionType
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.common.Address
import org.bouncycastle.math.ec.ECPoint

import java.io.IOException
import java.util.Arrays
import java.util.HashMap
import java.util.HashSet

class InvokeCode : Transaction(TransactionType.InvokeCode) {
    var code: ByteArray

    val addressU160ForVerifying: Array<Address>?
        get() = null

    @Throws(IOException::class)
    protected fun deserializeExclusiveData(reader: BinaryReader) {
        try {
            code = reader.readVarBytes()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    protected fun serializeExclusiveData(writer: BinaryWriter) {
        writer.writeVarBytes(code)
    }

    fun json(): Any {
        val obj = super.json() as Map<*, *>
        val payload = HashMap()
        payload.put("Code", Helper.toHexString(code))
        obj.put("Payload", payload)
        return obj
    }
}
