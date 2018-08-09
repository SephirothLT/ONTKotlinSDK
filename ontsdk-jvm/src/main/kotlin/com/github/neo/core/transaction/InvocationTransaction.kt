package com.github.neo.core.transaction


import com.github.ontio.common.Address
import com.github.ontio.common.Fixed8
import com.github.ontio.core.transaction.TransactionType
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter


import java.io.IOException

class InvocationTransaction : TransactionNeo(TransactionType.InvokeCode) {
    lateinit var script: ByteArray
    lateinit var gas: Fixed8
    val addressU160ForVerifying: Array<Address>?
        get() = null

    @Throws(IOException::class)
    protected fun deserializeExclusiveData(reader: BinaryReader) {
        try {
            script = reader.readVarBytes()
            gas = reader.readSerializable(Fixed8::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    protected fun serializeExclusiveData(writer: BinaryWriter) {
        writer.writeVarBytes(script)
        writer.writeSerializable(gas)
    }
}
