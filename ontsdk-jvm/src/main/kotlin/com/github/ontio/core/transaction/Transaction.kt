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

package com.github.ontio.core.transaction

import java.io.*
import java.util.*
import java.util.stream.*

import com.alibaba.fastjson.JSON
import com.github.ontio.common.*
import com.github.ontio.core.Inventory
import com.github.ontio.core.InventoryType
import com.github.ontio.core.asset.Sig
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter

/**
 *
 */
abstract class Transaction protected constructor(val txType: TransactionType) : Inventory() {

    var version: Byte = 0
    var nonce = Random().nextInt()
    var gasPrice: Long = 0
    var gasLimit: Long = 0
    var payer = Address()
    var attributes: Array<Attribute>
    var sigs = arrayOfNulls<Sig>(0)


    val addressU160ForVerifying: Array<Address>?
        get() = null

    @Throws(IOException::class)
    fun deserialize(reader: BinaryReader) {
        deserializeUnsigned(reader)
        try {
            sigs = reader.readSerializableArray(Sig::class.java)
        } catch (ex: InstantiationException) {
            throw RuntimeException(ex)
        } catch (ex: IllegalAccessException) {
            throw RuntimeException(ex)
        }

    }

    @Throws(IOException::class)
    fun deserializeUnsigned(reader: BinaryReader) {
        version = reader.readByte()
        if (txType.value() !== reader.readByte()) {
            throw IOException()
        }
        gasPrice = reader.readLong()
        gasLimit = reader.readLong()
        try {
            payer = reader.readSerializable(Address::class.java)
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }

        deserializeUnsignedWithoutType(reader)
    }

    @Throws(IOException::class)
    private fun deserializeUnsignedWithoutType(reader: BinaryReader) {
        try {
            deserializeExclusiveData(reader)
            attributes = reader.readSerializableArray(Attribute::class.java)
        } catch (ex: InstantiationException) {
            throw IOException(ex)
        } catch (ex: IllegalAccessException) {
            throw IOException(ex)
        }

    }

    @Throws(IOException::class)
    protected fun deserializeExclusiveData(reader: BinaryReader) {
    }

    @Throws(IOException::class)
    fun serialize(writer: BinaryWriter) {
        serializeUnsigned(writer)
        writer.writeSerializableArray(sigs)
    }

    @Throws(IOException::class)
    fun serializeUnsigned(writer: BinaryWriter) {
        writer.writeByte(version)
        writer.writeByte(txType.value())
        writer.writeInt(nonce)
        writer.writeLong(gasPrice)
        writer.writeLong(gasLimit)
        writer.writeSerializable(payer)
        serializeExclusiveData(writer)
        writer.writeSerializableArray(attributes)
    }

    @Throws(IOException::class)
    protected fun serializeExclusiveData(writer: BinaryWriter) {
    }

    fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        }
        if (obj == null) {
            return false
        }
        if (obj !is Transaction) {
            return false
        }
        val tx = obj as Transaction?
        return hash().equals(tx!!.hash())
    }

    fun hashCode(): Int {
        return hash().hashCode()
    }

    fun inventoryType(): InventoryType {
        return InventoryType.TX
    }

    open fun json(): Any {
        val json = HashMap()
        json.put("Hash", hash().toString())
        json.put("Version", version.toInt())
        json.put("Nonce", nonce and -0x1)
        json.put("TxType", txType.value() and 0xFF)
        json.put("GasPrice", gasPrice)
        json.put("GasLimit", gasLimit)
        json.put("Payer", payer.toBase58())
        json.put("Attributes", Arrays.stream(attributes).map { p -> p.json() }.toArray<Any>(Object[]::new  /* Currently unsupported in Kotlin */))
        json.put("Sigs", Arrays.stream<Sig>(sigs).map { p -> p.json() }.toArray<Any>(Object[]::new  /* Currently unsupported in Kotlin */))
        return json
    }

    fun verify(): Boolean {
        return true
    }

    companion object {

        @Throws(IOException::class)
        @JvmOverloads
        fun deserializeFrom(value: ByteArray, offset: Int = 0): Transaction {
            ByteArrayInputStream(value, offset, value.size - offset).use { ms -> BinaryReader(ms).use({ reader -> return deserializeFrom(reader) }) }
        }

        @Throws(IOException::class)
        fun deserializeFrom(reader: BinaryReader): Transaction {
            try {
                val ver = reader.readByte()
                val type = TransactionType.valueOf(reader.readByte())
                val typeName = "com.github.ontio.core.payload." + type.toString()
                val transaction = Class.forName(typeName).newInstance() as Transaction
                transaction.nonce = reader.readInt()
                transaction.version = ver
                transaction.gasPrice = reader.readLong()
                transaction.gasLimit = reader.readLong()
                transaction.payer = reader.readSerializable(Address::class.java)
                transaction.deserializeUnsignedWithoutType(reader)
                transaction.sigs = arrayOfNulls(reader.readVarInt() as Int)
                for (i in transaction.sigs.indices) {
                    transaction.sigs[i] = reader.readSerializable(Sig::class.java)
                }
                return transaction
            } catch (ex: ClassNotFoundException) {
                throw IOException(ex)
            } catch (ex: InstantiationException) {
                throw IOException(ex)
            } catch (ex: IllegalAccessException) {
                throw IOException(ex)
            }

        }
    }

}
