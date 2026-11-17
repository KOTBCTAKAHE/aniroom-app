package space.aniroom.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

enum class ThemeMode { SYSTEM, LIGHT, SEPIA, DARK }

data class AppPrefs(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val reactions: Boolean = true,
    val chatToasts: Boolean = true,
    val autoSkipOpening: Boolean = false,
    val haptics: Boolean = true,
    val autoPip: Boolean = true,
    val qualityMode: String = "auto",
    val dataSaver: Boolean = false,
)

/** Token, cached user and device prefs. SharedPreferences keeps startup instant. */
object Session {
    private lateinit var prefs: SharedPreferences

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _appPrefs = MutableStateFlow(AppPrefs())
    val appPrefs: StateFlow<AppPrefs> = _appPrefs.asStateFlow()

    /** Set when the server ended the session, shown on the login screen. */
    val endedReason = MutableStateFlow<String?>(null)

    fun init(context: Context) {
        prefs = context.getSharedPreferences("aniroom", Context.MODE_PRIVATE)
        _token.value = prefs.getString("token", null)
        _user.value = prefs.getString("user", null)?.let { runCatching { AppJson.decodeFromString<User>(it) }.getOrNull() }
        _appPrefs.value = AppPrefs(
            theme = runCatching { ThemeMode.valueOf(prefs.getString("theme", "SYSTEM")!!) }.getOrDefault(ThemeMode.SYSTEM),
            reactions = prefs.getBoolean("reactions", true),
            chatToasts = prefs.getBoolean("chatToasts", true),
            autoSkipOpening = prefs.getBoolean("autoSkipOpening", false),
            haptics = prefs.getBoolean("haptics", true),
            autoPip = prefs.getBoolean("autoPip", true),
            qualityMode = prefs.getString("qualityMode", "auto") ?: "auto",
            dataSaver = prefs.getBoolean("dataSaver", false),
        )
    }

    fun signIn(token: String, user: User) {
        prefs.edit().putString("token", token).apply()
        _token.value = token
        setUser(user)
        endedReason.value = null
    }

    fun setUser(user: User) {
        _user.value = user
        prefs.edit().putString("user", AppJson.encodeToString(user)).apply()
    }

    fun signOut(reason: String? = null) {
        prefs.edit().remove("token").remove("user").apply()
        _token.value = null
        _user.value = null
        if (reason != null) endedReason.value = reason
    }

    fun updatePrefs(transform: (AppPrefs) -> AppPrefs) {
        _appPrefs.update(transform)
        val p = _appPrefs.value
        prefs.edit()
            .putString("theme", p.theme.name)
            .putBoolean("reactions", p.reactions)
            .putBoolean("chatToasts", p.chatToasts)
            .putBoolean("autoSkipOpening", p.autoSkipOpening)
            .putBoolean("haptics", p.haptics)
            .putBoolean("autoPip", p.autoPip)
            .putString("qualityMode", p.qualityMode)
            .putBoolean("dataSaver", p.dataSaver)
            .apply()
    }

    fun watchedEpisodes(shikimoriId: String): Set<Int> {
        val userId = _user.value?.id ?: 0
        return prefs.getString("watched:$userId:$shikimoriId", "")!!
            .split(',').mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun markWatched(shikimoriId: String, episode: Int): Set<Int> {
        val userId = _user.value?.id ?: 0
        val next = watchedEpisodes(shikimoriId) + episode
        prefs.edit().putString("watched:$userId:$shikimoriId", next.sorted().joinToString(",")).apply()
        return next
    }

    var lastJoinCode: String?
        get() = prefs.getString("lastJoinCode", null)
        set(value) { prefs.edit().putString("lastJoinCode", value).apply() }
}
