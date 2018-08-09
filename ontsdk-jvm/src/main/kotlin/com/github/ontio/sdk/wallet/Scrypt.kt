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

/**
 */
class Scrypt : Cloneable {
    var n = 16384
    var r = 8
    var p = 8
    var dkLen = 64
    private val Salt: String? = null


    constructor() {}

    constructor(n: Int, r: Int, p: Int) {
        this.n = n
        this.r = r
        this.p = p
    }

    public override fun clone(): Scrypt {
        var o: Scrypt? = null
        try {
            o = super.clone() as Scrypt
        } catch (e: CloneNotSupportedException) {
            e.printStackTrace()
        }

        return o!!
    }

    override fun toString(): String {
        return JSON.toJSONString(this)
    }
}
