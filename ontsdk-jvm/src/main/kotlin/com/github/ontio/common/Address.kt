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

package com.github.ontio.common

import com.github.ontio.core.program.Program
import com.github.ontio.core.scripts.ScriptBuilder
import com.github.ontio.core.scripts.ScriptOp
import com.github.ontio.crypto.Base58
import com.github.ontio.crypto.Digest
import com.github.ontio.crypto.ECC
import com.github.ontio.crypto.KeyType
import com.github.ontio.io.BinaryWriter
import com.github.ontio.sdk.exception.SDKException

import com.github.ontio.crypto.KeyType.ECDSA
import com.github.ontio.crypto.KeyType.SM2

/**
 * Custom type which inherits base class defines 20-bit data,
 * it mostly used to defined contract address
 */
class Address @JvmOverloads constructor(value: ByteArray? = null) : UIntBase(20, value), Comparable<Address> {

    override fun compareTo(other: Address): Int {
        val x = this.data_bytes
        val y = other.data_bytes
        for (i in x.indices.reversed()) {
            val r = java.lang.Byte.toUnsignedInt(x[i]) - java.lang.Byte.toUnsignedInt(y[i])
            if (r != 0) {
                return r
            }
        }
        return 0
    }

    fun toBase58(): String {
        val data = ByteArray(25)
        data[0] = COIN_VERSION
        System.arraycopy(toArray(), 0, data, 1, 20)
        val checksum = Digest.sha256(Digest.sha256(data, 0, 21))
        System.arraycopy(checksum, 0, data, 21, 4)
        return Base58.encode(data)
    }

    companion object {
        val ZERO = Address()
        val COIN_VERSION: Byte = 0x17


        fun parse(value: String?): Address {
            var value: String? = value ?: throw NullPointerException()
            if (value!!.startsWith("0x")) {
                value = value.substring(2)
            }
            if (value.length != 40) {
                throw IllegalArgumentException()
            }
            val v = Helper.hexToBytes(value)
            return Address(v)
            //        return new UInt160(Helper.reverse(v));
        }

        fun tryParse(s: String, result: Address): Boolean {
            try {
                val v = parse(s)
                result.data_bytes = v.data_bytes
                return true
            } catch (e: Exception) {
                return false
            }

        }

        fun AddressFromVmCode(codeHexStr: String): Address {
            val code = Address.toScriptHash(Helper.hexToBytes(codeHexStr))
            return toScriptHash(Helper.hexToBytes(codeHexStr))
        }

        fun addressFromPubKey(publicKey: String): Address {
            return addressFromPubKey(Helper.hexToBytes(publicKey))
        }

        fun addressFromPubKey(publicKey: ByteArray): Address {
            val sb = ScriptBuilder()
            sb.push(publicKey)
            sb.add(ScriptOp.OP_CHECKSIG)
            return Address.toScriptHash(sb.toArray())
        }

        @Throws(Exception::class)
        fun addressFromMultiPubKeys(m: Int, vararg publicKeys: ByteArray): Address {
            return Address.toScriptHash(Program.ProgramFromMultiPubKey(m, publicKeys))
        }

        @Throws(SDKException::class)
        fun decodeBase58(address: String): Address {
            val data = Base58.decode(address)
            if (data.size != 25) {
                throw SDKException(ErrorCode.ParamError)
            }
            if (data[0] != COIN_VERSION) {
                throw SDKException(ErrorCode.ParamError)
            }
            val checksum = Digest.sha256(Digest.sha256(data, 0, 21))
            for (i in 0..3) {
                if (data[data.size - 4 + i] != checksum[i]) {
                    throw SDKException(ErrorCode.ParamError)
                }
            }
            val buffer = ByteArray(20)
            System.arraycopy(data, 1, buffer, 0, 20)
            return Address(buffer)
        }

        fun toScriptHash(script: ByteArray): Address {
            return Address(Digest.hash160(script))
        }
    }

}