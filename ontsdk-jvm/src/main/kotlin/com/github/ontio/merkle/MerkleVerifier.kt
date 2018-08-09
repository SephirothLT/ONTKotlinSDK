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
import com.github.ontio.common.UInt256
import com.github.ontio.sdk.exception.SDKException

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

object MerkleVerifier {
    private val hasher = TreeHasher()


    @Throws(Exception::class)
    fun VerifyLeafHashInclusion(leaf_hash: UInt256,
                                leaf_index: Int, proof: Array<UInt256>, root_hash: UInt256, tree_size: Int): Boolean {

        if (tree_size <= leaf_index) {
            throw SDKException(ErrorCode.MerkleVerifierErr)
        }
        val calculated_root_hash = calculate_root_hash_from_audit_path(leaf_hash,
                leaf_index, proof, tree_size)
        if (calculated_root_hash == UInt256()) {
            return false
        }
        if (calculated_root_hash != root_hash) {
            throw Exception("Constructed root hash differs from provided root hash. Constructed: %x, Expected: " +
                    calculated_root_hash + root_hash)
        }
        return true
    }

    fun calculate_root_hash_from_audit_path(leaf_hash: UInt256,
                                            node_index: Int, audit_path: Array<UInt256>, tree_size: Int): UInt256 {
        var node_index = node_index
        var calculated_hash = leaf_hash
        var last_node = tree_size - 1
        var pos = 0
        val path_len = audit_path.size
        while (last_node > 0) {
            if (pos >= path_len) {
                return UInt256()
            }
            if (node_index % 2 == 1) {
                calculated_hash = hasher.hash_children(audit_path[pos], calculated_hash)
                pos += 1
            } else if (node_index < last_node) {
                calculated_hash = hasher.hash_children(calculated_hash, audit_path[pos])
                pos += 1
            }
            node_index /= 2
            last_node /= 2
        }

        return if (pos < path_len) {
            UInt256()
        } else calculated_hash

    }

    fun getProof(leaf_hash: UInt256, node_index: Int, audit_path: Array<UInt256>, tree_size: Int): List<*> {
        var node_index = node_index
        val nodes = ArrayList<Any>()
        var last_node = tree_size - 1
        var pos = 0
        while (last_node > 0) {
            if (node_index % 2 == 1) {
                val map = HashMap()
                map.put("Direction", "Left")
                map.put("TargetHash", audit_path[pos].toHexString())
                nodes.add(map)
                pos += 1
            } else if (node_index < last_node) {
                val map = HashMap()
                map.put("Direction", "Right")
                map.put("TargetHash", audit_path[pos].toHexString())
                nodes.add(map)
                pos += 1
            }
            node_index /= 2
            last_node /= 2
        }
        return nodes
    }

    @Throws(SDKException::class)
    fun Verify(leaf_hash: UInt256, targetHashes: List<*>, root_hash: UInt256): Boolean {
        var calculated_hash = leaf_hash
        for (i in targetHashes.indices) {
            val direction = (targetHashes[i] as Map<*, *>)["Direction"] as String
            val tmp = (targetHashes[i] as Map<*, *>)["TargetHash"] as String
            val targetHash = UInt256.parse(tmp)
            if (direction == "Left") {
                calculated_hash = hasher.hash_children(targetHash, calculated_hash)
            } else if (direction == "Right") {
                calculated_hash = hasher.hash_children(calculated_hash, targetHash)
            } else {
                throw SDKException(ErrorCode.TargetHashesErr)
            }
        }
        if (calculated_hash == UInt256()) {
            return false
        }
        if (calculated_hash != root_hash) {
            throw SDKException(ErrorCode.ConstructedRootHashErr("Constructed root hash differs from provided root hash. Constructed: %x, Expected: " +
                    calculated_hash + root_hash))
        }
        return true
    }
}
