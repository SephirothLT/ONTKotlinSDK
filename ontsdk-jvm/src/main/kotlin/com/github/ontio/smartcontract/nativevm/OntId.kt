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

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.ontio.OntSdk
import com.github.ontio.account.Account
import com.github.ontio.common.*
import com.github.ontio.core.DataSignature
import com.github.ontio.core.ontid.Attribute
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.crypto.Curve
import com.github.ontio.crypto.KeyType
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.merkle.MerkleVerifier
import com.github.ontio.sdk.claim.Claim
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.sdk.wallet.Identity
import com.github.ontio.smartcontract.nativevm.abi.NativeBuildParams
import com.github.ontio.smartcontract.nativevm.abi.Struct

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

class OntId(private val sdk: OntSdk) {
    val contractAddress = "0000000000000000000000000000000000000003"

    /**
     * @param ident
     * @param password
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @param isPreExec
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    @JvmOverloads
    fun sendRegister(ident: Identity?, password: String?, payerAcct: Account?, gaslimit: Long, gasprice: Long, isPreExec: Boolean = false): String {
        if (ident == null || password == null || password == "" || payerAcct == null) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val tx = makeRegister(ident.ontid, password, ident.controls[0].getSalt(), payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.walletMgr!!.writeWallet()
        sdk.signTx(tx, ident.ontid, password, ident.controls[0].getSalt())
        sdk.addSign(tx, payerAcct)
        if (isPreExec) {
            val obj = sdk.connect!!.sendRawTransactionPreExec(tx.toHexString())
            val result = (obj as JSONObject).getString("Result")
            if (Integer.parseInt(result) == 0) {
                throw SDKException(ErrorCode.OtherError("sendRawTransaction PreExec error: $obj"))
            }
        } else {
            val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
            if (!b) {
                throw SDKException(ErrorCode.SendRawTxError)
            }
        }
        return tx.hash().toHexString()
    }

    @Throws(Exception::class)
    fun sendRegisterPreExec(ident: Identity, password: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String {
        return sendRegister(ident, password, payerAcct, gaslimit, gasprice, true)
    }


    /**
     * @param ontid
     * @param password
     * @param payer
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun makeRegister(ontid: String, password: String?, salt: ByteArray, payer: String?, gaslimit: Long, gasprice: Long): Transaction {
        if (password == null || password == "" || payer == null || payer == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val info = sdk.walletMgr!!.getIdentityInfo(ontid, password, salt)
        val pk = Helper.hexToBytes(info.pubkey)

        val list = ArrayList()
        list.add(Struct().add(info.ontid, pk))
        val args = NativeBuildParams.createCodeParamsScript(list)
        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "regIDWithPublicKey", args, payer, gaslimit, gasprice)
    }


    /**
     * @param ident
     * @param password
     * @param attributes
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */

    @Throws(Exception::class)
    fun sendRegisterWithAttrs(ident: Identity?, password: String?, attributes: Array<Attribute>, payerAcct: Account?, gaslimit: Long, gasprice: Long): String {
        if (ident == null || password == null || password == "" || payerAcct == null) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val info = sdk.walletMgr!!.getIdentityInfo(ident.ontid, password, ident.controls[0].getSalt())
        val ontid = info.ontid
        val tx = makeRegisterWithAttrs(ontid, password, ident.controls[0].getSalt(), attributes, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, ontid!!, password, ident.controls[0].getSalt())
        sdk.addSign(tx, payerAcct)
        val identity = sdk.walletMgr!!.wallet!!.addOntIdController(ontid, info.encryptedPrikey!!, info.ontid!!)
        sdk.walletMgr!!.writeWallet()
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return tx.hash().toHexString()
    }

