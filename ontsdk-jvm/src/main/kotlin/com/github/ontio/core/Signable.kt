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

package com.github.ontio.core

import java.io.ByteArrayOutputStream
import java.io.IOException
import com.github.ontio.account.Account
import com.github.ontio.crypto.Digest
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable

import com.github.ontio.common.Address


interface Signable : Serializable {

    val addressU160ForVerifying: Array<Address>?

    val hashData: ByteArray
        get() = try {
            ByteArrayOutputStream().use { ms ->
                BinaryWriter(ms).use({ writer ->
                    serializeUnsigned(writer)
                    writer.flush()
                    return ms.toByteArray()
                })
            }
        } catch (ex: IOException) {
            throw UnsupportedOperationException(ex)
        }

    @Throws(IOException::class)
    fun deserializeUnsigned(reader: BinaryReader)

    @Throws(IOException::class)
    fun serializeUnsigned(writer: BinaryWriter)

    @Throws(Exception::class)
    fun sign(account: Account, scheme: SignatureScheme): ByteArray {
        return account.generateSignature(Digest.sha256(Digest.sha256(hashData)), scheme, null)
    }

    @Throws(Exception::class)
    open fun verifySignature(account: Account, data: ByteArray, signature: ByteArray): Boolean {
        return account.verifySignature(Digest.sha256(Digest.sha256(data)), signature)
    }

}
