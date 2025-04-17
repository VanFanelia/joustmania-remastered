
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    application
    kotlin("plugin.serialization") version libs.versions.kotlin.version.get()
}

group = "de.vanfanel.joustmania"
version = "0.0.1"

application {
    mainClass.set("de.vanfanel.joustmania.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("--add-modules=javafx.media,javafx.controls", "-Dio.ktor.development=$isDevelopment", )
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


    // dbus library to talk to bluetooth adapter
    implementation("com.github.hypfvieh:dbus-java-core:5.1.0")
    implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:5.1.0")

    // generated latest psmoveapi.jar
    implementation(files("libs/psmoveapi.jar"))

    // Logging framework
    implementation ("io.github.oshai:kotlin-logging-jvm:7.0.3")

    // used for sound
    implementation("javazoom:jlayer:1.0.1")

    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // for tests:
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.1.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    testImplementation(libs.ktor.server.test.host)
}

tasks.test {
    useJUnitPlatform()
}