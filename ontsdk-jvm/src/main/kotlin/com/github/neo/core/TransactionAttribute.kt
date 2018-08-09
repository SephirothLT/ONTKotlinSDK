package com.github.neo.core


import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable

import java.io.IOException
import java.util.Arrays

/**
 *
 */
class TransactionAttribute : Serializable {
    /**
     *
     */
    lateinit var usage: TransactionAttributeUsage
    /**
     *
     */
   lateinit var data: ByteArray

    /**
     *
     */
    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        // usage
        writer.writeByte(usage.value())
        // data
        if (usage === TransactionAttributeUsage.Script) {
            writer.write(data)
        } else if (usage === TransactionAttributeUsage.DescriptionUrl
                || usage === TransactionAttributeUsage.Description
                || usage === TransactionAttributeUsage.Nonce) {
            writer.writeVarBytes(data)
        } else {
            throw IOException()
        }
    }

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
        // usage
        usage = TransactionAttributeUsage.valueOf(reader.readByte())
        // data
        if (usage === TransactionAttributeUsage.Script) {
            data = reader.readBytes(20)
        } else if (usage === TransactionAttributeUsage.DescriptionUrl
                || usage === TransactionAttributeUsage.Description
                || usage === TransactionAttributeUsage.Nonce) {
            data = reader.readVarBytes(255)
        } else {
            throw IOException()
        }
    }


    override fun toString(): String {
        return ("TransactionAttribute [usage=" + usage + ", data="
                + Arrays.toString(data) + "]")
    }
}
