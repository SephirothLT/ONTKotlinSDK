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

package com.github.ontio.core.program

import com.github.ontio.common.Common
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.scripts.ScriptBuilder
import com.github.ontio.core.scripts.ScriptOp
import com.github.ontio.crypto.ECC
import com.github.ontio.crypto.KeyType
import com.github.ontio.io.BinaryReader
import com.github.ontio.sdk.exception.SDKException

import java.io.ByteArrayInputStream
import java.io.IOException
import java.math.BigInteger
import java.util.ArrayList
import java.util.Arrays

/**
 *
 */
object Program {
    @Throws(IOException::class)
    fun ProgramFromParams(sigData: Array<ByteArray>): ByteArray {
        var sigData = sigData
        val sb = ScriptBuilder()
        sigData = Arrays.stream(sigData).sorted { o1, o2 -> Helper.toHexString(o1).compareTo(Helper.toHexString(o2)) }.toArray(byte[][]::new  /* Currently unsupported in Kotlin */)
        for (sig in sigData) {
            sb.push(sig)
        }
        return sb.toArray()
    }

    @Throws(Exception::class)
    fun ProgramFromPubKey(publicKey: ByteArray): ByteArray {
        val sb = ScriptBuilder()
        sb.push(publicKey)
        sb.add(ScriptOp.OP_CHECKSIG)
        return sb.toArray()
    }

    @Throws(Exception::class)
    fun ProgramFromMultiPubKey(m: Int, vararg publicKeys: ByteArray): ByteArray {
        var publicKeys = publicKeys
        val n = publicKeys.size

        if (m <= 0 || m > n || n > Common.MULTI_SIG_MAX_PUBKEY_SIZE) {
            throw SDKException(ErrorCode.ParamError)
        }
        ScriptBuilder().use({ sb ->
            sb.push(BigInteger.valueOf(m.toLong()))
            publicKeys = sortPublicKeys(*publicKeys)
            for (publicKey in publicKeys) {
                sb.push(publicKey)
            }
            sb.push(BigInteger.valueOf(publicKeys.size.toLong()))
            sb.add(ScriptOp.OP_CHECKMULTISIG)
            return sb.toArray()
        })
    }

