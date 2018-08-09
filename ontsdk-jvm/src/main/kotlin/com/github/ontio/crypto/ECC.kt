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

package com.github.ontio.crypto

import java.security.*

import com.github.ontio.common.Helper
import org.bouncycastle.asn1.x9.*
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.math.ec.ECPoint

object ECC {
    private val secp256r1nc = ECNamedCurveTable.getByName("secp256r1")
    val secp256r1 = ECDomainParameters(secp256r1nc.curve, secp256r1nc.g, secp256r1nc.n, secp256r1nc.h, secp256r1nc.seed)
    private val sm2p256v1nc = ECNamedCurveTable.getByName("sm2p256v1")
    val sm2p256v1 = ECDomainParameters(sm2p256v1nc.curve, sm2p256v1nc.g, sm2p256v1nc.n, sm2p256v1nc.h, sm2p256v1nc.seed)

    fun compare(a: ECPoint, b: ECPoint): Int {
        if (a === b) {
            return 0
        }
        val result = a.xCoord.toBigInteger().compareTo(b.xCoord.toBigInteger())
        return if (result != 0) {
            result
        } else a.yCoord.toBigInteger().compareTo(b.yCoord.toBigInteger())
    }

    fun generateKey(): ByteArray {
        val key = ByteArray(32)
        val sr = SecureRandom()
        sr.nextBytes(key)
        return key
    }

    fun toString(p: ECPoint): String {
        return Helper.toHexString(p.getEncoded(true))
    }


    fun generateKey(len: Int): ByteArray {
        val key = ByteArray(len)
        val sr = SecureRandom()
        sr.nextBytes(key)
        return key
    }
}
