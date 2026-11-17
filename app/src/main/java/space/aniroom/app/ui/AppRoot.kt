package space.aniroom.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import space.aniroom.app.data.AppNotification
import space.aniroom.app.data.Session
import space.aniroom.app.data.UserHub
import space.aniroom.app.data.describe
import space.aniroom.app.data.roomCode
import space.aniroom.app.ui.components.MangaPanel
import space.aniroom.app.ui.components.Mascot
import space.aniroom.app.ui.components.MascotMood
import space.aniroom.app.ui.screens.AuthScreen
import space.aniroom.app.ui.screens.FriendsTab
import space.aniroom.app.ui.screens.HomeTab
import space.aniroom.app.ui.screens.NotificationsTab
import space.aniroom.app.ui.screens.ProfileScreen
import space.aniroom.app.ui.screens.RoomScreen
import space.aniroom.app.ui.screens.SettingsScreen
import space.aniroom.app.ui.theme.Manga

/** Room code from a deep link / notification, consumed once signed in. */
object PendingNav {
    val room = MutableStateFlow<String?>(null)
}

@Composable
fun AppRoot() {
    val token by Session.token.collectAsState()
    if (token == null) {
        AuthScreen()
        return
    }
    val nav = rememberNavController()
    val pending by PendingNav.room.collectAsState()
    LaunchedEffect(pending) {
        val code = pending ?: return@LaunchedEffect
        PendingNav.room.value = null
        nav.navigate("room/$code") { launchSingleTop = true }
    }
    val m = Manga.colors
    Box(Modifier.fillMaxSize().background(m.paper)) {
        NavHost(
            navController = nav,
            startDestination = "main",
            enterTransition = { slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(320)) },
            exitTransition = { slideOutHorizontally(tween(320)) { -it / 6 } + fadeOut(tween(220)) },
            popEnterTransition = { slideInHorizontally(tween(320)) { -it / 6 } + fadeIn(tween(320)) },
            popExitTransition = { slideOutHorizontally(tween(320)) { it / 3 } + fadeOut(tween(220)) },
        ) {
            composable("main") {
                MainScreen(
                    openRoom = { nav.navigate("room/$it") { launchSingleTop = true } },
                    openUser = { nav.navigate("user/$it") },
                    openSettings = { nav.navigate("settings") },
                )
            }
            composable(
                "room/{code}",
                arguments = listOf(navArgument("code") { type = NavType.StringType }),
                enterTransition = { slideInVertically(tween(380)) { it / 2 } + fadeIn(tween(300)) },
                popExitTransition = { slideOutVertically(tween(320)) { it / 2 } + fadeOut(tween(260)) },
            ) { entry ->
                val code = entry.arguments?.getString("code").orEmpty().uppercase()
                RoomScreen(code, onBack = { nav.popBackStack() }, openUser = { nav.navigate("user/$it") })
            }
            composable("user/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                ProfileScreen(entry.arguments?.getInt("id") ?: 0, isTab = false, onBack = { nav.popBackStack() }, openSettings = { nav.navigate("settings") })
            }
            composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
        }
        NotificationToastHost(openRoom = { nav.navigate("room/$it") { launchSingleTop = true } })
    }
}

private enum class Tab(val label: String) { HOME("Главная"), FRIENDS("Друзья"), ALERTS("Новое"), PROFILE("Профиль") }

@Composable
private fun MainScreen(openRoom: (String) -> Unit, openUser: (Int) -> Unit, openSettings: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    val user by Session.user.collectAsState()
    val unread = UserHub.items.collectAsState().value.count { !it.isRead }
    val context = LocalContext.current
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            delay(1500)
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    BackHandler(tab != Tab.HOME) { tab = Tab.HOME }
    val padding = PaddingValues(bottom = 96.dp)
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            tab,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(tween(280)) { if (forward) it / 5 else -it / 5 } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(280)) { if (forward) -it / 5 else it / 5 } + fadeOut(tween(200)))
            },
            label = "tabs",
            modifier = Modifier.fillMaxSize(),
        ) { current ->
            when (current) {
                Tab.HOME -> HomeTab(openRoom, openSettings, openProfile = { tab = Tab.PROFILE }, contentPadding = padding)
                Tab.FRIENDS -> FriendsTab(openUser, openRoom, padding)
                Tab.ALERTS -> NotificationsTab(openRoom, openProfile = { tab = Tab.PROFILE }, openFriends = { tab = Tab.FRIENDS }, contentPadding = padding)
                Tab.PROFILE -> ProfileScreen(user?.id ?: 0, isTab = true, onBack = { tab = Tab.HOME }, openSettings = openSettings, contentPadding = padding)
            }
        }
        BottomBar(tab, unread, Modifier.align(Alignment.BottomCenter)) { tab = it }
    }
}

@Composable
private fun BottomBar(current: Tab, unread: Int, modifier: Modifier, onSelect: (Tab) -> Unit) {
    val m = Manga.colors
    val haptic = space.aniroom.app.util.rememberHaptic()
    MangaPanel(
        modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth().height(64.dp),
        color = m.paper2, radius = 22.dp, shadow = 4.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
            val w = maxWidth / Tab.entries.size
            val x by animateDpAsState(w * current.ordinal, spring(dampingRatio = 0.65f, stiffness = 420f), label = "indicator")
            Box(Modifier.offset(x = x).width(w).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(m.ink))
            Row(Modifier.fillMaxSize()) {
                Tab.entries.forEach { t ->
                    val selected = t == current
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { haptic(false); onSelect(t) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    ) {
                        Box {
                            Icon(
                                when (t) {
                                    Tab.HOME -> Icons.Rounded.Home
                                    Tab.FRIENDS -> Icons.Rounded.People
                                    Tab.ALERTS -> Icons.Rounded.Notifications
                                    Tab.PROFILE -> Icons.Rounded.Person
                                },
                                t.label, tint = if (selected) m.paper else m.ink, modifier = Modifier.size(24.dp),
                            )
                            if (t == Tab.ALERTS && unread > 0) Box(
                                Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp).clip(CircleShape).background(m.red).padding(horizontal = 4.dp),
                            ) { Text(if (unread > 9) "9+" else "$unread", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                        }
                        AnimatedVisibility(selected, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                            Text(t.label, color = m.paper, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationToastHost(openRoom: (String) -> Unit) {
    val m = Manga.colors
    var toast by remember { mutableStateOf<AppNotification?>(null) }
    LaunchedEffect(Unit) {
        UserHub.toasts.collect {
            toast = it
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(5000)
            toast = null
        }
    }
    AnimatedVisibility(
        toast != null,
        enter = slideInVertically(spring(dampingRatio = 0.7f)) { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = Modifier.statusBarsPadding().padding(12.dp),
    ) {
        val item = toast
        if (item != null) {
            val copy = item.describe()
            MangaPanel(Modifier.fillMaxWidth(), color = m.paper2, shadow = 4.dp, onClick = {
                UserHub.markRead(item)
                if (copy.action == "room") item.roomCode()?.let(openRoom)
                toast = null
            }) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Mascot(MascotMood.HAPPY, size = 44.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(copy.title, color = m.red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(copy.body, color = m.ink, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
