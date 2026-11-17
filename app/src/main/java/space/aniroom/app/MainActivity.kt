package space.aniroom.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import space.aniroom.app.data.Session
import space.aniroom.app.ui.AppRoot
import space.aniroom.app.ui.PendingNav
import space.aniroom.app.ui.theme.AniRoomTheme
import space.aniroom.app.ui.theme.resolveManga
import space.aniroom.app.util.PipState
import space.aniroom.app.util.RoomLinks

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setOnExitAnimationListener { provider ->
            val icon: View = runCatching { provider.iconView }.getOrNull() ?: provider.view
            val scaleX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 1.6f)
            val scaleY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 1.6f)
            val rotate = ObjectAnimator.ofFloat(icon, View.ROTATION, 0f, -12f)
            val fade = ObjectAnimator.ofFloat(provider.view, View.ALPHA, 1f, 0f)
            AnimatorSet().apply {
                playTogether(scaleX, scaleY, rotate, fade)
                duration = 380
                interpolator = AnticipateInterpolator()
                doOnEnd { provider.remove() }
                start()
            }
        }
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val prefs by Session.appPrefs.collectAsState()
            AniRoomTheme(prefs.theme, prefs.materialYou) {
                val dark = resolveManga(prefs.theme).isDark
                LaunchedEffect(dark) {
                    enableEdgeToEdge(
                        statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                        navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    )
                }
                AppRoot()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            RoomLinks.extract(text, allowBare = true)?.let { PendingNav.room.value = it }
            return
        }
        val data = intent.data ?: return
        if (data.scheme == "aniroom" && data.host == "action") {
            PendingNav.action.value = data.pathSegments.firstOrNull()
            return
        }
        val code = when (data.scheme) {
            "aniroom" -> data.pathSegments.firstOrNull() ?: data.lastPathSegment
            else -> data.pathSegments.let { segments ->
                val index = segments.indexOf("rooms")
                if (index >= 0) segments.getOrNull(index + 1) else null
            }
        }?.trim()?.uppercase()
        if (!code.isNullOrBlank()) PendingNav.room.value = code
    }

    private val pipReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            val action = intent?.getIntExtra(PipState.EXTRA, 0) ?: return
            PipState.onAction?.invoke(action)
        }
    }

    override fun onStart() {
        super.onStart()
        androidx.core.content.ContextCompat.registerReceiver(
            this, pipReceiver, android.content.IntentFilter(PipState.BROADCAST), androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(pipReceiver) }
        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && PipState.eligible && Session.appPrefs.value.autoPip) {
            runCatching { enterPictureInPictureMode(PipState.params(this, false)) }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PipState.inPip.value = isInPictureInPictureMode
        // Leaving PiP while not visible means the user closed the PiP window.
        if (!isInPictureInPictureMode && !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
            PipState.dismissed.tryEmit(Unit)
        }
    }
}
