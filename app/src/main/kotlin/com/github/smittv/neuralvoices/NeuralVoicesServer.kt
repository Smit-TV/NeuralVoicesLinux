package com.github.smittv.neuralvoices

import java.io.BufferedReader
import java.net.Socket
import java.net.ServerSocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NeuralVoicesServer(val port: Int,
val isDebug: Boolean,
val args: Array<String>?) {
    private val nv = NeuralVoices()
    private val coroutine = CoroutineScope(Dispatchers.IO)

    init {
        init()
    }

    fun init() {
        nv.isDebug = isDebug
        if (args != null) {
            nv.create(args)
            nv.run()
        }
        val server = try {
            ServerSocket(port)
        } catch (e: Exception) {
            nv.error("", "$e")
            return@init
        }

        while (true) {
            try {
                val client = server.accept()
                coroutine.launch {
                    handleClient(client)
                }
            } catch (e: Exception) {
                nv.error("", "$e")
            }
        }
    }

    fun handleClient(client: Socket) {
        try {
            nv.stop()
            val out = client.outputStream.bufferedWriter()
            val input = client.inputStream.bufferedReader()
            val args = getClientArgs(input)
            nv.create(args)
            nv.run()
        } catch (e: Exception) {
            nv.error("", "$e")
        }
    }

    fun getClientArgs(reader: BufferedReader): Array<String> {
        var line: String? = null
        val args = mutableListOf<String>()
        while (reader.readLine().also { line = it } != null) {
            args.add(line ?: "")
        }
        if (isDebug) {
            println(args)
        }
        return args.toTypedArray()
    }
}