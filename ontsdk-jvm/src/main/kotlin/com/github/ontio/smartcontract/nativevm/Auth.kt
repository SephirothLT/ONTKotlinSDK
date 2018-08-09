package com.github.ontio.smartcontract.nativevm

import com.alibaba.fastjson.JSONObject
import com.github.ontio.OntSdk
import com.github.ontio.account.Account
import com.github.ontio.common.Address
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.nativevm.abi.NativeBuildParams
import com.github.ontio.smartcontract.nativevm.abi.Struct

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.ArrayList


class Auth(private val sdk: OntSdk) {
    val contractAddress = "0000000000000000000000000000000000000006"

    @Throws(Exception::class)
    fun sendInit(adminOntId: String?, password: String, salt: ByteArray, contractAddr: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String {
        if (adminOntId == null || adminOntId == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        val bos = ByteArrayOutputStream()
        val bw = BinaryWriter(bos)
        bw.writeVarBytes(adminOntId.toByteArray())
        val tx = sdk.vm().makeInvokeCodeTransaction(contractAddr, "initContractAdmin", null, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, adminOntId, password, salt)
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        if (!b) {
            throw SDKException(ErrorCode.SendRawTxError)
        }
        return tx.hash().toHexString()
    }


    /**
     *
     * @param adminOntId
     * @param password
     * @param contractAddr
     * @param newAdminOntID
     * @param keyNo
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun sendTransfer(adminOntId: String?, password: String, salt: ByteArray, keyNo: Long, contractAddr: String?, newAdminOntID: String?, payerAcct: Account?, gaslimit: Long, gasprice: Long): String {
        if (adminOntId == null || adminOntId == "" || contractAddr == null || contractAddr == "" || newAdminOntID == null || newAdminOntID == "" || payerAcct == null) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("keyNo or gaslimit or gasprice should not be less than 0"))
        }
        val tx = makeTransfer(adminOntId, contractAddr, newAdminOntID, keyNo, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, adminOntId, password, salt)
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        if (!b) {
            throw SDKException(ErrorCode.SendRawTxError)
        }
        return tx.hash().toHexString()
    }

    /**
     *
     * @param adminOntID
     * @param contractAddr
     * @param newAdminOntID
     * @param keyNo
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws SDKException
     */
    @Throws(SDKException::class)
    fun makeTransfer(adminOntID: String?, contractAddr: String?, newAdminOntID: String?, keyNo: Long, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (adminOntID == null || adminOntID == "" || contractAddr == null || contractAddr == "" || newAdminOntID == null || newAdminOntID == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("keyNo or gaslimit or gasprice should not be less than 0"))
        }

        val list = ArrayList()
        list.add(Struct().add(Helper.hexToBytes(contractAddr), newAdminOntID.toByteArray(), keyNo))
        val arg = NativeBuildParams.createCodeParamsScript(list)

        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "transfer", arg, payer, gaslimit, gasprice)
    }

