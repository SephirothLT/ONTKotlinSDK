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

package com.github.ontio.common

import com.alibaba.fastjson.JSON
import java.util.*

object ErrorCode {

    //account error
    var InvalidParams = getError(51001, "Account Error,invalid params")
    var UnsupportedKeyType = getError(51002, "Account Error,unsupported key type")
    var InvalidMessage = getError(51003, "Account Error,invalid message")
    var WithoutPrivate = getError(51004, "Account Error,account without private key cannot generate signature")
    var InvalidSM2Signature = getError(51005, "Account Error,invalid SM2 signature parameter, ID (String) excepted")
    var AccountInvalidInput = getError(51006, "Account Error,account invalid input")
    var AccountWithoutPublicKey = getError(51007, "Account Error,account without public key cannot verify signature")
    var UnknownKeyType = getError(51008, "Account Error,unknown key type")
    var NullInput = getError(51009, "Account Error,null input")
    var InvalidData = getError(51010, "Account Error,invalid data")
    var Decoded3bytesError = getError(51011, "Account Error,decoded 3 bytes error")
    var DecodePrikeyPassphraseError = getError(51012, "Account Error,decode prikey passphrase error.")
    var PrikeyLengthError = getError(51013, "Account Error,Prikey length error")
    var EncryptedPriKeyError = getError(51014, "Account Error,Prikey length error")
    var encryptedPriKeyAddressPasswordErr = getError(51015, "Account Error,encryptedPriKey address password not match.")
    var EncriptPrivateKeyError = getError(51016, "Account Error, encript privatekey error,")


    //
    var ParamLengthErr = getError(52001, "Uint256 Error,param length error")
    var ChecksumNotValidate = getError(52002, "Base58 Error,Checksum does not validate")
    var InputTooShort = getError(52003, "Base58 Error,Input too short")
    var UnknownCurve = getError(52004, "Curve Error,unknown curve")
    var UnknownCurveLabel = getError(52005, "Curve Error,unknown curve label")
    var UnknownAsymmetricKeyType = getError(52006, "keyType Error,unknown asymmetric key type")
    var InvalidSignatureData = getError(52007, "Signature Error,invalid signature data: missing the ID parameter for SM3withSM2")
    var InvalidSignatureDataLen = getError(52008, "Signature Error,invalid signature data length")
    var MalformedSignature = getError(52009, "Signature Error,malformed signature")
    var UnsupportedSignatureScheme = getError(52010, "Signature Error,unsupported signature scheme:")
    var DataSignatureErr = getError(52011, "Signature Error,Data signature error.")
    var UnSupportOperation = getError(52012, "Address Error, UnsupportedOperationException")


    //Core Error
    var TxDeserializeError = getError(53001, "Core Error,Transaction deserialize failed")
    var BlockDeserializeError = getError(53002, "Core Error,Block deserialize failed")


    //merkle error
    var MerkleVerifierErr = getError(54001, "Wrong params: the tree size is smaller than the leaf index")
    var TargetHashesErr = getError(54002, "targetHashes error")

    var AsserFailedHashFullTree = getError(54004, "assert failed in hash full tree")
    var LeftTreeFull = getError(54005, "left tree always full")


    //SmartCodeTx Error
    var SendRawTxError = getError(58001, "SmartCodeTx Error,sendRawTransaction error")
    var TypeError = getError(58002, "SmartCodeTx Error,type error")

    //OntIdTx Error
    var NullCodeHash = getError(58003, "OntIdTx Error,null codeHash")
    var ParamError = getError(58004, "param error,")

    var DidNull = getError(58006, "OntIdTx Error,SendDid or receiverDid is null in metaData")
    var NotExistCliamIssuer = getError(58007, "OntIdTx Error,Not exist cliam issuer")
    var NotFoundPublicKeyId = getError(58008, "OntIdTx Error,not found PublicKeyId")
    var PublicKeyIdErr = getError(58009, "OntIdTx Error,PublicKeyId err")
    var BlockHeightNotMatch = getError(58010, "OntIdTx Error,BlockHeight not match")
    var NodesNotMatch = getError(58011, "OntIdTx Error,nodes not match")
    var ResultIsNull = getError(58012, "OntIdTx Error,result is null")
    var CreateOntIdClaimErr = getError(58013, "OntIdTx Error, createOntIdClaim error")
    var VerifyOntIdClaimErr = getError(58014, "OntIdTx Error, verifyOntIdClaim error")
    var WriteVarBytesError = getError(58015, "OntIdTx Error, writeVarBytes error")
    var SendRawTransactionPreExec = getError(58016, "OntIdTx Error, sendRawTransaction PreExec error")
    var SenderAmtNotEqPasswordAmt = getError(58017, "OntIdTx Error, senders amount is not equal password amount")
    var ExpireErr = getError(58017, "OntIdTx Error, expire is wrong")


    //OntAsset Error
    var AssetNameError = getError(58101, "OntAsset Error,asset name error")
    var DidError = getError(58102, "OntAsset Error,Did error")
    var NullPkId = getError(58103, "OntAsset Error,null pkId")
    var NullClaimId = getError(58104, "OntAsset Error,null claimId")
    var AmountError = getError(58105, "OntAsset Error,amount or gas is less than or equal to zero")
    var ParamLengthNotSame = getError(58105, "OntAsset Error,param length is not the same")

    //RecordTx Error
    var NullKeyOrValue = getError(58201, "RecordTx Error,null key or value")
    var NullKey = getError(58202, "RecordTx Error,null  key")


    //OntSdk Error
    var WebsocketNotInit = getError(58301, "OntSdk Error,websocket not init")
    var ConnRestfulNotInit = getError(58302, "OntSdk Error,connRestful not init")


    //abi error
    var SetParamsValueValueNumError = getError(58401, "AbiFunction Error,setParamsValue value num error")
    var ConnectUrlErr = getError(58402, "Interfaces Error,connect error:")

    //WalletManager Error
    var GetAccountByAddressErr = getError(58501, "WalletManager Error,getAccountByAddress err")

    fun getError(code: Int, msg: String): String {
        val map = HashMap<Any,Any>()
        map.put("Error", code)
        map.put("Desc", msg)
        return JSON.toJSONString(map)
    }

    fun ConstructedRootHashErr(msg: String): String {
        return getError(54003, "Other Error,$msg")
    }

    fun ParamErr(msg: String): String {
        return getError(58005, msg)
    }

    fun GetStatusErr(msg: String): String {
        return getError(58017, "GetStatus Error,$msg")
    }

    fun ConnectUrlErr(msg: String): String {
        return getError(58403, "connect error:$msg")
    }

    fun OtherError(msg: String): String {
        return getError(59000, "Other Error,$msg")
    }
}
