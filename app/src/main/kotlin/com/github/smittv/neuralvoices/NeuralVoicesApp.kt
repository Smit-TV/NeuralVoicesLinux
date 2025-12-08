package com.github.smittv.neuralvoices

import com.microsoft.cognitiveservices.speech.EmbeddedSpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.audio.AudioOutputStream
import com.microsoft.cognitiveservices.speech.audio.PushAudioOutputStreamCallback

import sonic.Sonic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.ResourceBundle
import java.text.MessageFormat

import javax.sound.sampled.*



class NeuralVoices : PushAudioOutputStreamCallback() {

    companion object {
        // Режим в котором все будет зачитано одним языком
        const val MODE_DEFAULT = 0
        // Мультиязычный режим
        const val MODE_MULTILINGUAL = 1
        // Латиница
        const val TEXT_LATIN = 0
        // Кирилица
        const val TEXT_CYRILLIC = 1
        // Это текст который не является буквенным, например это числа
        const val TEXT_DEFAULT = 2
    }

    val resources = ResourceBundle.getBundle("i18n/messages")
    // Голос который будет использоваться если latinVoice и cyrillicVoice не указаны
    // Так же этот голос будет использоваться если один из выше указаных не указан
    var voice: String? = null
    // Файлы голоса 
    var voiceDataPath: String? = null
    // лицензия голоса или ключ шифрования
    var voiceLicense: String? = null
    // Текст для синтеза, может быть ssml
    var text: String? = null
    // Отладка
    var isDebug = false
    // Стоит ли вывести результат синтеза в stdout вместо воспроизведения через динамики устройства
    var isSTDOut = false
    // следует ли обрабатывать текст как SSML
    var isTextSSML = false
    // Файлы голоса для чтения на латинице
    var latinVoiceDataPath: String? = null
    // Голос для чтения на  латинице
    var latinVoice: String? =null
    // Файлы  голоса для чтения на  кирилицы 
    var cyrillicVoiceDataPath: String? = null
    // Голос для чтения кирилицы
    var cyrillicVoice: String? = null
    // Синтезатор voice
    var defaultVoiceSynthesizer: SpeechSynthesizer? = null
    // Синтезатор для латинского голоса 
    var latinVoiceSynthesizer: SpeechSynthesizer? = null
    // Синтезатор кирилического голоса 
    var cyrillicVoiceSynthesizer: SpeechSynthesizer? = null
    // Режим
    var mode = MODE_DEFAULT
    // Лицензия или ключ от кирилического голоса
    var cyrillicVoiceLicense: String? = null// Лицензия или ключ от латинского синтезатора
    var latinVoiceLicense: String? = null
    // Список голосов
    val voices = mutableMapOf<String, String>()
    // Скорость речи
    var rate = 1.0f
    // Тон
    var pitch = 1.0f
    // Ускорение речи, тон, громкость
    var sonicProcessor: SonicProcessor? = null
    // Громкость
    var volume = 1f
    // Аудио поток
    val format = AudioFormat(
        24000f,
        16,
        1,
        true, 
        false
    )

    val line = AudioSystem.getLine(DataLine.Info(SourceDataLine::class.java, format)) as SourceDataLine

    val coroutine = CoroutineScope(Dispatchers.IO)

    init {
        line.open(format)
        line.start()
    }


    // Чтобы speech dispatcher работал с NeuralVoices без задержки нужно создать сервер и скормить ему данные для озвучивания
    fun runServer() {
        //NeuralVoicesServer.speak(this)
    }

    override fun write(data: ByteArray): Int {
        val processed = sonicProcessor!!.processWhole(data) 
        if (!isSTDOut) {
            line.write(processed, 0, processed.size)
            return data.size
        }

        System.out.write(processed)
        System.out.flush()
        return data.size
    }

    override fun close() {
    }

