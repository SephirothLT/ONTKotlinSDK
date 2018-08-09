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

package com.github.ontio.io

import com.github.ontio.common.Helper

import java.io.*

/**
 * Serialize interface
 */
interface Serializable {

    /**
     * @param reader
     * @throws IOException
     */
    @Throws(IOException::class)
    fun deserialize(reader: BinaryReader)

    /**
     * @param writer
     * @throws IOException
     */
    @Throws(IOException::class)
    fun serialize(writer: BinaryWriter)

    open fun toArray(): ByteArray {
        try {
            ByteArrayOutputStream().use { ms ->
                BinaryWriter(ms).use { writer ->
                    serialize(writer)
                    writer.flush()
                    return ms.toByteArray()
                }
            }
        } catch (ex: IOException) {
            throw UnsupportedOperationException(ex)
        }

    }

    open fun toHexString(): String {
        return Helper.toHexString(toArray())
    }

    companion object {
        @Throws(InstantiationException::class, IllegalAccessException::class)
        fun <T : Serializable> from(value: ByteArray, t: Class<T>): T {
            try {
                ByteArrayInputStream(value).use { ms -> BinaryReader(ms).use { reader -> return reader.readSerializable(t) } }
            } catch (ex: IOException) {
                throw IllegalArgumentException(ex)
            }

        }
    }


}
