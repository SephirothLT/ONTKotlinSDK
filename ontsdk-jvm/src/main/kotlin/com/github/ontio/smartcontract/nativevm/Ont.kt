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

package com.github.ontio.smartcontract.nativevm

import com.alibaba.fastjson.JSONObject
import com.github.ontio.OntSdk
import com.github.ontio.account.Account
import com.github.ontio.common.Address
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.core.asset.*
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.nativevm.abi.NativeBuildParams
import com.github.ontio.smartcontract.nativevm.abi.Struct

import java.util.ArrayList


/**
 *
 */
class Ont(private val sdk: OntSdk) {
    val contractAddress = "0000000000000000000000000000000000000001"
    private val nativeAbi = "{\"hash\":\"0000000000000000000000000000000000000001\",\"functions\":[{\"name\":\"init\",\"parameters\":[],\"returntype\":\"Bool\"},{\"name\":\"transfer\",\"parameters\":[{\"name\":\"transfers\",\"type\":\"Struct\",\"subType\":[{\"name\":\"from\",\"type\":\"Address\"},{\"name\":\"to\",\"type\":\"Address\"},{\"name\":\"value\",\"type\":\"Int\"}]}],\"returntype\":\"Bool\"},{\"name\":\"approve\",\"parameters\":[{\"name\":\"from\",\"type\":\"Address\"},{\"name\":\"to\",\"type\":\"Address\"},{\"name\":\"value\",\"type\":\"Int\"}],\"returntype\":\"Bool\"},{\"name\":\"transferFrom\",\"parameters\":[{\"name\":\"sender\",\"type\":\"Address\"},{\"name\":\"from\",\"type\":\"Address\"},{\"name\":\"to\",\"type\":\"Address\"},{\"name\":\"value\",\"type\":\"Int\"}],\"returntype\":\"Bool\"},{\"name\":\"name\",\"parameters\":[],\"returntype\":\"String\"},{\"name\":\"symbol\",\"parameters\":[],\"returntype\":\"String\"},{\"name\":\"decimals\",\"parameters\":[],\"returntype\":\"Int\"},{\"name\":\"totalSupply\",\"parameters\":[],\"returntype\":\"Int\"},{\"name\":\"balanceOf\",\"parameters\":[{\"name\":\"account\",\"type\":\"Address\"}],\"returntype\":\"Int\"},{\"name\":\"allowance\",\"parameters\":[{\"name\":\"account\",\"type\":\"Address\"}],\"returntype\":\"Int\"}],\"events\":[{\"name\":\"transfer\",\"parameters\":[{\"name\":\"from\",\"type\":\"Address\"},{\"name\":\"to\",\"type\":\"Address\"},{\"name\":\"value\",\"type\":\"Int\"}]}]}"

