package de.vanfanel.joustmania.sound

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.util.concurrent.LinkedBlockingQueue
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.DataLine
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine

object SoundManager {
    private val logger = KotlinLogging.logger {}
    private val queue = LinkedBlockingQueue<SoundFile>()
    private var isPlaying = false
    private var locale: SupportedSoundLocale = SupportedSoundLocale.EN

    fun addSoundToQueue(id: SoundId) {
        val soundFile = getSoundBy(id, locale)
        if (soundFile == null) {
            logger.error { "Cannot find sound with id: $id . No item was added to sound play queue." }
            return
        }
        logger.info { "Adding sound to queue: $id" }

        queue.offer(soundFile)
        if (!isPlaying) {
            playNext()
        }
    }

    private fun playNext() {
        isPlaying = true
        CoroutineScope(Dispatchers.IO).launch {
            while (queue.isNotEmpty()) {
                try {
                    val nextSound = queue.poll()
                    logger.info { "Playing ${nextSound.getWavSoundPath()}" }
                    playResource(nextSound.getWavSoundPath())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            isPlaying = false
        }
    }


    private suspend fun playResource(resourcePath: String, deviceIndex: Int = 0) {
        withContext(Dispatchers.IO) {
            val inputStream = javaClass.getResourceAsStream(resourcePath)

            if (inputStream == null) {
                logger.error { "Cannot find file with path: $resourcePath" }
                return@withContext
            }

            val bufferedInputStream = BufferedInputStream(inputStream)
            val originalAudioInputStream = AudioSystem.getAudioInputStream(bufferedInputStream)

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

            val clip = selectedMixer.getLine(info) as Clip
            clip.open(originalAudioInputStream)
            setVolume(clip)
            logger.debug {("Clip opened successfully. length: ${clip.microsecondLength / 1000} ms")}

            clip.start()
            logger.debug { "Clip started successfully." }


            delay(clip.microsecondLength / 1000)
            clip.stop()
            logger.debug { "Clip stopped" }
            clip.close()
            originalAudioInputStream.close()
            bufferedInputStream.close()
            inputStream.close()
        }
    }

    private fun getMixersWithAudioOutput() = AudioSystem.getMixerInfo().filter { mixerInfo ->
        val mixer = AudioSystem.getMixer(mixerInfo)
        val lineInfo = DataLine.Info(SourceDataLine::class.java, null)
        mixer.isLineSupported(lineInfo) // Prüft, ob der Mixer Ausgabe unterstützt
    }

    private fun setVolume(clip: Clip, volume: Float = 1.0f) {
        val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        val dB = (gainControl.maximum - gainControl.minimum) * volume + gainControl.minimum
        gainControl.value = dB
        logger.debug { "Volume set to: $volume (${gainControl.value} dB)" }
    }
}