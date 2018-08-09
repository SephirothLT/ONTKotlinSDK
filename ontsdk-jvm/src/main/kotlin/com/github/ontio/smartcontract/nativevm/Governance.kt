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
import com.github.ontio.core.VmType
import com.github.ontio.core.asset.Sig
import com.github.ontio.core.governance.PeerPoolItem
import com.github.ontio.core.governance.VoteInfo
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable
import com.github.ontio.network.exception.ConnectorException
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.nativevm.abi.NativeBuildParams
import com.github.ontio.smartcontract.nativevm.abi.Struct

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

class Governance(private val sdk: OntSdk) {
    val contractAddress = "0000000000000000000000000000000000000007"
    private val VOTE_INFO_POOL = "voteInfoPool"

    /**
     *
     * @return
     * @throws ConnectorException
     * @throws IOException
     */
    val peerInfoAll: String?
        @Throws(ConnectorException::class, IOException::class)
        get() = getPeerPoolMap(null)

    /**
     *
     * @param account
     * @param peerPubkey
     * @param initPos
     * @param ontid
     * @param ontidpwd
     * @param keyNo
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun registerCandidate(account: Account, peerPubkey: String, initPos: Long, ontid: String, ontidpwd: String, salt: ByteArray, keyNo: Long, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        //        byte[] params = new RegisterCandidateParam(peerPubkey,account.getAddressU160(),initPos,ontid.getBytes(),keyNo).toArray();
        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress,"registerCandidate",params,payerAcct.getAddressU160().toBase58(),gaslimit,gasprice);

        val list = ArrayList()
        list.add(Struct().add(peerPubkey, account.getAddressU160(), initPos, ontid.toByteArray(), keyNo))
        val args = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "registerCandidate", args, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(account)))
        sdk.addSign(tx, ontid, ontidpwd, salt)
        if (account != payerAcct) {
            sdk.addSign(tx, payerAcct)
        }
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    @Throws(Exception::class)
    fun unRegisterCandidate(account: Account, peerPubkey: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {

        val list = ArrayList()
        list.add(Struct().add(peerPubkey, account.getAddressU160()))
        val args = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "unRegisterCandidate", args, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(account)))
        if (account != payerAcct) {
            sdk.addSign(tx, payerAcct)
        }
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    @Throws(Exception::class)
    fun withdrawOng(account: Account, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        val list = ArrayList()
        list.add(Struct().add(account.getAddressU160()))
        val args = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "withdrawOng", args, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(account)))
        if (account != payerAcct) {
            sdk.addSign(tx, payerAcct)
        }
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param peerPubkey
     * @return
     * @throws ConnectorException
     * @throws IOException
     */
    @Throws(ConnectorException::class, IOException::class)
    fun getPeerInfo(peerPubkey: String): String? {
        return getPeerPoolMap(peerPubkey)
    }

    /**
     *
     * @return
     * @throws ConnectorException
     * @throws IOException
     */
    @Throws(ConnectorException::class, IOException::class)
    private fun getPeerPoolMap(peerPubkey: String?): String? {
        val view = sdk.connect!!.getStorage(Helper.reverse(contractAddress), Helper.toHexString("governanceView".toByteArray()))
        val governanceView = GovernanceView()
        val bais = ByteArrayInputStream(Helper.hexToBytes(view))
        val br = BinaryReader(bais)
        governanceView.deserialize(br)
        val baos = ByteArrayOutputStream()
        val bw = BinaryWriter(baos)
        bw.writeInt(governanceView.view)

        val viewBytes = baos.toByteArray()
        val peerPoolBytes = "peerPool".toByteArray()
        val keyBytes = ByteArray(peerPoolBytes.size + viewBytes.size)
        System.arraycopy(peerPoolBytes, 0, keyBytes, 0, peerPoolBytes.size)
        System.arraycopy(viewBytes, 0, keyBytes, peerPoolBytes.size, viewBytes.size)
        val value = sdk.connect!!.getStorage(Helper.reverse(contractAddress), Helper.toHexString(keyBytes))
        val bais2 = ByteArrayInputStream(Helper.hexToBytes(value))
        val reader = BinaryReader(bais2)
        val length = reader.readInt()
        val peerPoolMap = HashMap<String, PeerPoolItem>()
        for (i in 0 until length) {
            val item = PeerPoolItem()
            item.deserialize(reader)
            peerPoolMap[item.peerPubkey] = item.Json()
        }
        return if (peerPubkey != null) {
            if (!peerPoolMap.containsKey(peerPubkey)) {
                null
            } else JSON.toJSONString(peerPoolMap[peerPubkey])
        } else JSON.toJSONString(peerPoolMap)
    }

