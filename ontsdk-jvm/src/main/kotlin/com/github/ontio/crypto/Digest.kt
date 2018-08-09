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

package com.github.ontio.crypto

import java.security.*

import org.bouncycastle.jce.provider.BouncyCastleProvider

object Digest {
    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun hash160(value: ByteArray): ByteArray {
        return ripemd160(sha256(value))
    }

    fun hash256(value: ByteArray): ByteArray {
        return sha256(sha256(value))
    }

    fun ripemd160(value: ByteArray): ByteArray {
        try {
            val md = MessageDigest.getInstance("RipeMD160")
            return md.digest(value)
        } catch (ex: NoSuchAlgorithmException) {
            throw RuntimeException(ex)
        }

    }

    fun sha256(value: ByteArray): ByteArray {
        try {
            val md = MessageDigest.getInstance("SHA-256")
            return md.digest(value)
        } catch (ex: NoSuchAlgorithmException) {
            throw RuntimeException(ex)
        }

    }

    fun sha256(value: ByteArray, offset: Int, length: Int): ByteArray {
        var value = value
        if (offset != 0 || length != value.size) {
            val array = ByteArray(length)
            System.arraycopy(value, offset, array, 0, length)
            value = array
        }
        return sha256(value)
    }
}
