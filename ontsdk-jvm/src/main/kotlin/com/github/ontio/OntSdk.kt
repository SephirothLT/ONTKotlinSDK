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

package com.github.ontio

import com.github.ontio.account.Account
import com.github.ontio.common.Common
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.DataSignature
import com.github.ontio.core.program.Program
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.core.asset.Sig
import com.github.ontio.crypto.Digest
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.sdk.manager.*
import com.github.ontio.smartcontract.NativeVm
import com.github.ontio.smartcontract.NeoVm
import com.github.ontio.smartcontract.Vm
import com.github.ontio.smartcontract.WasmVm

import java.util.Arrays

/**
 * Ont Sdk
 */
class OntSdk private constructor() {
    /**
     * get Wallet Mgr
     * @return
     */
    var walletMgr: WalletMgr? = null
        private set
    private var connRpc: ConnectMgr? = null
    private var connRestful: ConnectMgr? = null
    private var connWebSocket: ConnectMgr? = null
    private var connDefault: ConnectMgr? = null

    private var vm: Vm? = null
    private var nativevm: NativeVm? = null
    private var neovm: NeoVm? = null
    private var wasmvm: WasmVm? = null
    private var signServer: SignServer? = null
    var defaultSignScheme = SignatureScheme.SHA256WITHECDSA
    var DEFAULT_GAS_LIMIT: Long = 30000
    var DEFAULT_DEPLOY_GAS_LIMIT: Long = 20000000
    val rpc: ConnectMgr
        @Throws(SDKException::class)
        get() {
            if (connRpc == null) {
                throw SDKException(ErrorCode.ConnRestfulNotInit)
            }
            return connRpc
        }

    val restful: ConnectMgr
        @Throws(SDKException::class)
        get() {
            if (connRestful == null) {
                throw SDKException(ErrorCode.ConnRestfulNotInit)
            }
            return connRestful
        }
    val connect: ConnectMgr?
        get() {
            if (connDefault != null) {
                return connDefault
            }
            if (connRpc != null) {
                return connRpc
            }
            if (connRestful != null) {
                return connRestful
            }
            return if (connWebSocket != null) {
                connWebSocket
            } else null
        }
    val webSocket: ConnectMgr
        @Throws(SDKException::class)
        get() {
            if (connWebSocket == null) {
                throw SDKException(ErrorCode.WebsocketNotInit)
            }
            return connWebSocket
        }

    @Throws(SDKException::class)
    fun getSignServer(): SignServer {
        if (signServer == null) {
            throw SDKException(ErrorCode.OtherError("signServer null"))
        }
        return signServer
    }

    @Throws(SDKException::class)
    fun nativevm(): NativeVm {
        if (nativevm == null) {
            vm()
            nativevm = NativeVm(getInstance())
        }
        return nativevm
    }

    fun neovm(): NeoVm {
        if (neovm == null) {
            vm()
            neovm = NeoVm(getInstance())
        }
        return neovm
    }

    fun wasmvm(): WasmVm {
        if (wasmvm == null) {
            vm()
            wasmvm = WasmVm(getInstance())
        }
        return wasmvm
    }

    fun vm(): Vm {
        if (vm == null) {
            vm = Vm(getInstance())
        }
        return vm
    }

    fun setDefaultConnect(conn: ConnectMgr) {
        connDefault = conn
    }


    /**
     *
     * @param scheme
     */
    fun setSignatureScheme(scheme: SignatureScheme) {
        defaultSignScheme = scheme
        walletMgr!!.signatureScheme = scheme
    }

    @Throws(Exception::class)
    fun setSignServer(url: String) {
        this.signServer = SignServer(url)
    }

    fun setRpc(url: String) {
        this.connRpc = ConnectMgr(url, "rpc")
    }

    fun setRestful(url: String) {
        this.connRestful = ConnectMgr(url, "restful")
    }

    fun setWesocket(url: String, lock: Any) {
        connWebSocket = ConnectMgr(url, "websocket", lock)
    }

