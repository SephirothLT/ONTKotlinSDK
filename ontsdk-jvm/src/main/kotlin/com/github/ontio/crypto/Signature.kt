package com.github.ontio.crypto

import com.github.ontio.common.ErrorCode
import com.github.ontio.sdk.exception.SDKException
import org.bouncycastle.jcajce.spec.SM2ParameterSpec

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.spec.AlgorithmParameterSpec
import java.util.Arrays

class Signature {
    var scheme: SignatureScheme? = null
        private set
    private var param: AlgorithmParameterSpec? = null
    var value: ByteArray? = null
        private set

    constructor(scheme: SignatureScheme, param: AlgorithmParameterSpec, signature: ByteArray) {
        this.scheme = scheme
        this.param = param
        this.value = signature
    }

    // parse a serialized bytes to signature structure
    @Throws(Exception::class)
    constructor(data: ByteArray?) {
        if (data == null) {
            throw SDKException(ErrorCode.ParamError)
        }

        if (data.size < 2) {
            throw Exception(ErrorCode.InvalidSignatureDataLen)
        }

        this.scheme = SignatureScheme.values()[data[0]]
        if (scheme === SignatureScheme.SM3WITHSM2) {
            var i = 0
            while (i < data.size && data[i].toInt() != 0) {
                i++
            }
            if (i >= data.size) {
                throw Exception(ErrorCode.InvalidSignatureData)
            }
            this.param = SM2ParameterSpec(Arrays.copyOfRange(data, 1, i))
            this.value = Arrays.copyOfRange(data, i + 1, data.size)
        } else {
            this.value = Arrays.copyOfRange(data, 1, data.size)
        }
    }

    // serialize to byte array
    fun toBytes(): ByteArray {
        val bs = ByteArrayOutputStream()
        try {
            val res = ByteArray(this.value!!.size + 1)
            bs.write((scheme!!.ordinal() as Byte).toInt())
            if (scheme === SignatureScheme.SM3WITHSM2) {
                // adding the ID
                bs.write((param as SM2ParameterSpec).id)
                // padding a 0 as the terminator
                bs.write(0.toByte().toInt())
            }
            bs.write(value!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return bs.toByteArray()
    }
}
