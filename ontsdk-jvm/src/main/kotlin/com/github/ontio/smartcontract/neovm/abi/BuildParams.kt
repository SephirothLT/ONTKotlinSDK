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

package com.github.ontio.smartcontract.neovm.abi

import com.alibaba.fastjson.JSON
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.scripts.ScriptBuilder
import com.github.ontio.sdk.exception.SDKException

import java.math.BigInteger
import java.util.ArrayList

object BuildParams {
    enum class Type private constructor(t: Int) {
        ByteArrayType(0x00),
        BooleanType(0x01),
        IntegerType(0x02),
        InterfaceType(0x40),
        ArrayType(0x80),
        StructType(0x81),
        MapType(0x82);

        val value: Byte

        init {
            this.value = t.toByte()
        }
    }

    /**
     * @param builder
     * @param list
     * @return
     */
    private fun createCodeParamsScript(builder: ScriptBuilder, list: List<Any>): ByteArray {
        try {
            for (i in list.indices.reversed()) {
                val `val` = list[i]
                if (`val` is ByteArray) {
                    builder.push(`val`)
                } else if (`val` is Boolean) {
                    builder.push(`val`)
                } else if (`val` is Int) {
                    builder.push(Helper.BigInt2Bytes(BigInteger.valueOf(`val`.toLong())))
                } else if (`val` is Long) {
                    builder.push(Helper.BigInt2Bytes(BigInteger.valueOf(`val`)))
                } else if (`val` is Map<*, *>) {
                    val bys = getMapBytes(`val`)
                    println(Helper.toHexString(bys))
                    builder.push(bys)
                } else if (`val` is Struct) {
                    val bys = getStructBytes(`val`)
                    println(Helper.toHexString(bys))
                    builder.push(bys)
                } else if (`val` is List<*>) {
                    createCodeParamsScript(builder, `val`)
                    builder.push(BigInteger(`val`.size.toString()))
                    builder.pushPack()

                } else {
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return builder.toArray()
    }

    fun getStructBytes(`val`: Any): ByteArray {

        var sb: ScriptBuilder? = null
        try {
            sb = ScriptBuilder()
            val list = (`val` as Struct).list
            sb.add(Type.StructType.value)
            sb.add(Helper.BigInt2Bytes(BigInteger.valueOf(list.size.toLong())))
            for (i in list.indices) {
                if (list.get(i) is ByteArray) {
                    sb.add(Type.ByteArrayType.value)
                    sb.push(list.get(i) as ByteArray)
                } else if (list.get(i) is String) {
                    sb.add(Type.ByteArrayType.value)
                    sb.push((list.get(i) as String).toByteArray())
                } else if (list.get(i) is Int) {
                    sb.add(Type.ByteArrayType.value)
                    sb.push(Helper.BigInt2Bytes(BigInteger.valueOf((list.get(i) as Int).toLong())))
                } else if (list.get(i) is Long) {
                    sb.add(Type.ByteArrayType.value)
                    sb.push(Helper.BigInt2Bytes(BigInteger.valueOf(list.get(i) as Long)))
                } else {
                    throw SDKException(ErrorCode.ParamError)
                }
            }
        } catch (e: SDKException) {
            e.printStackTrace()
        }

        return sb!!.toArray()
    }

    fun getMapBytes(`val`: Any): ByteArray {
        var sb: ScriptBuilder? = null
        try {
            sb = ScriptBuilder()
            val map = `val` as Map<*, *>
            sb.add(Type.MapType.value)
            sb.add(Helper.BigInt2Bytes(BigInteger.valueOf(map.size.toLong())))
            for ((key, value) in map) {
                sb.add(Type.ByteArrayType.value)
                sb.push((key as String).toByteArray())
                if (value is ByteArray) {
                    sb.add(Type.ByteArrayType.value)
                    sb.push(value)
                } else if (value is String) {
                    sb.add(Type.ByteArrayType.value)
                    sb.push(value.toByteArray())
                } else if (value is Int) {
                    sb.add(Type.IntegerType.value)
                    sb.push(Helper.BigInt2Bytes(BigInteger.valueOf(value.toLong())))
                } else if (value is Long) {
                    sb.add(Type.IntegerType.value)
                    sb.push(Helper.BigInt2Bytes(BigInteger.valueOf(value)))
                } else {
                    throw SDKException(ErrorCode.ParamError)
                }
            }
        } catch (e: SDKException) {
            e.printStackTrace()
        }

        return sb!!.toArray()
    }

    /**
     * @param list
     * @return
     */
    fun createCodeParamsScript(list: List<Any>): ByteArray {
        val sb = ScriptBuilder()
        try {
            for (i in list.indices.reversed()) {
                val `val` = list[i]
                if (`val` is ByteArray) {
                    sb.push(`val`)
                } else if (`val` is Boolean) {
                    sb.push(`val`)
                } else if (`val` is Int) {
                    sb.push(Helper.BigInt2Bytes(BigInteger.valueOf(`val`.toLong())))
                } else if (`val` is Long) {
                    sb.push(Helper.BigInt2Bytes(BigInteger.valueOf(`val`)))
                } else if (`val` is BigInteger) {
                    sb.push(`val`)
                } else if (`val` is Map<*, *>) {
                    val bys = getMapBytes(`val`)
                    sb.push(bys)
                } else if (`val` is Struct) {
                    val bys = getStructBytes(`val`)
                    sb.push(bys)
                } else if (`val` is List<*>) {
                    createCodeParamsScript(sb, `val`)
                    sb.push(BigInteger(`val`.size.toString()))
                    sb.pushPack()
                } else {
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return sb.toArray()
    }

    /**
     * @param abiFunction
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun serializeAbiFunction(abiFunction: AbiFunction): ByteArray {
        val list = ArrayList<Any>()
        list.add(abiFunction.name!!.toByteArray())
        val tmp = ArrayList<Any>()
        for (obj in abiFunction.parameters!!) {
            if ("ByteArray" == obj.getType()) {
                tmp.add(JSON.parseObject(obj.getValue(), ByteArray::class.java))
            } else if ("String" == obj.getType()) {
                tmp.add(obj.getValue())
            } else if ("Boolean" == obj.getType()) {
                tmp.add(JSON.parseObject(obj.getValue(), Boolean::class.javaPrimitiveType))
            } else if ("Integer" == obj.getType()) {
                tmp.add(JSON.parseObject(obj.getValue(), Long::class.java))
            } else if ("Array" == obj.getType()) {
                tmp.add(JSON.parseObject(obj.getValue(), List<*>::class.java))
            } else if ("InteropInterface" == obj.getType()) {
                tmp.add(JSON.parseObject(obj.getValue(), Any::class.java))
            } else if ("Void" == obj.getType()) {

            } else if ("Map" == obj.getType()) {
                tmp.add(JSON.parseObject(obj.getValue(), Map<*, *>::class.java))
            } else if ("Struct" == obj.getType()) {
                tmp.add(JSON.parseObject(obj.getValue(), Struct::class.java))
            } else {
                throw SDKException(ErrorCode.TypeError)
            }
        }
        if (list.size > 0) {
            list.add(tmp)
        }
        return createCodeParamsScript(list)
    }
}