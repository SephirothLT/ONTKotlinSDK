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

package com.github.ontio.sdk.manager

import com.github.ontio.OntSdk
import com.github.ontio.account.Account
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.crypto.KeyType
import com.github.ontio.crypto.SignatureScheme
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.Digest
import org.bouncycastle.crypto.digests.SHA1Digest
import org.bouncycastle.crypto.generators.KDF2BytesGenerator
import org.bouncycastle.crypto.kems.ECIESKeyEncapsulation
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.util.encoders.Hex

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

class ECIES(dig: Digest) {
    init {
        digest = dig
    }

    companion object {
        var keyType = KeyType.ECDSA
        var curveParaSpec = arrayOf<Any>("P-256")
        var signatureScheme = SignatureScheme.SHA256WITHECDSA
        var digest: Digest = SHA1Digest()
        fun setDigest(dig: Digest) {
            digest = dig
        }

        //keylen: 16/24/32
        @JvmOverloads
        fun Encrypt(pubkey: String, msg: ByteArray, keylen: Int = 32): Array<String>? {
            try {
                val account = com.github.ontio.account.Account(false, Helper.hexToBytes(pubkey))

                val spec = ECNamedCurveTable.getParameterSpec(curveParaSpec[0] as String)
                val ecDomain = ECDomainParameters(spec.curve, spec.g, spec.n)
                val keys = AsymmetricCipherKeyPair(
                        ECPublicKeyParameters((account.getPublicKey() as BCECPublicKey).q, ecDomain), null)

                val out = ByteArray(ecDomain.curve.fieldSize / 8 * 2 + 1)
                val kem = ECIESKeyEncapsulation(KDF2BytesGenerator(digest), SecureRandom())
                val key1: KeyParameter

                kem.init(keys.public)
                key1 = kem.encrypt(out, keylen) as KeyParameter //AES key = key1 (is encrypted in out)

                val IV = Hex.decode(getRandomString(keylen)) //choose random IV of length = keylen
                val ciphertext: ByteArray
                try {
                    val en = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC")
                    val key = SecretKeySpec(key1.key, "AES")
                    en.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(IV))
                    ciphertext = en.doFinal(msg)
                } catch (e: Exception) {
                    throw Exception("AES failed initialisation - " + e.toString(), e)
                }

                //(IV, out, ciphertext)
                return arrayOf(Helper.toHexString(IV), Helper.toHexString(out), Helper.toHexString(ciphertext))
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }

        @Throws(Exception::class)
        fun Decrypt(account: Account, params: Array<String>): ByteArray? {
            if (params.size != 3) {
                throw Exception(ErrorCode.ParamError)
            }
            return Decrypt(account.serializePrivateKey(), Helper.hexToBytes(params[0]), Helper.hexToBytes(params[1]), Helper.hexToBytes(params[2]), 32)
        }

        @Throws(Exception::class)
        fun Decrypt(prikey: String, params: Array<String>): ByteArray? {
            if (params.size != 3) {
                throw Exception(ErrorCode.ParamError)
            }
            return Decrypt(Helper.hexToBytes(prikey), Helper.hexToBytes(params[0]), Helper.hexToBytes(params[1]), Helper.hexToBytes(params[2]), 32)
        }

        @JvmOverloads
        fun Decrypt(prikey: ByteArray, IV: ByteArray, key_cxt: ByteArray, ciphertext: ByteArray, keylen: Int = 32): ByteArray? {
            try {
                val account = com.github.ontio.account.Account(prikey, signatureScheme)

                val spec = ECNamedCurveTable.getParameterSpec(curveParaSpec[0] as String)
                val ecDomain = ECDomainParameters(spec.curve, spec.g, spec.n)
                val keys = AsymmetricCipherKeyPair(null,
                        ECPrivateKeyParameters((account.getPrivateKey() as BCECPrivateKey).d, ecDomain))

                val kem = ECIESKeyEncapsulation(KDF2BytesGenerator(SHA1Digest()), SecureRandom())
                val key1: KeyParameter

                kem.init(keys.private)
                key1 = kem.decrypt(key_cxt, keylen) as KeyParameter

                val plaintext: ByteArray
                try {
                    val dec = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC")
                    val key = SecretKeySpec(key1.key, "AES")
                    dec.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(IV))
                    plaintext = dec.doFinal(ciphertext)
                } catch (e: Exception) {
                    throw Exception("AES failed initialisation - " + e.toString(), e)
                }

                return plaintext
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }

        fun getRandomString(length: Int): String {
            val KeyStr = "ABCDEF0123456789"
            val sb = StringBuffer()
            val len = KeyStr.length
            for (i in 0 until length) {
                sb.append(KeyStr[Math.round(Math.random() * (len - 1)).toInt()])
            }
            return sb.toString()
        }
    }
}
