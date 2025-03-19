
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
}

group = "de.vanfanel.joustmania"
version = "0.0.1"

application {
    mainClass.set("de.vanfanel.joustmania.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
tasks.withType<JavaCompile> {
    options.release.set(17)
}

tasks.withType<JavaExec> {
    jvmArgs = listOf(
        "-Djava.library.path=/lib:/usr/lib:/usr/lib/x86_64-linux-gnu"
    )
}

tasks {
    shadowJar {
        archiveBaseName.set("joustmania-ktor")
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.core)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

    // dbus library to talk to bluetooth adapter
    implementation("com.github.hypfvieh:dbus-java-core:5.1.0")
    implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:5.1.0")

    // generated latest psmoveapi.jar
    implementation(files("libs/psmoveapi.jar"))

    // Logging framework
    implementation ("io.github.oshai:kotlin-logging-jvm:7.0.3")
}
