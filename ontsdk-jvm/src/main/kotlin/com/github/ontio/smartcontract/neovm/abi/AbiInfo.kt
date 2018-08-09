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

import com.alibaba.fastjson.JSON

/**
 * smartcode abi information
 */
class AbiInfo {
    var hash: String
    var entrypoint: String
    var functions: List<AbiFunction>
    var events: List<AbiEvent>
    fun getFunctions(functions: List<AbiFunction>) {
        this.functions = functions
    }

    fun getEvents(events: List<AbiEvent>) {
        this.events = events
    }

    fun getFunction(name: String): AbiFunction? {
        for (e in functions) {
            if (e.name == name) {
                return e
            }
        }
        return null
    }

    fun getEvent(name: String): AbiEvent? {
        for (e in events) {
            if (e.name == name) {
                return e
            }
        }
        return null
    }

    fun clearFunctionsParamsValue() {
        for (e in functions) {
            e.clearParamsValue()
        }
    }

    fun clearEventsParamsValue() {
        for (e in events) {
            e.clearParamsValue()
        }
    }

    fun removeFunctionParamsValue(name: String) {
        for (e in functions) {
            if (e.name == name) {
                e.clearParamsValue()
            }
        }
    }

    fun removeEventParamsValue(name: String) {
        for (e in events) {
            if (e.name == name) {
                e.clearParamsValue()
            }
        }
    }

    override fun toString(): String {
        return JSON.toJSONString(this)
    }
}
