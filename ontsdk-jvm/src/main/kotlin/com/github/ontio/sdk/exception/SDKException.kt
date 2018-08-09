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

package com.github.ontio.sdk.exception


import com.alibaba.fastjson.JSON

class SDKException : Exception {

    constructor(message: String) : super(message) {
        initExMsg(message)
    }

    constructor(message: String, ex: Throwable) : super(message, ex) {
        initExMsg(message)
    }

    constructor(ex: Throwable) : super(ex) {}

    private fun initExMsg(message: String) {}

    companion object {

        private val serialVersionUID = -3056715808373341597L
    }

}

