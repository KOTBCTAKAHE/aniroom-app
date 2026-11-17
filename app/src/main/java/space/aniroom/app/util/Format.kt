package space.aniroom.app.util

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

fun formatTime(seconds: Double): String {
    if (!seconds.isFinite() || seconds < 0) return "00:00"
    val whole = floor(seconds).toLong()
    val h = whole / 3600
    val m = (whole % 3600) / 60
    val s = whole % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
}

fun formatHours(seconds: Double): String {
    val hours = seconds / 3600.0
    return when {
        hours < 1 -> "${(seconds / 60).toInt()} мин"
        hours < 10 -> String.format(Locale("ru"), "%.1f ч", hours)
        else -> "${hours.toInt()} ч"
    }
}

fun plural(n: Int, one: String, few: String, many: String): String {
    val mod10 = abs(n) % 10
    val mod100 = abs(n) % 100
    return when {
        mod10 == 1 && mod100 != 11 -> one
        mod10 in 2..4 && (mod100 < 12 || mod100 > 14) -> few
        else -> many
    }
}

fun parseInstant(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
        ?: runCatching { Instant.parse(value) }.getOrNull()
        ?: runCatching { Instant.parse(value + "Z") }.getOrNull()
}

private val clock = DateTimeFormatter.ofPattern("HH:mm")
private val dayClock = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("ru"))

fun chatTime(value: String?): String {
    val instant = parseInstant(value) ?: return ""
    val zoned = instant.atZone(ZoneId.systemDefault())
    val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
    return if (zoned.toLocalDate() == today) clock.format(zoned) else dayClock.format(zoned)
}

fun relativeTime(value: String?): String {
    val instant = parseInstant(value) ?: return ""
    val diff = (System.currentTimeMillis() - instant.toEpochMilli()) / 1000
    return when {
        diff < 60 -> "только что"
        diff < 3600 -> "${diff / 60} мин назад"
        diff < 86400 -> "${diff / 3600} ч назад"
        diff < 86400 * 7 -> "${diff / 86400} дн назад"
        else -> dayClock.format(instant.atZone(ZoneId.systemDefault()))
    }
}
