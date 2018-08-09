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

import java.io.IOException
import java.math.BigDecimal

import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable

/**
 *
 */
class Fixed8 @JvmOverloads constructor(data: Long = 0) : Comparable<Fixed8>, Serializable {
    var data: Long = 0
        private set

    init {
        this.data = data
    }

    fun abs(): Fixed8 {
        return if (data >= 0) {
            this
        } else Fixed8(-data)
    }

    override fun compareTo(other: Fixed8): Int {
        return java.lang.Long.compare(data, other.data)
    }

    override fun equals(obj: Any?): Boolean {
        return if (obj !is Fixed8) {
            false
        } else data == obj.data
    }

    override fun hashCode(): Int {
        return java.lang.Long.valueOf(data).hashCode()
    }


    override fun toString(): String {
        var v = BigDecimal(data)
        v = v.divide(BigDecimal(D), DefaultPrecision, BigDecimal.ROUND_UNNECESSARY)
        return v.toPlainString()
    }


    fun toLong(): Long {
        return data / D
    }

    fun multiply(other: Long): Fixed8 {
        return Fixed8(data * other)
    }

    fun divide(other: Long): Fixed8 {
        return Fixed8(data / other)
    }

    fun add(other: Fixed8): Fixed8 {
        return Fixed8(Math.addExact(this.data, other.data))
    }

    fun subtract(other: Fixed8): Fixed8 {
        return Fixed8(Math.subtractExact(this.data, other.data))
    }

    fun negate(): Fixed8 {
        return Fixed8(-data)
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeLong(data)
    }

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
        data = reader.readLong()
    }

    companion object {


        val MAX_VALUE = Fixed8(java.lang.Long.MAX_VALUE)

        val MIN_VALUE = Fixed8(java.lang.Long.MIN_VALUE)


        val SATOSHI = Fixed8(1)

        val ZERO = Fixed8(0)
        var DefaultPrecision = 8
        private val D = Math.pow(10.0, DefaultPrecision.toDouble()).toLong()
        val ONE = Fixed8(D)

        fun fromDecimal(`val`: BigDecimal): Fixed8 {
            return Fixed8(`val`.multiply(BigDecimal(D)).longValueExact())
        }

        fun fromLong(`val`: Long): Fixed8 {
            if (`val` < 0 || `val` > java.lang.Long.MAX_VALUE / D) {
                throw IllegalArgumentException()
            }
            return Fixed8(`val` * D)
        }

        fun parse(s: String): Fixed8 {
            return fromDecimal(BigDecimal(s))
        }

        fun max(first: Fixed8, vararg others: Fixed8): Fixed8 {
            var first = first
            for (other in others) {
                if (first.compareTo(other) < 0) {
                    first = other
                }
            }
            return first
        }

        fun min(first: Fixed8, vararg others: Fixed8): Fixed8 {
            var first = first
            for (other in others) {
                if (first.compareTo(other) > 0) {
                    first = other
                }
            }
            return first
        }

//        fun sum(values: Array<Fixed8>): Fixed8 {
//            return sum(values, { p -> p })
//        }

//        fun <T> sum(values: Array<T>, selector: (Any) -> ?): Fixed8 {
//            var sum = Fixed8.ZERO
//            for (item in values) {
//                sum = sum.add(selector.apply(item))
//            }
//            return sum
//        }

        fun tryParse(s: String, result: Fixed8): Boolean {
            try {
                val `val` = BigDecimal(s)
                result.data = `val`.longValueExact()
                return true
            } catch (ex: NumberFormatException) {
                return false
            } catch (ex: ArithmeticException) {
                return false
            }

        }
    }
}