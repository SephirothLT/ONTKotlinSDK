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

package com.github.ontio.smartcontract.nativevm.abi

import com.alibaba.fastjson.JSON
import com.github.ontio.common.ErrorCode
import com.github.ontio.sdk.exception.SDKException

class SubType {
    var parameters: List<Parameter>? = null
    @Throws(Exception::class)
    fun setParamsValue(vararg objs: Any) {
        if (objs.size != parameters!!.size) {
            throw SDKException(ErrorCode.ParamError)
        }
        for (i in objs.indices) {
            parameters!![i].setValue(objs[i])
        }
    }

    fun getParameter(name: String): Parameter? {
        for (e in parameters!!) {
            if (e.name == name) {
                return e
            }
        }
        return null
    }

    fun clearParamsValue() {
        for (e in parameters!!) {
            e.value = null
        }
    }

    override fun toString(): String {
        return JSON.toJSONString(this)
    }
}