    fun sortPublicKeys(vararg publicKeys: ByteArray): Array<ByteArray> {
        var publicKeys = publicKeys
        publicKeys = Arrays.stream(publicKeys).sorted { o1, o2 ->
            if (KeyType.fromPubkey(o1).getLabel() !== KeyType.fromPubkey(o2).getLabel()) {
                return@Arrays.stream(publicKeys).sorted if (KeyType.fromPubkey(o1).getLabel() > KeyType.fromPubkey(o2).getLabel()) 1 else -1
            }
            when (KeyType.fromPubkey(o1)) {
                KeyType.SM2 -> {
                    val p = ByteArray(33)
                    System.arraycopy(o1, 2, p, 0, p.size)
                    o1 = p
                    val p2 = ByteArray(33)
                    System.arraycopy(o2, 2, p2, 0, p2.size)
                    o2 = p2
                    val smPk1 = ECC.sm2p256v1.getCurve().decodePoint(o1)
                    val smPk2 = ECC.sm2p256v1.getCurve().decodePoint(o2)
                    return@Arrays.stream(publicKeys).sorted ECC . compare smPk1, smPk2)
                }
                KeyType.ECDSA -> {
                    val pk1 = ECC.secp256r1.getCurve().decodePoint(o1)
                    val pk2 = ECC.secp256r1.getCurve().decodePoint(o2)
                    return@Arrays.stream(publicKeys).sorted ECC.compare pk1, pk2)
                }
                KeyType.EDDSA ->
                    //TODO
                    return@Arrays.stream(publicKeys).sorted Helper . toHexString o1.compareTo(Helper.toHexString(o1))
                else -> return@Arrays.stream(publicKeys).sorted Helper . toHexString o1.compareTo(Helper.toHexString(o1))
            }
        }.toArray(byte[][]::new  /* Currently unsupported in Kotlin */)
        return publicKeys
    }

    fun getParamInfo(program: ByteArray): Array<ByteArray> {
        val bais = ByteArrayInputStream(program)
        val br = BinaryReader(bais)
        val list = ArrayList()
        while (true) {
            try {
                list.add(readBytes(br))
            } catch (e: IOException) {
                break
            }

        }
        val res = arrayOfNulls<ByteArray>(list.size)
        for (i in list.indices) {
            res[i] = list.get(i)
        }
        return res
    }

    @Throws(IOException::class)
    fun readBytes(br: BinaryReader): ByteArray {

        val code = br.readByte()
        val keyLen: Long
        if (code == ScriptOp.OP_PUSHDATA4.getByte()) {
            val temp: Int
            temp = br.readInt()
            keyLen = java.lang.Long.valueOf(temp.toLong())
        } else if (code == ScriptOp.OP_PUSHDATA2.getByte()) {
            val temp: Int
            temp = br.readShort()
            keyLen = java.lang.Long.valueOf(temp.toLong())
        } else if (code == ScriptOp.OP_PUSHDATA1.getByte()) {
            val temp: Int
            temp = br.readByte()
            keyLen = java.lang.Long.valueOf(temp.toLong())
        } else if (code <= ScriptOp.OP_PUSHBYTES75.getByte() && code >= ScriptOp.OP_PUSHBYTES1.getByte()) {
            keyLen = java.lang.Long.valueOf(code.toLong()) - java.lang.Long.valueOf(ScriptOp.OP_PUSHBYTES1.getByte()) + 1
        } else {
            keyLen = 0
        }
        val res = br.readBytes(keyLen.toInt())
        return br.readBytes(keyLen.toInt())
    }

    @Throws(IOException::class)
    fun getProgramInfo(program: ByteArray): ProgramInfo {
        val info = ProgramInfo()
        if (program.size <= 2) {

        }
        val end = program[program.size - 1]
        val temp = ByteArray(program.size - 1)
        System.arraycopy(program, 0, temp, 0, program.size - 1)
        val bais = ByteArrayInputStream(temp)
        val reader = BinaryReader(bais)
        if (end == ScriptOp.OP_CHECKSIG.getByte()) {
            try {
                val publicKey = readBytes(reader)
                info.setPublicKey(arrayOf(publicKey))
                info.setM(1.toShort())
            } catch (e: IOException) {
                e.printStackTrace()
            }

        } else if (end == ScriptOp.OP_CHECKMULTISIG.getByte()) {
            var m: Short = 0
            val len = program[program.size - 2] - ScriptOp.OP_PUSH1.getByte() + 1
            try {
                m = ((reader.readByte() - ScriptOp.OP_PUSH1.getByte() + 1) as Byte).toShort()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val pub = arrayOfNulls<ByteArray>(len)
            for (i in 0 until len.toInt()) {
                pub[i] = reader.readVarBytes()
            }
            info.setPublicKey(pub)
            info.setM(m)
        }
        return info
    }

    @Throws(IOException::class, SDKException::class)
    fun readNum(reader: BinaryReader): Short {
        val code = readOpCode(reader)
        if (code === ScriptOp.OP_PUSH0) {
            readOpCode(reader)
            return 0
        } else {
            val num = code.getByte() as Int - ScriptOp.OP_PUSH1.getByte() as Int + 1
            if (num >= 1 && num <= 16) {
                readOpCode(reader)
                return num.toShort()
            }
        }
        val buff = readBytes(reader)
        val bint = Helper.BigIntFromBytes(buff)
        val num = bint.toLong()
        if (num > java.lang.Short.MAX_VALUE || num < 16) {
            throw SDKException(ErrorCode.ParamErr("num is wrong"))
        }
        return num.toShort()
    }

    @Throws(IOException::class)
    fun readOpCode(reader: BinaryReader): ScriptOp {
        return ScriptOp.valueOf(reader.readByte())
    }

    fun programFromParams(sigs: Array<ByteArray>): ByteArray {
        val builder = ScriptBuilder()
        for (sigdata in sigs) {
            builder.push(sigdata)
        }
        return builder.toArray()
    }

    fun programFromPubKey(publicKey: ByteArray): ByteArray {
        val builder = ScriptBuilder()
        builder.push(publicKey)
        builder.add(ScriptOp.OP_CHECKSIG)
        return builder.toArray()
    }

    @Throws(SDKException::class)
    fun programFromMultiPubKey(publicKey: Array<ByteArray>, m: Short): ByteArray {
        val n = publicKey.size
        if (m >= 1 && m <= n && n <= 1024) {
            throw SDKException(ErrorCode.ParamErr("m is wrong"))
        }
        val builder = ScriptBuilder()
        builder.pushNum(m)
        builder.add(ScriptOp.OP_CHECKSIG)
        return builder.toArray()
    }
}

