package com.github.neo.core.transaction


import com.github.neo.core.ContractParameterType
import com.github.ontio.common.Address
import com.github.ontio.core.transaction.TransactionType
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter

import java.io.IOException
import java.util.ArrayList
import java.util.Arrays

class PublishTransaction : TransactionNeo(TransactionType.DeployCode) {
    lateinit var script: ByteArray
     var parameterList: Array<ContractParameterType>? = null
    lateinit var returnType: ContractParameterType
     var needStorage: Boolean = false
    lateinit var name: String
    lateinit var codeVersion: String
    lateinit var author: String
    lateinit var email: String
    lateinit var description: String
    override val addressU160ForVerifying: Array<Address>?
        get() = null

    @Throws(IOException::class)
    protected override fun deserializeExclusiveData(reader: BinaryReader) {
        script = reader.readVarBytes()
        parameterList = toEnum(reader.readVarBytes())
        returnType = toEnum(reader.readByte())
        needStorage = reader.readBoolean()
        name = reader.readVarString()
        codeVersion = reader.readVarString()
        author = reader.readVarString()
        email = reader.readVarString()
        description = reader.readVarString()
    }

    @Throws(IOException::class)
    protected override fun serializeExclusiveData(writer: BinaryWriter) {
        writer.writeVarBytes(script)
        writer.writeVarBytes(toByte(parameterList))
        writer.writeByte(returnType.ordinal.toByte())
        writer.writeBoolean(needStorage)
        writer.writeVarString(name)
        writer.writeVarString(codeVersion)
        writer.writeVarString(author)
        writer.writeVarString(email)
        writer.writeVarString(description)
    }

    private fun toEnum(bt: Byte): ContractParameterType {
        return Arrays.stream(ContractParameterType.values()).filter { p -> p.ordinal == bt.toInt() }.findAny().get()
    }

    private fun toEnum(bt: ByteArray?): Array<ContractParameterType>? {
        if (bt == null) {
            return null
        }
        val list = ArrayList<ContractParameterType>()
        for (b in bt) {
            val type = toEnum(b)
            list.add(type)
        }
        return list.stream().toArray(ContractParameterType[]::new  /* Currently unsupported in Kotlin */)
    }

    private fun toByte(types: Array<ContractParameterType>?): ByteArray {
        if (types == null) {
            return ByteArray(0)
        }
        val len = types.size
        val bt = ByteArray(len)
        for (i in 0 until len) {
            bt[i] = types[i].ordinal.toByte()
        }
        return bt
    }
}
