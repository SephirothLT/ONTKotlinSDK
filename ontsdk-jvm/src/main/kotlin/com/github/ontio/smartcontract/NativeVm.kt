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

package com.github.ontio.smartcontract

import com.github.ontio.OntSdk
import com.github.ontio.smartcontract.nativevm.*

class NativeVm(private val sdk: OntSdk) {
    private var ont: Ont? = null
    private var ong: Ong? = null
    private var ontId: OntId? = null
    private var globalParams: GlobalParams? = null
    private var auth: Auth? = null
    private var governance: Governance? = null
    /**
     * get OntAsset Tx
     * @return instance
     */
    fun ont(): Ont {
        if (ont == null) {
            ont = Ont(sdk)
        }
        return ont
    }

    fun ong(): Ong {
        if (ong == null) {
            ong = Ong(sdk)
        }
        return ong
    }

    fun ontId(): OntId {
        if (ontId == null) {
            ontId = OntId(sdk)
        }
        return ontId
    }

    fun gParams(): GlobalParams {
        if (globalParams == null) {
            globalParams = GlobalParams(sdk)
        }
        return globalParams
    }

    fun auth(): Auth {
        if (auth == null) {
            auth = Auth(sdk)
        }
        return auth
    }

    fun governance(): Governance {
        if (governance == null) {
            governance = Governance(sdk)
        }
        return governance
    }
}
