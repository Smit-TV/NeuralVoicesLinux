package com.github.smittv.neuralvoices

import sonic.Sonic
import java.io.ByteArrayOutputStream

class SonicProcessor(
    private val sampleRate: Int = 24000,
    private val channels: Int = 1,
    private val pitch: Float = 1f,
    private val speed: Float = 1f, // Переименовал rate в speed для ясности (Time Stretch)
    private val volume: Float = 1f
) {

    // Инициализируем Sonic сразу с нужными параметрами
    private val sonic = Sonic(sampleRate, channels)

    init {
        sonic.setSpeed(speed)      // Меняет длительность, сохраняет тон (то, что обычно нужно)
        sonic.setPitch(pitch)      // Меняет тон, сохраняет длительность
        sonic.setRate(1.0f)        // Varispeed (как винил). Оставляем 1.0, если не нужен эффект "бурундука" + ускорение
        sonic.setVolume(volume)
        sonic.setQuality(0)        // 0 = быстрее, 1 = качественнее
        sonic.setChordPitch(false)
        }

    private val bufferSize = 4096 // Стандартный размер буфера, достаточный для чтения

    /**
     * Обрабатывает ВЕСЬ массив данных целиком (Write -> Flush -> Read).
     * Идеально для конечных файлов/сообщений.
     */
    fun processWhole(data: ByteArray): ByteArray {
        // 1. Пишем данные в Sonic
        sonic.writeBytesToStream(data, data.size)
        
        // 2. Обязательно делаем flush, чтобы дообработать хвосты данных
        sonic.flushStream()

        // 3. Читаем всё, что получилось
        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(bufferSize)
        var bytesRead: Int

        // Читаем, пока есть данные
        do {
            bytesRead = sonic.readBytesFromStream(buffer, buffer.size)
            if (bytesRead > 0) {
                outputStream.write(buffer, 0, bytesRead)
            }
        } while (bytesRead > 0)

        return outputStream.toByteArray()
    }

    override fun toString(): String {
        return StringBuilder()
        .append("Sample rate: $sampleRate\n")
        .append("channels: $channels\n")
        .append("speed: $speed\n")
        .append("pitch: $pitch\n")
        .append("volume: $volume\n")
        .toString()
    }

    // ================= Builder =================
    class Builder {
        private var _sampleRate: Int = 24000
        private var _channels: Int = 1
        private var _pitch: Float = 1f
        private var _speed: Float = 1f
        private var _volume: Float = 1f

        fun setSampleRate(sampleRate: Int) = apply { _sampleRate = sampleRate }
        fun setChannels(channels: Int) = apply { _channels = channels }
        fun setPitch(pitch: Float) = apply { _pitch = pitch }
        fun setSpeed(speed: Float) = apply { _speed = speed } // Скорость воспроизведения
        fun setVolume(volume: Float) = apply { _volume = volume }

        fun build(): SonicProcessor = SonicProcessor(
            sampleRate = _sampleRate,
            channels = _channels,
            pitch = _pitch,
            speed = _speed,
            volume = _volume
        )
    }
}
