package space.aniroom.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import space.aniroom.app.R
import space.aniroom.app.data.Session

/**
 * Achievement sounds — the same recorded cues as the website (public/sounds/achievements),
 * preloaded into a SoundPool so they land exactly with the stamp animation.
 */
object Sounds {
    private var pool: SoundPool? = null
    private val ids = mutableMapOf<String, Int>()
    private val loaded = mutableSetOf<Int>()
    private lateinit var app: Context

    private val FILES = mapOf(
        "first_session" to R.raw.ach_first_session,
        "marathoner" to R.raw.ach_marathoner,
        "night_owl" to R.raw.ach_night_owl,
        "hundred_episodes" to R.raw.ach_hundred_episodes,
        "inseparable_duo" to R.raw.ach_inseparable_duo,
        "night_duo" to R.raw.ach_night_duo,
        "company" to R.raw.ach_company,
        "anime_squad" to R.raw.ach_anime_squad,
        "rezero_first_loop" to R.raw.ach_rezero_first_loop,
        "rezero_all_loops" to R.raw.ach_rezero_all_loops,
        "rezero_season" to R.raw.ach_rezero_season,
        "common" to R.raw.ach_common,
        "rare" to R.raw.ach_rare,
    )

    /** Keys → file, mirrors SOUND_FILES in achievementSound.ts. */
    private val ALIASES = mapOf("site_owner" to "anime_squad", "legendary" to "hundred_episodes")

    fun init(context: Context) {
        app = context.applicationContext
        val p = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build()
        p.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) loaded += sampleId }
        FILES.forEach { (key, res) -> ids[key] = p.load(app, res, 1) }
        runCatching { chatId = p.load(chatTone(app).absolutePath, 1) }
        pool = p
    }

    private var chatId = 0
    private var lastChat = 0L

    /** The site's «тук-тук»: two short triangle notes, 660 Hz then 880 Hz (prefs.ts → playChatSound). */
    private fun chatTone(context: Context): java.io.File {
        val file = java.io.File(context.cacheDir, "chat_tone_v1.wav")
        if (file.exists()) return file
        val rate = 44100
        val total = (rate * 0.26).toInt()
        val pcm = ShortArray(total)
        listOf(660.0 to 0.0, 880.0 to 0.09).forEach { (freq, start) ->
            val s0 = (start * rate).toInt()
            val len = (0.16 * rate).toInt()
            for (i in 0 until len) {
                val idx = s0 + i
                if (idx >= total) break
                val t = i.toDouble() / rate
                // exponential attack to peak in 15 ms, exponential decay to silence by 140 ms
                val env = if (t < 0.015) Math.pow(0.0001 / 0.06, 1 - t / 0.015) else Math.pow(0.0001 / 0.06, ((t - 0.015) / 0.125).coerceAtMost(1.0))
                val phase = (t * freq) % 1.0
                val tri = 4 * Math.abs(phase - 0.5) - 1
                val v = pcm[idx] + (tri * env * 0.42 * Short.MAX_VALUE).toInt()
                pcm[idx] = v.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        val data = java.nio.ByteBuffer.allocate(44 + total * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        data.put("RIFF".toByteArray()).putInt(36 + total * 2).put("WAVE".toByteArray())
        data.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        data.put("data".toByteArray()).putInt(total * 2)
        pcm.forEach { data.putShort(it) }
        file.writeBytes(data.array())
        return file
    }

    fun chat() {
        if (!Session.appPrefs.value.chatSound) return
        val now = System.currentTimeMillis()
        if (now - lastChat < 350) return
        lastChat = now
        val p = pool ?: return
        if (chatId != 0 && chatId in loaded) p.play(chatId, 0.7f, 0.7f, 0, 0, 1f)
    }

    private fun resolve(rarity: String?, key: String?): String {
        val k = key.orEmpty().lowercase()
        if (k in FILES) return k
        ALIASES[k]?.let { return it }
        val raw = "${rarity.orEmpty()} $k".lowercase()
        return when {
            "rezero" in raw || "season" in raw -> "rezero_season"
            "legendary" in raw || "легендарный" in raw -> "hundred_episodes"
            "rare" in raw || "редкий" in raw -> "rare"
            else -> "common"
        }
    }

    fun play(rarity: String?, key: String? = null) {
        if (!Session.appPrefs.value.achievementSound) return
        val p = pool ?: return
        val id = ids[resolve(rarity, key)] ?: return
        if (id in loaded) p.play(id, 0.55f, 0.55f, 1, 0, 1f)
    }

    /** Haptic "stamp": stronger and longer for rarer rewards. */
    fun stamp(strength: String) {
        if (!Session.appPrefs.value.haptics) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return
        val effect = when (strength) {
            "legendary" -> VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 30, 50, 90), intArrayOf(0, 255, 0, 160, 0, 255), -1)
            "rare" -> VibrationEffect.createWaveform(longArrayOf(0, 30, 50, 60), intArrayOf(0, 200, 0, 230), -1)
            else -> VibrationEffect.createOneShot(35, 180)
        }
        runCatching { vibrator.vibrate(effect) }
    }
}
