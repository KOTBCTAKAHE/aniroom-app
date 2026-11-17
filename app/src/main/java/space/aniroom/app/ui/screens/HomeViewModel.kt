package space.aniroom.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import space.aniroom.app.data.AnimeChoice
import space.aniroom.app.data.AppJson
import space.aniroom.app.data.Progress
import space.aniroom.app.data.Room
import space.aniroom.app.data.UserHub
import space.aniroom.app.data.api
import space.aniroom.app.data.obj

data class ContinueItem(val progress: Progress, val room: Room?)

data class HomeUi(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val history: List<Room> = emptyList(),
    val live: List<Room> = emptyList(),
    val continueWatching: List<ContinueItem> = emptyList(),
    val error: String? = null,
)

class HomeViewModel : ViewModel() {
    private val _ui = MutableStateFlow(HomeUi())
    val ui: StateFlow<HomeUi> = _ui.asStateFlow()

    init { refresh(initial = true) }

    fun refresh(initial: Boolean = false) {
        _ui.update { it.copy(refreshing = !initial, loading = initial && it.history.isEmpty()) }
        viewModelScope.launch {
            val history = async { api { history() } }
            val live = async { api { publicRooms() } }
            val progress = async { api { myProgress() } }
            launch { UserHub.refreshGame() }
            launch { UserHub.refresh() }
            val h = history.await()
            val rooms = h.getOrDefault(_ui.value.history)
            val byCode = rooms.associateBy { it.roomCode }
            val cont = progress.await().getOrDefault(emptyList())
                .filter { it.roomCode != null && it.positionSec >= 30 && (it.durationSec <= 0 || it.positionSec < it.durationSec - 90) }
                .sortedByDescending { it.updatedAt }
                .take(10)
                .map { ContinueItem(it, byCode[it.roomCode]) }
            _ui.update {
                it.copy(
                    loading = false,
                    refreshing = false,
                    history = rooms,
                    live = live.await().getOrDefault(it.live),
                    continueWatching = cont,
                    error = h.exceptionOrNull()?.message,
                )
            }
        }
    }

    suspend fun join(code: String): Result<Room> = api { joinRoom(obj("room_code" to code.trim().uppercase())) }

    suspend fun create(title: String, isPublic: Boolean, anime: AnimeChoice?): Result<Room> {
        val animeJson = anime?.let {
            val full = AppJson.encodeToJsonElement(AnimeChoice.serializer(), it) as kotlinx.serialization.json.JsonObject
            kotlinx.serialization.json.JsonObject(full - "skip_segments")
        } ?: JsonNull
        return api { createRoom(obj("title" to title, "is_public" to isPublic, "anime" to animeJson)) }
    }

    fun rename(room: Room, title: String) = viewModelScope.launch {
        api { updateRoom(room.roomCode, obj("title" to title)) }.onSuccess { updated ->
            _ui.update { s -> s.copy(history = s.history.map { if (it.roomCode == room.roomCode) it.copy(title = updated.title) else it }) }
        }.onFailure { e -> _ui.update { it.copy(error = e.message) } }
    }

    fun delete(room: Room) = viewModelScope.launch {
        api { deleteRoom(room.roomCode) }.onSuccess {
            _ui.update { s -> s.copy(history = s.history.filterNot { it.roomCode == room.roomCode }) }
        }.onFailure { e -> _ui.update { it.copy(error = e.message) } }
    }

    fun clearError() = _ui.update { it.copy(error = null) }
}
