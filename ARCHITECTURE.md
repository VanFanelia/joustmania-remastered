# JoustMania Remastered - Projektarchitektur & Analyse

*Letzte Aktualisierung: 2025-12-08*

## Inhaltsverzeichnis
1. [Projekt-Übersicht](#projekt-übersicht)
2. [Dependencies & Build-Konfiguration](#dependencies--build-konfiguration)
3. [Projektstruktur & Packages](#projektstruktur--packages)
4. [Architektur](#architektur)
5. [Entry Points & Main-Funktion](#entry-points--main-funktion)
6. [Kernkomponenten](#kernkomponenten)
7. [Threading-Modell](#threading-modell)
8. [Web-API Endpoints](#web-api-endpoints)
9. [Verbesserungsmöglichkeiten](#verbesserungsmöglichkeiten)

---

## Projekt-Übersicht

**JoustMania Remastered** ist ein Multiplayer-Party-Game für PlayStation Move Controller, inspiriert von "Johann Sebastian Joust". Spieler müssen ihre Controller ruhig halten, während sie versuchen, die Controller anderer Spieler durch Bewegung zu eliminieren.

### Technologie-Stack
- **Sprache:** Kotlin 2.1.10
- **JVM:** Java 17
- **Web-Framework:** Ktor 3.1.1
- **Hardware:** PSMoveAPI (JNI), D-Bus (Bluetooth)
- **Architektur:** Coroutines + Flow-basierte reaktive Programmierung

---

## Dependencies & Build-Konfiguration

### Haupt-Dependencies (build.gradle.kts)

**Web-Framework:**
- Ktor 3.1.1 (Server Core, Netty, CORS, Content Negotiation)
- Kotlinx Serialization JSON 1.6.0 ⚠️ *Update auf 1.8.1 empfohlen*

**Hardware & System:**
- PSMoveAPI (lokale JAR: `lib/psmoveapi.jar`)
- DBus Java Core 5.1.0 (Bluetooth über Linux D-Bus)
- DBus Java Transport Native UnixSocket 5.1.0
- JavaFX Media & Controls (Multimedia)

**Audio:**
- JLayer 1.0.1 ⚠️ *Sehr alt (2004), Alternative prüfen*
- MP3SPI 1.9.5.4

**Logging:**
- Kotlin-Logging JVM 7.0.3
- Logback Classic 1.5.13

**Testing:**
- JUnit Jupiter 5.10.0 ⚠️ *Update auf 5.11.x empfohlen*
- Kotlin Test JUnit5 2.1.10
- Kotlinx Coroutines Test 1.7.3 ⚠️ *Update auf 1.9.0 empfohlen*

### Build-Konfiguration
```kotlin
mainClass = "de.vanfanel.joustmania.ApplicationKt"
jvmToolchain = JavaLanguageVersion.of(17)
server.port = 80  // ⚠️ Root-Rechte erforderlich
```

---

## Projektstruktur & Packages

```
src/main/kotlin/de/vanfanel/joustmania/
│
├── Application.kt                    # Main Entry Point & Server Setup
├── GameState.kt                      # Game State Manager (FSM)
├── Routing.kt                        # REST API & SSE Endpoints (450 Zeilen)
│
├── config/                           # Konfiguration & Settings
│   ├── Settings.kt                   # Zentrale Settings mit Flow
│   └── SettingsDTOs.kt              # Data Transfer Objects
│
├── games/                            # Spielmodi
│   ├── Game.kt                       # Game Interface
│   ├── GameWithAcceleration.kt      # Basis für Bewegungs-Games
│   ├── FreeForAll.kt                # Standard FFA Modus
│   ├── SortingToddler.kt            # Farb-Sortier-Spiel
│   ├── Werewolf.kt                  # Werwolf-Modus
│   └── Zombie.kt                    # Zombie-Survival
│
├── hardware/                         # Hardware Abstraction Layer
│   ├── PSMoveApi.kt                 # Zentrale PSMove Controller API
│   ├── PSMovePairingManager.kt      # Pairing-Verwaltung
│   ├── AccelerationDebugger.kt      # Debug-Tools für Beschleunigung
│   │
│   ├── bluetooth/
│   │   ├── BluetoothControllerManager.kt  # D-Bus Bluetooth-Steuerung
│   │   └── BluetoothCommands.kt          # Bluetooth-Befehle
│   │
│   ├── psmove/
│   │   ├── GlobalMoveTicker.kt           # 5ms Polling Ticker
│   │   ├── PSMoveBluetoothConnectionWatcher.kt  # Verbindungs-Monitor
│   │   ├── PSMoveExtensions.kt           # Extension Functions
│   │   └── PSMoveStub.kt                 # PSMove Abstraktion
│   │
│   └── usb/
│       └── USBDevicesChangeWatcher.kt    # USB Hot-Plug Detection
│
├── lobby/
│   └── LobbyLoop.kt                  # Pre-Game Lobby Logic
│
├── sound/
│   ├── SoundManager.kt               # Audio-Wiedergabe
│   ├── soundMap.kt                   # Sound-ID Mapping
│   └── MusicConverter.kt             # Audio-Konvertierung
│
├── types/                            # Type Definitions & DTOs
│   ├── Definitions.kt
│   ├── PSMoveButton.kt
│   ├── RawMovingData.kt
│   └── SettingTypes.kt
│
├── util/                             # Utilities
│   ├── CustomThreadDispatcher.kt     # Thread-Pool Management
│   ├── Ticker.kt                     # Timer/Ticker Utility
│   ├── FixedSizeQueue.kt            # Bounded Queue
│   └── flowExtensions.kt             # Flow Helper Functions
│
└── os/dependencies/
    └── NativeLoader.kt               # JNI Native Library Loader
```

---

## Architektur

### Schichtenmodell

```
┌─────────────────────────────────────────────────────┐
│         Web Frontend (Browser, React/Vue)           │
│              Static Resources + SSE                 │
└────────────────┬────────────────────────────────────┘
                 │ HTTP REST + Server-Sent Events
┌────────────────▼────────────────────────────────────┐
│            Ktor Web Server (Port 80)                │
│  ┌──────────────────────────────────────────────┐  │
│  │  Routing.kt - REST API & SSE Endpoints       │  │
│  │  - /api/settings, /api/game, /api/hardware   │  │
│  │  - /sse/settings, /sse/bluetooth, /sse/game  │  │
│  └──────────────┬───────────────────────────────┘  │
└─────────────────┼──────────────────────────────────┘
                  │
┌─────────────────▼──────────────────────────────────┐
│           Application Core Layer                    │
│  ┌────────────────┐  ┌────────────────┐           │
│  │ GameStateManager│  │   LobbyLoop    │           │
│  │  (State Machine)│  │  (Pre-Game)    │           │
│  └────────┬───────┘  └────────┬───────┘           │
│           │                   │                     │
│  ┌────────▼───────────────────▼────────┐          │
│  │        Games (Interface)             │          │
│  │  - FreeForAll                        │          │
│  │  - Zombie, Werewolf, SortingToddler  │          │
│  └────────┬──────────────────────────────┘         │
└───────────┼────────────────────────────────────────┘
            │
┌───────────▼────────────────────────────────────────┐
│        Hardware Abstraction Layer                   │
│  ┌──────────────────────────────────────────────┐  │
│  │  PSMoveApi - Facade für Controller-Zugriff   │  │
│  │  - setColor(), rumble(), getBatteryLevel()   │  │
│  └──────────────┬───────────────────────────────┘  │
│                 │                                   │
│  ┌──────────────▼───────────────────────────────┐  │
│  │  PSMoveBluetoothConnectionWatcher            │  │
│  │  - Flow<List<PSMoveStub>>                    │  │
│  │  - Battery & Connection Monitoring           │  │
│  └──────────────┬───────────────────────────────┘  │
│                 │                                   │
│  ┌──────────────▼───────────────────────────────┐  │
│  │  GlobalMoveTicker (5ms Polling)              │  │
│  │  - Kontinuierliches PSMove Status-Polling    │  │
│  └──────────────┬───────────────────────────────┘  │
│                 │                                   │
│  ┌──────────────▼───────────────────────────────┐  │
│  │  BluetoothControllerManager (D-Bus)          │  │
│  │  PSMovePairingManager                        │  │
│  │  USBDevicesChangeWatcher                     │  │
│  └──────────────┬───────────────────────────────┘  │
└─────────────────┼──────────────────────────────────┘
                  │
┌─────────────────▼──────────────────────────────────┐
│      Native Layer (JNI + System)                    │
│  - PSMoveAPI (C++ Library via JNI)                 │
│  - Linux D-Bus (Bluetooth Stack)                   │
│  - lib/psmoveapi.jar                               │
└─────────────────────────────────────────────────────┘
```

---

## Entry Points & Main-Funktion

**Location:** `Application.kt:30`

### Initialisierungsreihenfolge

```kotlin
fun main() {
    // 1. Native Libraries laden
    val nativeLoader = NativeLoader

    // 2. Hardware-Manager initialisieren
    val usbDevicesChangeWatcher = USBDevicesChangeWatcher
    val psMoveBluetoothConnectionWatcher = PSMoveBluetoothConnectionWatcher
    val bluetoothControllerManager = BluetoothControllerManager
    val hardwareController = PSMovePairingManager

    // 3. Game State & Settings
    val gameStateManager = GameStateManager
    val settings = Settings
    val accelerationDebugger = AccelerationDebugger

    // 4. Global Ticker starten
    val globalMoveTicker = GlobalMoveTicker

    // 5. Background-Coroutines starten
    CoroutineScope(CustomThreadDispatcher.BLUETOOTH).launch {
        usbDevicesChangeWatcher.startEndlessLoopWithUSBDevicesScan()
    }

    CoroutineScope(CustomThreadDispatcher.BLUETOOTH).launch {
        psMoveBluetoothConnectionWatcher.startEndlessLoopWithPSMoveConnectionScan()
    }

    // 6. Ktor-Server starten (Port 80, blocking)
    val server = embeddedServer(Netty, port = 80, host = "0.0.0.0")
        .start(wait = true)

    // 7. Shutdown-Hook registrieren
    server.monitor.subscribe(ApplicationStopped) {
        CustomThreadDispatcher.shutdown()
        GlobalMoveTicker.stopPSMoveJobs()
    }
}
```

---

## Kernkomponenten

### 1. GameStateManager (GameState.kt:39)

**Finite State Machine** für Game-Lifecycle:

```kotlin
enum class GameState {
    LOBBY,              // Warten auf Spieler
    GAME_STARTING,      // Countdown läuft
    GAME_RUNNING,       // Spiel aktiv
    GAME_FINISHING,     // Spiel endet
    GAME_FINISHED,      // Spiel beendet
    GAME_INTERRUPTED    // Spiel abgebrochen
}
```

**Verantwortlichkeiten:**
- State-Transitionen verwalten
- Game-Instanz halten (`currentGame: Game?`)
- Spieler-Status tracken (`playersInGame`, `playerLostFlow`)
- 5ms Game-Ticker verwalten (`gameWatcherTicker`)

**State-Transitionen:**
```
LOBBY → GAME_STARTING → GAME_RUNNING → GAME_FINISHING → GAME_FINISHED → LOBBY
                                              ↓
                                     GAME_INTERRUPTED
```

### 2. PSMoveApi (hardware/PSMoveApi.kt:19)

**Zentrale Facade** für alle PSMove-Operationen:

```kotlin
object PSMoveApi {
    // Farben setzen
    fun setColor(macAddress: MacAddress, colorToSet: MoveColor)
    fun setColorOnAllMoveController(color: MoveColor)

    // Rumble/Vibration
    fun rumble(moves: Set<MacAddress>, intensity: Int, durationInMs: Long)
    fun clearRumbles(moves: Set<MacAddress>)

    // Status abfragen
    fun refreshMoveStatus(macAddress: MacAddress): PollResult?
    fun getBatteryLevel(macAddress: MacAddress): PSMoveBatteryLevel?
    fun getColor(macAddress: MacAddress): MoveColor?
}
```

**Design-Pattern:** Facade + Singleton
**Thread-Safety:** Delegiert an Thread-sichere PSMoveStubs

### 3. GlobalMoveTicker (hardware/psmove/GlobalMoveTicker.kt)

**5ms Polling-Loop** für alle PSMove-Controller:

- Ruft kontinuierlich `refreshMoveStatus()` auf
- Aktualisiert Farben und Rumble
- Läuft auf dediziertem Thread-Pool
- **Performance-Kritisch:** 200 Aufrufe/Sekunde pro Controller

### 4. Game Interface (games/Game.kt:11)

**Basis-Interface** für alle Spielmodi:

```kotlin
interface Game {
    val name: String
    val currentPlayingController: MutableMap<MacAddress, PSMoveStub>
    val minimumPlayers: Int
    val gameSelectedSound: SoundId

    suspend fun start(players: Set<PSMoveStub>)
    suspend fun checkForGameFinished()
    suspend fun forceGameEnd()
    fun cleanUpGame()
    fun playBackgroundMusic(): Job

    val playersLost: MutableSet<MacAddress>
    val playerLostFlow: Flow<List<MacAddress>>
}
```

**Implementierungen:**
- `FreeForAll` - Standard Free-For-All
- `SortingToddler` - Farb-Sortier-Spiel für Kinder
- `Werewolf` - Werwolf-Modus
- `Zombie` - Zombie-Survival

### 5. BluetoothControllerManager (hardware/bluetooth/BluetoothControllerManager.kt)

**D-Bus Integration** für Linux Bluetooth:

- Kommuniziert mit BlueZ (Linux Bluetooth Stack)
- Pairing & Unpairing
- Adapter-Verwaltung
- Flow-basierte Controller-Liste

---

## Threading-Modell

### CustomThreadDispatcher (util/CustomThreadDispatcher.kt)

**Dedizierte Thread-Pools** für verschiedene Aufgaben:

```kotlin
object CustomThreadDispatcher {
    val BLUETOOTH: CoroutineDispatcher    // Bluetooth-Operationen
    val GAME_STATE: CoroutineDispatcher   // Game State Updates
    val SOUND: CoroutineDispatcher        // Audio-Wiedergabe
    // ... weitere
}
```

**Vorteile:**
- Isolation von Hardware-Operationen
- Vermeidung von Blocking auf Main-Thread
- Bessere Performance durch dedizierte Pools

**Thread-Zuordnung:**
| Komponente | Dispatcher | Grund |
|------------|------------|-------|
| USB Scanning | BLUETOOTH | Hardware-I/O |
| PSMove Polling | BLUETOOTH | Hardware-I/O |
| Game Loop | GAME_STATE | Isolierung |
| Sound | SOUND | Audio-Threads |

---

## Web-API Endpoints

### REST Endpoints (Routing.kt:81-448)

#### Hardware Commands
```
GET  /api/clear-devices              # Alle PSMove trennen & vergessen
DELETE /api/clear-device/{mac}       # Einzelnen Controller entfernen
GET  /api/setColor/{color}           # Farbe für alle setzen
PUT  /api/setRainbowAnimation/{mac}/{duration}  # Rainbow-Animation
PUT  /api/setRumble/{mac}            # Controller vibrieren lassen
```

#### Settings
```
POST /api/settings/sensitivity       # Empfindlichkeit ändern
POST /api/settings/language          # Sprache ändern
POST /api/settings/globalVolume      # Master-Lautstärke
POST /api/settings/musicVolume       # Musik-Lautstärke
POST /api/settings/set-game-mode     # Spielmodus wählen
POST /api/settings/sortToddler/duration        # Rundenzeit
POST /api/settings/sortToddler/amountOfRounds  # Anzahl Runden
```

#### Game Control
```
POST /api/game/force-start           # Spiel sofort starten
POST /api/game/force-stop            # Laufendes Spiel abbrechen
```

#### Debug
```
GET /api/accelerations               # Beschleunigungsdaten (JSON)
GET /api/soundmap                    # Sound-ID Mapping
GET /api/playsound/{soundId}         # Test-Sound abspielen
GET /api/playAllSoundsAsMp3         # Alle Sounds als MP3
GET /api/playAllSoundsAsWav         # Alle Sounds als WAV
```

### Server-Sent Events (SSE) Streams

**Real-time Updates via SSE:**

#### /sse/settings
```json
{
  "sensitivity": "MEDIUM",
  "language": "EN",
  "globalVolume": 80,
  "musicVolume": 60,
  "sortToddlerDuration": 30,
  "sortToddlerRounds": 5
}
```

#### /sse/bluetooth
```json
[{
  "adapterId": "hci0",
  "macAddress": "00:11:22:33:44:55",
  "name": "Motion Controller",
  "pairedMotionController": [{
    "macAddress": "AA:BB:CC:DD:EE:FF",
    "connected": true,
    "isAdmin": false,
    "batteryLevel": 75
  }]
}]
```

#### /sse/game
```json
{
  "selectedGame": "FreeForAll",
  "currentGameState": "Running",
  "activeController": ["AA:BB:CC:DD:EE:FF", "11:22:33:44:55:66"],
  "playerInGame": ["AA:BB:CC:DD:EE:FF", "11:22:33:44:55:66"],
  "playerLost": ["11:22:33:44:55:66"]
}
```

#### /sse/stubsStatistics
Debug-Stream für PSMove-Statistiken (Polling-Rate, Latenz, etc.)

#### /sse/threads
Thread-Hierarchie für Monitoring (alle 5 Sekunden)

---

## Verbesserungsmöglichkeiten

### ⚠️ Kritische Issues

#### 1. Dependency Updates erforderlich
```kotlin
// build.gradle.kts - AKTUALISIEREN:
kotlinx-serialization-json: 1.6.0 → 1.8.1
junit-jupiter: 5.10.0 → 5.11.4
kotlinx-coroutines-test: 1.7.3 → 1.9.0
```

#### 2. Server läuft auf Port 80
**Problem:** Erfordert Root-Rechte, Sicherheitsrisiko

**Lösung:**
```kotlin
// Konfigurierbar machen
val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
embeddedServer(Netty, port = port, host = "0.0.0.0")
```

Dann Reverse-Proxy verwenden:
```nginx
# nginx.conf
server {
    listen 80;
    location / {
        proxy_pass http://localhost:8080;
    }
}
```

#### 3. CORS-Hosts hardcodiert
**Problem:** Hosts sind fest im Code (Routing.kt:100-105)

**Lösung:**
```kotlin
// application.conf
ktor {
    deployment {
        port = 8080
    }
    application {
        cors {
            allowedHosts = ["localhost", "joust.mania"]
        }
    }
}
```

#### 4. JLayer 1.0.1 ist uralt (2004)
**Problem:** Keine Wartung, potenzielle Bugs

**Alternativen:**
- JavaFX Media (bereits als Dependency)
- `javax.sound.sampled` (Standard-JDK)
- Moderne MP3-Library wie `jave2`

### 🔧 Code-Qualität

#### 5. Magic Numbers extrahieren
**Beispiele:**
- 5ms Polling-Intervall
- Port 80
- Delays in Animations

**Lösung:**
```kotlin
object GameConstants {
    const val PSMOVE_POLL_INTERVAL_MS = 5L
    const val GAME_TICK_INTERVAL_MS = 5L
    const val DEFAULT_RUMBLE_DURATION_MS = 1000L
}
```

#### 6. Error-Handling verbessern
**Probleme:**
- Viele `firstOrNull()` ohne Null-Checks
- Exceptions in PSMoveApi ohne zentrale Handler
- Native-Library-Fehler werden nicht gefangen

**Lösung:**
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: Throwable) : Result<Nothing>()
}

fun PSMoveApi.setColorSafe(mac: MacAddress, color: MoveColor): Result<Unit> {
    return try {
        setColor(mac, color)
        Result.Success(Unit)
    } catch (e: MoveNotFoundException) {
        Result.Error(e)
    }
}
```

#### 7. Memory Leak-Prävention
**Problem:** Nicht alle Coroutine-Jobs werden bei Shutdown gecancelt

**Lösung:**
```kotlin
object JobTracker {
    private val jobs = mutableListOf<Job>()

    fun track(job: Job): Job {
        jobs.add(job)
        return job
    }

    suspend fun cancelAll() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }
}

// In ApplicationStopped
server.monitor.subscribe(ApplicationStopped) {
    JobTracker.cancelAll()
    CustomThreadDispatcher.shutdown()
    GlobalMoveTicker.stopPSMoveJobs()
}
```

### 📊 Performance-Optimierungen

#### 8. 5ms Polling zu aggressiv?
**Problem:** 200 Aufrufe/Sekunde pro Controller = hohe CPU-Last

**Analyse:**
- 60 FPS = 16.67ms Frame-Zeit
- Typische Game-Loop: 16ms (60 Hz) oder 8ms (120 Hz)
- 5ms = 200 Hz ist für Motion-Controller übertrieben

**Empfehlung:**
```kotlin
// Reduzieren auf 10ms (100 Hz) oder 16ms (60 Hz)
val gameWatcherTicker = Ticker(10.milliseconds, CustomThreadDispatcher.GAME_STATE)
```

**Messung erforderlich:** CPU-Profiling mit verschiedenen Intervallen

#### 9. Thread-Monitoring optimieren
**Problem:** Routing.kt:439 - Thread-Hierarchie alle 5 Sekunden serialisieren

**Lösung:**
- Nur bei Bedarf aktivieren (Debug-Flag)
- Intervall auf 30-60 Sekunden erhöhen
- Caching der Thread-Liste

```kotlin
get("/sse/threads") {
    if (!Settings.debugMode) {
        call.respond(HttpStatusCode.Forbidden)
        return@get
    }
    // ... rest
}
```

### 🚀 Feature-Verbesserungen

#### 10. Health-Check-Endpoint
```kotlin
get("/api/health") {
    val health = mapOf(
        "status" to "UP",
        "connectedControllers" to PSMoveBluetoothConnectionWatcher.count(),
        "gameState" to GameStateManager.currentGameState.value.name,
        "uptime" to ManagementFactory.getRuntimeMXBean().uptime
    )
    call.respond(health)
}
```

#### 11. Metrics/Prometheus-Integration
```kotlin
// build.gradle.kts
implementation("io.ktor:ktor-server-metrics-micrometer:3.1.1")
implementation("io.micrometer:micrometer-registry-prometheus:1.12.0")

// Application.kt
install(MicrometerMetrics) {
    registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}
```

#### 12. Logging in Dateien (TODO in README)
```xml
<!-- logback.xml -->
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/joustmania.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/joustmania.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
    <encoder>
        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>
```

### 🔒 Sicherheit

#### 13. CORS-Konfiguration zu permissiv
**Problem:** Alle HTTP-Methoden erlaubt

**Empfehlung:**
```kotlin
install(CORS) {
    allowMethod(HttpMethod.Get)
    allowMethod(HttpMethod.Post)
    allowMethod(HttpMethod.Put)
    allowMethod(HttpMethod.Delete)
    // allowMethod(HttpMethod.Options) // Nur bei Bedarf

    allowHeader(HttpHeaders.ContentType)
    // allowHeader(HttpHeaders.Authorization) // Nicht genutzt?

    // Nur benötigte Hosts
    allowHost("localhost:5173", schemes = listOf("http"))
    allowHost("joust.mania", schemes = listOf("http"))
}
```

#### 14. Native Library Pfade absichern
```kotlin
// NativeLoader.kt - Validierung hinzufügen
object NativeLoader {
    init {
        val libPath = System.getProperty("java.library.path")
        require(libPath.isNotBlank()) { "java.library.path not set" }

        try {
            System.loadLibrary("psmoveapi")
        } catch (e: UnsatisfiedLinkError) {
            throw RuntimeException("Failed to load psmoveapi native library", e)
        }
    }
}
```

---

## Priorisierte TODO-Liste

### Sprint 1: Kritische Updates
- [ ] Dependencies aktualisieren (kotlinx-serialization, JUnit)
- [ ] Port 80 → 8080 + Nginx-Konfiguration
- [ ] CORS in application.conf auslagern
- [ ] Logging in Dateien aktivieren

### Sprint 2: Code-Qualität
- [ ] Magic Numbers in Constants extrahieren
- [ ] Error-Handling mit Result-Type
- [ ] Memory Leak-Prävention (JobTracker)
- [ ] Native-Library-Loading mit Fehlerbehandlung

### Sprint 3: Performance
- [ ] Polling-Intervall messen & optimieren
- [ ] Thread-Monitoring nur im Debug-Modus
- [ ] CPU-Profiling durchführen
- [ ] Health-Check-Endpoint

### Sprint 4: Features
- [ ] Multi-Language Support (DE)
- [ ] Metrics/Prometheus-Integration
- [ ] Audio-Output-Auswahl
- [ ] Colored Console Logs

---

## Architektur-Entscheidungen (ADR)

### ADR-001: Kotlin Coroutines + Flow
**Status:** ✅ Akzeptiert

**Kontext:** Asynchrone Hardware-Operationen + UI-Updates

**Entscheidung:** Kotlin Coroutines mit Flow-basierter reaktiver Programmierung

**Vorteile:**
- Strukturierte Concurrency
- Einfaches Error-Handling mit suspend-Functions
- Flow für reaktive Datenströme
- Keine Callback-Hölle

**Nachteile:**
- Lernkurve für neue Entwickler
- Debugging kann komplex sein

### ADR-002: Ktor statt Spring Boot
**Status:** ✅ Akzeptiert

**Kontext:** Leichtgewichtiger Web-Server für Raspberry Pi

**Entscheidung:** Ktor 3.x

**Vorteile:**
- Geringer Memory-Footprint
- Kotlin-native (keine Java-Annotations)
- Schneller Start
- Coroutines-Integration

**Nachteile:**
- Kleineres Ökosystem als Spring
- Weniger Enterprise-Features

### ADR-003: PSMoveAPI via JNI
**Status:** ✅ Akzeptiert

**Kontext:** Hardware-Zugriff auf PSMove-Controller

**Entscheidung:** Native Bindings via JNI (psmoveapi.jar)

**Vorteile:**
- Direkte Hardware-Kontrolle
- Bewährte C++-Bibliothek
- Low-Latency

**Nachteile:**
- Platform-spezifisch (nur Linux)
- Keine Compile-Zeit-Checks
- Schwierig zu debuggen

### ADR-004: D-Bus für Bluetooth
**Status:** ✅ Akzeptiert

**Kontext:** Bluetooth-Pairing & Controller-Management

**Entscheidung:** BlueZ via D-Bus (dbus-java)

**Vorteile:**
- Standard Linux-Bluetooth-Stack
- Zuverlässig
- Systemweite Verwaltung

**Nachteile:**
- Linux-only
- Komplexe API

---

## Kontakt & Wartung

**Aktueller Maintainer:** van (GitHub: @vanfanel)

**Ursprüngliches Projekt:** [JoustMania by adangert](https://github.com/adangert/JoustMania)

**PSMoveAPI:** [thp/psmoveapi](https://github.com/thp/psmoveapi)

---

*Diese Dokumentation wird bei Architektur-Änderungen aktualisiert.*
