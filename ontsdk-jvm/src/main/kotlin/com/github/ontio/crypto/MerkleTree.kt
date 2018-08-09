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

package com.github.ontio.crypto

import java.util.Arrays

import com.github.ontio.common.UInt256

/**
 * MerkleTree
 */
object MerkleTree {
    /**
     *
     * @param hashes
     * @return
     */
    fun computeRoot(hashes: Array<UInt256>): UInt256 {
        if (hashes.size == 0) {
            throw IllegalArgumentException()
        }
        return if (hashes.size == 1) {
            hashes[0]
        } else UInt256(computeRoot(Arrays.stream(hashes).map { p -> p.toArray() }.toArray<ByteArray>(byte[][]::new  /* Currently unsupported in Kotlin */)))
    }

    private fun computeRoot(hashes: Array<ByteArray>): ByteArray {
        var hashes = hashes
        if (hashes.size == 0) {
            throw IllegalArgumentException()
        }
        if (hashes.size == 1) {
            return hashes[0]
        }
        if (hashes.size % 2 == 1) {
            val temp = arrayOfNulls<ByteArray>(hashes.size + 1)
            System.arraycopy(hashes, 0, temp, 0, hashes.size)
            temp[temp.size - 1] = hashes[hashes.size - 1]
            hashes = temp
        }
        val hashes_new = arrayOfNulls<ByteArray>(hashes.size / 2)
        for (i in hashes_new.indices) {
            val buffer = ByteArray(hashes[i * 2].size + hashes[i * 2 + 1].size)
            System.arraycopy(hashes[i * 2], 0, buffer, 0, hashes[i * 2].size)
            System.arraycopy(hashes[i * 2 + 1], 0, buffer, hashes[i * 2].size, hashes[i * 2 + 1].size)
            hashes_new[i] = Digest.hash256(buffer)
        }
        return computeRoot(hashes_new)
    }
}
