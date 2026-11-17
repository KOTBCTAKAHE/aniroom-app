package space.aniroom.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import space.aniroom.app.R

/** Same icon per achievement as the site (BadgeGrid.tsx / VideoPlayer.tsx, Lucide). */
private val BADGE_ICONS: Map<String, Int> = mapOf(
    "first_session" to R.drawable.lc_star,
    "marathoner" to R.drawable.lc_trophy,
    "night_owl" to R.drawable.lc_moon_star,
    "hundred_episodes" to R.drawable.lc_medal,
    "company" to R.drawable.lc_users,
    "inseparable_duo" to R.drawable.lc_heart_handshake,
    "night_duo" to R.drawable.lc_moon_star,
    "anime_squad" to R.drawable.lc_users,
    "site_owner" to R.drawable.lc_crown,
    "critic" to R.drawable.lc_star,
    "loud_hall" to R.drawable.lc_megaphone,
    "chatterbox" to R.drawable.lc_message_circle,
    "quest_10" to R.drawable.lc_clipboard_check,
    "collector" to R.drawable.lc_book_open,
    "librarian" to R.drawable.lc_library,
    "streak_7" to R.drawable.lc_flame,
    "streak_30" to R.drawable.lc_calendar_range,
    "level_10" to R.drawable.lc_hash,
    "level_25" to R.drawable.lc_gem,
    "all_gasped" to R.drawable.lc_zap,
    "unanimous" to R.drawable.lc_vote,
    "season_together" to R.drawable.lc_heart_handshake,
    "host_party" to R.drawable.lc_party_popper,
    "dawn" to R.drawable.lc_sunrise,
    "no_sleep" to R.drawable.lc_coffee,
    "rewatch" to R.drawable.lc_rotate_ccw,
    "new_year" to R.drawable.lc_snowflake,
    "halloween" to R.drawable.lc_ghost,
    "valentine" to R.drawable.lc_heart,
)

@DrawableRes
fun badgeIconRes(key: String?, icon: String? = null): Int =
    BADGE_ICONS[icon.orEmpty()] ?: BADGE_ICONS[key.orEmpty()] ?: R.drawable.lc_award

fun isShardBadge(key: String?, icon: String? = null): Boolean =
    icon.orEmpty().startsWith("rezero-shard") || key == "rezero_first_loop" || key == "rezero_all_loops"

/** Achievement glyph: Re:Zero badges get the crystal shard, everything else its Lucide icon. */
@Composable
fun BadgeIcon(key: String?, icon: String?, size: Dp, tint: Color, locked: Boolean = false, hidden: Boolean = false) {
    if (!hidden && isShardBadge(key, icon)) {
        val gold = icon == "rezero-shard-gold" || key == "rezero_all_loops"
        val t = rememberInfiniteTransition(label = "shard")
        val shine by t.animateFloat(-1f, 2f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "s")
        Canvas(Modifier.size(size)) { drawShard(gold, if (locked) 0 else 4, 4, if (locked) -2f else shine) }
        return
    }
    Icon(
        painterResource(if (hidden) R.drawable.lc_circle_help else badgeIconRes(key, icon)),
        null, tint = tint, modifier = Modifier.size(size),
    )
}
