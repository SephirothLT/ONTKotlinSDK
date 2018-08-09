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

package com.github.ontio.smartcontract

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.ontio.OntSdk
import com.github.ontio.common.ErrorCode
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.smartcontract.neovm.abi.AbiFunction
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.neovm.abi.BuildParams

import java.util.ArrayList
import java.util.HashMap

class WasmVm(private val sdk: OntSdk) {

    @Throws(Exception::class)
    fun sendTransaction(contractAddr: String, payer: String, password: String, salt: ByteArray, gaslimit: Long, gas: Long, func: AbiFunction, preExec: Boolean): String {
        val params = BuildParams.serializeAbiFunction(func)
        if (preExec) {
            val tx = sdk.vm().makeInvokeCodeTransaction(contractAddr, null!!, params, payer, gaslimit, gas)
            val obj = sdk.connect!!.sendRawTransactionPreExec(tx.toHexString()) as String?
            val result = (obj as JSONObject).getString("Result")
            if (Integer.parseInt(result) == 0) {
                throw SDKException(ErrorCode.OtherError("sendRawTransaction PreExec error: $obj"))
            }
            return result
        } else {
            val tx = sdk.vm().makeInvokeCodeTransaction(contractAddr, null!!, params, payer, gaslimit, gas)
            sdk.signTx(tx, payer, password, salt)
            val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
            if (!b) {
                throw SDKException(ErrorCode.SendRawTxError)
            }
            return tx.hash().toString()
        }
    }

    fun buildWasmContractJsonParam(objs: Array<Any>): String {
        val params = ArrayList()
        for (i in objs.indices) {
            val `val` = objs[i]
            if (`val` is String) {
                val map = HashMap()
                map.put("type", "string")
                map.put("value", `val`)
                params.add(map)
            } else if (`val` is Int) {
                val map = HashMap()
                map.put("type", "int")
                map.put("value", `val`.toString())
                params.add(map)
            } else if (`val` is Long) {
                val map = HashMap()
                map.put("type", "int64")
                map.put("value", `val`.toString())
                params.add(map)
            } else if (`val` is IntArray) {
                val map = HashMap()
                map.put("type", "int_array")
                map.put("value", `val`)
                params.add(map)
            } else if (`val` is LongArray) {
                val map = HashMap()
                map.put("type", "int_array")
                map.put("value", `val`)
                params.add(map)
            } else {
                continue
            }
        }
        val result = HashMap()
        result.put("Params", params)
        return JSON.toJSONString(result)
    }
}
