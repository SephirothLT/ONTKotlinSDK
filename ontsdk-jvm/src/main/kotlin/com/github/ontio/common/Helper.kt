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

import com.github.ontio.crypto.Digest

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import kotlin.experimental.inv

/**
 * Byte Handle Helper
 */
object Helper {
    fun getbyteStr(bs: ByteArray): String {
        val sb = StringBuilder()
        for (b in bs) {
            sb.append(" ").append(java.lang.Byte.toUnsignedInt(b))
        }
        return sb.substring(1)
    }

    fun reverse(v: ByteArray): ByteArray {
        val result = ByteArray(v.size)
        for (i in v.indices) {
            result[i] = v[v.size - i - 1]
        }
        return result
    }

    fun BigInt2Bytes(bi: BigInteger): ByteArray {
        if (bi.toLong() == 0L) {
            return byteArrayOf()
        }
        var bs = bi.toByteArray()
        val b = bs[0]
        if (bi.toLong() < 0) {
            for (i in bs.indices) {
                bs[i] = b.inv().toByte()
            }
            val temp = BigInteger(bs)
            val temp2 = temp.add(BigInteger.valueOf(1))
            bs = temp2.toByteArray()
            val res = reverse(bs)
            if (b shr 7 == 1) {
                val t = ByteArray(res.size + 1)
                System.arraycopy(res, 0, t, 0, res.size)
                t[res.size] = 0.toByte()
                return t
            }
            return res
        } else {
            val res = reverse(bs)
            if (b shr 7 == 1) {
                val t = ByteArray(res.size + 1)
                System.arraycopy(res, 0, t, 0, res.size)
                t[res.size] = 0.toByte()
                return t
            }
            return res
        }
    }

    fun BigIntFromBytes(ba: ByteArray): BigInteger {
        if (ba.size == 0) {
            return BigInteger.valueOf(0)
        }
        val bs = reverse(ba)
        if (bs[0] shr 7 == 1) {
            for (i in bs.indices) {
                bs[i] = bs[i].inv().toByte()
            }
            val temp = BigInteger(bs)
            temp.add(BigInteger.valueOf(1))
            return temp.negate()
        }
        return BigInteger(bs)
    }

    fun hexToBytes(value: String?): ByteArray {
        if (value == null || value.length == 0) {
            return ByteArray(0)
        }
        if (value.length % 2 == 1) {
            throw IllegalArgumentException()
        }
        val result = ByteArray(value.length / 2)
        for (i in result.indices) {
            result[i] = Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16).toByte()
        }
        return result
    }

    fun toHexString(value: ByteArray): String {
        val sb = StringBuilder()
        for (b in value) {
            val v = java.lang.Byte.toUnsignedInt(b)
            sb.append(Integer.toHexString(v.ushr(4)))
            sb.append(Integer.toHexString(v and 0x0f))
        }
        return sb.toString()
    }

    fun reverse(value: String): String {
        return toHexString(reverse(hexToBytes(value)))
    }

    fun removePrevZero(bt: ByteArray): ByteArray {
        return if (bt.size == 33 && bt[0].toInt() == 0) {
            Arrays.copyOfRange(bt, 1, 33)
        } else bt
    }

    fun addBytes(data1: ByteArray, data2: ByteArray): ByteArray {
        val data3 = ByteArray(data1.size + data2.size)
        System.arraycopy(data1, 0, data3, 0, data1.size)
        System.arraycopy(data2, 0, data3, data1.size, data2.size)
        return data3
    }


    fun now(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
    }

    fun toString(map: Map<String, Any>): String {
        val sb = StringBuilder()
        for ((key, value) in map) {
            sb.append("\n").append("$key: $value")
        }
        return sb.toString()
    }

    fun print(map: Map<String, Any>) {
        println(toString(map))
    }
}
