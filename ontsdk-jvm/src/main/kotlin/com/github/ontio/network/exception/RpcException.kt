package com.github.ontio.network.exception

/**
 *
 */
class RpcException(val code: Int, message: String) : ConnectorException(message) {
    companion object {
        private val serialVersionUID = -8558006777817318117L
    }
}