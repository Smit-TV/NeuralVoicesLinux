package com.github.smittv.neuralvoices

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.ConnectException

/**
 * Клиент подключается к серверу
 * задержки не будет за счет того что нам не нужно создавать экземпляры синтезаторов снова
 */
class NeuralVoicesClient(val port: Int, 
val isDebug: Boolean) {
    private lateinit var socket: Socket
    private val nv = NeuralVoices()
    var isNotConnected = false

    init {
        nv.isDebug = isDebug
        try {
            socket = Socket("127.0.0.1", port)
        } catch (e: ConnectException) {
            nv.error("fail_to_connect_to_server", "$e")
        } catch (e: SocketTimeoutException) {
            nv.error("fail_to_connect_timeout", "$e")
        } catch (e: IOException) {
            nv.error("", "$e")
        } finally {
            isNotConnected = ::socket.isInitialized != true
        }
    }

    fun send(args: Array<String>) {
        if (!::socket.isInitialized) {
            return
        }
        try {
            socket.use { socket -> 
            val out = socket.outputStream.bufferedWriter()
            val input = socket.inputStream.bufferedReader()
            out.write(args.joinToString("\n"))
            out.flush()
            }
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}