    /**
     *
     * @param path
     */
    fun openWalletFile(path: String) {

        try {
            this.walletMgr = WalletMgr(path, defaultSignScheme)
            setSignatureScheme(defaultSignScheme)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     *
     * @param tx
     * @param addr
     * @param password
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun addSign(tx: Transaction, addr: String, password: String, salt: ByteArray): Transaction {
        return addSign(tx, walletMgr!!.getAccount(addr, password, salt))
    }

    @Throws(Exception::class)
    fun addSign(tx: Transaction, acct: Account): Transaction {
        if (tx.sigs == null) {
            tx.sigs = arrayOfNulls(0)
        } else {
            if (tx.sigs.size >= Common.TX_MAX_SIG_SIZE) {
                throw SDKException(ErrorCode.ParamErr("the number of transaction signatures should not be over 16"))
            }
        }
        val sigs = arrayOfNulls<Sig>(tx.sigs.size + 1)
        for (i in tx.sigs.indices) {
            sigs[i] = tx.sigs[i]
        }
        sigs[tx.sigs.size] = Sig()
        sigs[tx.sigs.size]!!.M = 1
        sigs[tx.sigs.size]!!.pubKeys = arrayOfNulls(1)
        sigs[tx.sigs.size]!!.sigData = arrayOfNulls(1)
        sigs[tx.sigs.size]!!.pubKeys[0] = acct.serializePublicKey()
        sigs[tx.sigs.size]!!.sigData[0] = tx.sign(acct, acct.getSignatureScheme())
        tx.sigs = sigs
        return tx
    }

    /**
     *
     * @param tx
     * @param M
     * @param pubKeys
     * @param acct
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun addMultiSign(tx: Transaction, M: Int, pubKeys: Array<ByteArray>?, acct: Account?): Transaction {
        var pubKeys = pubKeys
        pubKeys = Program.sortPublicKeys(*pubKeys!!)
        if (tx.sigs == null) {
            tx.sigs = arrayOfNulls(0)
        } else {
            if (tx.sigs.size > Common.TX_MAX_SIG_SIZE || M > pubKeys.size || M <= 0 || acct == null || pubKeys == null) {
                throw SDKException(ErrorCode.ParamError)
            }
            for (i in tx.sigs.indices) {
                if (Arrays.equals(tx.sigs[i].pubKeys, pubKeys)) {
                    if (tx.sigs[i].sigData.size + 1 > pubKeys.size) {
                        throw SDKException(ErrorCode.ParamErr("too more sigData"))
                    }
                    val len = tx.sigs[i].sigData.size
                    val sigData = arrayOfNulls<ByteArray>(len + 1)
                    for (j in tx.sigs[i].sigData.indices) {
                        sigData[j] = tx.sigs[i].sigData[j]
                    }
                    sigData[len] = tx.sign(acct, acct.getSignatureScheme())
                    tx.sigs[i].sigData = sigData
                    return tx
                }
            }
        }
        val sigs = arrayOfNulls<Sig>(tx.sigs.size + 1)
        for (i in tx.sigs.indices) {
            sigs[i] = tx.sigs[i]
        }
        sigs[tx.sigs.size] = Sig()
        sigs[tx.sigs.size].M = M
        sigs[tx.sigs.size].pubKeys = pubKeys
        sigs[tx.sigs.size].sigData = arrayOfNulls(1)
        sigs[tx.sigs.size].sigData[0] = tx.sign(acct!!, acct.getSignatureScheme())

        tx.sigs = sigs
        return tx
    }

    @Throws(Exception::class)
    fun signTx(tx: Transaction, address: String, password: String, salt: ByteArray): Transaction {
        var address = address
        address = address.replace(Common.didont, "")
        signTx(tx, arrayOf(arrayOf(walletMgr!!.getAccount(address, password, salt))))
        return tx
    }

    /**
     * sign tx
     * @param tx
     * @param accounts
     * @return
     */
    @Throws(Exception::class)
    fun signTx(tx: Transaction, accounts: Array<Array<Account>>): Transaction {
        if (accounts.size > Common.TX_MAX_SIG_SIZE) {
            throw SDKException(ErrorCode.ParamErr("the number of transaction signatures should not be over 16"))
        }
        val sigs = arrayOfNulls<Sig>(accounts.size)
        for (i in accounts.indices) {
            sigs[i] = Sig()
            sigs[i].pubKeys = arrayOfNulls(accounts[i].size)
            sigs[i].sigData = arrayOfNulls(accounts[i].size)
            for (j in 0 until accounts[i].size) {
                sigs[i].M++
                val signature = tx.sign(accounts[i][j], accounts[i][j].getSignatureScheme())
                sigs[i].pubKeys[j] = accounts[i][j].serializePublicKey()
                sigs[i].sigData[j] = signature
            }
        }
        tx.sigs = sigs
        return tx
    }

    /**
     * signTx
     * @param tx
     * @param accounts
     * @param M
     * @return
     * @throws SDKException
     */
    @Throws(Exception::class)
    fun signTx(tx: Transaction, accounts: Array<Array<Account>>, M: IntArray): Transaction {
        var tx = tx
        if (accounts.size > Common.TX_MAX_SIG_SIZE) {
            throw SDKException(ErrorCode.ParamErr("the number of transaction signatures should not be over 16"))
        }
        if (M.size != accounts.size) {
            throw SDKException(ErrorCode.ParamError)
        }
        tx = signTx(tx, accounts)
        for (i in tx.sigs.indices) {
            if (M[i] > tx.sigs[i].pubKeys!!.size || M[i] < 0) {
                throw SDKException(ErrorCode.ParamError)
            }
            tx.sigs[i].M = M[i]
        }
        return tx
    }

    @Throws(SDKException::class)
    fun signatureData(acct: com.github.ontio.account.Account, data: ByteArray): ByteArray {
        var data = data
        var sign: DataSignature? = null
        try {
            data = Digest.sha256(Digest.sha256(data))
            sign = DataSignature(defaultSignScheme, acct, data)
            return sign.signature()
        } catch (e: Exception) {
            throw SDKException(e)
        }

    }

    @Throws(SDKException::class)
    fun verifySignature(pubkey: ByteArray, data: ByteArray, signature: ByteArray): Boolean {
        var data = data
        var sign: DataSignature? = null
        try {
            sign = DataSignature()
            data = Digest.sha256(Digest.sha256(data))
            return sign.verifySignature(com.github.ontio.account.Account(false, pubkey), data, signature)
        } catch (e: Exception) {
            throw SDKException(e)
        }

    }

    companion object {


        private var instance: OntSdk? = null
        @Synchronized
        fun getInstance(): OntSdk {
            if (instance == null) {
                instance = OntSdk()
            }
            return instance
        }
    }
}
