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

package com.github.ontio.smartcontract.nativevm.abi

import com.alibaba.fastjson.JSON
import com.github.ontio.common.Address
import com.github.ontio.common.ErrorCode
import com.github.ontio.core.ontid.Attribute
import com.github.ontio.core.scripts.ScriptBuilder
import com.github.ontio.core.scripts.ScriptOp
import com.github.ontio.io.BinaryWriter
import com.github.ontio.sdk.exception.SDKException

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.reflect.Array
import java.math.BigInteger
import java.util.ArrayList

object NativeBuildParams {
    @Throws(SDKException::class)
    fun buildParams(vararg params: Any): ByteArray {
        val baos = ByteArrayOutputStream()
        val bw = BinaryWriter(baos)
        try {
            for (param in params) {
                if (param is Int) {
                    bw.writeInt(param.toInt())
                } else if (param is ByteArray) {
                    bw.writeVarBytes(param)
                } else if (param is String) {
                    bw.writeVarString(param)
                } else if (param is Array<Attribute>) {
                    bw.writeSerializableArray(param as Array<Attribute>)
                } else if (param is Attribute) {
                    bw.writeSerializable(param)
                } else if (param is Address) {
                    bw.writeSerializable(param)
                } else {
                    throw SDKException(ErrorCode.WriteVarBytesError)
                }
            }
        } catch (e: IOException) {
            throw SDKException(ErrorCode.WriteVarBytesError)
        }

        return baos.toByteArray()
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
                    builder.push(BigInteger.valueOf(`val`.toLong()))
                } else if (`val` is Long) {
                    builder.push(BigInteger.valueOf(`val`))
                } else if (`val` is Address) {
                    builder.push(`val`.toArray())
                } else if (`val` is String) {
                    builder.push(`val`.toByteArray())
                } else if (`val` is List<*>) {
                    createCodeParamsScript(builder, `val`)
                    builder.push(BigInteger(`val`.size.toString()))
                    builder.pushPack()

                } else {
                    throw SDKException(ErrorCode.OtherError("not this type"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return builder.toArray()
    }

    private fun createCodeParamsScript(builder: ScriptBuilder, obj: Any): ByteArray {
        try {
            if (obj is ByteArray) {
                builder.push(obj)
            } else if (obj is Boolean) {
                builder.push(obj)
            } else if (obj is Int) {
                builder.push(BigInteger.valueOf(obj.toLong()))
            } else if (obj is Long) {
                builder.push(BigInteger.valueOf(obj))
            } else if (obj is Address) {
                builder.push(obj.toArray())
            } else if (obj is String) {
                builder.push(obj.toByteArray())
            } else if (obj is Struct) {
                for (k in 0 until (obj as Struct).list.size()) {
                    val o = (obj as Struct).list.get(k)
                    createCodeParamsScript(builder, o)
                    builder.add(ScriptOp.OP_DUPFROMALTSTACK)
                    builder.add(ScriptOp.OP_SWAP)
                    builder.add(ScriptOp.OP_APPEND)
                }
            } else {
                throw SDKException(ErrorCode.OtherError("not this type"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return builder.toArray()
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
                    sb.push(BigInteger.valueOf(`val`.toLong()))
                } else if (`val` is Long) {
                    sb.push(BigInteger.valueOf(`val`))
                } else if (`val` is BigInteger) {
                    sb.push(`val`)
                } else if (`val` is Address) {
                    sb.push(`val`.toArray())
                } else if (`val` is String) {
                    sb.push(`val`.toByteArray())
                } else if (`val` is Struct) {
                    sb.push(BigInteger.valueOf(0))
                    sb.add(ScriptOp.OP_NEWSTRUCT)
                    sb.add(ScriptOp.OP_TOALTSTACK)
                    for (k in 0 until (`val` as Struct).list.size()) {
                        val o = (`val` as Struct).list.get(k)
                        createCodeParamsScript(sb, o)
                        sb.add(ScriptOp.OP_DUPFROMALTSTACK)
                        sb.add(ScriptOp.OP_SWAP)
                        sb.add(ScriptOp.OP_APPEND)
                    }
                    sb.add(ScriptOp.OP_FROMALTSTACK)
                } else if (`val` is Array<Struct>) {
                    sb.push(BigInteger.valueOf(0))
                    sb.add(ScriptOp.OP_NEWSTRUCT)
                    sb.add(ScriptOp.OP_TOALTSTACK)
                    val structs = `val` as Array<Struct>
                    for (k in structs.indices) {
                        createCodeParamsScript(sb, structs[k])
                    }
                    sb.add(ScriptOp.OP_FROMALTSTACK)
                    sb.push(BigInteger(structs.size.toString()))
                    sb.pushPack()
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

    @Throws(Exception::class)
    fun serializeAbiFunction(abiFunction: AbiFunction): ByteArray {
        val list = ArrayList<Any>()
        list.add(abiFunction.name!!.toByteArray())
        val tmp = ArrayList<Any>()
        for (obj in abiFunction.parameters!!) {
            if ("Byte" == obj.getType()) {
                tmp.add(JSON.parseObject(obj.getValue(), Byte::class.javaPrimitiveType))
            } else if ("ByteArray" == obj.getType()) {
                tmp.add(JSON.parseObject(obj.getValue(), ByteArray::class.java))
            } else if ("String" == obj.getType()) {
                tmp.add(obj.getValue())
            } else if ("Bool" == obj.getType()) {
                tmp.add(JSON.parseObject(obj.getValue(), Boolean::class.javaPrimitiveType))
            } else if ("Int" == obj.getType()) {
                tmp.add(JSON.parseObject(obj.getValue(), Long::class.java))
            } else if ("Array" == obj.getType()) {
                tmp.add(JSON.parseObject(obj.getValue(), Array::class.java))
            } else if ("Struct" == obj.getType()) {
                //tmp.add(JSON.parseObject(obj.getValue(), Object.class));
            } else if ("Uint256" == obj.getType()) {

            } else if ("Address" == obj.getType()) {

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