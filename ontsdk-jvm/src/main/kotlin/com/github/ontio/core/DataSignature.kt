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

import com.github.ontio.account.Account
import com.github.ontio.common.Address
import com.github.ontio.common.ErrorCode
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.sdk.exception.SDKException

import java.io.IOException


class DataSignature : Signable {
    private var account: Account? = null
    var data: ByteArray? = null
        private set
    private var scheme: SignatureScheme? = null

    val addressU160ForVerifying: Array<Address>?
        get() = null

    constructor() {}

    constructor(data: ByteArray) {
        this.data = data
    }

    constructor(scheme: SignatureScheme, acct: Account, data: ByteArray) {
        this.scheme = scheme
        this.account = acct
        this.data = data
    }

    constructor(scheme: SignatureScheme, acct: Account, data: String) {
        this.scheme = scheme
        this.account = acct
        this.data = data.toByteArray()
    }

    fun signature(): ByteArray {
        try {
            val signData = sign(account, scheme)
            return sign(account, scheme)
        } catch (e: Exception) {
            throw RuntimeException(ErrorCode.DataSignatureErr)
        }

    }

    @Throws(Exception::class)
    fun sign(account: Account?, scheme: SignatureScheme?): ByteArray {
        return account!!.generateSignature(getHashData(), scheme, null)
    }

    @Throws(Exception::class)
    fun verifySignature(account: Account, data: ByteArray, signature: ByteArray): Boolean {
        return account.verifySignature(data, signature)
    }

    @Throws(IOException::class)
    fun deserialize(reader: BinaryReader) {
    }

    @Throws(IOException::class)
    fun deserializeUnsigned(reader: BinaryReader) {
    }

    @Throws(IOException::class)
    fun serializeUnsigned(writer: BinaryWriter) {
        writer.write(data)
    }

    @Throws(IOException::class)
    fun serialize(writer: BinaryWriter) {
    }
}
