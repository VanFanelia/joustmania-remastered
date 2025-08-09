package de.vanfanel.joustmania.sound

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


data class SoundQueueEntry(
    val soundFile: SoundFile,
    val abortOnNewSound: Boolean,
    val onSoundFilePlayed: suspend () -> Unit = {},
)

const val DEFAULT_SOUND_DEVICE_INDEX: Int = 0

object SoundManager {
    private val logger = KotlinLogging.logger {}
    private val queue = LinkedBlockingQueue<SoundQueueEntry>()
    private var currentBackgroundSound: SoundId? = null
    private var currentBackgroundSoundJob: Job? = null
    private var isPlaying = false
    private var lastSound: SoundQueueEntry? = null
    private var locale: SupportedSoundLocale = SupportedSoundLocale.EN
    private var lastPlayJob: Job? = null

    fun asyncAddSoundToQueue(id: SoundId, abortOnNewSound: Boolean = true, onSoundFilePlayed: suspend () -> Unit = {}) {
        val soundFile = getSoundBy(id, locale)
        if (soundFile == null) {
            logger.error { "Cannot find sound with id: $id . No item was added to sound play queue." }
            return
        }
        logger.info { "Adding sound to queue: $id" }

        queue.offer(
            SoundQueueEntry(
                soundFile = soundFile,
                abortOnNewSound = abortOnNewSound,
                onSoundFilePlayed = onSoundFilePlayed
            )
        )
        if (isPlaying && lastSound?.abortOnNewSound == true) {
            lastPlayJob?.cancel()
        }

        if (!isPlaying) {
            playNext()
        }
    }

    suspend fun addSoundToQueueAndWaitForPlayerFinishedThisSound(
        id: SoundId,
        abortOnNewSound: Boolean,
        minDelay: Long = 0L
    ) =
        suspendCoroutine { continuation ->
            val start = Instant.now().toEpochMilli()
            this.asyncAddSoundToQueue(id = id, abortOnNewSound = abortOnNewSound) {
                val duration = Instant.now().toEpochMilli() - start
                if (duration < minDelay) {
                    val delay = minDelay - duration
                    logger.debug { "Sound play time was less then minDelay. Add a delay of $delay ms" }
                    delay(minDelay - duration)
                }
                continuation.resume(Unit)
            }
        }

    private fun playNext() {
        isPlaying = true
        CoroutineScope(Dispatchers.IO).launch {
            while (queue.isNotEmpty()) {
                try {
                    val nextSound = queue.poll()
                    lastSound = nextSound
                    logger.info { "Playing ${nextSound.soundFile.getWavSoundPath()}" }
                    lastPlayJob = launch {
                        playResource(nextSound.soundFile.getWavSoundPath())
                    }
                    try {
                        lastPlayJob?.join()
                    } catch (e: CancellationException) {
                        logger.debug(e) { "current play job was cancelled" }
                    } finally {
                        lastPlayJob = null
                    }
                    nextSound.onSoundFilePlayed()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            isPlaying = false
        }
    }

    private fun convertMp3ToWav(inputStream: InputStream): ByteArray? {
        try {
            ByteArrayOutputStream().use { output ->
                convertMp3InputStreamToWavOutputStream(inputStream, output)

                return output.toByteArray()
            }
        } catch (e: Exception) {
            logger.error(e) { "Cannot convert mp3 to wav" }
            return null
        }
    }

    private suspend fun playResource(
        resourcePath: String,
        deviceIndex: Int = DEFAULT_SOUND_DEVICE_INDEX,
        volume: Float = 1.0f,
        isMp3: Boolean = false
    ) {
        withContext(Dispatchers.IO) {
            var inputStream: InputStream? = null
            var bufferedInputStream: BufferedInputStream? = null
            var originalAudioInputStream: AudioInputStream? = null
            var clip: Clip? = null
            try {
                inputStream = javaClass.getResourceAsStream(resourcePath)

                if (inputStream == null) {
                    logger.error { "Cannot find file with path: $resourcePath" }
                    return@withContext
                }

                if (isMp3) {
                    logger.debug { "Converting mp3 to wav" }
                    val convertedBytes = convertMp3ToWav(inputStream)
                    if (convertedBytes != null) {
                        inputStream = convertedBytes.inputStream()
                    } else {
                        logger.error { "Cannot convert mp3 file to wav" }
                    }
                }

                bufferedInputStream = BufferedInputStream(inputStream)
                originalAudioInputStream = AudioSystem.getAudioInputStream(bufferedInputStream)

                val outputMixers = getMixersWithAudioOutput()

                logger.debug {
                    "found ${outputMixers.size} Mixer: ${
                        outputMixers.mapIndexed { index, info ->
                            "#$index (${info.description})"
                        }
                    }"
                }

                if (deviceIndex < 0 || deviceIndex >= outputMixers.size) {
                    logger.error { "Cannot find audio device with id: $deviceIndex " }
                    return@withContext
                }
                val selectedMixer = AudioSystem.getMixer(outputMixers[deviceIndex])
                logger.debug { "use mixer: ${selectedMixer.mixerInfo.name} ${selectedMixer.mixerInfo.description}" }

                val info = DataLine.Info(Clip::class.java, originalAudioInputStream.format)

                if (!selectedMixer.isLineSupported(info)) {
                    logger.error { "The selected device does not support the audio format" }
                    return@withContext
                }

                clip = selectedMixer.getLine(info) as Clip
                clip.open(originalAudioInputStream)
                setVolume(clip, volume)
                logger.debug { ("Clip opened successfully. length: ${clip.microsecondLength / 1000} ms") }

                clip.start()
                logger.debug { "Clip started successfully." }

                try {
                    delay(clip.microsecondLength / 1000)
                } catch (_: InterruptedException) {
                    logger.info { "Interrupted while waiting for player finished current sound play" }
                }
            } finally {
                clip?.stop()
                logger.debug { "Clip stopped" }
                clip?.close()
                originalAudioInputStream?.close()
                bufferedInputStream?.close()
                inputStream?.close()
                logger.debug { "Cleanup finished" }
            }
        }
    }

    private fun getMixersWithAudioOutput() = AudioSystem.getMixerInfo().filter { mixerInfo ->
        val mixer = AudioSystem.getMixer(mixerInfo)
        val lineInfo = DataLine.Info(SourceDataLine::class.java, null)
        mixer.isLineSupported(lineInfo)
    }

    private fun setVolume(clip: Clip, volume: Float = 1.0f) {
        val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        val dB = (gainControl.maximum - gainControl.minimum) * volume + gainControl.minimum
        gainControl.value = dB
        logger.debug { "Volume set to: $volume (${gainControl.value} dB)" }
    }

    fun clearSoundQueue() {
        logger.info { "Clearing sound queue" }
        queue.clear()
    }

    fun stopSoundPlay() {
        logger.info { "Force stop for sound" }
        queue.clear()
    }

    fun stopBackgroundSound() {
        currentBackgroundSoundJob?.cancel(CancellationException("Force stopped background sound"))
    }


    fun playBackground(soundId: SoundId) {
        currentBackgroundSound = soundId
        this.logger.info { "Playing background sound: ${soundId.name}" }
        currentBackgroundSoundJob = CoroutineScope(Dispatchers.IO).launch {
            while (currentBackgroundSound != null) {
                currentBackgroundSound?.let {
                    val soundFile = getSoundBy(it, locale)
                    soundFile?.let { file ->
                        playResource(resourcePath = file.getMp3SoundPath(), volume = 0.8f, isMp3 = true)
                    }
                }
            }
        }

    }
}
