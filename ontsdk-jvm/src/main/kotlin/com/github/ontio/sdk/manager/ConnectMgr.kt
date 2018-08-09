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

import java.io.IOException

import com.alibaba.fastjson.JSON

import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.block.Block
import com.github.ontio.network.rpc.RpcClient
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.network.exception.ConnectorException
import com.github.ontio.network.connect.IConnector
import com.github.ontio.network.rest.RestClient
import com.github.ontio.network.rest.Result
import com.github.ontio.network.websocket.WebsocketClient
import com.github.ontio.sdk.exception.SDKException

/**
 *
 */
class ConnectMgr {
    private var connector: IConnector? = null

    private val url: String
        get() = connector!!.url

    val generateBlockTime: Int
        @Throws(ConnectorException::class, IOException::class)
        get() = connector!!.generateBlockTime

    val nodeCount: Int
        @Throws(ConnectorException::class, IOException::class)
        get() = connector!!.nodeCount

    val blockHeight: Int
        @Throws(ConnectorException::class, IOException::class)
        get() = connector!!.blockHeight

    val memPoolTxCount: Any
        @Throws(ConnectorException::class, IOException::class)
        get() = connector!!.memPoolTxCount
    val version: String
        @Throws(ConnectorException::class, IOException::class)
        get() = connector!!.version

    constructor(url: String, type: String, lock: Any) {
        if (type == "websocket") {
            setConnector(WebsocketClient(url, lock))
        }
    }

    constructor(url: String, type: String) {
        if (type == "rpc") {
            setConnector(RpcClient(url))
        } else if (type == "restful") {
            setConnector(RestClient(url))
        }
    }

    fun startWebsocketThread(log: Boolean) {
        if (connector is WebsocketClient) {
            (connector as WebsocketClient).startWebsocketThread(log)
        }
    }

    fun setReqId(n: Long) {
        if (connector is WebsocketClient) {
            (connector as WebsocketClient).setReqId(n)
        }
    }

    fun send(map: Map<*, *>) {
        if (connector is WebsocketClient) {
            (connector as WebsocketClient).send(map)
        }
    }

    fun sendHeartBeat() {
        if (connector is WebsocketClient) {
            (connector as WebsocketClient).sendHeartBeat()
        }
    }

    fun sendSubscribe(map: MutableMap<Any, Any>) {
        if (connector is WebsocketClient) {
            (connector as WebsocketClient).sendSubscribe(map)
        }
    }

    constructor(connector: IConnector) {
        setConnector(connector)
    }

    fun setConnector(connector: IConnector) {
        this.connector = connector
    }

    @Throws(ConnectorException::class, IOException::class)
    fun sendRawTransaction(tx: Transaction): Boolean {
        val rs = connector!!.sendRawTransaction(Helper.toHexString(tx.toArray())) as String
        if (connector is RpcClient) {
            return true
        }
        if (connector is WebsocketClient) {
            return true
        }
        val rr = JSON.parseObject(rs, Result::class.java)
        return if (rr.Error == 0L) {
            true
        } else false
    }

    @Throws(ConnectorException::class, IOException::class)
    fun sendRawTransaction(hexData: String): Boolean {
        val rs = connector!!.sendRawTransaction(hexData) as String
        if (connector is RpcClient) {
            return true
        }
        if (connector is WebsocketClient) {
            return true
        }
        val rr = JSON.parseObject(rs, Result::class.java)
        return if (rr.Error == 0L) {
            true
        } else false
    }

    @Throws(ConnectorException::class, IOException::class)
    fun sendRawTransactionPreExec(hexData: String): Any? {
        val rs = connector!!.sendRawTransaction(true, null!!, hexData)
        if (connector is RpcClient) {
            return rs
        }
        if (connector is WebsocketClient) {
            return rs
        }
        val rr = JSON.parseObject(rs as String?, Result::class.java)
        return if (rr.Error == 0L) {
            rr.Result
        } else null
    }

    @Throws(ConnectorException::class, IOException::class)
    fun getTransaction(txhash: String): Transaction? {
        var txhash = txhash
        txhash = txhash.replace("0x", "")
        return connector!!.getRawTransaction(txhash)
    }

    @Throws(ConnectorException::class, IOException::class)
    fun getTransactionJson(txhash: String): Any? {
        var txhash = txhash
        txhash = txhash.replace("0x", "")
        return connector!!.getRawTransactionJson(txhash)
    }

    @Throws(ConnectorException::class, IOException::class, SDKException::class)
    fun getBlock(height: Int): Block? {
        if (height < 0) {
            throw SDKException(ErrorCode.ParamError)
        }
        return connector!!.getBlock(height)
    }

    @Throws(ConnectorException::class, IOException::class)
    fun getBlock(hash: String): Block? {
        return connector!!.getBlock(hash)

    }

    @Throws(ConnectorException::class, IOException::class)
    fun getBalance(address: String): Any? {
        return connector!!.getBalance(address)
    }

    @Throws(ConnectorException::class, IOException::class)
    fun getBlockJson(height: Int): Any? {
        return connector!!.getBlockJson(height)
    }

    @Throws(ConnectorException::class, IOException::class)
    fun getBlockJson(hash: String): Any {
        return connector!!.getBlockJson(hash)
    }

    @Throws(ConnectorException::class, IOException::class)
    fun getContract(hash: String): Any? {
        var hash = hash
        hash = hash.replace("0x", "")
        return connector!!.getContractJson(hash)
    }

    @Throws(ConnectorException::class, IOException::class)
    fun getContractJson(hash: String): Any? {
        var hash = hash
        hash = hash.replace("0x", "")
        return connector!!.getContractJson(hash)
    }

    @Throws(ConnectorException::class, IOException::class)
    fun getSmartCodeEvent(height: Int): Any {
        return connector!!.getSmartCodeEvent(height)
    }

    @Throws(ConnectorException::class, IOException::class)
    fun getSmartCodeEvent(hash: String): Any {
        return connector!!.getSmartCodeEvent(hash)
    }

    @Throws(ConnectorException::class, IOException::class)
    fun getBlockHeightByTxHash(hash: String): Int {
        var hash = hash
        hash = hash.replace("0x", "")
        return connector!!.getBlockHeightByTxHash(hash)
    }

    @Throws(ConnectorException::class, IOException::class)
    fun getStorage(codehash: String, key: String): String {
        var codehash = codehash
        codehash = codehash.replace("0x", "")
        return connector!!.getStorage(codehash, key)
    }

    @Throws(ConnectorException::class, IOException::class)
    fun getMerkleProof(hash: String): Any {
        var hash = hash
        hash = hash.replace("0x", "")
        return connector!!.getMerkleProof(hash)
    }

    @Throws(ConnectorException::class, IOException::class)
    fun getAllowance(asset: String, from: String, to: String): String {
        return connector!!.getAllowance(asset, from, to)
    }

    @Throws(ConnectorException::class, IOException::class)
    fun getMemPoolTxState(hash: String): Any {
        var hash = hash
        hash = hash.replace("0x", "")
        return connector!!.getMemPoolTxState(hash)
    }

    @Throws(Exception::class)
    fun waitResult(hash: String): Any {
        for (i in 0..19) {
            try {
                Thread.sleep(3000)
                val obj = connector!!.getSmartCodeEvent(hash)
                if ((obj as Map<*, *>)["Notify"] != null) {
                    return obj
                }
            } catch (e: Exception) {
                if (!e.message!!.contains("INVALID TRANSACTION")) {
                    break
                }
            }

        }
        throw SDKException(ErrorCode.ParamError)
    }
}


