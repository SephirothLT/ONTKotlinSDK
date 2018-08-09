package com.github.ontio.account


import com.github.ontio.common.ErrorCode
import com.github.ontio.crypto.*
import com.github.ontio.common.Helper
import com.github.ontio.common.Address
import com.github.ontio.crypto.Base58
import com.github.ontio.crypto.Digest
import com.github.ontio.crypto.Signature
import com.github.ontio.sdk.exception.SDKException
import org.bouncycastle.crypto.generators.SCrypt
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.jcajce.spec.SM2ParameterSpec
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.ECPointUtil
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import org.bouncycastle.util.Strings
import java.io.ByteArrayOutputStream

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.*
import java.util.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import kotlin.experimental.xor

public  class Account{

    private var keyType: KeyType? = null
    private var curveParams: Array<Any>? = null
    private var privateKey: PrivateKey? = null
    private var publicKey: PublicKey? = null
    private var addressU160: Address? = null
    private var signatureScheme: SignatureScheme? = null

    // create an account with the specified key type
    @Throws(Exception::class)
    fun Account(scheme: SignatureScheme)  {
        Security.addProvider(BouncyCastleProvider())
        val gen: KeyPairGenerator
        val paramSpec: AlgorithmParameterSpec
        signatureScheme = scheme

        if (scheme === SignatureScheme.SHA256WITHECDSA) {
            this.keyType = KeyType.ECDSA
            this.curveParams = arrayOf(Curve.P256.toString())
        } else if (scheme === SignatureScheme.SM3WITHSM2) {
            this.keyType = KeyType.SM2
            this.curveParams = arrayOf(Curve.SM2P256V1.toString())
        }

        when (scheme) {
            SignatureScheme.SHA256WITHECDSA, SignatureScheme.SM3WITHSM2 -> {
                if (curveParams!![0] !is String) {
                    throw Exception(ErrorCode.InvalidParams)
                }
                val curveName = curveParams!![0] as String
                paramSpec = ECGenParameterSpec(curveName)
                gen = KeyPairGenerator.getInstance("EC", "BC")
            }
            else ->
                //should not reach here
                throw Exception(ErrorCode.UnsupportedKeyType)
        }
        gen.initialize(paramSpec, SecureRandom())
        val keyPair = gen.generateKeyPair()
        this.privateKey = keyPair.private
        this.publicKey = keyPair.public
        this.addressU160 = Address.addressFromPubKey(serializePublicKey()!!)
    }

    @Throws(Exception::class)
    fun Account(prikey: ByteArray, scheme: SignatureScheme) {
        Security.addProvider(BouncyCastleProvider())
        signatureScheme = scheme

        if (scheme === SignatureScheme.SM3WITHSM2) {
            this.keyType = KeyType.SM2
            this.curveParams = arrayOf(Curve.SM2P256V1.toString())
        } else if (scheme === SignatureScheme.SHA256WITHECDSA) {
            this.keyType = KeyType.ECDSA
            this.curveParams = arrayOf(Curve.P256.toString())
        }

        when (scheme) {
            SignatureScheme.SHA256WITHECDSA, SignatureScheme.SM3WITHSM2 -> {
                val d = BigInteger(1, prikey)
                val spec = ECNamedCurveTable.getParameterSpec(this.curveParams!![0] as String)
                val paramSpec = ECNamedCurveSpec(spec.name, spec.curve, spec.g, spec.n)
                val priSpec = ECPrivateKeySpec(d, paramSpec)
                val kf = KeyFactory.getInstance("EC", "BC")
                this.privateKey = kf.generatePrivate(priSpec)

                val Q = spec.g.multiply(d).normalize()
                if (Q == null || Q.affineXCoord == null || Q.affineYCoord == null) {
                    throw SDKException(ErrorCode.OtherError("normalize error"))
                }
                val pubSpec = ECPublicKeySpec(
                        ECPoint(Q.affineXCoord.toBigInteger(), Q.affineYCoord.toBigInteger()),
                        paramSpec)
                this.publicKey = kf.generatePublic(pubSpec)
                this.addressU160 = Address.addressFromPubKey(serializePublicKey()!!)
            }
            else -> throw Exception(ErrorCode.UnsupportedKeyType)
        }
    }

    // construct an account from a serialized pubic key or private key
    @Throws(Exception::class)
    fun Account(fromPrivate: Boolean, pubkey: ByteArray) {
        Security.addProvider(BouncyCastleProvider())
        if (fromPrivate) {
            //parsePrivateKey(data);
        } else {
            parsePublicKey(pubkey)
        }
    }

