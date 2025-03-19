package de.vanfanel.joustmania

import de.vanfanel.joustmania.NativeLoader.getOSType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.BufferedReader
import java.io.InputStreamReader

object BluetoothCommands {

    private val logger = KotlinLogging.logger {}

    private val _blueToothRestarted: MutableSharedFlow<Unit> = MutableSharedFlow(
        replay = 0,
        extraBufferCapacity = 1
    )
    val blueToothRestarted: Flow<Unit> = _blueToothRestarted

    private fun runCommand(command: String): String {
        val process = Runtime.getRuntime().exec(command)
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = StringBuilder()

        reader.useLines { lines ->
            lines.forEach { line ->
                output.append(line).append("\n")
            }
        }
        process.waitFor()
        return output.toString()
    }

    suspend fun restartBluetooth() {
        logger.info { "try to restart bluetooth ..." }

        when (getOSType()) {
            OSType.ANDROID -> logger.warn { "Detect Android. Cannot restart bluetooth on Android yet :(" }
            OSType.LINUX -> runCommand("systemctl restart bluetooth.service")
            OSType.MAC -> logger.warn { "Detect MAC OS. Cannot restart bluetooth on MAC OS yet :(" }
            OSType.WINDOWS -> {
                runCommand("cmd /c net stop bthserv")
                runCommand("cmd /c net start bthserv")
            }

            OSType.UNSUPPORTED -> logger.warn { "cannot detect OS Type. Cannot restart bluetooth. " }
        }
        _blueToothRestarted.tryEmit(Unit)
        delay(1000)

        logger.info { "restart bluetooth finished" }
    }
}