package space.aniroom.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** The user's own sticker pack (same API and limits as the site's StickerManager). */
object StickerStore {
    private val _stickers = MutableStateFlow<List<Sticker>?>(null)
    val stickers: StateFlow<List<Sticker>?> = _stickers.asStateFlow()

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

    suspend fun load(force: Boolean = false) {
        if (!force && _stickers.value != null) return
        api { stickers() }.onSuccess { _stickers.value = it }.onFailure { if (_stickers.value == null) _stickers.value = emptyList() }
    }

    /** Returns an error message or null on success. */
    suspend fun upload(context: Context, uri: Uri, name: String): String? {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/png"
        val isVideo = mime.startsWith("video/")
        if (!mime.startsWith("image/") && !isVideo) return "Нужна картинка, GIF или короткое видео"
        val size = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c -> if (c.moveToFirst()) c.getLong(0) else 0L }
        }.getOrNull() ?: 0L
        val limitMb = if (isVideo) 12 else 8
        if (size > limitMb * 1024L * 1024L) return "Файл должен быть до $limitMb МБ"
        _uploading.value = true
        try {
            val bytes = withContext(Dispatchers.IO) { resolver.openInputStream(uri)?.use { it.readBytes() } } ?: return "Файл не читается"
            if (bytes.size > limitMb * 1024 * 1024) return "Файл должен быть до $limitMb МБ"
            val ext = mime.substringAfter('/').substringBefore(';').ifBlank { "png" }
            val part = MultipartBody.Part.createFormData("file", "sticker.$ext", bytes.toRequestBody(mime.toMediaTypeOrNull()))
            val namePart = name.trim().take(40).toRequestBody("text/plain".toMediaTypeOrNull())
            var error: String? = null
            api { uploadSticker(part, namePart) }
                .onSuccess { sticker -> _stickers.update { listOf(sticker) + it.orEmpty() } }
                .onFailure { error = it.message }
            return error
        } finally {
            _uploading.value = false
        }
    }

    suspend fun rename(sticker: Sticker, name: String): String? {
        var error: String? = null
        api { renameSticker(sticker.id, obj("name" to name.trim().take(40))) }
            .onSuccess { updated -> _stickers.update { list -> list?.map { if (it.id == sticker.id) updated else it } } }
            .onFailure { error = it.message }
        return error
    }

    suspend fun delete(sticker: Sticker): String? {
        val before = _stickers.value
        _stickers.update { list -> list?.filterNot { it.id == sticker.id } }
        var error: String? = null
        api { deleteSticker(sticker.id) }.onFailure { error = it.message; _stickers.value = before }
        return error
    }

    fun clear() { _stickers.value = null }
}
