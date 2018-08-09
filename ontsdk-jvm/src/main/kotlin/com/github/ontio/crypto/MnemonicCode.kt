package com.github.ontio.crypto


import com.github.ontio.account.Account
import com.github.ontio.common.ErrorCode
import com.github.ontio.sdk.exception.SDKException

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Arrays
import java.util.Base64

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.SeedCalculator
import io.github.novacrypto.bip39.Words
import io.github.novacrypto.bip39.wordlists.English
import org.bouncycastle.crypto.generators.SCrypt

object MnemonicCode {

    fun generateMnemonicCodesStr(): String {
        val sb = StringBuilder()
        val entropy = ByteArray(Words.TWELVE.byteLength())
        SecureRandom().nextBytes(entropy)
        MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, object : MnemonicGenerator.Target() {
            fun append(string: CharSequence) {
                sb.append(string)
            }
        })
        SecureRandom().nextBytes(entropy)
        return sb.toString()
    }

    fun getPrikeyFromMnemonicCodesStr(mnemonicCodesStr: String): ByteArray {
        var mnemonicCodesStr = mnemonicCodesStr
        var mnemonicCodesArray: Array<String>? = mnemonicCodesStr.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val seed = SeedCalculator()
                .withWordsFromWordList(English.INSTANCE)
                .calculateSeed(Arrays.asList<T>(*mnemonicCodesArray!!), "")
        mnemonicCodesArray = null
        mnemonicCodesStr = null
        val prikey = Arrays.copyOfRange(seed, 0, 32)
        return Arrays.copyOfRange(seed, 0, 32)
    }

    @Throws(Exception::class)
    fun encryptMnemonicCodesStr(mnemonicCodesStr: String, password: String, address: String): String {
        var mnemonicCodesStr = mnemonicCodesStr
        var password = password
        val N = 4096
        val r = 8
        val p = 8
        val dkLen = 64

        val addresshashTmp = Digest.sha256(Digest.sha256(address.toByteArray()))
        val salt = Arrays.copyOfRange(addresshashTmp, 0, 4)
        val derivedkey = SCrypt.generate(password.toByteArray(StandardCharsets.UTF_8), salt, N, r, p, dkLen)
        password = null

        val derivedhalf2 = ByteArray(32)
        val iv = ByteArray(16)
        System.arraycopy(derivedkey, 0, iv, 0, 16)
        System.arraycopy(derivedkey, 32, derivedhalf2, 0, 32)

        val skeySpec = SecretKeySpec(derivedhalf2, "AES")
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, IvParameterSpec(iv))
        val encryptedkey = cipher.doFinal(mnemonicCodesStr.toByteArray())
        mnemonicCodesStr = null

        return String(Base64.getEncoder().encode(encryptedkey))

    }

    @Throws(Exception::class)
    fun decryptMnemonicCodesStr(encryptedMnemonicCodesStr: String?, password: String?, address: String): String {
        var password = password
        if (encryptedMnemonicCodesStr == null) {
            throw SDKException(ErrorCode.ParamError)
        }
        val encryptedkey = Base64.getDecoder().decode(encryptedMnemonicCodesStr)

        val N = 4096
        val r = 8
        val p = 8
        val dkLen = 64

        val addresshashTmp = Digest.sha256(Digest.sha256(address.toByteArray()))
        val salt = Arrays.copyOfRange(addresshashTmp, 0, 4)

        val derivedkey = SCrypt.generate(password!!.toByteArray(StandardCharsets.UTF_8), salt, N, r, p, dkLen)
        password = null
        val derivedhalf2 = ByteArray(32)
        val iv = ByteArray(16)
        System.arraycopy(derivedkey, 0, iv, 0, 16)
        System.arraycopy(derivedkey, 32, derivedhalf2, 0, 32)

        val skeySpec = SecretKeySpec(derivedhalf2, "AES")
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, IvParameterSpec(iv))
        val rawMns = cipher.doFinal(encryptedkey)
        val mnemonicCodesStr = String(rawMns)
        val rawkey = MnemonicCode.getPrikeyFromMnemonicCodesStr(mnemonicCodesStr)
        val addressNew = Account(rawkey, SignatureScheme.SHA256WITHECDSA).getAddressU160().toBase58()
        val addressNewHashTemp = Digest.sha256(Digest.sha256(addressNew.toByteArray()))
        val saltNew = Arrays.copyOfRange(addressNewHashTemp, 0, 4)
        if (!Arrays.equals(saltNew, salt)) {
            throw SDKException(ErrorCode.EncryptedPriKeyError)
        }
        return mnemonicCodesStr
    }


    fun getChars(bytes: ByteArray): CharArray {
        val chars = CharArray(bytes.size)
        for (i in bytes.indices) {
            chars[i] = bytes[i].toChar()
        }
        return chars
    }
}
