package space.aniroom.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import space.aniroom.app.MainActivity
import space.aniroom.app.R

object RoomLinks {
    private val linkRegex = Regex("""aniroom\.space/rooms/([A-Za-z0-9]{4,12})""", RegexOption.IGNORE_CASE)
    private val appRegex = Regex("""aniroom://room/([A-Za-z0-9]{4,12})""", RegexOption.IGNORE_CASE)
    private val codeRegex = Regex("""^\s*([A-Za-z0-9]{6})\s*$""")

    /** Room code from a link, a deep link, or (when [allowBare]) a bare 6-char code. */
    fun extract(text: String?, allowBare: Boolean = false): String? {
        if (text.isNullOrBlank()) return null
        linkRegex.find(text)?.let { return it.groupValues[1].uppercase() }
        appRegex.find(text)?.let { return it.groupValues[1].uppercase() }
        if (allowBare) codeRegex.find(text)?.let { return it.groupValues[1].uppercase() }
        return null
    }

    fun qr(text: String, size: Int = 720, dark: Int = 0xFF141210.toInt(), light: Int = 0xFFFAF7F0.toInt()): Bitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
        val pixels = IntArray(size * size) { i -> if (matrix.get(i % size, i / size)) dark else light }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    /** Long-press the launcher icon → recent rooms. */
    fun pushShortcut(context: Context, code: String, title: String) {
        runCatching {
            val shortcut = ShortcutInfoCompat.Builder(context, "room-$code")
                .setShortLabel(title.take(24).ifBlank { code })
                .setLongLabel("Комната $code · ${title.take(30)}")
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_room))
                .setIntent(Intent(Intent.ACTION_VIEW, Uri.parse("aniroom://room/$code"), context, MainActivity::class.java))
                .build()
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
        }
    }
}
