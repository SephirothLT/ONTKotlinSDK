package com.github.neo.core

import com.alibaba.fastjson.JSON
import com.github.ontio.common.ErrorCode
import com.github.ontio.network.exception.RpcException

import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.HashMap

/**
 * @Description:
 * @date 2018/6/22
 */
object NeoRpc {

    @Throws(Exception::class)
    fun sendRawTransaction(url: String, sData: String): Any {
        return call(url, "sendrawtransaction", sData)
    }

    @Throws(Exception::class)
    fun getBalance(url: String, contractAddr: String, addr: String): Any {
        return call(url, "getstorage", contractAddr, addr)
    }

    @Throws(RpcException::class, IOException::class)
    fun call(url: String, method: String, vararg params: Any): Any {
        val req = makeRequest(method, params)
        val response = send(url, req) as Map<*, *>?
        if (response == null) {
            throw RpcException(0, ErrorCode.ConnectUrlErr(url + "response is null. maybe is connect error"))
        } else if (response["result"] != null) {
            return response.get("result")
        } else if (response["Result"] != null) {
            return response.get("Result")
        } else if (response["error"] != null) {
            throw RpcException(0, JSON.toJSONString(response))
        } else {
            throw RpcException(0, JSON.toJSONString(response))
        }
    }

    private fun makeRequest(method: String, params: Array<Any>): Map<*, *> {
        val request = HashMap()
        request.put("jsonrpc", "2.0")
        request.put("method", method)
        request.put("params", params)
        request.put("id", 1)
        println(String.format("POST %s", JSON.toJSONString(request)))
        return request
    }


    @Throws(IOException::class)
    fun send(url: String, request: Any): Any? {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
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
                return JSON.parseObject(temp.toString(), Map<*, *>::class.java)
            }
        } catch (e: IOException) {
        }

        return null
    }
}
