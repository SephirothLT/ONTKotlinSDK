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

package com.github.ontio.sdk.claim

import com.alibaba.fastjson.annotation.JSONField
import com.github.ontio.account.Account
import com.github.ontio.common.Helper
import com.github.ontio.core.DataSignature
import com.github.ontio.crypto.Digest
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.ontio.crypto.SignatureScheme
import sun.misc.BASE64Encoder

import java.security.spec.AlgorithmParameterSpec
import java.text.SimpleDateFormat
import java.util.*

/**
 * Claim
 */
class Claim {
    private var context = ""
    private var id = UUID.randomUUID().toString()
    private val claim = HashMap<String, Any>()
    var claimStr = ""
        internal set

    constructor(scheme: SignatureScheme, acct: Account, ctx: String, claimMap: Map<*, *>?, metadata: MutableMap<String, Any>, publicKeyId: String) {
        context = ctx
        claim["Context"] = context
        if (claimMap != null) {
            claim["Content"] = claimMap
        }
        claim["Metadata"] = MetaData(metadata).json
        id = Helper.toHexString(Digest.sha256(JSON.toJSONString(claim).toByteArray()))
        claim["Id"] = id
        claim["Version"] = "v1.0"
        val sign = DataSignature(scheme, acct, getClaim())
        val signature = sign.signature()
        val info = SignatureInfo("", "", publicKeyId, signature)
        claim["Signature"] = info.json

    }

    /**
     *
     * @param scheme
     * @param acct
     * @param ctx
     * @param clmMap
     * @param metadata
     * @param publicKeyId
     * @param expireTime
     * @throws Exception
     */
    @Throws(Exception::class)
    constructor(scheme: SignatureScheme, acct: Account, ctx: String, clmMap: Map<String,Any>, metadata: Map<String, String>, clmRevMap: Map<String, Any>, publicKeyId: String, expireTime: Long) {
        val iss = metadata["Issuer"]
        val sub = metadata["Subject"]
        val header = Header("", "", publicKeyId)
        val payload = Payload("v1.0", iss!!, sub!!, System.currentTimeMillis() / 1000, expireTime, ctx, clmMap, clmRevMap)
        val headerStr = JSONObject.toJSONString(header.json)
        val payloadStr = JSONObject.toJSONString(payload.json)
        val headerBytes = Base64.getEncoder().encode(headerStr.toByteArray())
        val payloadBytes = Base64.getEncoder().encode(payloadStr.toByteArray())
        val sign = DataSignature(scheme, acct, String(headerBytes) + "." + String(payloadBytes))
        val signature = sign.signature()
        claimStr += String(headerBytes) + "." + String(payloadBytes) + "." + String(Base64.getEncoder().encode(signature))
    }


    fun getClaim(): String {
        val tmp = HashMap<String, Any>()
        for ((key, value) in claim) {
            tmp[key] = value
        }
        return JSONObject.toJSONString(tmp)
    }
}

internal class Header(alg: String, typ: String, var Kid: String)//        Alg = alg;
//        Typ = typ;
{
    var Alg = "ONT-ES256"
    var Typ = "JWT-X"
    val json: Any
        get() {
            val header = HashMap<String, Any>()
            header["alg"] = Alg
            header["typ"] = Typ
            header["kid"] = Kid
            return header
        }
}

internal class Payload(var Ver: String, var Iss: String, var Sub: String, var Iat: Long, var Exp: Long, @field:JSONField(name = "@context")
var Context: String, clmMap: Map<String, Any>, clmRevMap: Map<String , Any>) {
    var Jti: String
    var ClmMap: Map<String, Any> = HashMap()
    var ClmRevMap: Map<String, Any> = HashMap()

    val json: Any
        get() {
            val payload = HashMap<String, Any>()
            payload["ver"] = Ver
            payload["iss"] = Iss
            payload["sub"] = Sub
            payload["iat"] = Iat
            payload["exp"] = Exp
            payload["jti"] = Jti
            payload["@context"] = Context
            payload["clm"] = ClmMap
            payload["clm-rev"] = ClmRevMap
            return payload
        }

    init {
        ClmMap = clmMap
        ClmRevMap = clmRevMap
        Jti = Helper.toHexString(Digest.sha256(JSON.toJSONString(json).toByteArray()))
    }
}

internal class SignatureInfo(format: String, alg: String, private val PublicKeyId: String, private val Value: ByteArray) {

    private val Format = "pgp"
    private val Algorithm = "ECDSAwithSHA256"

    val json: Any
        get() {
            val signature = HashMap<String, Any>()
            signature["Format"] = Format
            signature["Algorithm"] = Algorithm
            signature["Value"] = Value
            signature["PublicKeyId"] = PublicKeyId
            return signature
        }
}

internal class MetaData(map: MutableMap<String, Any>?) {

    private val CreateTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(Date())//"2017-08-25T10:03:04Z";
    private var meta: MutableMap<String, Any> = HashMap()

    val json: Any
        get() {
            meta["CreateTime"] = CreateTime
            val tmp = HashMap<String, Any>()
            for ((key, value) in meta) {
                tmp[key] = value
            }
            return tmp
        }

    init {
        if (map != null) {
            meta = map
        }
    }
}