    /**
     * @param ontid
     * @param password
     * @param attributes
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun makeRegisterWithAttrs(ontid: String?, password: String?, salt: ByteArray, attributes: Array<Attribute>, payer: String?, gaslimit: Long, gasprice: Long): Transaction {
        if (password == null || password == "" || payer == null || payer == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val info = sdk.walletMgr!!.getIdentityInfo(ontid!!, password, salt)
        val pk = Helper.hexToBytes(info.pubkey)

        val list = ArrayList()
        val struct = Struct().add(ontid.toByteArray(), pk)
        struct.add(attributes.size)
        for (i in attributes.indices) {
            struct.add(attributes[i].key, attributes[i].valueType, attributes[i].value)
        }
        list.add(struct)
        val args = NativeBuildParams.createCodeParamsScript(list)
        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "regIDWithAttributes", args, payer, gaslimit, gasprice)
    }

    @Throws(Exception::class)
    private fun serializeAttributes(attributes: Array<Attribute>): ByteArray {
        val baos = ByteArrayOutputStream()
        val bw = BinaryWriter(baos)
        bw.writeSerializableArray(attributes)
        return baos.toByteArray()
    }

    /**
     * @param ontid
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendGetPublicKeys(ontid: String?): String {
        if (ontid == null || ontid == "") {
            throw SDKException(ErrorCode.ParamErr("ontid should not be null"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }

        val list = ArrayList()
        list.add(ontid.toByteArray())
        val arg = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "getPublicKeys", arg, null, 0, 0)

        val obj = sdk.connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        if (res == "") {
            return res
        }
        val bais = ByteArrayInputStream(Helper.hexToBytes(res))
        val br = BinaryReader(bais)
        val pubKeyList = ArrayList()
        while (true) {
            try {
                val publicKeyMap = HashMap()
                publicKeyMap.put("PubKeyId", ontid + "#keys-" + br.readInt().toString())
                val pubKey = br.readVarBytes()
                publicKeyMap.put("Type", KeyType.fromLabel(pubKey[0]))
                publicKeyMap.put("Curve", Curve.fromLabel(pubKey[1]))
                publicKeyMap.put("Value", Helper.toHexString(pubKey))
                pubKeyList.add(publicKeyMap)
            } catch (e: Exception) {
                break
            }

        }
        return JSON.toJSONString(pubKeyList)
    }

    /**
     * @param ontid
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendGetKeyState(ontid: String?, index: Int): String {
        if (ontid == null || ontid == "" || index < 0) {
            throw SDKException(ErrorCode.ParamErr("parameter is wrong"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        //        byte[] parabytes = NativeBuildParams.buildParams(ontid.getBytes(), index);
        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress, "getKeyState", parabytes, null, 0, 0);

        val list = ArrayList()
        list.add(Struct().add(ontid.toByteArray(), index))
        val arg = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "getKeyState", arg, null, 0, 0)

        val obj = sdk.connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        return if (res == "") {
            res
        } else String(Helper.hexToBytes(res))
    }

    @Throws(Exception::class)
    fun sendGetAttributes(ontid: String?): String {
        if (ontid == null || ontid == "") {
            throw SDKException(ErrorCode.ParamErr("ontid should not be null"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }


        val list = ArrayList()
        list.add(ontid.toByteArray())
        val arg = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "getAttributes", arg, null, 0, 0)

        val obj = sdk.connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        if (res == "") {
            return res
        }

        val bais = ByteArrayInputStream(Helper.hexToBytes(res))
        val br = BinaryReader(bais)
        val attrsList = ArrayList()
        while (true) {
            try {
                val attributeMap = HashMap()
                attributeMap.put("Key", String(br.readVarBytes()))
                attributeMap.put("Type", String(br.readVarBytes()))
                attributeMap.put("Value", String(br.readVarBytes()))
                attrsList.add(attributeMap)
            } catch (e: Exception) {
                break
            }

        }

        return JSON.toJSONString(attrsList)
    }


    /**
     * @param ontid
     * @param password
     * @param newpubkey
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendAddPubKey(ontid: String, password: String, salt: ByteArray, newpubkey: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        return sendAddPubKey(ontid, null, password, salt, newpubkey, payerAcct, gaslimit, gasprice)
    }

    /**
     * @param ontid
     * @param recoveryOntid
     * @param password
     * @param newpubkey
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendAddPubKey(ontid: String?, recoveryOntid: String?, password: String?, salt: ByteArray, newpubkey: String, payerAcct: Account?, gaslimit: Long, gasprice: Long): String? {
        if (ontid == null || ontid == "" || password == null || password == "" || payerAcct == null) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val tx = makeAddPubKey(ontid, recoveryOntid, password, salt, newpubkey, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        val addr: String
        if (recoveryOntid != null) {
            addr = recoveryOntid.replace(Common.didont, "")
        } else {
            addr = ontid.replace(Common.didont, "")
        }
        sdk.signTx(tx, addr, password, salt)
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param ontid
     * @param password
     * @param newpubkey
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun makeAddPubKey(ontid: String, password: String, salt: ByteArray, newpubkey: String, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        return makeAddPubKey(ontid, null, password, salt, newpubkey, payer, gaslimit, gasprice)
    }

    /**
     * @param ontid
     * @param recoveryOntid
     * @param password
     * @param newpubkey
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun makeAddPubKey(ontid: String?, recoveryOntid: String?, password: String, salt: ByteArray, newpubkey: String?, payer: String?, gaslimit: Long, gasprice: Long): Transaction {
        if (ontid == null || ontid == "" || payer == null || payer == "" || newpubkey == null || newpubkey == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val arg: ByteArray
        if (recoveryOntid == null) {
            val info = sdk.walletMgr!!.getAccountInfo(ontid, password, salt)
            val pk = Helper.hexToBytes(info.pubkey)
            //            parabytes = NativeBuildParams.buildParams(ontid.getBytes(), Helper.hexToBytes(newpubkey), pk);

            val list = ArrayList()
            list.add(Struct().add(ontid.toByteArray(), Helper.hexToBytes(newpubkey), pk))
            arg = NativeBuildParams.createCodeParamsScript(list)
        } else {
            //            parabytes = NativeBuildParams.buildParams(ontid, Helper.hexToBytes(newpubkey), Address.decodeBase58(recoveryOntid.replace(Common.didont,"")).toArray());

            val list = ArrayList()
            list.add(Struct().add(ontid.toByteArray(), Helper.hexToBytes(newpubkey), Address.decodeBase58(recoveryOntid.replace(Common.didont, "")).toArray()))
            arg = NativeBuildParams.createCodeParamsScript(list)
        }
        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress, "addKey", parabytes,payer, gaslimit, gasprice);


        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "addKey", arg, payer, gaslimit, gasprice)
    }


    /**
     * @param ontid
     * @param password
     * @param removePubkey
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendRemovePubKey(ontid: String, password: String, salt: ByteArray, removePubkey: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        return sendRemovePubKey(ontid, null, password, salt, removePubkey, payerAcct, gaslimit, gasprice)
    }

    /**
     * @param ontid
     * @param recoveryOntid
     * @param password
     * @param removePubkey
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendRemovePubKey(ontid: String?, recoveryOntid: String?, password: String?, salt: ByteArray, removePubkey: String, payerAcct: Account?, gaslimit: Long, gasprice: Long): String? {
        if (ontid == null || ontid == "" || password == null || password == "" || payerAcct == null) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val tx = makeRemovePubKey(ontid, recoveryOntid, password, salt, removePubkey, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        val addr: String
        if (recoveryOntid == null) {
            addr = ontid.replace(Common.didont, "")
        } else {
            addr = recoveryOntid.replace(Common.didont, "")
        }
        sdk.signTx(tx, addr, password, salt)
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param ontid
     * @param password
     * @param removePubkey
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun makeRemovePubKey(ontid: String, password: String, salt: ByteArray, removePubkey: String, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        return makeRemovePubKey(ontid, null, password, salt, removePubkey, payer, gaslimit, gasprice)
    }

    /**
     * @param ontid
     * @param recoveryAddr
     * @param password
     * @param removePubkey
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun makeRemovePubKey(ontid: String?, recoveryAddr: String?, password: String?, salt: ByteArray, removePubkey: String?, payer: String?, gaslimit: Long, gasprice: Long): Transaction {
        if (ontid == null || ontid == "" || password == null || password == "" || payer == null || payer == "" || removePubkey == null || removePubkey == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val arg: ByteArray
        if (recoveryAddr == null) {
            val info = sdk.walletMgr!!.getAccountInfo(ontid.replace(Common.didont, ""), password, salt)
            val pk = Helper.hexToBytes(info.pubkey)
            //            parabytes = NativeBuildParams.buildParams(ontid, Helper.hexToBytes(removePubkey), pk);
            val list = ArrayList()
            list.add(Struct().add(ontid.toByteArray(), Helper.hexToBytes(removePubkey), pk))
            arg = NativeBuildParams.createCodeParamsScript(list)

        } else {
            //            parabytes = NativeBuildParams.buildParams(ontid, Helper.hexToBytes(removePubkey), Address.decodeBase58(recoveryAddr).toArray());
            val list = ArrayList()
            list.add(Struct().add(ontid.toByteArray(), Helper.hexToBytes(removePubkey), Address.decodeBase58(recoveryAddr.replace(Common.didont, "")).toArray()))
            arg = NativeBuildParams.createCodeParamsScript(list)
        }

        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress, "removeKey", parabytes, payer, gaslimit, gasprice);

        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "removeKey", arg, payer, gaslimit, gasprice)
    }

    /**
     * @param ontid
     * @param password
     * @param recoveryAddr
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */

