package de.vanfanel.joustmania.sound

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

val wavAudioFormat: AudioFormat = AudioFormat(44100f, 16, 1, true, false)

fun convertMp3InputStreamToWavOutputStream(input: InputStream, output: OutputStream) {
    try {
        ByteArrayOutputStream().use { rawOutputStream ->
            convert(input, rawOutputStream, wavAudioFormat)
            val rawResult = rawOutputStream.toByteArray()
            val audioInputStream = AudioInputStream(
                ByteArrayInputStream(rawResult), wavAudioFormat, rawResult.size.toLong()
            )
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, output)
        }
    } catch (e: Exception) {
        throw ConversionException(e)
    }
}

@Throws(Exception::class)
private fun convert(input: InputStream, output: OutputStream, targetFormat: AudioFormat?) {
    AudioSystem.getAudioInputStream(input).use { rawSourceStream ->
        val sourceFormat = rawSourceStream.getFormat()
        val convertFormat = getAudioFormat(sourceFormat)
        AudioSystem.getAudioInputStream(convertFormat, rawSourceStream).use { sourceStream ->
            AudioSystem.getAudioInputStream(targetFormat, sourceStream).use { convertStream ->
                var read: Int
                val buffer = ByteArray(8192)
                while ((convertStream.read(buffer, 0, buffer.size).also { read = it }) >= 0) {
                    output.write(buffer, 0, read)
                }
            }
        }
    }
}

private fun getAudioFormat(sourceFormat: AudioFormat): AudioFormat {
    return AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(),

        sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false
    )
}

class ConversionException(cause: Throwable?) : RuntimeException("Failed to convert audio data", cause)