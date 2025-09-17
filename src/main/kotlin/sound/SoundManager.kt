package de.vanfanel.joustmania.sound

import de.vanfanel.joustmania.config.Settings
import de.vanfanel.joustmania.util.CustomThreadDispatcher.SOUND
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tinysound.Music
import tinysound.TinySound
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


data class SoundQueueEntry(
    val soundFile: SoundFile,
    val abortOnNewSound: Boolean,
    val onSoundFilePlayed: suspend () -> Unit = {},
    val playMp3: Boolean = false
)

object SoundManager {
    private val logger = KotlinLogging.logger {}
    private val queue = LinkedBlockingQueue<SoundQueueEntry>()
    private var currentBackgroundSound: SoundId? = null
    private var currentBackgroundSoundJob: Job? = null
    private var isPlaying = false
    private var lastSound: SoundQueueEntry? = null
    private var locale: SupportedSoundLocale = SupportedSoundLocale.EN
    private var lastPlayJob: Job? = null
    private var musicVolume: Double = 0.5

    init {
        TinySound.init()
        TinySound.setGlobalVolume((Settings.getGlobalVolume() / 100.0))

        CoroutineScope(SOUND).launch {
            Settings.currentConfigFlow.collect {
                logger.info { "New global volume: ${it.globalVolume}, new music volume: ${it.musicVolume}" }
                TinySound.setGlobalVolume((it.globalVolume / 100.0))
                setMusicVolume(it.musicVolume / 100.0)
            }
        }
    }

    fun clearQueueFromAllOptionalSounds() {
        queue.forEach {
            if (it.abortOnNewSound) {
                queue.remove(it)
            }
        }
    }

    fun asyncAddSoundToQueue(
        id: SoundId,
        abortOnNewSound: Boolean = true,
        playMp3: Boolean = false,
        onSoundFilePlayed: suspend () -> Unit = {}
    ) {
        val soundFile = getSoundBy(id, locale)
        if (soundFile == null) {
            logger.error { "Cannot find sound with id: $id . No item was added to sound play queue." }
            return
        }
        logger.info { "Adding sound to queue: $id" }

        clearQueueFromAllOptionalSounds()

        queue.offer(
            SoundQueueEntry(
                soundFile = soundFile,
                abortOnNewSound = abortOnNewSound,
                onSoundFilePlayed = onSoundFilePlayed,
                playMp3 = playMp3
            )
        )

        if (isPlaying && lastSound?.abortOnNewSound == true) {
            lastPlayJob?.cancel()
        }

        if (!isPlaying) {
            playNextSound()
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

    private fun playNextSound() {
        isPlaying = true
        CoroutineScope(SOUND).launch {
            while (queue.isNotEmpty()) {
                try {
                    val nextSound = queue.poll()
                    lastSound = nextSound
                    val soundFile =
                        if (nextSound.playMp3) nextSound.soundFile.getMp3SoundPath() else nextSound.soundFile.getWavSoundPath()
                    logger.info { "Playing $soundFile" }
                    lastPlayJob = launch {
                        playSound(resourcePath = soundFile)
                        delay(nextSound.soundFile.durationInMs)
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

    private fun playSound(
        resourcePath: String,
    ) {
        val soundToPlay = TinySound.loadSound(resourcePath, true)
        soundToPlay.play()
    }

    var currentPlayingMusic: Music? = null

    private fun playBackgroundMusic(
        resourcePath: String,
    ) {
        currentPlayingMusic?.let {
            it.stop()
            currentPlayingMusic = null
        }
        val musicToPlay = TinySound.loadMusic(resourcePath, true)
        currentPlayingMusic = musicToPlay
        musicToPlay.play(true, 0.10 * musicVolume)
    }

    private fun setMusicVolume(musicVolume: Double) {
        this.musicVolume = musicVolume
        currentPlayingMusic?.setVolume(0.10 * musicVolume)
    }

    @Deprecated("not yet implemented correct")
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
        currentBackgroundSound?.let {
            val soundFile = getSoundBy(it, locale)
            soundFile?.let { file ->
                playBackgroundMusic(file.getWavSoundPath())
            }
        }
    }
}
