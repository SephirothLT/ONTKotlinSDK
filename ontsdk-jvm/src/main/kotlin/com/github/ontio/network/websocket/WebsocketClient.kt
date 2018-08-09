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

package com.github.ontio.network.websocket

import com.alibaba.fastjson.JSON
import com.github.ontio.core.block.Block
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.network.connect.AbstractConnector
import com.github.ontio.network.exception.ConnectorException
import okhttp3.*

import java.io.IOException
import java.util.*

/**
 *
 */
class WebsocketClient(url: String, private val lock: Any) : AbstractConnector() {
    private var mWebSocket: WebSocket? = null
    private var logFlag: Boolean = false
    private var reqId: Long = 0
    private var wsClient: WebsocketClient? = null
    override val url: String
        get() = wsUrl
    override val generateBlockTime: Int
        @Throws(ConnectorException::class, IOException::class)
        get() {
            val map = HashMap<Any, Any>()
            map["Action"] = "getgenerateblocktime"
            map["Version"] = "1.0.0"
            map["Id"] = generateReqId()
            mWebSocket!!.send(JSON.toJSONString(map))
            return 0
        }
    override val nodeCount: Int
        @Throws(ConnectorException::class, IOException::class)
        get() {
            val map = HashMap<Any, Any>()
            map["Action"] = "getconnectioncount"
            map["Version"] = "1.0.0"
            map["Id"] = generateReqId()
            mWebSocket!!.send(JSON.toJSONString(map))
            return 0
        }
    override val blockHeight: Int
        @Throws(ConnectorException::class, IOException::class)
        get() {
            val map = HashMap<Any, Any>()
            map["Action"] = "getblockheight"
            map["Version"] = "1.0.0"
            map["Id"] = generateReqId()
            mWebSocket!!.send(JSON.toJSONString(map))
            return 0
        }
    override val memPoolTxCount: Any
        @Throws(ConnectorException::class, IOException::class)
        get() {
            val map = HashMap<Any, Any>()
            map["Action"] = "getmempooltxcount"
            map["Version"] = "1.0.0"
            map["Id"] = generateReqId()
            mWebSocket!!.send(JSON.toJSONString(map))
            return ""
        }
    override val version: String
        @Throws(ConnectorException::class, IOException::class)
        get() {
            val map = HashMap<Any, Any>()
            map["Action"] = "getversion"
            map["Version"] = "1.0.0"
            map["Id"] = generateReqId()
            mWebSocket!!.send(JSON.toJSONString(map))
            return ""
        }

    init {
        wsUrl = url
        wsClient = this
    }


    fun setLog(b: Boolean) {
        logFlag = b
    }

    fun startWebsocketThread(log: Boolean) {
        this.logFlag = log
        val thread = Thread(
                Runnable { wsClient!!.wsStart() })
        thread.start()
    }

    fun sendHeartBeat() {
        val map = HashMap<Any, Any>()
        map["Action"] = "heartbeat"
        map["Version"] = "V1.0.0"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
    }

    fun sendSubscribe(map: MutableMap<Any, Any>) {
        map["Action"] = "subscribe"
        map["Version"] = "V1.0.0"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
    }

    fun send(map: Map<*, *>) {
        mWebSocket!!.send(JSON.toJSONString(map))
    }

    fun setReqId(reqId: Long) {
        this.reqId = reqId
    }