    fun initVoices() {
        val config = EmbeddedSpeechConfig.fromPath(getDefaultVoicesDirectory())
        config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Riff24Khz16BitMonoPcm)
        val synthesizer = SpeechSynthesizer(config)
            try {
                val future = synthesizer.getVoicesAsync()
                val voiceResult = future.get()
                                for (voice in voiceResult.getVoices()) {
                voices[voice.name] = voice.getVoicePath()
                voices[voice.shortName] = voice.voicePath
            }
            } catch (e: Exception) {
                log("$e")
            }


            if (isDebug) {
                println(voices.toString())
            }
            config.close()
            synthesizer.close()

            latinVoiceDataPath?.let {
                voices[latinVoice ?: ""] = it
            }

            cyrillicVoiceDataPath?.let {
                voices[cyrillicVoice ?: ""] = it
            }
    }

    fun init() {
        initVoices()
        mode = when {
            latinVoice != null ||
            cyrillicVoice != null -> MODE_MULTILINGUAL
            else -> MODE_DEFAULT
        }

        log(modeToString())

        if (voice != null) {
            log("setting_default_synthesizer")
            defaultVoiceSynthesizer = getSynthesizer(voice)
            if (defaultVoiceSynthesizer != null) {
                log("success_create_synth")
            }
        }
        if (latinVoice != null) {
            log("setting_latin_synthesizer")
            log("info_latin_voice_name", latinVoice ?: "")
            log("info_latin_voice_data_path", latinVoiceDataPath ?: "")
            latinVoiceSynthesizer = getSynthesizer(latinVoice, latinVoiceLicense, latinVoiceDataPath)
            if (latinVoiceSynthesizer != null) {
                log("success_create_synth")
            }
        }
        if (cyrillicVoice != null) {
            log("setting_cyrillic_voice")
            log("info_cyrillic_voice_name", cyrillicVoice ?: "")
            log("info_cyrillic_voice_data_path", cyrillicVoiceDataPath ?: "")
            cyrillicVoiceSynthesizer = getSynthesizer(cyrillicVoice, cyrillicVoiceLicense, cyrillicVoiceDataPath)
            if (cyrillicVoiceSynthesizer != null) {
                log("success_create_synth")
            }
        }
        if (mode == MODE_MULTILINGUAL && latinVoice == null) {
            log("setting_latin_to_default")
            latinVoiceSynthesizer = defaultVoiceSynthesizer
        }
        if (cyrillicVoice == null && mode == MODE_MULTILINGUAL) {
            log("setting_cyrillic_to_default")
            cyrillicVoiceSynthesizer = defaultVoiceSynthesizer
        }

        sonicProcessor = SonicProcessor.Builder()
        .setPitch(pitch)
        .setSpeed(rate)
        .setVolume(volume)
        .setChannels(1)
        .setSampleRate(24000)
        .build()

        log("sonic_info", sonicProcessor.toString())
    }

    fun modeToString(): String {
        return when (mode) {
            MODE_DEFAULT -> "mode_default"
            else -> "mode_multilingual"
        }
    }

    fun log(stringName: String, extraMsg: String = "") {
        if (!isDebug) {
            return
        }
        println(getString(stringName) + extraMsg)
    }

    fun getSynthesizer(voiceName: String?, voiceLicenseOrKey: String? = voiceLicense, voicePath: String? = voiceDataPath): SpeechSynthesizer? {
        if (voiceName == null) {
            log("err_create_synth_voice_name_is_empty")
            return null
        }

                if (voiceLicenseOrKey == null) {
            log("warn_create_synth_license_or_key_is_empty")
        }

        val config = EmbeddedSpeechConfig.fromPath(voicePath ?: getDefaultVoicesDirectory())
        config.setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Raw24Khz16BitMonoPcm)
        config.setSpeechSynthesisVoice(voiceName, voiceLicenseOrKey ?: getLicense(voiceName, voicePath ?: getDefaultVoicesDirectory()))
        val audioConfig = createAudioConfig()
        return SpeechSynthesizer(config, audioConfig)
    }

    fun getString(stringName: String): String {
        return resources.getString(stringName)
    }

    // Выводит текст на экран, но с учетом языка
    fun print(stringName: String) {
        println(getString(stringName))
    }

    // Ошибка stderr
    // Ошибка должна быть строкой из messages.properties
    fun error(stringName: String, err: String = "") {
        if (err.isNotEmpty()) {
            System.err.println(MessageFormat.format(getString(stringName), err))
        } else {
            System.err.println(getString(stringName))
        }
        System.err.flush()
    }

    // Возвращает директорию с моделями TTS по умолчанию
    fun getDefaultVoicesDirectory(): String = System.getProperty("user.home") + "/.neuralvoices/"

        // Каждая модель TTs должна содержать в себе файл model.key
    fun getLicense(voiceName: String, voicePath: String? = voiceDataPath): String {
        val voiceDir = voicePath ?: voices[voiceName] ?: return ""
        val licenseFile = File(voiceDir, "model.key")

                log("voice_data_path", voiceDir)

        if (!licenseFile.exists()) {
            error("warn_no_license_or_enc_key")
            log("for_eg", licenseFile.absolutePath)
            return ""
        }
        return try {
            licenseFile.readText()
        } catch (e: IOException) {
            error("$e")
            "" // Чисто технически пустая лицензия возможно будет работать, особенно с голосами из docker контейнеров
        }
    }

    fun createAudioConfig(): AudioConfig? {
        //if (isSTDOut) {
            val outStream = AudioOutputStream.createPushStream(this)
            return AudioConfig.fromStreamOutput(outStream)
        //}
        return AudioConfig.fromDefaultSpeakerOutput()
    }

    fun run() {
            log("preparing_to_speak")

            if (mode == MODE_DEFAULT) {
                defaultVoiceSynthesizer?.let {
                    speak(it)
                }
            } else {
                speakMultilingual()
            }

            line?.drain()
            line.stop()
            line.close()

            log("done")
    }

    fun speakMultilingual() {
        if (text == null) {
            log("enter_text")
            return
        }
        log("preparing_to_speak_multilingual")

        val tokens = splitText()

        if (isDebug) {
            println(tokens.joinToString(separator="\n"))
        }

        for (token in tokens) {
            val synth = when (token.first) {
                TEXT_LATIN -> latinVoiceSynthesizer
                TEXT_DEFAULT -> defaultVoiceSynthesizer ?: latinVoiceSynthesizer ?: cyrillicVoiceSynthesizer
                else -> cyrillicVoiceSynthesizer
            } ?: continue
            speak(synth, token.second)
        }
    }

    fun splitText(): List<Pair<Int, String>> {
        val tokens = mutableListOf<Pair<Int, String>>()
        val sb = StringBuilder()
        var localMode = TEXT_DEFAULT

        for (char in text ?: return tokens) {
            val unicodeBlock = Character.UnicodeBlock.of(char)
            if (!char.isLetter()) {
                if (sb.isEmpty()) {
                    localMode = TEXT_DEFAULT
                }
                sb.append(char.toString())
            }
            // Латиница
            if (char.isLetter() && unicodeBlock == Character.UnicodeBlock.BASIC_LATIN) {
                if (localMode != TEXT_LATIN) {
                    tokens.add(Pair(localMode, sb.toString()))
                    sb.clear()
                }
                localMode = TEXT_LATIN
                sb.append(char.toString())
            }
            // Кирилица
            if (char.isLetter() 
            && (unicodeBlock == Character.UnicodeBlock.CYRILLIC 
            || unicodeBlock == Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY
            || unicodeBlock == Character.UnicodeBlock.CYRILLIC_EXTENDED_A
            || unicodeBlock == Character.UnicodeBlock.CYRILLIC_EXTENDED_B)) {
                if (localMode != TEXT_CYRILLIC) {
                    tokens.add(Pair(localMode, sb.toString()))
                    sb.clear()
                }
                localMode = TEXT_CYRILLIC
                sb.append(char.toString())
            }
        }
        tokens.add(Pair(localMode, sb.toString()))
        return tokens
    }

    fun speak(synth: SpeechSynthesizer, text: String = this.text ?: "") {
        val result = if (isTextSSML) {
            synth.SpeakSsml(text)
        } else {
            synth.SpeakText(text)
        }

        if (result.reason == ResultReason.SynthesizingAudioCompleted) {
            onDone(result)
        } else {
            onError(result)
        }
    }

    fun onDone(result: SpeechSynthesisResult) {
        if (!isSTDOut) {
            return
        }

        // System.out.write(result.audioData)
        // System.out.flush()
    }

    fun onError(result: SpeechSynthesisResult) {
        val details = SpeechSynthesisCancellationDetails.fromResult(result)
        System.err.println(details.errorDetails)
        System.err.flush()
    }
}

     fun main(args: Array<String>) {
    val nv = NeuralVoices()
    if (args.size == 0) {
        nv.error("help")
        return
    }

    var i = 0
    while (i < args.size) {
        val arg = args[i]
        when (arg) {
            "-h", "--help" -> {
                nv.print("help")
                return
            }
            "-v", "--voice" -> nv.voice = args.getOrNull(++i)
            "-std" -> nv.isSTDOut = true
            "-vdp", "--voice-data-path" -> nv.voiceDataPath = args.getOrNull(++i)
            "-log" -> nv.isDebug = true
            "-t", "--text" -> {
                if (nv.text != null && nv.text!!.trim().isNotEmpty() == true) {
                    break
                }
                if (i+1 >= args.size) {
                    nv.error("enter_text")
                    return
                }
                nv.text = args.copyOfRange(i+1, args.size).joinToString(separator = " ")
                break
            }
            "-l", "--license",
            "-k", "--key" -> nv.voiceLicense = args.getOrNull(++i)
            "-ssml" -> nv.isTextSSML = true
            "-lv", "--latin-voice" -> nv.latinVoice = args.getOrNull(++i)
            "-cv", "--cyrillic-voice" -> nv.cyrillicVoice = args.getOrNull(++i)
            "-lvdp", "--latin-voice-data-path" -> nv.latinVoiceDataPath = args.getOrNull(++i)
            "-cvdp", "--cyrillic-voice-data-path" -> nv.cyrillicVoiceDataPath = args.getOrNull(++i)
            "-cvl", "--cyrillic-voice-license" -> nv.cyrillicVoiceLicense = args.getOrNull(++i)
            "-lvl", "--latin-voice-license" -> nv.latinVoiceLicense = args.getOrNull(++i)
            "-spd" -> nv.runServer()
            "-stdin" -> {
                nv.text = System.`in`.bufferedReader().readText().trimEnd()
            }
            "-p", "--pitch" -> nv.pitch = args.getOrNull(++i)?.toFloatOrNull() ?: 1f
            "-r", "--rate" -> nv.rate = args.getOrNull(++i)?.toFloatOrNull() ?: 1f
            "-V", "--volume" -> nv.volume = args.getOrNull(++i)?.toFloatOrNull() ?: 1f
            else -> {
                nv.error("unknown_argument", arg)
                return
            }
        }
        ++i
    }

    nv.init()

    if (nv.defaultVoiceSynthesizer == null && nv.mode == NeuralVoices.MODE_DEFAULT) {
        nv.error("enter_voice_name")
        return
    }
    if (nv.text == null) {
        nv.error("enter_text")
        return
    }
    if (nv.latinVoiceSynthesizer == null && nv.mode == NeuralVoices.MODE_MULTILINGUAL) {
        nv.error("enter_latin_voice_name")
        return
    }
    if (nv.cyrillicVoiceSynthesizer == null && nv.mode == NeuralVoices.MODE_MULTILINGUAL) {
        nv.error("enter_cyrillic_voice_name")
        return
    }
    nv.run()
}