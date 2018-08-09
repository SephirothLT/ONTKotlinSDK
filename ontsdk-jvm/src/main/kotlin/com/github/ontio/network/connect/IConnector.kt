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

package com.github.ontio.network.connect

import java.io.IOException
import com.github.ontio.core.block.Block
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.network.exception.ConnectorException

interface IConnector {

    val url: String
    val generateBlockTime: Int
    val nodeCount: Int
    val blockHeight: Int
    val memPoolTxCount: Any
    val version: String
    @Throws(ConnectorException::class, IOException::class)
    fun sendRawTransaction(preExec: Boolean, userid: String, hexData: String): Any?

    @Throws(ConnectorException::class, IOException::class)
    fun sendRawTransaction(hexData: String): Any

    @Throws(ConnectorException::class, IOException::class)
    fun getRawTransaction(txhash: String): Transaction?

    @Throws(ConnectorException::class, IOException::class)
    fun getRawTransactionJson(txhash: String): Any?

    @Throws(ConnectorException::class, IOException::class)
    fun getBlock(height: Int): Block?

    @Throws(ConnectorException::class, IOException::class)
    fun getBlock(hash: String): Block?

    @Throws(ConnectorException::class, IOException::class)
    fun getBlockJson(height: Int): Any?

    @Throws(ConnectorException::class, IOException::class)
    fun getBlockJson(hash: String): Any

    @Throws(ConnectorException::class, IOException::class)
    fun getBalance(address: String): Any?

    @Throws(ConnectorException::class, IOException::class)
    fun getContract(hash: String): Any?

    @Throws(ConnectorException::class, IOException::class)
    fun getContractJson(hash: String): Any?

    @Throws(ConnectorException::class, IOException::class)
    fun getSmartCodeEvent(height: Int): Any

    @Throws(ConnectorException::class, IOException::class)
    fun getSmartCodeEvent(hash: String): Any

    @Throws(ConnectorException::class, IOException::class)
    fun getBlockHeightByTxHash(hash: String): Int

    @Throws(ConnectorException::class, IOException::class)
    fun getStorage(codehash: String, key: String): String

    @Throws(ConnectorException::class, IOException::class)
    fun getMerkleProof(hash: String): Any

    @Throws(ConnectorException::class, IOException::class)
    fun getAllowance(asset: String, from: String, to: String): String

    @Throws(ConnectorException::class, IOException::class)
    fun getMemPoolTxState(hash: String): Any
}