package com.github.neo.core


import com.github.ontio.common.UInt256
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable

import java.io.IOException

/**
 *
 */
class TransactionInput : Serializable {
    /**
     *
     */
    lateinit var prevHash: UInt256
    /**
     *
     */
    var prevIndex: Short = 0

    constructor() {}

    constructor(prevHash: UInt256, prevIndex: Int) {
        this.prevHash = prevHash
        this.prevIndex = prevIndex.toShort()
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (null == obj) {
            return false
        }
        if (obj !is TransactionInput) {
            return false
        }
        val other = obj as TransactionInput?
        return prevHash == other!!.prevHash && prevIndex == other.prevIndex
    }

    override fun hashCode(): Int {
        return prevHash.hashCode() + prevIndex
    }

    /**
     *
     */
    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
        try {
            prevHash = reader.readSerializable(UInt256::class.java)
            prevIndex = reader.readShort()
            //			prevIndex = (short) reader.readVarInt();
        } catch (e: InstantiationException) {
        } catch (e: IllegalAccessException) {
        }

    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeSerializable(prevHash)
        writer.writeShort(prevIndex)
        //		writer.writeVarInt(prevIndex);
    }


    override fun toString(): String {
        return ("TransactionInput [prevHash=" + prevHash + ", prevIndex="
                + prevIndex + "]")
    }
}
