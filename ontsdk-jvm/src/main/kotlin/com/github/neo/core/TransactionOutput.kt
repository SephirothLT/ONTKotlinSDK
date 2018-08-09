package com.github.neo.core


import com.github.ontio.common.Address
import com.github.ontio.common.Fixed8
import com.github.ontio.common.UInt256
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable

import java.io.IOException

/**
 *
 */
class TransactionOutput : Serializable {
    /**
     *
     */
    lateinit var assetId: UInt256
    /**
     *
     */
    lateinit var value: Fixed8
    /**
     *
     */
    lateinit var scriptHash: Address

    /**
     * byte
     */
    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeSerializable(assetId)
        writer.writeSerializable(value)
        writer.writeSerializable(scriptHash)
    }

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
        try {
            assetId = reader.readSerializable(UInt256::class.java)
            value = reader.readSerializable(Fixed8::class.java)
            scriptHash = reader.readSerializable(Address::class.java)
        } catch (e: InstantiationException) {
            throw IOException()
        } catch (e: IllegalAccessException) {
            throw IOException()
        }

    }


    override fun toString(): String {
        return ("TransactionOutput [assetId=" + assetId + ", value=" + value
                + ", scriptHash=" + scriptHash + "]")
    }
}
