package com.github.neo.core.transaction


import com.github.neo.core.Program
import com.github.neo.core.TransactionInput
import com.github.ontio.account.Account
import com.github.ontio.common.Fixed8
import com.github.ontio.common.Helper
import com.github.ontio.core.Inventory
import com.github.ontio.core.InventoryType
import com.github.ontio.core.transaction.TransactionType
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.neo.core.TransactionAttribute
import com.github.neo.core.TransactionOutput

import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.*
import java.util.stream.Stream

/**
 *
 */
abstract class TransactionNeo protected constructor(
        /**
         *
         */
        val type: TransactionType) : Inventory() {
    /**
     *
     */
    var version: Byte = 0
    /**
     *
     */
    var nonce: Long = 0
    /**
     *
     */
    lateinit var attributes: Array<TransactionAttribute>
    /**
     *
     */
    lateinit var inputs: Array<TransactionInput>
    /**
     *
     */
    lateinit var outputs: Array<TransactionOutput>
    /**
     *
     */
    var scripts = arrayOfNulls<Program>(0)

    val allInputs: Stream<TransactionInput>
        get() = Arrays.stream(inputs)

    val allOutputs: Stream<TransactionOutput>
        get() = Arrays.stream(outputs)

    //[NonSerialized]
    private val _references: Map<TransactionInput, TransactionOutput>? = null

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
        deserializeUnsigned(reader)
        try {
            scripts = reader.readSerializableArray(Program::class.java)
        } catch (ex: InstantiationException) {
            throw RuntimeException(ex)
        } catch (ex: IllegalAccessException) {
            throw RuntimeException(ex)
        }

        onDeserialized()
    }

    @Throws(IOException::class)
    override fun deserializeUnsigned(reader: BinaryReader) {
        if (type.value() != reader.readByte()) { // type
            throw IOException()
        }
        deserializeUnsignedWithoutType(reader)
    }

    @Throws(IOException::class)
    private fun deserializeUnsignedWithoutType(reader: BinaryReader) {
        try {
            version = reader.readByte()
            deserializeExclusiveData(reader)
            attributes = reader.readSerializableArray(TransactionAttribute::class.java)
            inputs = reader.readSerializableArray(TransactionInput::class.java)
            val inputs_all = allInputs.toArray<TransactionInput>(TransactionInput[]::new  /* Currently unsupported in Kotlin */)
            for (i in 1 until inputs_all.size) {
                for (j in 0 until i) {
                    if (inputs_all[i].prevHash === inputs_all[j].prevHash && inputs_all[i].prevIndex == inputs_all[j].prevIndex) {
                        throw IOException()
                    }
                }
            }
            outputs = reader.readSerializableArray(TransactionOutput::class.java)
        } catch (ex: InstantiationException) {
            throw IOException(ex)
        } catch (ex: IllegalAccessException) {
            throw IOException(ex)
        }

    }

    @Throws(IOException::class)
    protected open fun deserializeExclusiveData(reader: BinaryReader) {
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        serializeUnsigned(writer)
        writer.writeSerializableArray(scripts)
    }

    @Throws(IOException::class)
    override fun serializeUnsigned(writer: BinaryWriter) {
        writer.writeByte(type.value())
        writer.writeByte(version)
        serializeExclusiveData(writer)
        writer.writeSerializableArray(attributes)
        writer.writeSerializableArray(inputs)
        writer.writeSerializableArray(outputs)
    }

    @Throws(IOException::class)
    protected open fun serializeExclusiveData(writer: BinaryWriter) {
    }

    override fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        }
        if (obj == null) {
            return false
        }
        if (obj !is TransactionNeo) {
            return false
        }
        val tx = obj as TransactionNeo?
        return hash() == tx!!.hash()
    }

    override fun hashCode(): Int {
        return hash().hashCode()
    }


    override fun inventoryType(): InventoryType {
        return InventoryType.TX
    }


    @Throws(IOException::class)
    protected fun onDeserialized() {
    }


    /**
     *
     */
    override fun verify(): Boolean {
        return true
    }

    @Throws(Exception::class)
    override fun sign(account: Account, scheme: SignatureScheme): ByteArray {
        val bys = account.generateSignature(hashData, scheme, null)
        val signature = ByteArray(64)
        System.arraycopy(bys, 1, signature, 0, 64)
        return signature
    }

    companion object {

        @Throws(IOException::class)
        @JvmOverloads
        fun deserializeFrom(value: ByteArray, offset: Int = 0): TransactionNeo {
            ByteArrayInputStream(value, offset, value.size - offset).use { ms -> BinaryReader(ms).use { reader -> return deserializeFrom(reader) } }
        }

        @Throws(IOException::class)
        fun deserializeFrom(reader: BinaryReader): TransactionNeo {
            try {
                val type = TransactionType.valueOf(reader.readByte())
                val typeName = "NEO.Core." + type.toString()
                val transaction = Class.forName(typeName).newInstance() as TransactionNeo
                transaction.deserializeUnsignedWithoutType(reader)
                transaction.scripts = reader.readSerializableArray(Program::class.java)
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
/**
 * 反序列化Transaction(static)
 */
