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
    val chatFont: String = "md",
    val preferredDub: String = "",
    val materialYou: Boolean = false,
    val gestures: Boolean = true,
    val clipboardHints: Boolean = true,
    val achievementSound: Boolean = true,
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
            chatFont = prefs.getString("chatFont", "md") ?: "md",
            preferredDub = prefs.getString("preferredDub", "") ?: "",
            materialYou = prefs.getBoolean("materialYou", false),
            gestures = prefs.getBoolean("gestures", true),
            clipboardHints = prefs.getBoolean("clipboardHints", true),
            achievementSound = prefs.getBoolean("achievementSound", true),
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
            .putString("chatFont", p.chatFont)
            .putString("preferredDub", p.preferredDub)
            .putBoolean("materialYou", p.materialYou)
            .putBoolean("gestures", p.gestures)
            .putBoolean("clipboardHints", p.clipboardHints)
            .putBoolean("achievementSound", p.achievementSound)
            .apply()
    }

    /** Applies the account's synced client prefs (shared with the website). */
    fun hydrateFromServer(user: User) {
        val cp = user.clientPrefs ?: return
        fun str(key: String) = (cp[key] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it.isString }?.content
        fun bool(key: String) = (cp[key] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull()
        _appPrefs.update { p ->
            p.copy(
                theme = when (str("theme")) { "light" -> ThemeMode.LIGHT; "sepia" -> ThemeMode.SEPIA; "dark" -> ThemeMode.DARK; "system" -> ThemeMode.SYSTEM; else -> p.theme },
                reactions = bool("reactions") ?: p.reactions,
                chatToasts = bool("chatToasts") ?: p.chatToasts,
                autoSkipOpening = bool("autoSkipOpening") ?: p.autoSkipOpening,
                chatFont = str("chatFont") ?: p.chatFont,
                preferredDub = str("preferredDub") ?: p.preferredDub,
                achievementSound = bool("achievementSound") ?: p.achievementSound,
            )
        }
        updatePrefs { it }
    }

    fun clientPrefsJson(p: AppPrefs = _appPrefs.value) = obj(
        "theme" to p.theme.name.lowercase(),
        "reactions" to p.reactions,
        "chatToasts" to p.chatToasts,
        "autoSkipOpening" to p.autoSkipOpening,
        "chatFont" to p.chatFont,
        "preferredDub" to p.preferredDub,
        "dataSaver" to p.dataSaver,
        "achievementSound" to p.achievementSound,
    )

    var lastUpdateCheck: Long
        get() = prefs.getLong("lastUpdateCheck", 0)
        set(value) { prefs.edit().putLong("lastUpdateCheck", value).apply() }

    var skippedUpdate: Int
        get() = prefs.getInt("skippedUpdate", 0)
        set(value) { prefs.edit().putInt("skippedUpdate", value).apply() }

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
