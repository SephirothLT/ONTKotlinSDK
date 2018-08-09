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

import com.github.ontio.common.ErrorCode
import com.github.ontio.sdk.exception.SDKException

import java.math.BigInteger
import java.util.Arrays

object Base58 {
    /**
     * base58
     */
    val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val BASE = BigInteger.valueOf(ALPHABET.length.toLong())

    /**
     * decode base58
     * @param input
     * @return
     */
    fun decode(input: String): ByteArray {
        var bi = BigInteger.ZERO
        for (i in input.length - 1 downTo 0) {
            val index = ALPHABET.indexOf(input[i].toInt())
            if (index == -1) {
                throw IllegalArgumentException()
            }
            bi = bi.add(BASE.pow(input.length - 1 - i).multiply(BigInteger.valueOf(index.toLong())))
        }
        val bytes = bi.toByteArray()
        val stripSignByte = bytes.size > 1 && bytes[0].toInt() == 0 && bytes[1] < 0
        var leadingZeros = 0
        while (leadingZeros < input.length && input[leadingZeros] == ALPHABET[0]) {
            leadingZeros++
        }
        val tmp = ByteArray(bytes.size - (if (stripSignByte) 1 else 0) + leadingZeros)
        System.arraycopy(bytes, if (stripSignByte) 1 else 0, tmp, leadingZeros, tmp.size - leadingZeros)
        return tmp
    }

    /**
     * encode
     * @param input
     * @return
     */
    fun encode(input: ByteArray): String {
        var value = BigInteger(1, input)
        val sb = StringBuilder()
        while (value.compareTo(BASE) >= 0) {
            val r = value.divideAndRemainder(BASE)
            sb.insert(0, ALPHABET[r[1].toInt()])
            value = r[0]
        }
        sb.insert(0, ALPHABET[value.toInt()])
        for (b in input) {
            if (b.toInt() == 0) {
                sb.insert(0, ALPHABET[0])
            } else {
                break
            }
        }
        return sb.toString()
    }

    fun checkSumEncode(`in`: ByteArray): String {
        val hash = Digest.sha256(Digest.sha256(`in`))
        val checksum = Arrays.copyOfRange(hash, 0, 4)
        val input = ByteArray(`in`.size + 4)
        System.arraycopy(`in`, 0, input, 0, `in`.size)
        System.arraycopy(checksum, 0, input, `in`.size, 4)
        return encode(input)
    }

    @Throws(Exception::class)
    fun decodeChecked(input: String): ByteArray {

        val decoded = decode(input)
        if (decoded.size < 4) {
            throw Exception(ErrorCode.InputTooShort)
        }
        val data = Arrays.copyOfRange(decoded, 0, decoded.size - 4)
        val checksum = Arrays.copyOfRange(decoded, decoded.size - 4, decoded.size)
        val actualChecksum = Arrays.copyOfRange(Digest.sha256(Digest.sha256(data)), 0, 4)
        if (!Arrays.equals(checksum, actualChecksum)) {
            throw SDKException(ErrorCode.ChecksumNotValidate)
        }
        return decoded
    }
}
