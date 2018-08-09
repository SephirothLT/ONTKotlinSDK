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

package com.github.ontio.sdk.wallet

import com.alibaba.fastjson.JSON
import com.github.ontio.common.ErrorCode
import com.github.ontio.sdk.exception.SDKException

import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Arrays
import java.util.Date

/**
 *
 */
class Wallet : Cloneable {
    var name = "com.github.ontio"
    var version = "1.0"
    var createTime = ""
    var defaultOntid = ""
    var defaultAccountAddress = ""
    var scrypt = Scrypt()
    var extra: Any? = null
    var identities: MutableList<Identity> = ArrayList()
    private var accounts: MutableList<Account> = ArrayList()

    init {
        identities.clear()
    }

    fun getAccounts(): List<Account> {
        return accounts
    }

    fun setAccounts(accountList: MutableList<Account>) {
        this.accounts = accountList
    }

    fun removeAccount(address: String): Boolean {
        for (e in accounts) {
            if (e.address == address) {
                accounts = ArrayList(accounts)
                accounts.remove(e)
                return true
            }
        }
        return false
    }

    fun clearAccount(): Boolean {
        accounts = ArrayList()
        return true
    }

    fun getAccount(address: String): Account? {
        for (e in accounts) {
            if (e.address == address) {
                return e
            }
        }
        return null
    }

    fun removeIdentity(ontid: String): Boolean {
        for (e in identities) {
            if (e.ontid == ontid) {
                identities = ArrayList(identities)
                identities.remove(e)
                return true
            }
        }
        return false
    }

    fun clearIdentity(): Boolean {
        identities = ArrayList()
        return true
    }

    fun getIdentity(ontid: String): Identity? {
        for (e in identities) {
            if (e.ontid == ontid) {
                return e
            }
        }
        return null
    }

    @Throws(Exception::class)
    fun setDefaultAccount(index: Int) {
        if (index >= accounts.size) {
            throw SDKException(ErrorCode.ParamError)
        }
        for (e in accounts) {
            e.isDefault = false
        }
        accounts[index].isDefault = true
        defaultAccountAddress = accounts[index].address
    }

    fun setDefaultAccount(address: String) {
        for (e in accounts) {
            if (e.address == address) {
                e.isDefault = true
                defaultAccountAddress = address
            } else {
                e.isDefault = false
            }
        }
    }

    @Throws(Exception::class)
    fun setDefaultIdentity(index: Int) {
        if (index >= identities.size) {
            throw SDKException(ErrorCode.ParamError)
        }
        for (e in identities) {
            e.isDefault = false
        }
        identities[index].isDefault = true
        defaultOntid = identities[index].ontid
    }

    fun setDefaultIdentity(ontid: String) {
        for (e in identities) {
            if (e.ontid == ontid) {
                e.isDefault = true
                defaultOntid = ontid
            } else {
                e.isDefault = false
            }
        }
    }

    private fun addIdentity(ontid: String): Identity {
        for (e in identities) {
            if (e.ontid == ontid) {
                return e
            }
        }
        val identity = Identity()
        identity.ontid = ontid
        identity.controls = ArrayList()
        identities.add(identity)
        return identity
    }

    private fun addIdentity(idt: Identity) {
        for (e in identities) {
            if (e.ontid == idt.ontid) {
                return
            }
        }
        identities.add(idt)
    }

    fun addOntIdController(ontid: String, key: String, id: String): Identity {
        var identity = getIdentity(ontid)
        if (identity == null) {
            identity = addIdentity(ontid)
        }
        for (e in identity.controls) {
            if (e.key == key) {
                return identity
            }
        }
        val control = Control(key, id)

        identity.controls.add(control)
        return identity
    }

    public override fun clone(): Wallet {
        var o: Wallet? = null
        try {
            o = super.clone() as Wallet
            val srcAccounts = o.accounts.toTypedArray()
            val destAccounts = arrayOfNulls<Account>(srcAccounts.size)
            System.arraycopy(srcAccounts, 0, destAccounts, 0, srcAccounts.size)
            o.accounts = Arrays.asList(*destAccounts)

            val srcIdentitys = o.identities.toTypedArray()
            val destIdentitys = arrayOfNulls<Identity>(srcIdentitys.size)
            System.arraycopy(srcIdentitys, 0, destIdentitys, 0, srcIdentitys.size)
            o.identities = Arrays.asList(*destIdentitys)

            o.scrypt = o.scrypt.clone()
        } catch (e: CloneNotSupportedException) {
            e.printStackTrace()
        }

        return o!!
    }

    override fun toString(): String {
        return JSON.toJSONString(this)
    }
}
