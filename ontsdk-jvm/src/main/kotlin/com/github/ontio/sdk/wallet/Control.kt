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

class Control {
    var algorithm = "ECDSA"
    var parameters: MutableMap<String, Any> = HashMap()
    var id = ""
    var key = ""
    var salt = ""
    var hash = "sha256"
    @JSONField(name = "enc-alg")
    var encAlg = "aes-256-gcm"
    var address = ""

    constructor() {

    }

    constructor(key: String, id: String) {
        this.key = key
        this.algorithm = "ECDSA"
        this.id = id
        this.parameters["curve"] = "secp256r1"
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