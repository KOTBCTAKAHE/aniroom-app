package space.aniroom.app.util

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Rational
import kotlinx.coroutines.flow.MutableStateFlow

/** Picture-in-picture bridge between the room screen and the activity. */
object PipState {
    val inPip = MutableStateFlow(false)

    @Volatile
    var eligible: Boolean = false
        private set

    fun update(activity: Activity?, eligible: Boolean) {
        this.eligible = eligible
        if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        runCatching { activity.setPictureInPictureParams(params(eligible)) }
    }

    fun params(autoEnter: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter).setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
