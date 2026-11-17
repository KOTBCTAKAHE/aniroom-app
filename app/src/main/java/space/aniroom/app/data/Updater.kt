package space.aniroom.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import okhttp3.Request
import space.aniroom.app.AniRoomApp
import space.aniroom.app.BuildConfig
import java.io.File

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val version: Int, val notes: String, val url: String, val sizeBytes: Long) : UpdateState
    data class Downloading(val version: Int, val progress: Float) : UpdateState
    data class Ready(val version: Int, val file: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/** In-app updates from the GitHub releases that CI publishes. */
object Updater {
    private const val API = "https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest"

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val currentVersion: Int get() = BuildConfig.VERSION_CODE

    /** Checks at most every 3 hours unless [force]. */
    suspend fun check(force: Boolean = false) {
        if (_state.value is UpdateState.Downloading) return
        if (!force && System.currentTimeMillis() - Session.lastUpdateCheck < 3 * 3600_000L) return
        _state.value = UpdateState.Checking
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(API).header("Accept", "application/vnd.github+json").build()
                AniRoomApp.http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("GitHub ответил ${response.code}")
                    AppJson.decodeFromString<GithubRelease>(response.body!!.string())
                }
            }
        }
        Session.lastUpdateCheck = System.currentTimeMillis()
        result.onSuccess { release ->
            val version = release.tagName.substringAfterLast('-').toIntOrNull() ?: 0
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk") }
            _state.value = if (version > currentVersion && apk != null && (force || Session.skippedUpdate != version)) {
                UpdateState.Available(version, release.body.orEmpty().trim(), apk.browserDownloadUrl, apk.size)
            } else UpdateState.UpToDate
        }.onFailure { _state.value = if (force) UpdateState.Failed(it.message ?: "Не удалось проверить") else UpdateState.Idle }
    }

    fun skip(version: Int) {
        Session.skippedUpdate = version
        _state.value = UpdateState.Idle
    }

    fun dismiss() { if (_state.value !is UpdateState.Downloading) _state.value = UpdateState.Idle }

    suspend fun download(context: Context, available: UpdateState.Available) {
        _state.value = UpdateState.Downloading(available.version, 0f)
        val dir = File(context.cacheDir, "updates").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val file = File(dir, "AniRoom-${available.version}.apk")
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(available.url).build()
                AniRoomApp.http.newBuilder().cache(null).build().newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("Ошибка загрузки ${response.code}")
                    val body = response.body!!
                    val total = body.contentLength().takeIf { it > 0 } ?: available.sizeBytes
                    body.byteStream().use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var read: Int
                            var done = 0L
                            var lastEmit = 0L
                            while (input.read(buffer).also { read = it } >= 0) {
                                output.write(buffer, 0, read)
                                done += read
                                if (done - lastEmit > 128 * 1024) {
                                    lastEmit = done
                                    _state.value = UpdateState.Downloading(available.version, if (total > 0) done.toFloat() / total else 0f)
                                }
                            }
                        }
                    }
                }
            }
        }
        ok.onSuccess {
            _state.value = UpdateState.Ready(available.version, file)
            install(context, file)
        }.onFailure { _state.value = UpdateState.Failed(it.message ?: "Не удалось скачать") }
    }

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun install(context: Context, file: File) {
        if (!canInstall(context)) {
            openInstallSettings(context)
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }.onFailure { _state.value = UpdateState.Failed("Не удалось открыть установщик") }
    }
}