    private fun generateReqId(): Long {
        return if (reqId == 0L) {
            (Random().nextInt() and Integer.MAX_VALUE).toLong()
        } else reqId
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun sendRawTransaction(preExec: Boolean, userid: String, hexData: String): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "sendrawtransaction"
        map["Version"] = "1.0.0"
        map["Data"] = hexData
        map["Id"] = generateReqId()
        if (preExec) {
            map["PreExec"] = "1"
        }
        mWebSocket!!.send(JSON.toJSONString(map))
        return if (preExec) {
            "0"
        } else ""
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun sendRawTransaction(hexData: String): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "sendrawtransaction"
        map["Version"] = "1.0.0"
        map["Data"] = hexData
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return ""
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getRawTransaction(txhash: String): Transaction? {
        val map = HashMap<Any, Any>()
        map["Action"] = "gettransaction"
        map["Version"] = "1.0.0"
        map["Hash"] = txhash
        map["Raw"] = "1"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return null
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getRawTransactionJson(txhash: String): Any? {
        val map = HashMap<Any, Any>()
        map["Action"] = "gettransaction"
        map["Version"] = "1.0.0"
        map["Hash"] = txhash
        map["Raw"] = "0"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return null
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getBlock(height: Int): Block? {
        val map = HashMap<Any, Any>()
        map["Action"] = "getblockbyheight"
        map["Version"] = "1.0.0"
        map["Height"] = height
        map["Raw"] = "1"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return null
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getBlock(hash: String): Block? {
        val map = HashMap<Any, Any>()
        map["Action"] = "getblockbyhash"
        map["Version"] = "1.0.0"
        map["Hash"] = hash
        map["Raw"] = "1"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return null
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getBlockJson(height: Int): Block? {
        val map = HashMap<Any, Any>()
        map["Action"] = "getblockbyheight"
        map["Version"] = "1.0.0"
        map["Height"] = height
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return null
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getBlockJson(hash: String): Block? {
        val map = HashMap<Any, Any>()
        map["Action"] = "getblockbyhash"
        map["Version"] = "1.0.0"
        map["Hash"] = hash
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return null
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getBalance(address: String): Any? {
        val map = HashMap<Any, Any>()
        map["Action"] = "getbalance"
        map["Version"] = "1.0.0"
        map["Addr"] = address
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return null
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getContract(hash: String): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "getcontract"
        map["Version"] = "1.0.0"
        map["Raw"] = "1"
        map["Hash"] = hash
        map["Id"] = generateReqId()
        return mWebSocket!!.send(JSON.toJSONString(map))
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getContractJson(hash: String): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "getcontract"
        map["Version"] = "1.0.0"
        map["Raw"] = "0"
        map["Hash"] = hash
        map["Id"] = generateReqId()
        return mWebSocket!!.send(JSON.toJSONString(map))
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getSmartCodeEvent(height: Int): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "getsmartcodeeventtxs"
        map["Version"] = "1.0.0"
        map["Height"] = height
        map["Id"] = generateReqId()
        return mWebSocket!!.send(JSON.toJSONString(map))
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getSmartCodeEvent(hash: String): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "getsmartcodeevent"
        map["Version"] = "1.0.0"
        map["Hash"] = hash
        return mWebSocket!!.send(JSON.toJSONString(map))
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getBlockHeightByTxHash(hash: String): Int {
        val map = HashMap<Any, Any>()
        map["Action"] = "getblockheightbytxhash"
        map["Version"] = "1.0.0"
        map["Hash"] = hash
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return 0
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getStorage(codehash: String, key: String): String {
        val map = HashMap<Any, Any>()
        map["Action"] = "getstorage"
        map["Version"] = "1.0.0"
        map["Hash"] = codehash
        map["Key"] = key
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return ""
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getMerkleProof(hash: String): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "getmerkleproof"
        map["Version"] = "1.0.0"
        map["Hash"] = hash
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return ""
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getAllowance(asset: String, from: String, to: String): String {
        val map = HashMap<Any, Any>()
        map["Action"] = "getmerkleproof"
        map["Version"] = "1.0.0"
        map["Asset"] = asset
        map["From"] = from
        map["To"] = to
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return ""
    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getMemPoolTxState(hash: String): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "getmempooltxstate"
        map["Version"] = "1.0.0"
        map["Hash"] = hash
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return ""
    }

    fun wsStart() {
        //request = new Request.Builder().url(WS_URL).build();
        var httpUrl: String? = null
        if (wsUrl.contains("wss")) {
            httpUrl = "https://" + wsUrl.split("://".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        } else {
            httpUrl = "http://" + wsUrl.split("://".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        }
        val request = Request.Builder().url(wsUrl).addHeader("Origin", httpUrl).build()
        val mClient = OkHttpClient.Builder().build()
        mWebSocket = mClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket?, response: Response?) {
                println("opened websocket connection")
                sendHeartBeat()
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        sendHeartBeat()
                    }
                }, 1000, 30000)
            }

            override fun onMessage(webSocket: WebSocket?, s: String?) {
                if (logFlag) {
                    println("websoket onMessage:" + s!!)
                }
                val result = JSON.parseObject(s, Result::class.java)
                try {
                    synchronized(lock) {
                        MsgQueue.addResult(result)
                        lock.notify()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
                println(reason)
            }

            override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
                println("close:" + reason!!)
            }

            override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
                println("onFailure:" + response!!)
                wsStart()
            }
        })

    }

    companion object {
        var wsUrl = ""
    }
}