    /**
     *
     * @param peerPubkey
     * @param addr
     * @return
     */
    fun getVoteInfo(peerPubkey: String, addr: Address): VoteInfo? {
        val peerPubkeyPrefix = Helper.hexToBytes(peerPubkey)
        val address = addr.toArray()
        val voteInfoPool = VOTE_INFO_POOL.toByteArray()
        val key = ByteArray(voteInfoPool.size + peerPubkeyPrefix.size + address.size)
        System.arraycopy(voteInfoPool, 0, key, 0, voteInfoPool.size)
        System.arraycopy(peerPubkeyPrefix, 0, key, voteInfoPool.size, peerPubkeyPrefix.size)
        System.arraycopy(address, 0, key, voteInfoPool.size + peerPubkeyPrefix.size, address.size)
        var res: String? = null
        try {
            res = sdk.connect!!.getStorage(Helper.reverse(contractAddress), Helper.toHexString(key))
            if (res != null && res != "") {
                return Serializable.from(Helper.hexToBytes(res), VoteInfo::class.java)
            }
        } catch (e: ConnectorException) {
            return null
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        }

        return null
    }

    /**
     *
     * @param peerPubkey
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun approveCandidate(adminAccount: Account, peerPubkey: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        //        byte[] params = new ApproveCandidateParam(peerPubkey).toArray();
        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress,"approveCandidate",params,payerAcct.getAddressU160().toBase58(),gaslimit,gasprice);

        val list = ArrayList()
        list.add(Struct().add(peerPubkey))
        val args = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "approveCandidate", args, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(adminAccount)))
        if (adminAccount != payerAcct) {
            sdk.addSign(tx, payerAcct)
        }
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param multiAddress
     * @param M
     * @param accounts
     * @param publicKeys
     * @param peerPubkey
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun approveCandidate(multiAddress: Address, M: Int, accounts: Array<Account>, publicKeys: Array<ByteArray>, peerPubkey: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {

        val pks = arrayOfNulls<ByteArray>(accounts.size + publicKeys.size)
        for (i in accounts.indices) {
            pks[i] = accounts[i].serializePublicKey()
        }
        for (i in publicKeys.indices) {
            pks[i + accounts.size] = publicKeys[i]
        }
        if (multiAddress != Address.addressFromMultiPubKeys(M, pks)) {
            throw SDKException(ErrorCode.ParamErr("mutilAddress doesnot match accounts and publicKeys"))
        }
        val list = ArrayList()
        list.add(Struct().add(peerPubkey))
        val args = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "approveCandidate", args, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        val sigs = arrayOfNulls<Sig>(1)
        sigs[0] = Sig()
        sigs[0].pubKeys = arrayOfNulls(pks.size)
        sigs[0].sigData = arrayOfNulls(M)
        sigs[0].M = M
        for (i in pks.indices) {
            sigs[0].pubKeys[i] = pks[i]
        }
        for (i in 0 until sigs[0].M) {
            val signature = tx.sign(accounts[i], accounts[i].getSignatureScheme())
            sigs[0].sigData[i] = signature
        }
        tx.sigs = sigs
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param peerPubkey
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun rejectCandidate(adminAccount: Account, peerPubkey: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        //        byte[] params = new RejectCandidateParam(peerPubkey).toArray();
        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress,"rejectCandidate",params, payerAcct.getAddressU160().toBase58(),gaslimit,gasprice);

        val list = ArrayList()
        list.add(Struct().add(peerPubkey))
        val args = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "rejectCandidate", args, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(adminAccount)))
        if (adminAccount != payerAcct) {
            sdk.addSign(tx, payerAcct)
        }
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param multiAddress
     * @param M
     * @param accounts
     * @param publicKeys
     * @param peerPubkey
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun rejectCandidate(multiAddress: Address, M: Int, accounts: Array<Account>, publicKeys: Array<ByteArray>, peerPubkey: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        val pks = arrayOfNulls<ByteArray>(accounts.size + publicKeys.size)
        for (i in accounts.indices) {
            pks[i] = accounts[i].serializePublicKey()
        }
        for (i in publicKeys.indices) {
            pks[i + accounts.size] = publicKeys[i]
        }
        if (multiAddress != Address.addressFromMultiPubKeys(M, pks)) {
            throw SDKException(ErrorCode.ParamErr("mutilAddress doesnot match accounts and publicKeys"))
        }
        val list = ArrayList()
        list.add(Struct().add(peerPubkey))
        val args = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "rejectCandidate", args, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        val sigs = arrayOfNulls<Sig>(1)
        sigs[0] = Sig()
        sigs[0].pubKeys = arrayOfNulls(pks.size)
        sigs[0].sigData = arrayOfNulls(M)
        sigs[0].M = M
        for (i in pks.indices) {
            sigs[0].pubKeys[i] = pks[i]
        }
        for (i in 0 until sigs[0].M) {
            val signature = tx.sign(accounts[i], accounts[i].getSignatureScheme())
            sigs[0].sigData[i] = signature
        }
        tx.sigs = sigs
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param account
     * @param peerPubkey
     * @param posList
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun voteForPeer(account: Account, peerPubkey: Array<String>, posList: LongArray, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (peerPubkey.size != posList.size) {
            throw SDKException(ErrorCode.ParamError)
        }
        val map = HashMap()
        for (i in peerPubkey.indices) {
            map.put(peerPubkey[i], posList[i])
        }
        //        byte[] params = new VoteForPeerParam(account.getAddressU160(),peerPubkey,posList).toArray();
        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress,"voteForPeer",params,payerAcct.getAddressU160().toBase58(),gaslimit,gasprice);

        val list = ArrayList()
        val struct = Struct()
        struct.add(account.getAddressU160())
        struct.add(peerPubkey.size)
        for (i in peerPubkey.indices) {
            struct.add(peerPubkey[i])
        }
        struct.add(posList.size)
        for (i in peerPubkey.indices) {
            struct.add(posList[i])
        }
        list.add(struct)
        val args = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "voteForPeer", args, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(account)))
        if (account != payerAcct) {
            sdk.addSign(tx, payerAcct)
        }
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param account
     * @param peerPubkey
     * @param posList
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun unVoteForPeer(account: Account, peerPubkey: Array<String>, posList: LongArray, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (peerPubkey.size != posList.size) {
            throw SDKException(ErrorCode.ParamError)
        }
        val map = HashMap()
        for (i in peerPubkey.indices) {
            map.put(peerPubkey[i], posList[i])
        }
        //        byte[] params = new VoteForPeerParam(account.getAddressU160(),peerPubkey,posList).toArray();
        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress,"unVoteForPeer",params,payerAcct.getAddressU160().toBase58(),gaslimit,gasprice);

        val list = ArrayList()
        val struct = Struct()
        struct.add(account.getAddressU160())
        struct.add(peerPubkey.size)
        for (i in peerPubkey.indices) {
            struct.add(peerPubkey[i])
        }
        struct.add(posList.size)
        for (i in peerPubkey.indices) {
            struct.add(posList[i])
        }
        list.add(struct)
        val args = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "unVoteForPeer", args, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(account)))
        if (account != payerAcct) {
            sdk.addSign(tx, payerAcct)
        }
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param account
     * @param peerPubkey
     * @param withdrawList
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun withdraw(account: Account, peerPubkey: Array<String>, withdrawList: LongArray, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (peerPubkey.size != withdrawList.size) {
            throw SDKException(ErrorCode.ParamError)
        }
        val map = HashMap()
        for (i in peerPubkey.indices) {
            map.put(peerPubkey[i], withdrawList[i])
        }
        //        byte[] params = new WithdrawParam(account.getAddressU160(),peerPubkey,withdrawList).toArray();
        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress,"withdraw",params,payerAcct.getAddressU160().toBase58(),gaslimit,gasprice);

        val list = ArrayList()
        val struct = Struct()
        struct.add(account.getAddressU160())
        struct.add(peerPubkey.size)
        for (i in peerPubkey.indices) {
            struct.add(peerPubkey[i])
        }
        struct.add(withdrawList.size)
        for (i in peerPubkey.indices) {
            struct.add(withdrawList[i])
        }
        list.add(struct)
        val args = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "withdraw", args, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(account)))
        if (account != payerAcct) {
            sdk.addSign(tx, payerAcct)
        }
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param adminAccount
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun commitDpos(adminAccount: Account, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress,"commitDpos",new byte[]{},payerAcct.getAddressU160().toBase58(),gaslimit,gasprice);

        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "commitDpos", byteArrayOf(0), payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(adminAccount)))
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    @Throws(Exception::class)
    fun commitDpos(multiAddress: Address, M: Int, accounts: Array<Account>, publicKeys: Array<ByteArray>, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        val pks = arrayOfNulls<ByteArray>(accounts.size + publicKeys.size)
        for (i in accounts.indices) {
            pks[i] = accounts[i].serializePublicKey()
        }
        for (i in publicKeys.indices) {
            pks[i + accounts.size] = publicKeys[i]
        }
        if (multiAddress != Address.addressFromMultiPubKeys(M, pks)) {
            throw SDKException(ErrorCode.ParamErr("mutilAddress doesnot match accounts and publicKeys"))
        }
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "commitDpos", byteArrayOf(0), payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)

        val sigs = arrayOfNulls<Sig>(1)
        sigs[0] = Sig()
        sigs[0].pubKeys = arrayOfNulls(pks.size)
        sigs[0].sigData = arrayOfNulls(M)
        sigs[0].M = M
        for (i in pks.indices) {
            sigs[0].pubKeys[i] = pks[i]
        }
        for (i in 0 until sigs[0].M) {
            val signature = tx.sign(accounts[i], accounts[i].getSignatureScheme())
            sigs[0].sigData[i] = signature
        }
        tx.sigs = sigs
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param peerPubkey
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun blackNode(peerPubkey: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        //        byte[] params = new BlackNodeParam(peerPubkey).toArray();
        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress,"blackNode",params,payerAcct.getAddressU160().toBase58(),gaslimit,gasprice);

        val list = ArrayList()
        list.add(Struct().add(peerPubkey))
        val args = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "blackNode", args, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(payerAcct)))
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param peerPubkey
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun whiteNode(peerPubkey: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        //        byte[] params = new WhiteNodeParam(peerPubkey).toArray();
        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress,"whiteNode",params,payerAcct.getAddressU160().toBase58(),gaslimit,gasprice);

        val list = ArrayList()
        list.add(Struct().add(peerPubkey))
        val args = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "whiteNode", args, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(payerAcct)))
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param account
     * @param peerPubkey
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun quitNode(account: Account, peerPubkey: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        //        byte[] params = new QuitNodeParam(peerPubkey,account.getAddressU160()).toArray();
        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress,"quitNode",params,payerAcct.getAddressU160().toBase58(),gaslimit,gasprice);

        val list = ArrayList()
        list.add(Struct().add(peerPubkey, account.getAddressU160()))
        val args = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "quitNode", args, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(account)))
        if (account != payerAcct) {
            sdk.addSign(tx, payerAcct)
        }
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }


    /**
     *
     * @param config
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun updateConfig(config: Configuration, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        //        byte[] params = config.toArray();
        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress,"updateConfig",params,payerAcct.getAddressU160().toBase58(),gaslimit,gasprice);
        val list = ArrayList()
        list.add(Struct().add(*config.toArray()))
        val args = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "updateConfig", args, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(payerAcct)))
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param candidateFee
     * @param minInitStake
     * @param candidateNum
     * @param A
     * @param B
     * @param Yita
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun updateGlobalParam(candidateFee: Long, minInitStake: Long, candidateNum: Long, A: Long, B: Long, Yita: Long, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        //        byte[] params = new GovernanceGlobalParam(candidateFee,minInitStake,candidateNum,A,B,Yita).toArray();
        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress,"updateGlobalParam",params,payerAcct.getAddressU160().toBase58(),gaslimit,gasprice);

        val list = ArrayList()
        list.add(Struct().add(candidateFee, minInitStake, candidateNum, A, B, Yita))
        val args = NativeBuildParams.createCodeParamsScript(list)
        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "updateGlobalParam", args, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(payerAcct)))
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun callSplit(payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        //        Transaction tx = sdk.vm().makeInvokeCodeTransaction(contractAddress,"updateConfig",new byte[]{},payerAcct.getAddressU160().toBase58(),gaslimit,gasprice);

        val tx = sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "callSplit", byteArrayOf(), payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, arrayOf(arrayOf(payerAcct)))
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }
}

internal class GovernanceView : Serializable {
    var view: Int = 0
    var height: Int = 0
    lateinit var txhash: UInt256

    constructor() {}
    constructor(view: Int, height: Int, txhash: UInt256) {
        this.view = view
        this.height = height
        this.txhash = txhash
    }

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
        this.view = reader.readInt()
        this.height = reader.readInt()
        try {
            this.txhash = reader.readSerializable(UInt256::class.java)
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeInt(view)
        writer.writeInt(height)
        writer.writeSerializable(txhash)
    }
}

internal class RegisterSyncNodeParam(var peerPubkey: String, var address: String, var initPos: Long) : Serializable {
    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarString(peerPubkey)
        writer.writeVarString(address)
        writer.writeLong(initPos)
    }
}

internal class ApproveCandidateParam(var peerPubkey: String) : Serializable {
    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarString(peerPubkey)
    }
}

internal class RejectCandidateParam(var peerPubkey: String) : Serializable {

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {

    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarString(peerPubkey)
    }
}

internal class RegisterCandidateParam(var peerPubkey: String, var address: Address, var initPos: Long, var caller: ByteArray, var keyNo: Long) : Serializable {
    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarString(peerPubkey)
        writer.writeSerializable(address)
        writer.writeVarInt(initPos)
        writer.writeVarBytes(caller)
        writer.writeLong(keyNo)
    }
}

internal class VoteForPeerParam(var address: Address, var peerPubkeys: Array<String>, var posList: LongArray) : Serializable {
    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeSerializable(address)
        writer.writeVarInt(peerPubkeys.size.toLong())
        for (peerPubkey in peerPubkeys) {
            writer.writeVarString(peerPubkey)
        }
        writer.writeVarInt(posList.size.toLong())
        for (pos in posList) {
            writer.writeVarInt(pos)
        }
    }
}

internal class WithdrawParam(var address: Address, var peerPubkeys: Array<String>, var withdrawList: LongArray) : Serializable {
    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeSerializable(address)
        writer.writeVarInt(peerPubkeys.size.toLong())
        for (peerPubkey in peerPubkeys) {
            writer.writeVarString(peerPubkey)
        }
        writer.writeVarInt(withdrawList.size.toLong())
        for (withdraw in withdrawList) {
            writer.writeVarInt(withdraw)
        }
    }
}

internal class QuitNodeParam(var peerPubkey: String, var address: Address) : Serializable {
    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarString(peerPubkey)
        writer.writeSerializable(address)
    }
}

internal class BlackNodeParam(var peerPubkey: String) : Serializable {
    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarString(peerPubkey)
    }
}

internal class WhiteNodeParam(var peerPubkey: String) : Serializable {
    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarString(peerPubkey)
    }
}

internal class VoteCommitDposParam(var address: String, var pos: Long) : Serializable {
    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarString(address)
        writer.writeVarString(pos.toString())
    }
}

internal class Configuration : Serializable {
    var N: Long = 7
    var C: Long = 2
    var K: Long = 7
    var L: Long = 112
    var blockMsgDelay: Long = 10000
    var hashMsgDelay: Long = 10000
    var peerHandshakeTimeout: Long = 10
    var maxBlockChangeView: Long = 1000
    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarInt(N)
        writer.writeVarInt(C)
        writer.writeVarInt(K)
        writer.writeVarInt(L)
        writer.writeVarInt(blockMsgDelay)
        writer.writeVarInt(hashMsgDelay)
        writer.writeVarInt(peerHandshakeTimeout)
        writer.writeVarInt(maxBlockChangeView)
    }
}

internal class GovernanceGlobalParam(var candidateFee: Long, var minInitStake: Long, var candidateNum: Long, var A: Long, var B: Long, var Yita: Long) : Serializable {

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {

    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarInt(candidateFee)
        writer.writeVarInt(minInitStake)
        writer.writeVarInt(candidateNum)
        writer.writeVarInt(A)
        writer.writeVarInt(B)
        writer.writeVarInt(Yita)
    }
}