    /**
     *
     * @param ontid
     * @param password
     * @param contractAddr
     * @param funcName
     * @param keyNo
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun verifyToken(ontid: String?, password: String?, salt: ByteArray, keyNo: Long, contractAddr: String?, funcName: String?): String {
        if (ontid == null || ontid == "" || password == null || password == "" || contractAddr == null || contractAddr == "" || funcName == null || funcName == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0) {
            throw SDKException(ErrorCode.ParamErr("key or gaslimit or gas price should not be less than 0"))
        }
        val tx = makeVerifyToken(ontid, contractAddr, funcName, keyNo)
        sdk.signTx(tx, ontid, password, salt)
        val obj = sdk.connect!!.sendRawTransactionPreExec(tx.toHexString())
                ?: throw SDKException(ErrorCode.OtherError("sendRawTransaction PreExec error: "))
        return (obj as JSONObject).getString("Result")
    }

    /**
     *
     * @param ontid
     * @param contractAddr
     * @param funcName
     * @param keyNo
     * @return
     * @throws SDKException
     */
    @Throws(SDKException::class)
    fun makeVerifyToken(ontid: String?, contractAddr: String?, funcName: String?, keyNo: Long): Transaction {
        if (ontid == null || ontid == "" || contractAddr == null || contractAddr == "" || funcName == null || funcName == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0) {
            throw SDKException(ErrorCode.ParamErr("key or gaslimit or gas price should not be less than 0"))
        }
        val list = ArrayList()
        list.add(Struct().add(Helper.hexToBytes(contractAddr), ontid.toByteArray(), funcName.toByteArray(), keyNo))
        val arg = NativeBuildParams.createCodeParamsScript(list)

        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "verifyToken", arg, null, 0, 0)
    }

    /**
     *
     * @param adminOntID
     * @param password
     * @param contractAddr
     * @param role
     * @param funcName
     * @param keyNo
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun assignFuncsToRole(adminOntID: String?, password: String, salt: ByteArray, keyNo: Long, contractAddr: String?, role: String?, funcName: Array<String>?, payerAcct: Account?, gaslimit: Long, gasprice: Long): String? {
        if (adminOntID == null || adminOntID == "" || contractAddr == null || contractAddr == "" || role == null || role == "" || funcName == null || funcName.size == 0 || payerAcct == null) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("keyNo or gaslimit or gas price should not be less than 0"))
        }
        val tx = makeAssignFuncsToRole(adminOntID, contractAddr, role, funcName, keyNo, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, adminOntID, password, salt)
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    /**
     *
     * @param adminOntID
     * @param contractAddr
     * @param role
     * @param funcName
     * @param keyNo
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws SDKException
     */
    @Throws(SDKException::class)
    fun makeAssignFuncsToRole(adminOntID: String?, contractAddr: String?, role: String?, funcName: Array<String>?, keyNo: Long, payer: String?, gaslimit: Long, gasprice: Long): Transaction {
        if (adminOntID == null || adminOntID == "" || contractAddr == null || contractAddr == "" || role == null || role == "" || funcName == null || funcName.size == 0
                || payer == null || payer == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("keyNo or gaslimit or gas price should not be less than 0"))
        }

        val list = ArrayList()
        val struct = Struct()
        struct.add(Helper.hexToBytes(contractAddr), adminOntID.toByteArray(), role.toByteArray())
        struct.add(funcName.size)
        for (i in funcName.indices) {
            struct.add(funcName[i])
        }
        struct.add(keyNo)
        list.add(struct)
        val arg = NativeBuildParams.createCodeParamsScript(list)

        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "assignFuncsToRole", arg, payer, gaslimit, gasprice)
    }

    /**
     *
     * @param adminOntId
     * @param password
     * @param contractAddr
     * @param role
     * @param ontIDs
     * @param keyNo
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun assignOntIdsToRole(adminOntId: String?, password: String?, salt: ByteArray, keyNo: Long, contractAddr: String?, role: String?, ontIDs: Array<String>?, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (adminOntId == null || adminOntId == "" || password == null || password == "" || contractAddr == null || contractAddr == "" ||
                role == null || role == "" || ontIDs == null || ontIDs.size == 0) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("keyNo or gaslimit or gasprice should not be less than 0"))
        }
        val tx = makeAssignOntIDsToRole(adminOntId, contractAddr, role, ontIDs, keyNo, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, adminOntId, password, salt)
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    /**
     *
     * @param adminOntId
     * @param contractAddr
     * @param role
     * @param ontIDs
     * @param keyNo
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws SDKException
     */
    @Throws(SDKException::class)
    fun makeAssignOntIDsToRole(adminOntId: String?, contractAddr: String?, role: String?, ontIDs: Array<String>?, keyNo: Long, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (adminOntId == null || adminOntId == "" || contractAddr == null || contractAddr == "" ||
                role == null || role == "" || ontIDs == null || ontIDs.size == 0) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("keyNo or gaslimit or gasprice should not be less than 0"))
        }
        val ontId = arrayOfNulls<ByteArray>(ontIDs.size)
        for (i in ontIDs.indices) {
            ontId[i] = ontIDs[i].toByteArray()
        }
        val list = ArrayList()
        val struct = Struct()
        struct.add(Helper.hexToBytes(contractAddr), adminOntId.toByteArray(), role.toByteArray())
        struct.add(ontId.size)
        for (i in ontId.indices) {
            struct.add(*ontId[i])
        }
        struct.add(keyNo)
        list.add(struct)
        val arg = NativeBuildParams.createCodeParamsScript(list)

        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "assignOntIDsToRole", arg, payer, gaslimit, gasprice)
    }

    /**
     *
     * @param ontid
     * @param password
     * @param contractAddr
     * @param toOntId
     * @param role
     * @param period
     * @param level
     * @param keyNo
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun delegate(ontid: String?, password: String?, salt: ByteArray, keyNo: Long, contractAddr: String?, toOntId: String?, role: String?, period: Long, level: Long, payerAcct: Account?, gaslimit: Long, gasprice: Long): String? {
        if (ontid == null || ontid == "" || password == null || password == "" || contractAddr == null || contractAddr == "" || toOntId == null || toOntId == "" ||
                role == null || role == "" || payerAcct == null) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (period < 0 || level < 0 || keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("period level key gaslimit or gasprice should not be less than 0"))
        }
        val tx = makeDelegate(ontid, contractAddr, toOntId, role, period, level, keyNo, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, ontid, password, salt)
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    /**
     *
     * @param ontid
     * @param contractAddr
     * @param toAddr
     * @param role
     * @param period
     * @param level
     * @param keyNo
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws SDKException
     */
    @Throws(SDKException::class)
    fun makeDelegate(ontid: String?, contractAddr: String?, toAddr: String?, role: String?, period: Long, level: Long, keyNo: Long, payer: String?, gaslimit: Long, gasprice: Long): Transaction {
        if (ontid == null || ontid == "" || contractAddr == null || contractAddr == "" || toAddr == null || toAddr == "" ||
                role == null || role == "" || payer == null || payer == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (period < 0 || level < 0 || keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("period level keyNo gaslimit or gasprice should not be less than 0"))
        }

        val list = ArrayList()
        list.add(Struct().add(Helper.hexToBytes(contractAddr), ontid.toByteArray(), toAddr.toByteArray(), role.toByteArray(), period, level, keyNo))
        val arg = NativeBuildParams.createCodeParamsScript(list)

        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "delegate", arg, payer, gaslimit, gasprice)
    }

    /**
     *
     * @param initiatorOntid
     * @param password
     * @param contractAddr
     * @param delegate
     * @param role
     * @param keyNo
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun withdraw(initiatorOntid: String?, password: String?, salt: ByteArray, keyNo: Long, contractAddr: String?, delegate: String, role: String?, payerAcct: Account?, gaslimit: Long, gasprice: Long): String? {
        if (initiatorOntid == null || initiatorOntid == "" || password == null || password == "" || contractAddr == null || contractAddr == "" ||
                role == null || role == "" || payerAcct == null) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("keyNo or gaslimit or gasprice should not be less than 0"))
        }
        val tx = makeWithDraw(initiatorOntid, contractAddr, delegate, role, keyNo, payerAcct.getAddressU160().toBase58(), gaslimit, gasprice)
        sdk.signTx(tx, initiatorOntid, password, salt)
        sdk.addSign(tx, payerAcct)
        val b = sdk.connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    /**
     *
     * @param ontid
     * @param contractAddr
     * @param delegate
     * @param role
     * @param keyNo
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws SDKException
     */
    @Throws(SDKException::class)
    fun makeWithDraw(ontid: String?, contractAddr: String?, delegate: String, role: String?, keyNo: Long, payer: String?, gaslimit: Long, gasprice: Long): Transaction {
        if (ontid == null || ontid == "" || contractAddr == null || contractAddr == "" ||
                role == null || role == "" || payer == null || payer == "") {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("key gaslimit or gasprice should not be less than 0"))
        }
        val list = ArrayList()
        list.add(Struct().add(Helper.hexToBytes(contractAddr), ontid.toByteArray(), delegate.toByteArray(), role.toByteArray(), keyNo))
        val arg = NativeBuildParams.createCodeParamsScript(list)

        return sdk.vm().buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "withdraw", arg, payer, gaslimit, gasprice)
    }

    @Throws(Exception::class)
    fun queryAuth(contractAddr: String, ontid: String): Any {
        return sdk.connect!!.getStorage(contractAddr, contractAddr + Helper.toHexString("role".toByteArray()) + Helper.toHexString(ontid.toByteArray()))
    }
}

