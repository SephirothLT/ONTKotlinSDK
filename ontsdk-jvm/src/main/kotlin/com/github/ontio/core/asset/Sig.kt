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

package com.github.ontio.core.asset

import com.github.ontio.common.Helper
import com.github.ontio.core.program.Program
import com.github.ontio.core.program.ProgramInfo
import com.github.ontio.io.*
import com.github.ontio.crypto.ECC

import java.io.IOException
import java.math.BigInteger
import java.util.*

import com.github.ontio.core.program.Program.*

/**
 *
 */
class Sig : Serializable {
    var pubKeys: Array<ByteArray>? = null
    var M: Int = 0
    var sigData: Array<ByteArray>

    @Throws(IOException::class)
    fun deserialize(reader: BinaryReader) {
        val invocationScript = reader.readVarBytes()
        val verificationScript = reader.readVarBytes()
        sigData = Program.getParamInfo(invocationScript)
        val info = Program.getProgramInfo(verificationScript)
        pubKeys = info.publicKey
        M = info.m
    }

    @Throws(IOException::class)
    fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(ProgramFromParams(sigData))
        try {
            if (pubKeys!!.size == 1) {
                writer.writeVarBytes(ProgramFromPubKey(pubKeys!![0]))
            } else if (pubKeys!!.size > 1) {
                writer.writeVarBytes(ProgramFromMultiPubKey(M, pubKeys))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun json(): Any {
        val json = HashMap<Any, Any>()
        json["M"] = M
        val list = ArrayList()
        for (i in pubKeys!!.indices) {
            list.add(Helper.toHexString(pubKeys!![i]))
        }
        val list2 = ArrayList()
        for (i in sigData.indices) {
            list2.add(Helper.toHexString(sigData[i]))
        }
        json["PubKeys"] = list
        json["SigData"] = list2
        //json.put("PubKeys", Arrays.stream(pubKeys).map(p->Helper.toHexString(p)).toArray(String[]::new));
        //json.put("SigData", Arrays.stream(sigData).map(p->Helper.toHexString(p)).toArray(String[]::new));
        return json
    }

}
