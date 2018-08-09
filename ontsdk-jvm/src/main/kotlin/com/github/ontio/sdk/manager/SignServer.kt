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

package com.github.ontio.sdk.manager

import com.alibaba.fastjson.JSON
import com.github.ontio.common.ErrorCode
import com.github.ontio.network.exception.RpcException
import com.github.ontio.network.rpc.Interfaces
import com.github.ontio.network.rpc.RpcClient

import java.util.HashMap

class SignServer @Throws(Exception::class)
constructor(url: String) {
    private val rpcClient: Interfaces
    private val url = ""

    init {
        this.url = url
        rpcClient = Interfaces(url)
    }

    @Throws(Exception::class)
    fun sendSigRawTx(rawTx: String): Any {
        val req = HashMap()
        req.put("jsonrpc", "2.0")
        req.put("method", "sigrawtx")
        val params = HashMap()
        params.put("raw_tx", rawTx)
        req.put("params", params)
        req.put("id", 1)
        return send(req)
    }

    @Throws(Exception::class)
    fun sendMultiSigRawTx(rawTx: String, m: Int, pubkeys: Array<String>): Any {
        val req = HashMap()
        req.put("jsonrpc", "2.0")
        req.put("method", "sigmutilrawtx")
        val params = HashMap()
        params.put("raw_tx", rawTx)
        params.put("m", m)
        params.put("pub_keys", pubkeys)
        req.put("params", params)
        req.put("id", 1)
        return send(req)
    }

    @Throws(Exception::class)
    fun sendSigTransferTx(asset: String, from: String, to: String, amount: Long, gasLimit: Long, gasPrice: Long): Any {
        val req = HashMap()
        req.put("jsonrpc", "2.0")
        req.put("method", "sigtransfertx")
        val params = HashMap()
        params.put("asset", asset)
        params.put("from", from)
        params.put("to", to)
        params.put("amount", amount)
        params.put("gas_limit", gasLimit)
        params.put("gas_price", gasPrice)
        req.put("params", params)
        req.put("id", 1)
        return send(req)
    }

    @Throws(Exception::class)
    fun sendSigNativeInvokeTx(contractAddr: String, method: String, version: Int, gasLimit: Long, gasPrice: Long, parameters: Map<*, *>): Any {
        val req = HashMap()
        req.put("jsonrpc", "2.0")
        req.put("method", "sigtransfertx")
        val params = HashMap()
        params.put("address", contractAddr)
        params.put("method", method)
        params.put("version", version)
        params.put("gas_limit", gasLimit)
        params.put("gas_price", gasPrice)
        params.put("params", parameters)
        req.put("params", params)
        req.put("id", 1)
        return send(req)
    }

    @Throws(Exception::class)
    fun sendSigNeoInvokeTx(contractAddr: String, version: Int, gasLimit: Long, gasPrice: Long, parameters: Map<*, *>): Any {
        val req = HashMap()
        req.put("jsonrpc", "2.0")
        req.put("method", "sigtransfertx")
        val params = HashMap()
        params.put("address", contractAddr)
        params.put("version", version)
        params.put("gas_limit", gasLimit)
        params.put("gas_price", gasPrice)
        params.put("params", parameters)
        req.put("params", params)
        req.put("id", 1)
        return send(req)
    }

    @Throws(Exception::class)
    private fun send(req: Map<*, *>): Any {
        val response = rpcClient.send(req) as Map<*, *>?
        println(response)
        return if (response == null) {
            throw RpcException(0, ErrorCode.ConnectUrlErr(url + "response is null. maybe is connect error"))
        } else if (response["error_code"] as Int == 0) {
            response["result"]
        } else {
            throw RpcException(0, JSON.toJSONString(response))
        }
    }
}
