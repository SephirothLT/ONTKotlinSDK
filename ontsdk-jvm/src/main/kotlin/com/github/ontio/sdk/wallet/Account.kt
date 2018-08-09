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

package com.github.ontio.sdk.wallet

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.annotation.JSONField

import java.util.Base64
import java.util.HashMap

/**
 *
 */
class Account {
    var label = ""
    var address = ""
    var isDefault = false
    var lock = false
    var algorithm = ""
    var parameters: MutableMap<String, Any> = HashMap<String,Any>()
    var key = ""
    @JSONField(name = "enc-alg")
    var encAlg = "aes-256-gcm"
    var salt = ""
    var hash = "sha256"
    var signatureScheme = "SHA256withECDSA"
    var extra: Any? = null

    constructor() {

    }

    constructor(alg: String, params: Array<Any>, encAlg: String, scheme: String, hash: String) {
        this.algorithm = alg
        this.parameters["curve"] = params[0]
        this.signatureScheme = scheme
        this.encAlg = encAlg
        this.hash = hash
        this.extra = null
    }

    fun getSalt(): ByteArray {
        return Base64.getDecoder().decode(salt)
    }

    fun setSalt(salt: ByteArray) {
        this.salt = String(Base64.getEncoder().encode(salt))
    }

    override fun toString(): String {
        return JSON.toJSONString(this)
    }


}

