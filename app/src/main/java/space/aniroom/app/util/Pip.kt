package space.aniroom.app.util

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import space.aniroom.app.R

/** Picture-in-picture bridge between the room screen and the activity. */
object PipState {
    const val BROADCAST = "space.aniroom.app.PIP_ACTION"
    const val EXTRA = "action"
    const val ACTION_TOGGLE = 1
    const val ACTION_SYNC = 2
    const val ACTION_REACT = 3

    val inPip = MutableStateFlow(false)

    /** The user closed the PiP window (activity stays stopped). */
    val dismissed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Set by the room screen; called when a PiP window button is pressed. */
    @Volatile
    var onAction: ((Int) -> Unit)? = null

    @Volatile
    var eligible: Boolean = false
        private set
    private var playing = false
    private var owner = false

    fun supported(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    fun update(activity: Activity?, eligible: Boolean, playing: Boolean = this.playing, owner: Boolean = this.owner) {
        this.eligible = eligible
        this.playing = playing
        this.owner = owner
        if (activity == null) return
        runCatching { activity.setPictureInPictureParams(params(activity, eligible)) }
    }

    fun enter(activity: Activity?, playing: Boolean, owner: Boolean) {
        activity ?: return
        this.playing = playing
        this.owner = owner
        runCatching { activity.enterPictureInPictureMode(params(activity, eligible)) }
    }

    fun params(context: Context, autoEnter: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setActions(actions(context))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter).setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    private fun actions(context: Context): List<RemoteAction> {
        fun action(code: Int, icon: Int, title: String): RemoteAction {
            val intent = Intent(BROADCAST).setPackage(context.packageName).putExtra(EXTRA, code)
            val pending = PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            return RemoteAction(Icon.createWithResource(context, icon), title, title, pending)
        }
        val list = mutableListOf<RemoteAction>()
        list += action(ACTION_REACT, R.drawable.ic_pip_fire, "Реакция")
        list += if (owner) {
            if (playing) action(ACTION_TOGGLE, R.drawable.ic_pip_pause, "Пауза") else action(ACTION_TOGGLE, R.drawable.ic_pip_play, "Смотреть")
        } else action(ACTION_SYNC, R.drawable.ic_pip_sync, "Синхронизировать")
        return list
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
