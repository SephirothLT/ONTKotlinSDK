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

import com.github.ontio.common.Address
import com.github.ontio.common.Common
import com.github.ontio.core.scripts.ScriptOp
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.common.Helper
import com.github.ontio.core.payload.DeployCode
import com.github.ontio.core.payload.InvokeCode
import com.github.ontio.core.scripts.ScriptBuilder
import com.github.ontio.OntSdk
import com.github.ontio.sdk.exception.SDKException

import java.math.BigInteger
import java.util.*

/**
 *
 */
class Vm(private val sdk: OntSdk) {
    var codeAddress: String? = null
        set(codeHash) {
            field = codeHash.replace("0x", "")
        }


    /**
     *
     * @param codeStr
     * @param needStorage
     * @param name
     * @param codeVersion
     * @param author
     * @param email
     * @param desp
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws SDKException
     */
    @Throws(SDKException::class)
    fun makeDeployCodeTransaction(codeStr: String, needStorage: Boolean, name: String, codeVersion: String, author: String, email: String, desp: String, payer: String?, gaslimit: Long, gasprice: Long): DeployCode {
        val tx = DeployCode()
        if (payer != null) {
            tx.payer = Address.decodeBase58(payer.replace(Common.didont, ""))
        }
        tx.attributes = arrayOfNulls(0)
        tx.nonce = Random().nextInt()
        tx.code = Helper.hexToBytes(codeStr)
        tx.version = codeVersion
        tx.needStorage = needStorage
        tx.name = name
        tx.author = author
        tx.email = email
        tx.gasLimit = gaslimit
        tx.gasPrice = gasprice
        tx.description = desp
        return tx
    }

    //NEO makeInvokeCodeTransaction
    @Throws(SDKException::class)
    fun makeInvokeCodeTransaction(codeAddr: String, method: String, params: ByteArray, payer: String?, gaslimit: Long, gasprice: Long): InvokeCode {
        var params = params
        params = Helper.addBytes(params, byteArrayOf(0x67))
        params = Helper.addBytes(params, Address.parse(codeAddr).toArray())
        val tx = InvokeCode()
        tx.attributes = arrayOfNulls(0)
        tx.nonce = Random().nextInt()
        tx.code = params
        tx.gasLimit = gaslimit
        tx.gasPrice = gasprice
        if (payer != null) {
            tx.payer = Address.decodeBase58(payer.replace(Common.didont, ""))
        }
        return tx
    }

    /**
     * Native makeInvokeCodeTransaction
     * @param params
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws SDKException
     */
    @Throws(SDKException::class)
    fun makeInvokeCodeTransaction(params: ByteArray, payer: String?, gaslimit: Long, gasprice: Long): InvokeCode {

        val tx = InvokeCode()
        tx.attributes = arrayOfNulls(0)
        tx.nonce = Random().nextInt()
        tx.code = params
        tx.gasLimit = gaslimit
        tx.gasPrice = gasprice
        if (payer != null) {
            tx.payer = Address.decodeBase58(payer.replace(Common.didont, ""))
        }
        return tx
    }

    @Throws(SDKException::class)
    fun buildNativeParams(codeAddr: Address, initMethod: String, args: ByteArray, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        val sb = ScriptBuilder()
        if (args.size > 0) {
            sb.add(args)
        }
        sb.push(initMethod.toByteArray())
        sb.push(codeAddr.toArray())
        sb.push(BigInteger.valueOf(0))
        sb.add(ScriptOp.OP_SYSCALL)
        sb.push(NATIVE_INVOKE_NAME.toByteArray())
        return makeInvokeCodeTransaction(sb.toArray(), payer, gaslimit, gasprice)
    }

    companion object {
        var NATIVE_INVOKE_NAME = "Ontology.Native.Invoke"
    }
}
