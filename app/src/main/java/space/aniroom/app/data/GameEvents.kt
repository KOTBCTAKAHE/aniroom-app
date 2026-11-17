package space.aniroom.app.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable

@Serializable
data class UnlockItem(val kind: String = "title", val key: String = "", val name: String = "", val level: Int = 0)

@Serializable
data class LevelUpFull(
    val level: Int = 1,
    val fromLevel: Int = 0,
    val progress: Double = 0.0,
    val toNext: Int = 0,
    val unlocked: List<UnlockItem> = emptyList(),
)

@Serializable
data class XpFull(
    val gained: Int = 0,
    val level: Int = 1,
    val progress: Double = 0.0,
    val levelUp: Boolean = false,
    val reasons: List<XpReason> = emptyList(),
)

@Serializable
data class MomentPayload(val kind: String = "", val reaction: String? = null, val users: List<String> = emptyList(), val count: Int = 0)

@Serializable
data class SeasonPayload(val season: Int = 0, val completedSeasons: Int = 0, val totalSeasons: Int = 4, val title: String = "")

@Serializable
data class AchievementFull(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val rarity: String = "common",
    val icon: String = "",
    val completedSeasons: Int? = null,
    val totalSeasons: Int? = null,
)

@Serializable
data class RoomGoalPayload(
    val roomCode: String = "",
    val title: String? = null,
    val done: Int = 0,
    val total: Int = 0,
    val completed: Boolean = false,
    val rewardXp: Int = 0,
    val justCompleted: Boolean = false,
)

/** Everything the "game layer" announces: achievements, XP, levels, quests, streaks, moments. */
sealed interface GameEvent {
    val id: Long

    data class Achievement(
        override val id: Long,
        val key: String,
        val title: String,
        val description: String,
        val rarity: String,
        val icon: String,
        val season: Boolean = false,
        val completedSeasons: Int = 0,
        val totalSeasons: Int = 4,
    ) : GameEvent

    data class Xp(override val id: Long, val gained: Int, val label: String, val progress: Float, val level: Int) : GameEvent
    data class Quest(override val id: Long, val title: String, val xp: Int) : GameEvent
    data class Streak(override val id: Long, val days: Int, val bonus: Int) : GameEvent
    data class LevelUp(override val id: Long, val event: LevelUpFull) : GameEvent
    data class Moment(override val id: Long, val payload: MomentPayload) : GameEvent
    data class Goal(override val id: Long, val goal: RoomGoalPayload) : GameEvent
}

object GameEvents {
    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()
    private val seen = LinkedHashSet<String>()
    private var seq = 0L

    fun nextId() = ++seq

    /** Emits unless do-not-disturb is on; [dedupe] drops repeats (same achievement via socket + notification). */
    fun emit(event: GameEvent, dedupe: String? = null) {
        if (Session.user.value?.doNotDisturb == true) return
        if (dedupe != null) {
            if (!seen.add(dedupe)) return
            if (seen.size > 200) seen.remove(seen.first())
        }
        _events.tryEmit(event)
    }
}
