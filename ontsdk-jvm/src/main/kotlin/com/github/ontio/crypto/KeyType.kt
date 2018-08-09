package com.github.ontio.crypto

import com.github.ontio.common.ErrorCode

enum class KeyType private constructor(val label: Int) {
    ECDSA(0x12),
    SM2(0x13),
    EDDSA(0x14);


    companion object {


        // get the crypto.KeyType according to the input label
        @Throws(Exception::class)
        fun fromLabel(label: Byte): KeyType {
            for (k in KeyType.values()) {
                if (k.label == label.toInt()) {
                    return k
                }
            }
            throw Exception(ErrorCode.UnknownAsymmetricKeyType)
        }

        fun fromPubkey(pubkey: ByteArray): KeyType? {
            try {
                return if (pubkey.size == 33) {
                    KeyType.ECDSA
                } else {
                    KeyType.fromLabel(pubkey[0])
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }
    }
}
