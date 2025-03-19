package de.vanfanel.joustmania.os.dependencies

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.Locale

enum class Architecture {
    X86,
    X86_64,
    ARM_32,
    ARM_64,
    UNSUPPORTED,
}

enum class OSType {
    ANDROID,
    LINUX,
    MAC,
    WINDOWS,
    UNSUPPORTED,
}

object NativeLoader {

    private val logger = KotlinLogging.logger {}
    private var osType: OSType
    private var arch: Architecture

    init {
        osType = detectOS()
        arch = detectArchitecture()
        logger.info { "Detect OS type $osType with architecture $arch" }

        loadLibPsMoveApi()
    }

    fun getOSType(): OSType {
        return osType
    }

    private fun detectOS(): OSType {
        val osName = System.getProperty("os.name")
        return when {
            osName.startsWith("Linux") -> {
                if ("dalvik" == System.getProperty("java.vm.name").lowercase(Locale.getDefault())) {
                    OSType.ANDROID
                } else {
                    OSType.LINUX
                }
            }

            osName.startsWith("Mac") || osName.startsWith("Darwin") -> {
                OSType.MAC
            }

            osName.startsWith("Windows") -> {
                OSType.WINDOWS
            }

            else -> {
                OSType.UNSUPPORTED
            }
        }

    }

    private fun detectArchitecture(): Architecture {
        val arch = System.getProperty("os.arch")

        return when (arch) {
            "x86", "i386", "i586", "i686" -> Architecture.X86
            "x86_64", "amd64" -> Architecture.X86_64
            "arm", "armv7l" -> Architecture.ARM_32
            "aarch64", "warm64" -> Architecture.ARM_64
            else -> Architecture.UNSUPPORTED
        }
    }

    private fun loadLibPsMoveApi() {
        val path = System.getProperty("user.dir")
        // bindings upfuck so we make a shortcut here :(

        try {
            when (osType) {
                OSType.LINUX -> {
                    when (arch) {
                        Architecture.X86 -> System.load("${path}/libs/linux/libpsmoveapi.x86.so")
                        Architecture.X86_64 -> System.load("${path}/libs/linux/libpsmoveapi.x86_64.so")
                        Architecture.ARM_32 -> System.load("${path}/libs/linux/libpsmoveapi.arm32.so")
                        Architecture.ARM_64 -> System.load("${path}/libs/linux/libpsmoveapi.arm64.so")
                        Architecture.UNSUPPORTED -> throw IllegalStateException("Unsupported Processor architecture found. Cannot find Opencv library for $arch")
                    }
                }

                else -> {
                    throw IllegalStateException("Unsupported OS. Cannot load LibPsMoveApi on OS: $osType")
                }
            }
        } catch (exception: Exception) {
            logger.error(exception) { "Cannot load native library. Error: ${exception.message}" }
        }
    }
}
