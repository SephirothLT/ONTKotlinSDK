package com.github.ontio.crypto

import com.github.ontio.common.ErrorCode
import com.github.ontio.sdk.exception.SDKException
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.math.ec.ECCurve

enum class Curve private constructor(val label: Int, private val name: String) {
    P224(1, "P-224"),
    P256(2, "P-256"),
    P384(3, "P-384"),
    P521(4, "P-521"),
    SM2P256V1(20, "sm2p256v1"),
    ED25519(25, "ED25519");

    override fun toString(): String {
        return name
    }

    companion object {

        @Throws(Exception::class)
        fun valueOf(v: ECCurve): Curve {
            for (c in Curve.values()) {
                if (ECNamedCurveTable.getParameterSpec(c.toString()).curve.equals(v)) {
                    return c
                }
            }

            throw Exception(ErrorCode.UnknownCurve)
        }

        @Throws(Exception::class)
        fun fromLabel(v: Int): Curve {
            for (c in Curve.values()) {
                if (c.label == v) {
                    return c
                }
            }

            throw SDKException(ErrorCode.UnknownCurveLabel)
        }
    }
}
