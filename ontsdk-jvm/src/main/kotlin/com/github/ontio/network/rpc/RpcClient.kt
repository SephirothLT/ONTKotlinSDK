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

package com.github.ontio.network.rpc

import java.io.IOException
import java.net.MalformedURLException

import com.github.ontio.common.Helper
import com.github.ontio.common.UInt256
import com.github.ontio.core.block.Block
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.io.Serializable
import com.github.ontio.network.connect.AbstractConnector
import com.github.ontio.network.exception.ConnectorException
import com.github.ontio.network.exception.RpcException

class RpcClient(url: String) : AbstractConnector() {
    private var rpc: Interfaces? = null

    override val url: String
        get() = rpc!!.host

    override val generateBlockTime: Int
        @Throws(RpcException::class, IOException::class)
        get() {
            val result = rpc!!.call("getgenerateblocktime")
            return result as Int
        }

    override val nodeCount: Int
        @Throws(RpcException::class, IOException::class)
        get() {
            val result = rpc!!.call("getconnectioncount")
            return result as Int
        }

    override val blockHeight: Int
        @Throws(RpcException::class, IOException::class)
        get() {
            val result = rpc!!.call("getblockcount")
            return result as Int
        }

    val blockCount: Int
        @Throws(RpcException::class, IOException::class)
        get() {
            val result = rpc!!.call("getblockcount")
            return result as Int
        }
    override val memPoolTxCount: Any
        @Throws(ConnectorException::class, IOException::class)
        get() {
            val result = rpc!!.call("getmempooltxcount")
            try {
                return result
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

        }
    override val version: String
        @Throws(ConnectorException::class, IOException::class)
        get() {
            val result = rpc!!.call("getversion")
            try {
                return result as String
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

        }

    init {
        try {
            this.rpc = Interfaces(url)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }

    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getBalance(address: String): Any?{
        var result: Any? = null
        result = rpc!!.call("getbalance", address)
        return result
    }

    @Throws(RpcException::class, IOException::class)
    override fun sendRawTransaction(sData: String): String {
        val result = rpc!!.call("sendrawtransaction", sData)
        return result as String
    }

    @Throws(RpcException::class, IOException::class)
    override fun sendRawTransaction(preExec: Boolean, userid: String, sData: String): Any? {
        var result: Any? = null
        if (preExec) {
            result = rpc!!.call("sendrawtransaction", sData, 1)
        } else {
            result = rpc!!.call("sendrawtransaction", sData)
        }
        return result
    }

    @Throws(RpcException::class, IOException::class)
    override fun getRawTransaction(txhash: String): Transaction {
        val result = rpc!!.call("getrawtransaction", txhash)
        return Transaction.deserializeFrom(Helper.hexToBytes(result as String))
    }

    @Throws(RpcException::class, IOException::class)
    override fun getRawTransactionJson(txhash: String): Any {
        var result: Any? = null
        result = rpc!!.call("getrawtransaction", txhash)
        return Transaction.deserializeFrom(Helper.hexToBytes(result as String?)).json()
    }

    @Throws(RpcException::class, IOException::class)
    override fun getBlockJson(index: Int): Any? {
        var result: Any? = null
        result = rpc!!.call("getblock", index, 1)
        return result
    }

    @Throws(RpcException::class, IOException::class)
    override fun getBlockJson(hash: String): Any? {
        var result: Any? = null
        result = rpc!!.call("getblock", hash, 1)
        return result
    }

    @Throws(RpcException::class, IOException::class)
    override fun getContract(hash: String): Any? {
        var result: Any? = null
        result = rpc!!.call("getcontractstate", hash)
        return result
    }

    @Throws(RpcException::class, IOException::class)
    override fun getContractJson(hash: String): Any?{
        var result: Any? = null
        result = rpc!!.call("getcontractstate", hash, 1)
        return result
    }

    @Throws(RpcException::class, IOException::class)
    fun getRawTransaction(txhash: UInt256): String {
        val result = rpc!!.call("getrawtransaction", txhash.toString())
        return result as String
    }


    @Throws(RpcException::class, IOException::class)
    fun getBlock(hash: UInt256): Block {
        val result = rpc!!.call("getblock", hash.toString())
        try {

            val bb = Serializable.from(Helper.hexToBytes(result as String), Block::class.java)
            return Serializable.from(Helper.hexToBytes(result as String), Block::class.java)
        } catch (e: InstantiationException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }

    }

    @Throws(RpcException::class, IOException::class)
    override fun getBlock(index: Int): Block {
        val result = rpc!!.call("getblock", index)
        try {
            val bb = Serializable.from(Helper.hexToBytes(result as String), Block::class.java)
            return Serializable.from(Helper.hexToBytes(result as String), Block::class.java)
        } catch (e: InstantiationException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }

    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getBlock(hash: String): Block {
        val result = rpc!!.call("getblock", hash)
        try {
            val bb = Serializable.from(Helper.hexToBytes(result as String), Block::class.java)
            return Serializable.from(Helper.hexToBytes(result as String), Block::class.java)
        } catch (e: InstantiationException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }

    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getSmartCodeEvent(height: Int): Any {
        val result = rpc!!.call("getsmartcodeevent", height)
        try {
            return result
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getSmartCodeEvent(hash: String): Any {
        val result = rpc!!.call("getsmartcodeevent", hash)
        try {
            return result
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getBlockHeightByTxHash(hash: String): Int {
        val result = rpc!!.call("getblockheightbytxhash", hash)
        try {
            return result as Int
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getStorage(codehash: String, key: String): String {
        val result = rpc!!.call("getstorage", codehash, key)
        try {
            return result as String
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getMerkleProof(hash: String): Any {
        val result = rpc!!.call("getmerkleproof", hash)
        try {
            return result
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getAllowance(asset: String, from: String, to: String): String {
        val result = rpc!!.call("getallowance", asset, from, to)
        try {
            return result as String
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    @Throws(ConnectorException::class, IOException::class)
    override fun getMemPoolTxState(hash: String): Any {
        val result = rpc!!.call("getmempooltxstate", hash)
        try {
            return result
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }
}

