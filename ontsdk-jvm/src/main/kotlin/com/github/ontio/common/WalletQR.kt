package com.github.ontio.common


import com.github.ontio.sdk.wallet.Account
import com.github.ontio.sdk.wallet.Control
import com.github.ontio.sdk.wallet.Identity
import com.github.ontio.sdk.wallet.Wallet

import java.security.NoSuchAlgorithmException
import java.util.HashMap

object WalletQR {
    @Throws(Exception::class)
    fun exportIdentityQRCode(walletFile: Wallet, identity: Identity): Map<*, *> {
        val control = identity.controls.get(0)
        val address = identity.ontid.substring(8)
        val map = HashMap<Any,Any>()
        map.put("type", "I")
        map.put("label", identity.label)
        map.put("key", control.key)
        map.put("parameters", control.parameters)
        map.put("algorithm", "ECDSA")
        map.put("scrypt", walletFile.getScrypt())
        map.put("address", address)
        map.put("salt", control.salt)
        return map
    }

    @Throws(Exception::class)
    fun exportAccountQRCode(walletFile: Wallet, account: Account): Map<*, *> {
        val map = HashMap<Any,Any>()
        map.put("type", "A")
        map.put("label", account.label)
        map.put("key", account.key)
        map.put("parameters", account.parameters)
        map.put("algorithm", "ECDSA")
        map.put("scrypt", walletFile.getScrypt())
        map.put("address", account.address)
        map.put("salt", account.salt)
        return map
    }
}
