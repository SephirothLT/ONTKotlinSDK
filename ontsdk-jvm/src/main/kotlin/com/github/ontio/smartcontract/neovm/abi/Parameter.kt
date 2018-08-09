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

package com.github.ontio.smartcontract.neovm.abi

import com.github.ontio.common.ErrorCode
import com.github.ontio.sdk.exception.SDKException
import com.alibaba.fastjson.JSON

import java.math.BigInteger

/**
 *
 */
class Parameter {
    var name: String? = null
    var type: String? = null
    var value: String? = null

    fun setValue(value: Any?): Boolean {
        try {
            if (value == null) {
                this.value = null
            } else if ("ByteArray" == type) {
                val tmp = value as ByteArray?
                this.value = JSON.toJSONString(tmp)
            } else if ("String" == type) {
                this.value = value as String?
            } else if ("Boolean" == type) {
                val tmp = value as Boolean
                this.value = JSON.toJSONString(tmp)
            } else if ("Integer" == type) {
                val tmp = value as Long
                this.value = JSON.toJSONString(tmp)
            } else if ("Array" == type) {
                val tmp = value as List<*>?
                this.value = JSON.toJSONString(tmp)
            } else if ("InteropInterface" == type) {
                val tmp = value as Any?
                this.value = JSON.toJSONString(tmp)
            } else if ("Void" == type) {
            } else if ("Map" == type) {
                val tmp = value as Map<*, *>?
                this.value = JSON.toJSONString(tmp)
            } else if ("Struct" == type) {
                val tmp = value as Struct?
                this.value = JSON.toJSONString(tmp)
            } else {
                throw SDKException(ErrorCode.TypeError)
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    override fun toString(): String {
        return JSON.toJSONString(this)
    }
}
