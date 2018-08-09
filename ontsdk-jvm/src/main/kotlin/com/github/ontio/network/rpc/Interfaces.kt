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

package com.github.ontio.network.rpc

import com.alibaba.fastjson.JSON
import com.github.ontio.common.ErrorCode
import com.github.ontio.network.exception.RpcException

import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.HashMap
import java.util.Random

/**
 *
 */
class Interfaces @Throws(MalformedURLException::class)
constructor(url: String) {
    private val url: URL

    private val nextId: Double
        get() {
            var d = 0.0
            do {
                d = Math.random()
            } while (("" + d).indexOf("E") != -1)
            return d
        }
    val host: String
        get() = url.host + " " + url.port


    init {
        this.url = URL(url)
    }

    @Throws(RpcException::class, IOException::class)
    fun call(method: String, vararg params: Any): Any? {
        val req = makeRequest(method, params as Array<Any>)
        val response = send(req) as Map<*, *>?
        return if (response == null) {
            throw RpcException(0, ErrorCode.ConnectUrlErr(url.toString() + "response is null. maybe is connect error"))
        } else if (response["error"] as Int == 0) {
            response["result"]
        } else {
            throw RpcException(0, JSON.toJSONString(response))
        }
    }

    private fun makeRequest(method: String, params: Array<Any>): Map<Any,Any> {
        val request = HashMap<Any,Any>()
        request.put("jsonrpc", "2.0")
        request.put("method", method)
        request.put("params", params)
        request.put("id", 1)
        println(String.format("POST url=%s,%s", this.url, JSON.toJSONString(request)))
        return request
    }


    @Throws(IOException::class)
    fun send(request: Any): Any? {
        try {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            OutputStreamWriter(connection.outputStream).use { w -> w.write(JSON.toJSONString(request)) }
            InputStreamReader(connection.inputStream).use { r ->
                val temp = StringBuffer()
                var c = 0
                while ((c = r.read()) != -1) {
                    temp.append(c.toChar())
                }
                //System.out.println("result:"+temp.toString());
                return JSON.parseObject(temp.toString(), Map<Any, Any>::class.java)
            }
        } catch (e: IOException) {
        }

        return null
    }
}
