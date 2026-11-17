package space.aniroom.app.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import space.aniroom.app.data.Session

@Composable
fun rememberHaptic(): (Boolean) -> Unit {
    val haptic = LocalHapticFeedback.current
    return { strong ->
        if (Session.appPrefs.value.haptics) {
            haptic.performHapticFeedback(if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
        }
    }
}
