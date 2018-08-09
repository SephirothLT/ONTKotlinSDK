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

package com.github.ontio.merkle

import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.common.UInt256
import com.github.ontio.crypto.Digest
import com.github.ontio.sdk.exception.SDKException

import java.security.NoSuchAlgorithmException
import java.util.ArrayList
import java.util.Arrays

class TreeHasher {
    fun hash_empty(): UInt256 {
        return UInt256()
    }

    fun hash_leaf(data: ByteArray): UInt256 {
        val tmp = Helper.addBytes(byteArrayOf(0), data)
        return UInt256(Digest.sha256(tmp))
    }

    fun hash_children(left: UInt256, right: UInt256): UInt256 {
        var data = Helper.addBytes(byteArrayOf(1), left.toArray())
        data = Helper.addBytes(data, right.toArray())
        return UInt256(Digest.sha256(data))
    }

    fun countBit(num: Long): Long {
        var num = num
        var count: Long = 0
        while (num != 0L) {
            num = num and num - 1
            count += 1
        }
        return count
    }

    @Throws(Exception::class)
    fun HashFullTreeWithLeafHash(leaves: Array<UInt256>): UInt256 {
        val length = leaves.size.toLong()
        val obj = _hash_full(leaves, 0, length)

        if (obj.hashes.size.toLong() != countBit(length)) {
            throw SDKException(ErrorCode.AsserFailedHashFullTree)
        }
        return obj.root_hash
    }

    @Throws(Exception::class)
    fun HashFullTree(leaves: Array<ByteArray>): UInt256 {
        val length = leaves.size
        val leafhashes = arrayOfNulls<UInt256>(length)
        for (i in 0 until length) {
            leafhashes[i] = hash_leaf(leaves[i])
        }
        val obj = _hash_full(leafhashes, 0, length.toLong())

        if (obj.hashes.size.toLong() != countBit(length.toLong())) {
            throw Exception(ErrorCode.AsserFailedHashFullTree)
        }
        return obj.root_hash
    }

    @Throws(Exception::class)
    fun _hash_full(leaves: Array<UInt256>, l_idx: Long, r_idx: Long): Obj {
        val width = r_idx - l_idx
        if (width == 0L) {
            return Obj(hash_empty(), null)
        } else if (width == 1L) {
            val leaf_hash = leaves[l_idx.toInt()]
            return Obj(leaf_hash, arrayOf(leaf_hash))
        } else {
            val split_width = 1 shl countBit(width - 1) - 1
            val lObj = _hash_full(leaves, l_idx, l_idx + split_width)
            if (lObj.hashes.size != 1) {
                throw Exception(ErrorCode.LeftTreeFull)
            }
            val rObj = _hash_full(leaves, l_idx + split_width, r_idx)
            val root_hash = hash_children(lObj.root_hash, rObj.root_hash)
            var hashes: Array<UInt256>? = null
            if ((split_width * 2).toLong() == width) {
                hashes = arrayOf(root_hash)
            } else {
                hashes = Arrays.copyOf(lObj.hashes, lObj.hashes.size + rObj.hashes.size)
                System.arraycopy(rObj.hashes, 0, hashes!!, lObj.hashes.size, rObj.hashes.size)
            }
            return Obj(root_hash, hashes)
        }
    }

    fun _hash_fold(hashes: Array<UInt256>): UInt256 {
        val l = hashes.size
        var accum = hashes[l - 1]
        for (i in l - 2 downTo 0) {
            accum = hash_children(hashes[i], accum)
        }
        return accum
    }

    internal inner class Obj(var root_hash: UInt256, var hashes: Array<UInt256>)
}

