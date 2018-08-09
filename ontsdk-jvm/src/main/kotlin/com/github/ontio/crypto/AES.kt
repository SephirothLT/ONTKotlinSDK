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

import java.io.UnsupportedEncodingException
import java.security.*
import java.security.spec.InvalidParameterSpecException
import java.util.Arrays

import javax.crypto.*
import javax.crypto.spec.*
import javax.security.auth.DestroyFailedException

import org.bouncycastle.jce.provider.BouncyCastleProvider

object AES {
    private val KEY_ALGORITHM = "AES"
    private val CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding"

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    @Throws(IllegalBlockSizeException::class, BadPaddingException::class)
    fun decrypt(encryptedData: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        if (key.size != 32 || iv.size != 16) {
            throw IllegalArgumentException()
        }
        try {
            val secretKey = SecretKeySpec(key, KEY_ALGORITHM)
            val params = AlgorithmParameters.getInstance(KEY_ALGORITHM)
            params.init(IvParameterSpec(iv))
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BC")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, params)
            return cipher.doFinal(encryptedData)
        } catch (ex: NoSuchAlgorithmException) {
            throw RuntimeException(ex)
        } catch (ex: InvalidParameterSpecException) {
            throw RuntimeException(ex)
        } catch (ex: NoSuchPaddingException) {
            throw RuntimeException(ex)
        } catch (ex: InvalidKeyException) {
            throw RuntimeException(ex)
        } catch (ex: InvalidAlgorithmParameterException) {
            throw RuntimeException(ex)
        } catch (ex: NoSuchProviderException) {
            throw RuntimeException(ex)
        }

    }

    fun encrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        if (key.size != 32 || iv.size != 16) {
            throw IllegalArgumentException()
        }
        try {
            val secretKey = SecretKeySpec(key, KEY_ALGORITHM)
            val params = AlgorithmParameters.getInstance(KEY_ALGORITHM)
            params.init(IvParameterSpec(iv))
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM, "BC")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, params)
            return cipher.doFinal(data)
        } catch (ex: NoSuchAlgorithmException) {
            throw RuntimeException(ex)
        } catch (ex: InvalidParameterSpecException) {
            throw RuntimeException(ex)
        } catch (ex: NoSuchPaddingException) {
            throw RuntimeException(ex)
        } catch (ex: InvalidKeyException) {
            throw RuntimeException(ex)
        } catch (ex: InvalidAlgorithmParameterException) {
            throw RuntimeException(ex)
        } catch (ex: IllegalBlockSizeException) {
            throw RuntimeException(ex)
        } catch (ex: BadPaddingException) {
            throw RuntimeException(ex)
        } catch (ex: NoSuchProviderException) {
            throw RuntimeException(ex)
        }

    }

    fun generateIV(): ByteArray {
        val iv = ByteArray(16)
        val rng = SecureRandom()
        rng.nextBytes(iv)
        return iv
    }

    fun generateKey(): ByteArray {
        var key: SecretKey? = null
        try {
            val keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM)
            keyGenerator.init(256)
            key = keyGenerator.generateKey()
            return key!!.encoded
        } catch (ex: NoSuchAlgorithmException) {
            throw RuntimeException(ex)
        } finally {
            if (key != null) {
                try {
                    key.destroy()
                } catch (ex: DestroyFailedException) {
                }

            }
        }
    }

    fun generateKey(password: String): ByteArray {
        var passwordBytes: ByteArray? = null
        var passwordHash: ByteArray? = null
        try {
            passwordBytes = password.toByteArray(charset("UTF-8"))
            passwordHash = Digest.sha256(passwordBytes)
            return Digest.sha256(passwordHash)
        } catch (ex: UnsupportedEncodingException) {
            throw RuntimeException(ex)
        } finally {
            if (passwordBytes != null) {
                Arrays.fill(passwordBytes, 0.toByte())
            }
            if (passwordHash != null) {
                Arrays.fill(passwordHash, 0.toByte())
            }
        }
    }
}