    @Throws(Exception::class)
    fun sendAddRecovery(ontid: String?, password: String?, salt: ByteArray, recoveryAddr: String?, payerAcct: Account?, gaslimit: Long, gasprice: Long): String? {
        if (ontid == null || ontid == "" || password == null || password == "" || payerAcct == null || recoveryAddr == null || recoveryAddr == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val addr = ontid.replace(Common.didont, "")
        val tx = makeAddRecovery(ontid, password, salt, recoveryAddr, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, addr, password, salt)
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param ontid
     * @param password
     * @param recoveryAddr
     * @param payer
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun makeAddRecovery(ontid: String?, password: String?, salt: ByteArray, recoveryAddr: String?, payer: String?, gaslimit: Long, gasprice: Long): Transaction {
        if (ontid == null || ontid == "" || password == null || password == "" || payer == null || payer == "" || recoveryAddr == null || recoveryAddr == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val addr = ontid.replace(Common.didont, "")
        val info = sdk.walletMgr!!.getAccountInfo(addr, password, salt)
        val pk = Helper.hexToBytes(info.pubkey)

        val list = ArrayList()
        list.add(Struct().add(ontid, Address.decodeBase58(recoveryAddr), pk))
        val arg = NativeBuildParams.createCodeParamsScript(list)
        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "addRecovery", arg, payer, gaslimit, gasprice)
    }

    /**
     * @param ontid
     * @param password
     * @param newRecovery
     * @param oldRecovery
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendChangeRecovery(ontid: String?, newRecovery: String, oldRecovery: String, password: String?, salt: ByteArray, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (ontid == null || ontid == "" || password == null || password == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val tx = makeChangeRecovery(ontid, newRecovery, oldRecovery, password, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, oldRecovery, password, salt)
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param ontid
     * @param newRecoveryOntId
     * @param oldRecoveryOntId
     * @param password
     * @param gasprice
     * @return
     * @throws SDKException
     */
    @Throws(SDKException::class)
    fun makeChangeRecovery(ontid: String?, newRecoveryOntId: String?, oldRecoveryOntId: String?, password: String?, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (ontid == null || ontid == "" || password == null || password == "" || newRecoveryOntId == null || oldRecoveryOntId == null) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val newAddr = Address.decodeBase58(newRecoveryOntId.replace(Common.didont, ""))
        val oldAddr = Address.decodeBase58(oldRecoveryOntId.replace(Common.didont, ""))

        val list = ArrayList()
        list.add(Struct().add(ontid, newAddr, oldAddr))
        val arg = NativeBuildParams.createCodeParamsScript(list)
        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "changeRecovery", arg, payer, gaslimit, gasprice)
    }

    /**
     *
     * @param ontid
     * @param newRecoveryOntId
     * @param oldRecoveryOntId
     * @param accounts
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    private fun sendChangeRecovery(ontid: String?, newRecoveryOntId: String?, oldRecoveryOntId: String?, accounts: Array<Account>?, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (ontid == null || ontid == "" || accounts == null || accounts.size == 0 || newRecoveryOntId == null || oldRecoveryOntId == null) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val newAddr = Address.decodeBase58(newRecoveryOntId.replace(Common.didont, ""))
        val oldAddr = Address.decodeBase58(oldRecoveryOntId.replace(Common.didont, ""))
        val parabytes = NativeBuildParams.buildParams(ontid.toByteArray(), newAddr.toArray(), oldAddr.toArray())
        val tx = sdk.vm().makeInvokeCodeTransaction(contractAddress, "changeRecovery", parabytes, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(accounts))
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param ontid
     * @param password
     * @param attributes
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendAddAttributes(ontid: String?, password: String?, salt: ByteArray, attributes: Array<Attribute>?, payerAcct: Account?, gaslimit: Long, gasprice: Long): String? {
        if (ontid == null || ontid == "" || password == null || attributes == null || attributes.size == 0 || payerAcct == null) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val addr = ontid.replace(Common.didont, "")
        val tx = makeAddAttributes(ontid, password, salt, attributes, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, addr, password, salt)
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param ontid
     * @param password
     * @param attributes
     * @param payer
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun makeAddAttributes(ontid: String?, password: String?, salt: ByteArray, attributes: Array<Attribute>?, payer: String?, gaslimit: Long, gasprice: Long): Transaction {
        var password = password
        if (ontid == null || ontid == "" || password == null || attributes == null || attributes.size == 0 || payer == null || payer == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val addr = ontid.replace(Common.didont, "")
        val info = sdk.walletMgr!!.getAccountInfo(addr, password, salt)
        password = null
        val pk = Helper.hexToBytes(info.pubkey)
        val list = ArrayList()
        val struct = Struct().add(*ontid.toByteArray())
        struct.add(attributes.size)
        for (i in attributes.indices) {
            struct.add(attributes[i].key, attributes[i].valueType, attributes[i].value)
        }
        struct.add(*pk)
        list.add(struct)
        val args = NativeBuildParams.createCodeParamsScript(list)

        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "addAttributes", args, payer, gaslimit, gasprice)
    }

    /**
     * @param ontid
     * @param password
     * @param path
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendRemoveAttribute(ontid: String?, password: String?, salt: ByteArray, path: String?, payerAcct: Account?, gaslimit: Long, gasprice: Long): String? {
        if (ontid == null || ontid == "" || password == null || payerAcct == null || path == null || path == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val addr = ontid.replace(Common.didont, "")
        val tx = makeRemoveAttribute(ontid, password, salt, path, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, addr, password, salt)
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param ontid
     * @param password
     * @param path
     * @param payer
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun makeRemoveAttribute(ontid: String?, password: String?, salt: ByteArray, path: String?, payer: String?, gaslimit: Long, gasprice: Long): Transaction {
        if (ontid == null || ontid == "" || password == null || payer == null || payer == "" || path == null || path == "" || payer == null || payer == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }
        val addr = ontid.replace(Common.didont, "")
        val info = sdk.walletMgr!!.getAccountInfo(addr, password, salt)
        val pk = Helper.hexToBytes(info.pubkey)

        val list = ArrayList()
        list.add(Struct().add(ontid.toByteArray(), path.toByteArray(), pk))
        val arg = NativeBuildParams.createCodeParamsScript(list)
        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "removeAttribute", arg, payer, gaslimit, gasprice)
    }

    /**
     * @param txhash
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun getMerkleProof(txhash: String?): Any {
        if (txhash == null || txhash == "") {
            throw SDKException(ErrorCode.ParamErr("txhash should not be null"))
        }
        val proof = HashMap()
        val map = HashMap()
        val height = sdk.connect!!.getBlockHeightByTxHash(txhash)
        map.put("Type", "MerkleProof")
        map.put("TxnHash", txhash)
        map.put("BlockHeight", height)

        val tmpProof = sdk.connect!!.getMerkleProof(txhash) as Map<*, *>
        val txroot = UInt256.parse(tmpProof["TransactionsRoot"] as String)
        val blockHeight = tmpProof["BlockHeight"] as Int
        val curBlockRoot = UInt256.parse(tmpProof["CurBlockRoot"] as String)
        val curBlockHeight = tmpProof["CurBlockHeight"] as Int
        val hashes = tmpProof["TargetHashes"] as List<*>
        val targetHashes = arrayOfNulls<UInt256>(hashes.size)
        for (i in hashes.indices) {
            targetHashes[i] = UInt256.parse(hashes[i] as String)
        }
        map.put("MerkleRoot", curBlockRoot.toHexString())
        map.put("Nodes", MerkleVerifier.getProof(txroot, blockHeight, targetHashes, curBlockHeight + 1))
        proof.put("Proof", map)
        return proof
    }

    /**
     * @param merkleProof
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun verifyMerkleProof(merkleProof: String?): Boolean {
        if (merkleProof == null || merkleProof == "") {
            throw SDKException(ErrorCode.ParamErr("claim should not be null"))
        }
        try {
            val obj = JSON.parseObject(merkleProof)
            val proof = obj.getJSONObject("Proof") as Map<*, *>
            val txhash = proof["TxnHash"] as String
            val blockHeight = proof["BlockHeight"] as Int
            val merkleRoot = UInt256.parse(proof["MerkleRoot"] as String)
            val block = sdk.connect!!.getBlock(blockHeight)
            if (block!!.height !== blockHeight) {
                throw SDKException("blockHeight not match")
            }
            var containTx = false
            for (i in 0 until block!!.transactions.length) {
                if (block.transactions[i].hash().toHexString().equals(txhash)) {
                    containTx = true
                }
            }
            if (!containTx) {
                throw SDKException(ErrorCode.OtherError("not contain this tx"))
            }
            val txsroot = block.transactionsRoot

            val nodes = proof["Nodes"] as List<*>
            return MerkleVerifier.Verify(txsroot, nodes, merkleRoot)
        } catch (e: Exception) {
            e.printStackTrace()
            throw SDKException(e)
        }

    }

    /**
     * @param signerOntid
     * @param password
     * @param context
     * @param claimMap
     * @param metaData
     * @param clmRevMap
     * @param expire
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun createOntIdClaim(signerOntid: String?, password: String?, salt: ByteArray, context: String?, claimMap: Map<String, Any>?, metaData: Map<*, *>?, clmRevMap: Map<*, *>?, expire: Long): String {
        if (signerOntid == null || signerOntid == "" || password == null || password == "" || context == null || context == "" || claimMap == null || metaData == null || clmRevMap == null || expire < 0) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (expire < System.currentTimeMillis() / 1000) {
            throw SDKException(ErrorCode.ExpireErr)
        }
        var claim: Claim? = null
        try {
            val sendDid = metaData["Issuer"] as String
            val receiverDid = metaData["Subject"] as String
            if (sendDid == null || receiverDid == null) {
                throw SDKException(ErrorCode.DidNull)
            }
            val issuerDdo = sendGetDDO(sendDid)
            val owners = JSON.parseObject(issuerDdo).getJSONArray("Owners")
                    ?: throw SDKException(ErrorCode.NotExistCliamIssuer)
            var pubkeyId: String? = null
            val acct = sdk.walletMgr!!.getAccount(signerOntid, password, salt)
            val pk = Helper.toHexString(acct.serializePublicKey())
            for (i in owners.indices) {
                val obj = owners.getJSONObject(i)
                if (obj.getString("Value") == pk) {
                    pubkeyId = obj.getString("PubKeyId")
                    break
                }
            }
            if (pubkeyId == null) {
                throw SDKException(ErrorCode.NotFoundPublicKeyId)
            }
            val receiverDidStr = receiverDid.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (receiverDidStr.size != 3) {
                throw SDKException(ErrorCode.DidError)
            }
            claim = Claim(sdk.walletMgr!!.signatureScheme!!, acct, context, claimMap, metaData, clmRevMap, pubkeyId, expire)
            return claim.claimStr
        } catch (e: SDKException) {
            throw SDKException(ErrorCode.CreateOntIdClaimErr)
        }

    }

    /**
     * @param claim
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun verifyOntIdClaim(claim: String?): Boolean {
        if (claim == null) {
            throw SDKException(ErrorCode.ParamErr("claim should not be null"))
        }
        var sign: DataSignature? = null
        try {

            val obj = claim.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (obj.size != 3) {
                throw SDKException(ErrorCode.ParamError)
            }
            val payloadBytes = Base64.getDecoder().decode(obj[1].toByteArray())
            val payloadObj = JSON.parseObject(String(payloadBytes))
            val issuerDid = payloadObj.getString("iss")
            val str = issuerDid.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (str.size != 3) {
                throw SDKException(ErrorCode.DidError)
            }
            val issuerDdo = sendGetDDO(issuerDid)
            val owners = JSON.parseObject(issuerDdo).getJSONArray("Owners")
                    ?: throw SDKException(ErrorCode.NotExistCliamIssuer)
            val signatureBytes = Base64.getDecoder().decode(obj[2])
            val headerBytes = Base64.getDecoder().decode(obj[0].toByteArray())
            val header = JSON.parseObject(String(headerBytes))
            val kid = header.getString("kid")
            val id = kid.split("#keys-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            val pubkeyStr = owners.getJSONObject(Integer.parseInt(id) - 1).getString("Value")
            sign = DataSignature()
            val data = (obj[0] + "." + obj[1]).toByteArray()
            return sign.verifySignature(Account(false, Helper.hexToBytes(pubkeyStr)), data, signatureBytes)
        } catch (e: Exception) {
            throw SDKException(ErrorCode.VerifyOntIdClaimErr)
        }

    }


    /**
     * @param ontid
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendGetDDO(ontid: String?): String {
        if (ontid == null) {
            throw SDKException(ErrorCode.ParamErr("ontid should not be null"))
        }
        if (contractAddress == null) {
            throw SDKException(ErrorCode.NullCodeHash)
        }

        val list = ArrayList()
        list.add(ontid.toByteArray())
        val arg = NativeBuildParams.createCodeParamsScript(list)

        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "getDDO", arg, null, 0, 0)
        val obj = sdk.connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        if (res == "") {
            return res
        }
        val map = parseDdoData(ontid, res)
        return if (map.size == 0) {
            ""
        } else JSON.toJSONString(map)
    }

    @Throws(Exception::class)
    private fun parseDdoData(ontid: String, obj: String): Map<*, *> {
        val bys = Helper.hexToBytes(obj)

        val bais = ByteArrayInputStream(bys)
        val br = BinaryReader(bais)
        var publickeyBytes: ByteArray
        var attributeBytes: ByteArray
        var recoveryBytes: ByteArray
        try {
            publickeyBytes = br.readVarBytes()
        } catch (e: Exception) {
            publickeyBytes = byteArrayOf()
        }

        try {
            attributeBytes = br.readVarBytes()
        } catch (e: Exception) {
            e.printStackTrace()
            attributeBytes = byteArrayOf()
        }

        try {
            recoveryBytes = br.readVarBytes()
        } catch (e: Exception) {
            recoveryBytes = byteArrayOf()
        }

        val pubKeyList = ArrayList()
        if (publickeyBytes.size != 0) {
            val bais1 = ByteArrayInputStream(publickeyBytes)
            val br1 = BinaryReader(bais1)
            while (true) {
                try {
                    val publicKeyMap = HashMap()
                    publicKeyMap.put("PubKeyId", ontid + "#keys-" + br1.readInt().toString())
                    val pubKey = br1.readVarBytes()
                    if (pubKey.size == 33) {
                        publicKeyMap.put("Type", KeyType.ECDSA.name)
                        publicKeyMap.put("Curve", Curve.P256)
                        publicKeyMap.put("Value", Helper.toHexString(pubKey))
                    } else {
                        publicKeyMap.put("Type", KeyType.fromLabel(pubKey[0]))
                        publicKeyMap.put("Curve", Curve.fromLabel(pubKey[1]))
                        publicKeyMap.put("Value", Helper.toHexString(pubKey))
                    }

                    pubKeyList.add(publicKeyMap)
                } catch (e: Exception) {
                    break
                }

            }
        }
        val attrsList = ArrayList()
        if (attributeBytes.size != 0) {
            val bais2 = ByteArrayInputStream(attributeBytes)
            val br2 = BinaryReader(bais2)
            while (true) {
                try {
                    val attributeMap = HashMap()
                    attributeMap.put("Key", String(br2.readVarBytes()))
                    attributeMap.put("Type", String(br2.readVarBytes()))
                    attributeMap.put("Value", String(br2.readVarBytes()))
                    attrsList.add(attributeMap)
                } catch (e: Exception) {
                    break
                }

            }
        }

        val map = HashMap()
        map.put("Owners", pubKeyList)
        map.put("Attributes", attrsList)
        if (recoveryBytes.size != 0) {
            map.put("Recovery", Address.parse(Helper.toHexString(recoveryBytes)).toBase58())
        }
        map.put("OntId", ontid)
        return map
    }
}