internal class TransferParam(var contractAddr: ByteArray, var newAdminOntID: ByteArray, var KeyNo: Long) : Serializable {

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
        this.contractAddr = reader.readVarBytes()
        this.newAdminOntID = reader.readVarBytes()
        KeyNo = reader.readVarInt()
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(this.contractAddr)
        writer.writeVarBytes(this.newAdminOntID)
        writer.writeVarInt(KeyNo)
    }
}

internal class VerifyTokenParam(var contractAddr: ByteArray, var caller: ByteArray, var fn: ByteArray, var keyNo: Long) : Serializable {

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {

    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(this.contractAddr)
        writer.writeVarBytes(this.caller)
        writer.writeVarBytes(this.fn)
        writer.writeVarInt(keyNo)
    }
}

internal class FuncsToRoleParam(var contractAddr: ByteArray, var adminOntID: ByteArray, var role: ByteArray, var funcNames: Array<String>, var keyNo: Long) : Serializable {

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
        this.contractAddr = reader.readVarBytes()
        this.adminOntID = reader.readVarBytes()
        this.role = reader.readVarBytes()
        val length = reader.readVarInt().toInt()
        this.funcNames = arrayOfNulls(length)
        for (i in 0 until length) {
            this.funcNames[i] = reader.readVarString()
        }
        this.keyNo = reader.readVarInt()
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(this.contractAddr)
        writer.writeVarBytes(this.adminOntID)
        writer.writeVarBytes(this.role)
        writer.writeVarInt(this.funcNames.size.toLong())
        for (name in this.funcNames) {
            writer.writeVarString(name)
        }
        writer.writeVarInt(this.keyNo)
    }
}

