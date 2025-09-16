package de.vanfanel.joustmania.util

import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.serialization.Serializable
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory


object CustomThreadDispatcher {
    val POLLING =
        Executors.newFixedThreadPool(16, NamedThreadFactory("MovePollingSinglePool-Thread", priority = 6) ).asCoroutineDispatcher()
    val OBSERVE_ACCELERATION =
        Executors.newFixedThreadPool(4, NamedThreadFactory("AccelerationFixPool-Thread", priority = 6)).asCoroutineDispatcher()
    val GAME_LOGIC =
        Executors.newSingleThreadExecutor(NamedThreadFactory("GameLogicSinglePool-Thread", priority = 5)).asCoroutineDispatcher()
    val GAME_STATE =
        Executors.newSingleThreadExecutor(NamedThreadFactory("GameStateSinglePool-Thread", priority = 5)).asCoroutineDispatcher()
    val SOUND = Executors.newSingleThreadExecutor(NamedThreadFactory("SoundSinglePool-Thread", priority = 3)).asCoroutineDispatcher()
    val BACKGROUND_SOUND = Executors.newSingleThreadExecutor(NamedThreadFactory("BackgroundSoundSinglePool-Thread", priority = 2))
        .asCoroutineDispatcher()
    val BLUETOOTH =
        Executors.newFixedThreadPool(3, NamedThreadFactory("BluetoothFixPool-Thread", priority = 6)).asCoroutineDispatcher()
    val DEBUG_UI = Executors.newCachedThreadPool(NamedThreadFactory("DebugUICachedPool-Thread", priority = 1)).asCoroutineDispatcher()
    val GAME_CONTROLLER_ACTION =
        Executors.newSingleThreadExecutor(NamedThreadFactory("GameControllerActionSinglePool-Thread", priority = 7))
            .asCoroutineDispatcher()

    fun shutdown() {
        POLLING.close()
        OBSERVE_ACCELERATION.close()
        GAME_LOGIC.close()
        GAME_STATE.close()
        SOUND.close()
        BACKGROUND_SOUND.close()
        BLUETOOTH.close()
        DEBUG_UI.close()
        GAME_CONTROLLER_ACTION.close()
    }
}

class NamedThreadFactory(private val baseName: String, private val priority: Int = Thread.NORM_PRIORITY) :
    ThreadFactory {
    private var counter = 0

    override fun newThread(r: Runnable): Thread {
        val thread = Thread(r, "$baseName-${counter++}")
        thread.priority = priority
        return thread
    }
}

@Serializable
data class ThreadData(
    val name: String,
    val id: Long,
    val state: String,
    val isDaemon: Boolean,
    val priority: Int,
    val parentId: Long?,
    val children: MutableList<Long> = mutableListOf()
)

@Serializable
data class ThreadGroupData(
    val name: String,
    val maxPriority: Int,
    val isDaemon: Boolean,
    val parentName: String?,
    val threads: List<ThreadData> = emptyList(),
    val subGroups: List<ThreadGroupData> = emptyList()
)

@Serializable
data class ThreadHierarchy(
    val rootGroup: ThreadGroupData,
    val totalThreadCount: Int
)

fun getCompleteThreadHierarchy(): ThreadHierarchy {
    val rootGroup = Thread.currentThread().threadGroup
    var topGroup = rootGroup

    // Finde die oberste ThreadGroup
    while (topGroup?.parent != null) {
        topGroup = topGroup.parent
    }

    fun mapThreadGroup(group: ThreadGroup): ThreadGroupData {
        val threads = mutableListOf<Thread>()
        val subGroups = mutableListOf<ThreadGroup>()

        // current threads in a group
        val threadArray = arrayOfNulls<Thread>(group.activeCount() * 2)
        val threadCount = group.enumerate(threadArray, false)
        for (i in 0 until threadCount) {
            threadArray[i]?.let { threads.add(it) }
        }

        // Sub-ThreadGroups
        val groupArray = arrayOfNulls<ThreadGroup>(group.activeGroupCount() * 2)
        val groupCount = group.enumerate(groupArray, false)
        for (i in 0 until groupCount) {
            groupArray[i]?.let { subGroups.add(it) }
        }

        return ThreadGroupData(
            name = group.name,
            maxPriority = group.maxPriority,
            isDaemon = group.isDaemon,
            parentName = group.parent?.name,
            threads = threads.map { thread ->
                ThreadData(
                    name = thread.name,
                    id = thread.id,
                    state = thread.state.toString(),
                    isDaemon = thread.isDaemon,
                    priority = thread.priority,
                    parentId = null // Bei ThreadGroups ist das anders organisiert
                )
            }.sortedBy { it.name },
            subGroups = subGroups.map { mapThreadGroup(it) }.sortedBy { it.name }
        )
    }

    val hierarchy = topGroup?.let { mapThreadGroup(it) } ?: ThreadGroupData("Unknown", 0, false, null)
    val totalCount = countAllThreads(hierarchy)

    return ThreadHierarchy(hierarchy, totalCount)
}

private fun countAllThreads(group: ThreadGroupData): Int {
    return group.threads.size + group.subGroups.sumOf { countAllThreads(it) }
}
