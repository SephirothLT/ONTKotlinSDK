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

package com.github.ontio.smartcontract.neovm

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.ontio.OntSdk
import com.github.ontio.account.Account
import com.github.ontio.common.Common
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.neovm.abi.AbiInfo
import com.github.ontio.smartcontract.neovm.abi.BuildParams

import java.io.ByteArrayInputStream
import java.io.IOException


class ClaimRecord(private val sdk: OntSdk) {
    var contractAddress: String? = "36bb5c053b6b839c8f6b923fe852f91239b9fccc"
        set(codeHash) {
            field = codeHash!!.replace("0x", "")
        }

    private val abi = "{\"hash\":\"0x36bb5c053b6b839c8f6b923fe852f91239b9fccc\",\"entrypoint\":\"Main\",\"functions\":[{\"name\":\"Main\",\"parameters\":[{\"name\":\"operation\",\"type\":\"String\"},{\"name\":\"args\",\"type\":\"Array\"}],\"returntype\":\"Any\"},{\"name\":\"Commit\",\"parameters\":[{\"name\":\"claimId\",\"type\":\"ByteArray\"},{\"name\":\"commiterId\",\"type\":\"ByteArray\"},{\"name\":\"ownerId\",\"type\":\"ByteArray\"}],\"returntype\":\"Boolean\"},{\"name\":\"Revoke\",\"parameters\":[{\"name\":\"claimId\",\"type\":\"ByteArray\"},{\"name\":\"ontId\",\"type\":\"ByteArray\"}],\"returntype\":\"Boolean\"},{\"name\":\"GetStatus\",\"parameters\":[{\"name\":\"claimId\",\"type\":\"ByteArray\"}],\"returntype\":\"ByteArray\"}],\"events\":[{\"name\":\"ErrorMsg\",\"parameters\":[{\"name\":\"id\",\"type\":\"ByteArray\"},{\"name\":\"error\",\"type\":\"String\"}],\"returntype\":\"Void\"},{\"name\":\"Push\",\"parameters\":[{\"name\":\"id\",\"type\":\"ByteArray\"},{\"name\":\"msg\",\"type\":\"String\"},{\"name\":\"args\",\"type\":\"ByteArray\"}],\"returntype\":\"Void\"}]}"

    /**
     *
     * @param issuerOntid
     * @param password
     * @param subjectOntid
     * @param claimId
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendCommit(issuerOntid: String?, password: String?, salt: ByteArray, subjectOntid: String?, claimId: String?, payerAcct: Account?, gaslimit: Long, gasprice: Long): String? {
        if (issuerOntid == null || issuerOntid == "" || password == null || password == "" || subjectOntid == null || subjectOntid == ""
                || claimId == null || claimId == "" || payerAcct == null) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("gaslimit or gasprice is less than 0"))
        }
        if (this.contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val addr = issuerOntid.replace(Common.didont, "")
        val tx = makeCommit(issuerOntid, subjectOntid, claimId, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, addr, password, salt)
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param issuerOntid
     * @param subjectOntid
     * @param claimId
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun makeCommit(issuerOntid: String?, subjectOntid: String?, claimId: String?, payer: String?, gaslimit: Long, gasprice: Long): Transaction {
        if (issuerOntid == null || issuerOntid == "" || subjectOntid == null || subjectOntid == "" || payer == null || payer == ""
                || claimId == null || claimId == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("gaslimit or gasprice is less than 0"))
        }

        val abiinfo = JSON.parseObject(abi, AbiInfo::class.java)
        val name = "Commit"
        val func = abiinfo.getFunction(name)
        func!!.name = name
        func.setParamsValue(claimId.toByteArray(), issuerOntid.toByteArray(), subjectOntid.toByteArray())
        val params = BuildParams.serializeAbiFunction(func)
        return sdk.vm().makeInvokeCodeTransaction(Helper.reverse(contractAddress!!), null, params, payer, gaslimit, gasprice)
    }

    /**
     *
     * @param issuerOntid
     * @param password
     * @param claimId
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendRevoke(issuerOntid: String?, password: String?, salt: ByteArray, claimId: String?, payerAcct: Account?, gaslimit: Long, gasprice: Long): String? {
        if (issuerOntid == null || issuerOntid == "" || password == null || password == ""
                || claimId == null || claimId == "" || payerAcct == null) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("gaslimit or gasprice is less than 0"))
        }
        if (this.contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val addr = issuerOntid.replace(Common.didont, "")
        val tx = makeRevoke(issuerOntid, claimId, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, addr, password, salt)
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    @Throws(Exception::class)
    fun makeRevoke(issuerOntid: String, claimId: String, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        val abiinfo = JSON.parseObject(abi, AbiInfo::class.java)
        val name = "Revoke"
        val func = abiinfo.getFunction(name)
        func!!.name = name
        func.setParamsValue(claimId.toByteArray(), issuerOntid.toByteArray())
        val params = BuildParams.serializeAbiFunction(func)
        return sdk.vm().makeInvokeCodeTransaction(Helper.reverse(contractAddress!!), null, params, payer, gaslimit, gasprice)
    }

    @Throws(Exception::class)
    fun sendGetStatus(claimId: String?): String {
        if (this.contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        if (claimId == null || claimId === "") {
            throw SDKException(ErrorCode.NullKeyOrValue)
        }
        val abiinfo = JSON.parseObject(abi, AbiInfo::class.java)
        val name = "GetStatus"
        val func = abiinfo.getFunction(name)
        func!!.name = name
        func.setParamsValue(*claimId.toByteArray())
        val obj = sdk.neovm().sendTransaction(Helper.reverse(this.contractAddress!!), null, null, 0, 0, func, true)
        val res = (obj as JSONObject).getString("Result")
        if (res == "") {
            return ""
        }
        val bais = ByteArrayInputStream(Helper.hexToBytes(res))
        val br = BinaryReader(bais)
        val claimTx = ClaimTx()
        claimTx.deserialize(br)
        return if (claimTx.status.size == 0) {
            String(claimTx.claimId) + "." + "00" + "." + String(claimTx.issuerOntId) + "." + String(claimTx.subjectOntId)
        } else String(claimTx.claimId) + "." + Helper.toHexString(claimTx.status) + "." + String(claimTx.issuerOntId) + "." + String(claimTx.subjectOntId)
    }
}

internal class ClaimTx : Serializable {
    lateinit var claimId: ByteArray
    lateinit var issuerOntId: ByteArray
    lateinit var subjectOntId: ByteArray
    lateinit var status: ByteArray

    constructor() {}
    constructor(claimId: ByteArray, issuerOntId: ByteArray, subjectOntId: ByteArray, status: ByteArray) {
        this.claimId = claimId
        this.issuerOntId = issuerOntId
        this.subjectOntId = subjectOntId
        this.status = status
    }

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
        val dataType = reader.readByte()
        val length = reader.readVarInt()
        val dataType2 = reader.readByte()
        this.claimId = reader.readVarBytes()
        val dataType3 = reader.readByte()
        this.issuerOntId = reader.readVarBytes()
        val dataType4 = reader.readByte()
        this.subjectOntId = reader.readVarBytes()
        val dataType5 = reader.readByte()
        this.status = reader.readVarBytes()
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {

    }
}