internal class OntIDsToRoleParam(var contractAddr: ByteArray, var adminOntID: ByteArray, var role: ByteArray, var persons: Array<ByteArray?>, var keyNo: Long) : Serializable {

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
        this.contractAddr = reader.readVarBytes()
        this.adminOntID = reader.readVarBytes()
        this.role = reader.readVarBytes()
        val length = reader.readVarInt().toInt()
        this.persons = arrayOfNulls(length)
        for (i in 0 until length) {
            this.persons[i] = reader.readVarBytes()
        }
        this.keyNo = reader.readVarInt()
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(this.contractAddr)
        writer.writeVarBytes(this.adminOntID)
        writer.writeVarBytes(this.role)
        writer.writeVarInt(this.persons.size.toLong())
        for (p in this.persons) {
            writer.writeVarBytes(p)
        }
        writer.writeVarInt(this.keyNo)
    }
}

internal class DelegateParam(var contractAddr: ByteArray, var from: ByteArray, var to: ByteArray, var role: ByteArray, var period: Long, var level: Long, var keyNo: Long) : Serializable {

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {

    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(this.contractAddr)
        writer.writeVarBytes(this.from)
        writer.writeVarBytes(this.to)
        writer.writeVarBytes(this.role)
        writer.writeVarInt(this.period)
        writer.writeVarInt(this.level)
        writer.writeVarInt(this.keyNo)
    }
}

internal class AuthWithdrawParam(var contractAddr: ByteArray, var initiator: ByteArray, var delegate: ByteArray, var role: ByteArray, var keyNo: Long) : Serializable {
    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {

    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(this.contractAddr)
        writer.writeVarBytes(this.initiator)
        writer.writeVarBytes(this.delegate)
        writer.writeVarBytes(this.role)
        writer.writeVarInt(this.keyNo)
    }
}