    /**
     * Private Key From WIF
     *
     * @param wif get private from wif
     * @return
     */
    fun getPrivateKeyFromWIF(wif: String?): ByteArray {
        if (wif == null) {
            throw NullPointerException()
        }
        val data = Base58.decode(wif)
        if (data.size != 38 || data[0] != 0x80.toByte() || data[33].toInt() != 0x01) {
            throw IllegalArgumentException()
        }
        val checksum = Digest.sha256(Digest.sha256(data, 0, data.size - 4))
        for (i in 0..3) {
            if (data[data.size - 4 + i] != checksum[i]) {
                throw IllegalArgumentException()
            }
        }
        val privateKey = ByteArray(32)
        System.arraycopy(data, 1, privateKey, 0, privateKey.size)
        Arrays.fill(data, 0.toByte())
        return privateKey
    }

    fun getSignatureScheme(): SignatureScheme? {
        return signatureScheme
    }

    fun getKeyType(): KeyType? {
        return keyType
    }

    fun getCurveParams(): Array<Any>? {
        return curveParams
    }

    /**
     * @param encryptedPriKey encryptedPriKey
     * @param passphrase passphrase
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    fun getEcbDecodedPrivateKey(encryptedPriKey: String?, passphrase: String, n: Int, scheme: SignatureScheme): String {
        if (encryptedPriKey == null) {
            throw SDKException(ErrorCode.ParamError)
        }
        val decoded = Base58.decodeChecked(encryptedPriKey)
        if (decoded.size != 43 || decoded[0] != 0x01.toByte() || decoded[1] != 0x42.toByte() || decoded[2] != 0xe0.toByte()) {
            throw SDKException(ErrorCode.Decoded3bytesError)
        }
        val data = Arrays.copyOfRange(decoded, 0, decoded.size - 4)
        return decode(passphrase, data, n, scheme)
    }

    @Throws(Exception::class)
    private fun decode(passphrase: String, input: ByteArray, n: Int, scheme: SignatureScheme): String {
        val r = 8
        val p = 8
        val dkLen = 64
        val addresshash = ByteArray(4)
        val encryptedkey = ByteArray(32)
        System.arraycopy(input, 3, addresshash, 0, 4)
        System.arraycopy(input, 7, encryptedkey, 0, 32)

        val derivedkey = SCrypt.generate(passphrase.toByteArray(StandardCharsets.UTF_8), addresshash, n, r, p, dkLen)
        val derivedhalf1 = ByteArray(32)
        val derivedhalf2 = ByteArray(32)
        System.arraycopy(derivedkey, 0, derivedhalf1, 0, 32)
        System.arraycopy(derivedkey, 32, derivedhalf2, 0, 32)

        val skeySpec = SecretKeySpec(derivedhalf2, "AES")
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")// AES/ECB/NoPadding
        cipher.init(Cipher.DECRYPT_MODE, skeySpec)
        val rawkey = cipher.doFinal(encryptedkey)

        val priKey = Helper.toHexString(XOR(rawkey, derivedhalf1))
        val account = com.github.ontio.account.Account(Helper.hexToBytes(priKey), scheme)
        val script_hash = Address.addressFromPubKey(account.serializePublicKey()!!)
        val address = script_hash.toBase58()
        val addresshashTmp = Digest.sha256(Digest.sha256(address.toByteArray()))
        val addresshashNew = Arrays.copyOfRange(addresshashTmp, 0, 4)

        if (String(addresshash) != String(addresshashNew)) {
            throw SDKException(ErrorCode.DecodePrikeyPassphraseError + Helper.toHexString(addresshash) + "," + Helper.toHexString(addresshashNew))
        }
        return priKey
    }

    @Throws(Exception::class)
    private fun XOR(x: ByteArray, y: ByteArray): ByteArray {
        if (x.size != y.size) {
            throw SDKException(ErrorCode.ParamError)
        }
        val ret = ByteArray(x.size)
        for (i in x.indices) {
            ret[i] = (x[i] xor  y[i]).toByte()
        }
        return ret
    }

    fun getAddressU160(): Address? {
        return addressU160
    }

    fun getPublicKey(): PublicKey ?{
        return publicKey
    }

    fun getPrivateKey(): PrivateKey? {
        return privateKey
    }

    @Throws(Exception::class)
    fun generateSignature(msg: ByteArray?, scheme: SignatureScheme, param: Any?): ByteArray {
        if (msg == null || msg.size == 0) {
            throw Exception(ErrorCode.InvalidMessage)
        }
        if (this.privateKey == null) {
            throw Exception(ErrorCode.WithoutPrivate)
        }

        val ctx = SignatureHandler(keyType!!, signatureScheme!!)
        var paramSpec: AlgorithmParameterSpec? = null
        if (signatureScheme === SignatureScheme.SM3WITHSM2) {
            if (param is String) {
                paramSpec = SM2ParameterSpec(Strings.toByteArray((param as String?)!!))
            } else if (param == null) {
                paramSpec = SM2ParameterSpec("1234567812345678".toByteArray())
            } else {
                throw Exception(ErrorCode.InvalidSM2Signature)
            }
        }
        return Signature(
                signatureScheme!!,
                paramSpec!!,
                ctx.generateSignature(privateKey!!, msg, paramSpec)
        ).toBytes()
    }

    @Throws(Exception::class)
    fun verifySignature(msg: ByteArray?, signature: ByteArray?): Boolean {
        if (msg == null || signature == null || msg.size == 0 || signature.size == 0) {
            throw Exception(ErrorCode.AccountInvalidInput)
        }
        if (this.publicKey == null) {
            throw Exception(ErrorCode.AccountWithoutPublicKey)
        }
        val sig = Signature(signature)
        val ctx = SignatureHandler(keyType!!, sig.scheme!!)
        return ctx.verifySignature(publicKey!!, msg, sig.value!!)
    }


    fun serializePublicKey(): ByteArray? {
        val bs = ByteArrayOutputStream()
        val pub = publicKey as BCECPublicKey
        try {
            when (this.keyType) {
                KeyType.ECDSA ->
                    //bs.write(this.keyType.getLabel());
                    //bs.write(Curve.valueOf(pub.getParameters().getCurve()).getLabel());
                    bs.write(pub.q.getEncoded(true))
                KeyType.SM2 -> {
                    bs.write(this.keyType!!.label)
                    bs.write(Curve.valueOf(pub.parameters.curve).label)
                    bs.write(pub.q.getEncoded(true))
                }
                else ->
                    // Should not reach here
                    throw Exception(ErrorCode.UnknownKeyType)
            }
        } catch (e: Exception) {
            // Should not reach here
            e.printStackTrace()
            return null
        }

        return bs.toByteArray()
    }

    @Throws(Exception::class)
    private fun parsePublicKey(data: ByteArray?) {
        if (data == null) {
            throw Exception(ErrorCode.NullInput)
        }
        if (data.size < 2) {
            throw Exception(ErrorCode.InvalidData)
        }
        if (data.size == 33) {
            this.keyType = KeyType.ECDSA
        } else if (data.size == 35) {
            this.keyType = KeyType.fromLabel(data[0])
        }
        this.privateKey = null
        this.publicKey = null
        when (this.keyType) {
            KeyType.ECDSA -> {
                this.keyType = KeyType.ECDSA
                this.curveParams = arrayOf(Curve.P256.toString())
                val spec0 = ECNamedCurveTable.getParameterSpec(Curve.P256.toString())
                val param0 = ECNamedCurveSpec(spec0.name, spec0.curve, spec0.g, spec0.n)
                val pubSpec0 = ECPublicKeySpec(
                        ECPointUtil.decodePoint(
                                param0.curve,
                                Arrays.copyOfRange(data, 0, data.size)),
                        param0)
                val kf0 = KeyFactory.getInstance("EC", "BC")
                this.publicKey = kf0.generatePublic(pubSpec0)
            }
            KeyType.SM2 -> {
                //                this.keyType = KeyType.fromLabel(data[0]);
                val c = Curve.fromLabel(data[1].toInt())
                this.curveParams = arrayOf(c.toString())
                val spec = ECNamedCurveTable.getParameterSpec(c.toString())
                val param = ECNamedCurveSpec(spec.name, spec.curve, spec.g, spec.n)
                val pubSpec = ECPublicKeySpec(
                        ECPointUtil.decodePoint(
                                param.curve,
                                Arrays.copyOfRange(data, 2, data.size)),
                        param)
                val kf = KeyFactory.getInstance("EC", "BC")
                this.publicKey = kf.generatePublic(pubSpec)
            }
            else -> throw Exception(ErrorCode.UnknownKeyType)
        }
    }

    @Throws(Exception::class)
    fun serializePrivateKey(): ByteArray {
        when (this.keyType) {
            KeyType.ECDSA, KeyType.SM2 -> {
                val pri = this.privateKey as BCECPrivateKey
                val curveName = Curve.valueOf(pri.parameters.curve).toString()
                val d = ByteArray(32)
                if (pri.d.toByteArray().size == 33) {
                    System.arraycopy(pri.d.toByteArray(), 1, d, 0, 32)
                } else {
                    return pri.d.toByteArray()
                }
                return d
            }
            else ->
                // should not reach here
                throw Exception(ErrorCode.UnknownKeyType)
        }
    }

    @Throws(Exception::class)
    fun exportWif(): String {
        val data = ByteArray(38)
        data[0] = 0x80.toByte()
        val prikey = serializePrivateKey()
        System.arraycopy(prikey, 0, data, 1, 32)
        data[33] = 0x01.toByte()
        val checksum = Digest.sha256(Digest.sha256(data, 0, data.size - 4))
        System.arraycopy(checksum, 0, data, data.size - 4, 4)
        val wif = Base58.encode(data)
        Arrays.fill(data, 0.toByte())
        return wif
    }

    @Throws(SDKException::class)
    fun exportEcbEncryptedPrikey(passphrase: String, n: Int): String? {
        val r = 8
        val p = 8
        val dkLen = 64
        val script_hash = Address.addressFromPubKey(serializePublicKey()!!)
        val address = script_hash.toBase58()

        val addresshashTmp = Digest.sha256(Digest.sha256(address.toByteArray()))
        val addresshash = Arrays.copyOfRange(addresshashTmp, 0, 4)

        val derivedkey = SCrypt.generate(passphrase.toByteArray(StandardCharsets.UTF_8), addresshash, n, r, p, dkLen)
        val derivedhalf1 = ByteArray(32)
        val derivedhalf2 = ByteArray(32)
        System.arraycopy(derivedkey, 0, derivedhalf1, 0, 32)
        System.arraycopy(derivedkey, 32, derivedhalf2, 0, 32)
        try {
            val skeySpec = SecretKeySpec(derivedhalf2, "AES")
            val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
            val derived = XOR(serializePrivateKey(), derivedhalf1)
            val encryptedkey = cipher.doFinal(derived)

            val buffer = ByteArray(encryptedkey.size + 7)
            buffer[0] = 0x01.toByte()
            buffer[1] = 0x42.toByte()
            buffer[2] = 0xe0.toByte()
            System.arraycopy(addresshash, 0, buffer, 3, addresshash.size)
            System.arraycopy(encryptedkey, 0, buffer, 7, encryptedkey.size)
            return Base58.checkSumEncode(buffer)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    @Throws(Exception::class)
    fun exportCtrEncryptedPrikey(passphrase: String, n: Int): String {
        val r = 8
        val p = 8
        val dkLen = 64
        val script_hash = Address.addressFromPubKey(serializePublicKey()!!)
        val address = script_hash.toBase58()

        val addresshashTmp = Digest.sha256(Digest.sha256(address.toByteArray()))
        val addresshash = Arrays.copyOfRange(addresshashTmp, 0, 4)

        val derivedkey = SCrypt.generate(passphrase.toByteArray(StandardCharsets.UTF_8), addresshash, n, r, p, dkLen)
        val derivedhalf2 = ByteArray(32)
        val iv = ByteArray(16)
        System.arraycopy(derivedkey, 0, iv, 0, 16)
        System.arraycopy(derivedkey, 32, derivedhalf2, 0, 32)
        try {
            val skeySpec = SecretKeySpec(derivedhalf2, "AES")
            val cipher = Cipher.getInstance("AES/CTR/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, IvParameterSpec(iv))
            val encryptedkey = cipher.doFinal(serializePrivateKey())
            return String(Base64.getEncoder().encode(encryptedkey))
        } catch (e: Exception) {
            throw SDKException(ErrorCode.EncriptPrivateKeyError)
        }

    }

    @Throws(Exception::class)
    fun exportGcmEncryptedPrikey(passphrase: String, salt: ByteArray, n: Int): String {
        val r = 8
        val p = 8
        val dkLen = 64
        if (salt.size != 16) {
            throw SDKException(ErrorCode.ParamError)
        }
        Security.addProvider(BouncyCastleProvider())
        val derivedkey = SCrypt.generate(passphrase.toByteArray(StandardCharsets.UTF_8), salt, n, r, p, dkLen)
        val derivedhalf2 = ByteArray(32)
        val iv = ByteArray(12)
        System.arraycopy(derivedkey, 0, iv, 0, 12)
        System.arraycopy(derivedkey, 32, derivedhalf2, 0, 32)
        try {
            val skeySpec = SecretKeySpec(derivedhalf2, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, GCMParameterSpec(128, iv))
            cipher.updateAAD(getAddressU160()!!.toBase58().toByteArray())
            val encryptedkey = cipher.doFinal(serializePrivateKey())
            return String(Base64.getEncoder().encode(encryptedkey))
        } catch (e: Exception) {
            e.printStackTrace()
            throw SDKException(ErrorCode.EncriptPrivateKeyError)
        }


    }

    @Throws(Exception::class)
    fun getCtrDecodedPrivateKey(encryptedPriKey: String, passphrase: String, address: String, n: Int, scheme: SignatureScheme): String {
        val addresshashTmp = Digest.sha256(Digest.sha256(address.toByteArray()))
        val addresshash = Arrays.copyOfRange(addresshashTmp, 0, 4)
        return getCtrDecodedPrivateKey(encryptedPriKey, passphrase, addresshash, n, scheme)
    }

    @Throws(Exception::class)
    fun getCtrDecodedPrivateKey(encryptedPriKey: String?, passphrase: String, salt: ByteArray, n: Int, scheme: SignatureScheme): String {
        if (encryptedPriKey == null) {
            throw SDKException(ErrorCode.EncryptedPriKeyError)
        }
        if (salt.size != 4) {
            throw SDKException(ErrorCode.ParamError)
        }
        Security.addProvider(BouncyCastleProvider())
        val encryptedkey = Base64.getDecoder().decode(encryptedPriKey)

        val r = 8
        val p = 8
        val dkLen = 64

        val derivedkey = SCrypt.generate(passphrase.toByteArray(StandardCharsets.UTF_8), salt, n, r, p, dkLen)
        val derivedhalf2 = ByteArray(32)
        val iv = ByteArray(16)
        System.arraycopy(derivedkey, 0, iv, 0, 16)
        System.arraycopy(derivedkey, 32, derivedhalf2, 0, 32)

        val skeySpec = SecretKeySpec(derivedhalf2, "AES")
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, IvParameterSpec(iv))
        val rawkey = cipher.doFinal(encryptedkey)
        val address = com.github.ontio.account.Account(rawkey, scheme).getAddressU160()!!.toBase58()
        val addresshashTmp = Digest.sha256(Digest.sha256(address.toByteArray()))
        val addresshash = Arrays.copyOfRange(addresshashTmp, 0, 4)
        if (!Arrays.equals(addresshash, salt)) {
            throw SDKException(ErrorCode.encryptedPriKeyAddressPasswordErr)
        }
        return Helper.toHexString(rawkey)
    }

    @Throws(Exception::class)
    fun getGcmDecodedPrivateKey(encryptedPriKey: String?, passphrase: String, address: String, salt: ByteArray, n: Int, scheme: SignatureScheme): String {
        if (encryptedPriKey == null) {
            throw SDKException(ErrorCode.EncryptedPriKeyError)
        }
        if (salt.size != 16) {
            throw SDKException(ErrorCode.ParamError)
        }

        val encryptedkey = Base64.getDecoder().decode(encryptedPriKey)

        val r = 8
        val p = 8
        val dkLen = 64

        val derivedkey = SCrypt.generate(passphrase.toByteArray(StandardCharsets.UTF_8), salt, n, r, p, dkLen)
        val derivedhalf2 = ByteArray(32)
        val iv = ByteArray(12)
        System.arraycopy(derivedkey, 0, iv, 0, 12)
        System.arraycopy(derivedkey, 32, derivedhalf2, 0, 32)

        var rawkey = ByteArray(0)
        try {
            val skeySpec = SecretKeySpec(derivedhalf2, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, GCMParameterSpec(128, iv))
            cipher.updateAAD(address.toByteArray())
            rawkey = cipher.doFinal(encryptedkey)
        } catch (e: Exception) {
            e.printStackTrace()
            throw SDKException(ErrorCode.encryptedPriKeyAddressPasswordErr)
        }

        val account = com.github.ontio.account.Account(rawkey, scheme)
        if (address != account.getAddressU160()!!.toBase58()) {
            throw SDKException(ErrorCode.encryptedPriKeyAddressPasswordErr)
        }
        return Helper.toHexString(rawkey)
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        return if (obj !is Account) {
            false
        } else addressU160 == obj.addressU160
    }

    override fun hashCode(): Int {
        return addressU160!!.hashCode()
    }

    fun compareTo(account: Account): Int {
        val pub0=serializePublicKey();
        val pub1=account.serializePublicKey();
        var i = 0
        while (i < pub0!!.size && i < pub1!!.size) {
            if (pub0[i] != pub1[i]) {
                return pub0[i] - pub1[i]
            }
            i++
        }
        return pub0.size - pub1!!.size


    }



}