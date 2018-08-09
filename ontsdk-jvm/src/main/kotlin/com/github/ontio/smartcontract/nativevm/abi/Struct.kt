package com.github.ontio.smartcontract.nativevm.abi

import com.github.ontio.common.Address

import java.util.ArrayList


class Struct {
    var list: MutableList<Any> = ArrayList()
    fun add(vararg objs: Any): Struct {
        for (i in objs.indices) {
            list.add(objs[i])
        }
        return this
    }
}
