package com.github.neo.core


import com.github.ontio.common.Common
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.scripts.ScriptBuilder
import com.github.ontio.core.scripts.ScriptOp
import com.github.ontio.crypto.ECC
import com.github.ontio.crypto.KeyType
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable
import com.github.ontio.sdk.exception.SDKException
import org.bouncycastle.math.ec.ECPoint

import java.io.IOException
import java.math.BigInteger
import java.util.Arrays

/**
 *
 */
class Program : Serializable {
    lateinit var parameter: ByteArray
    lateinit var code: ByteArray
    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
        parameter = reader.readVarBytes()    // sign data
        code = reader.readVarBytes()        // pubkey
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(parameter)
        writer.writeVarBytes(code)
    }

    companion object {
        @Throws(IOException::class)
        fun ProgramFromParams(sigData: Array<ByteArray>): ByteArray {
            return com.github.ontio.core.program.Program.ProgramFromParams(sigData)
        }

        @Throws(Exception::class)
        fun ProgramFromPubKey(publicKey: ByteArray): ByteArray {
            return com.github.ontio.core.program.Program.ProgramFromPubKey(publicKey)
        }

        @Throws(Exception::class)
        fun ProgramFromMultiPubKey(m: Int, vararg publicKeys: ByteArray): ByteArray {
            return com.github.ontio.core.program.Program.ProgramFromMultiPubKey(m, *publicKeys)
        }
    }

}
