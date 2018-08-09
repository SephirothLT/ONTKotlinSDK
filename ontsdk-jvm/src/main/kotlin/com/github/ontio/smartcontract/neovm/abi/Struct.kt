package com.github.ontio.smartcontract.neovm.abi

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