    /**
     *
     * @param sendAcct
     * @param recvAddr
     * @param amount
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendTransfer(sendAcct: Account?, recvAddr: String, amount: Long, payerAcct: Account?, gaslimit: Long, gasprice: Long): String? {
        if (sendAcct == null || payerAcct == null) {
            throw SDKException(ErrorCode.ParamErr("parameters should not be null"))
        }

        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"))
        }

        val tx = makeTransfer(sendAcct.getAddressU160().toBase58(), recvAddr, amount, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(sendAcct)))
        if (sendAcct != payerAcct) {
            sdk.addSign(tx, payerAcct)
        }
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param M
     * @param pubKeys
     * @param sendAccts
     * @param recvAddr
     * @param amount
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendTransferFromMultiSignAddr(M: Int, pubKeys: Array<ByteArray>, sendAccts: Array<Account>?, recvAddr: String, amount: Long, payerAcct: Account?, gaslimit: Long, gasprice: Long): String? {
        if (sendAccts == null || sendAccts.size <= 1 || payerAcct == null) {
            throw SDKException(ErrorCode.ParamErr("parameters should not be null"))
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"))
        }

        val multiAddr = Address.addressFromMultiPubKeys(sendAccts.size, pubKeys)
        val tx = makeTransfer(multiAddr.toBase58(), recvAddr, amount, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        for (i in sendAccts.indices) {
            sdk.addMultiSign(tx, M, pubKeys, sendAccts[i])
        }
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param sender
     * @param recvAddr
     * @param amount
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun makeTransfer(sender: String?, recvAddr: String?, amount: Long, payer: String?, gaslimit: Long, gasprice: Long): Transaction {
        if (sender == null || recvAddr == null || recvAddr == "" || payer == null || payer == "") {
            throw SDKException(ErrorCode.ParamErr("parameters should not be null"))
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"))
        }

        //        AbiInfo abiinfo = JSON.parseObject(nativeAbi, AbiInfo.class);
        //        AbiFunction func = abiinfo.getFunction("transfer");
        //        func.name = "transfer";
        //        func.setParamsValue(new Struct().add(Address.decodeBase58(sender),Address.decodeBase58(recvAddr),amount));
        //        System.out.println(func.toString());
        //        byte[] params = NativeBuildParams.serializeAbiFunction(func);
        //        System.out.println("params:"+Helper.toHexString(params));

        val list = ArrayList()
        val structs = arrayOf(Struct().add(Address.decodeBase58(sender), Address.decodeBase58(recvAddr), amount))
        list.add(structs)
        val args = NativeBuildParams.createCodeParamsScript(list)
        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "transfer", args, payer, gaslimit, gasprice)
    }

    @Throws(Exception::class)
    fun makeTransfer(states: Array<State>?, payer: String?, gaslimit: Long, gasprice: Long): Transaction {
        if (states == null || payer == null || payer == "") {
            throw SDKException(ErrorCode.ParamErr("parameters should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamError)
        }
        val list = ArrayList()
        val structs = arrayOfNulls<Struct>(states.size)
        for (i in states.indices) {
            structs[i] = Struct().add(states[i].from, states[i].to, states[i].value)
        }
        list.add(structs)
        val args = NativeBuildParams.createCodeParamsScript(list)
        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "transfer", args, payer, gaslimit, gasprice)
    }

    /**
     * @param address
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun queryBalanceOf(address: String?): Long {
        if (address == null || address == "") {
            throw SDKException(ErrorCode.ParamErr("address should not be null"))
        }
        val list = ArrayList()
        list.add(Address.decodeBase58(address))
        val arg = NativeBuildParams.createCodeParamsScript(list)

        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "balanceOf", arg, null, 0, 0)
        val obj = sdk.connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        return if (res == null || res == "") {
            0
        } else java.lang.Long.valueOf(Helper.reverse(res), 16)
    }

    /**
     * @param fromAddr
     * @param toAddr
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun queryAllowance(fromAddr: String?, toAddr: String?): Long {
        if (fromAddr == null || fromAddr == "" || toAddr == null || toAddr == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        val list = ArrayList()
        list.add(Struct().add(Address.decodeBase58(fromAddr), Address.decodeBase58(toAddr)))
        val arg = NativeBuildParams.createCodeParamsScript(list)

        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "allowance", arg, null, 0, 0)
        val obj = sdk.connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        return if (res == null || res == "") {
            0
        } else java.lang.Long.valueOf(Helper.reverse(res), 16)
    }

    /**
     *
     * @param sendAcct
     * @param recvAddr
     * @param amount
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendApprove(sendAcct: Account?, recvAddr: String, amount: Long, payerAcct: Account?, gaslimit: Long, gasprice: Long): String? {
        if (sendAcct == null || payerAcct == null) {
            throw SDKException(ErrorCode.ParamErr("parameters should not be null"))
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"))
        }
        val tx = makeApprove(sendAcct.getAddressU160().toBase58(), recvAddr, amount, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(sendAcct)))
        if (sendAcct != payerAcct) {
            sdk.addSign(tx, payerAcct)
        }
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    /**
     * @param sender
     * @param recvAddr
     * @param amount
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun makeApprove(sender: String?, recvAddr: String?, amount: Long, payer: String?, gaslimit: Long, gasprice: Long): Transaction {
        if (sender == null || recvAddr == null || recvAddr == "" || payer == null || payer == "") {
            throw SDKException(ErrorCode.ParamErr("parameters should not be null"))
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"))
        }
        val list = ArrayList()
        list.add(Struct().add(Address.decodeBase58(sender), Address.decodeBase58(recvAddr), amount))
        val args = NativeBuildParams.createCodeParamsScript(list)
        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "approve", args, payer, gaslimit, gasprice)
    }

    /**
     *
     * @param sendAcct
     * @param fromAddr
     * @param toAddr
     * @param amount
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendTransferFrom(sendAcct: Account?, fromAddr: String, toAddr: String, amount: Long, payerAcct: Account?, gaslimit: Long, gasprice: Long): String? {
        if (sendAcct == null || payerAcct == null) {
            throw SDKException(ErrorCode.ParamErr("parameters should not be null"))
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"))
        }
        val tx = makeTransferFrom(sendAcct.getAddressU160().toBase58(), fromAddr, toAddr, amount, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(sendAcct)))
        if (sendAcct != payerAcct) {
            sdk.addSign(tx, payerAcct)
        }
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    /**
     * @param sender
     * @param fromAddr
     * @param toAddr
     * @param amount
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun makeTransferFrom(sender: String?, fromAddr: String?, toAddr: String?, amount: Long, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (sender == null || fromAddr == null || fromAddr == "" || toAddr == null || toAddr == "") {
            throw SDKException(ErrorCode.ParamErr("parameters should not be null"))
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"))
        }
        val list = ArrayList()
        list.add(Struct().add(Address.decodeBase58(sender), Address.decodeBase58(fromAddr), Address.decodeBase58(toAddr), amount))
        val args = NativeBuildParams.createCodeParamsScript(list)
        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "transferFrom", args, payer, gaslimit, gasprice)
    }

    /**
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun queryName(): String {
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "name", byteArrayOf(0), null, 0, 0)

        val obj = sdk.connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        return String(Helper.hexToBytes(res))
    }

    /**
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun querySymbol(): String {
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "symbol", byteArrayOf(0), null, 0, 0)
        val obj = sdk.connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        return String(Helper.hexToBytes(res))
    }

    /**
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun queryDecimals(): Long {
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "decimals", byteArrayOf(0), null, 0, 0)
        val obj = sdk.connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        return if ("" == res) {
            0
        } else java.lang.Long.valueOf(Helper.reverse(res), 16)
    }

    /**
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun queryTotalSupply(): Long {
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "totalSupply", byteArrayOf(0), null, 0, 0)
        val obj = sdk.connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        return if (res == null || res == "") {
            0
        } else java.lang.Long.valueOf(Helper.reverse(res), 16)
    }

}
