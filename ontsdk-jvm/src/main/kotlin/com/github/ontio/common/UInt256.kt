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

import com.alibaba.fastjson.JSON

/**
 * Custom type which inherits base class defines 32-bit data,
 * it mostly used to defined transaction identity
 */
class UInt256 @JvmOverloads constructor(value: ByteArray? = null) : UIntBase(32, value), Comparable<UInt256> {

    override fun compareTo(other: UInt256): Int {
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

    companion object {
        val ZERO = UInt256()


        fun parse(s: String?): UInt256 {
            var s: String? = s ?: throw NullPointerException()
            if (s!!.startsWith("0x")) {
                s = s.substring(2)
            }
            if (s.length != 64) {
                throw IllegalArgumentException(ErrorCode.ParamLengthErr)
            }
            val v = Helper.hexToBytes(s)
            //        return new UInt256(v);
            return UInt256(Helper.reverse(v))
        }

        fun tryParse(s: String, result: UInt256): Boolean {
            try {
                val v = parse(s)
                result.data_bytes = v.data_bytes
                return true
            } catch (e: Exception) {
                return false
            }

        }
    }
}