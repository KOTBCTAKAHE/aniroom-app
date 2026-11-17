package space.aniroom.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import space.aniroom.app.data.Session
import space.aniroom.app.data.api
import space.aniroom.app.data.obj
import space.aniroom.app.ui.components.MangaButton
import space.aniroom.app.ui.components.MangaField
import space.aniroom.app.ui.components.MangaPanel
import space.aniroom.app.ui.components.Mascot
import space.aniroom.app.ui.components.MascotMood
import space.aniroom.app.ui.components.SpeedLines
import space.aniroom.app.ui.theme.DisplayFont
import space.aniroom.app.ui.theme.Kicker
import space.aniroom.app.ui.theme.Manga

@Composable
fun AuthScreen() {
    val m = Manga.colors
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var register by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val ended by Session.endedReason.collectAsState()

    fun submit() {
        if (busy) return
        error = null
        if (email.isBlank() || password.isBlank() || (register && username.isBlank())) {
            error = "Заполни все поля"
            return
        }
        busy = true
        focus.clearFocus()
        scope.launch {
            val result = if (register) api { register(obj("username" to username.trim(), "email" to email.trim(), "password" to password)) }
            else api { login(obj("email" to email.trim(), "password" to password)) }
            busy = false
            result.onSuccess { Session.signIn(it.accessToken, it.user) }
                .onFailure { error = it.message }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(m.paper),
    ) {
        SpeedLines()
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Mascot(mood = if (busy) MascotMood.SEARCH else if (error != null) MascotMood.SHOCK else MascotMood.HAPPY, size = 96.dp)
                Spacer(Modifier.width(8.dp))
                MangaPanel(Modifier.rotate(-3f), color = m.paper2, radius = 18.dp, shadow = 3.dp) {
                    Text(
                        if (register) "Добро пожаловать\nв клуб!" else "Смотрим аниме\nвместе?",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = m.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("ANIROOM", fontFamily = DisplayFont, fontSize = 40.sp, color = m.ink)
            Box(
                Modifier
                    .offset(y = (-4).dp)
                    .rotate(-2f)
                    .background(m.red)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text("СИНХРОННО · С ДРУЗЬЯМИ", style = Kicker, color = Color.White)
            }
            Spacer(Modifier.height(26.dp))

            AnimatedVisibility(ended != null) {
                MangaPanel(Modifier.fillMaxWidth().padding(bottom = 14.dp), color = m.yellow, shadow = 3.dp) {
                    Text(ended ?: "", Modifier.padding(12.dp), color = m.onYellow, fontWeight = FontWeight.SemiBold)
                }
            }

            ModeSwitch(register) { register = it; error = null }
            Spacer(Modifier.height(18.dp))

            MangaField(
                value = email, onValueChange = { email = it }, label = "Почта", placeholder = "you@mail.ru",
                icon = Icons.Rounded.AlternateEmail,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            AnimatedVisibility(register, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                MangaField(
                    value = username, onValueChange = { username = it.take(32) }, label = "Ник", placeholder = "Как тебя звать в комнатах",
                    icon = Icons.Rounded.Person,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            MangaField(
                value = password, onValueChange = { password = it }, label = "Пароль", placeholder = "••••••••",
                icon = Icons.Rounded.Lock,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                trailing = {
                    Icon(
                        if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Показать пароль",
                        tint = m.muted,
                        modifier = Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).clickable { showPassword = !showPassword },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            AnimatedVisibility(error != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Text(
                    error ?: "", color = m.red, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            MangaButton(
                text = if (register) "Создать аккаунт" else "Войти",
                onClick = { submit() },
                loading = busy,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "Комнаты, синхронный плеер, чат и реакции — прямо в телефоне.",
                style = MaterialTheme.typography.bodySmall, color = m.muted,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModeSwitch(register: Boolean, onChange: (Boolean) -> Unit) {
    val m = Manga.colors
    val bias by animateFloatAsState(if (register) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 500f), label = "mode")
    MangaPanel(Modifier.fillMaxWidth().height(50.dp), color = m.paper3, radius = 14.dp, shadow = 3.dp) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(4.dp)) {
            val half = maxWidth / 2
            Box(
                Modifier
                    .offset(x = half * bias)
                    .width(half)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(m.ink),
            )
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Вход" to false, "Регистрация" to true).forEach { (label, value) ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(10.dp)).clickable { onChange(value) },
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = register == value,
                            transitionSpec = { (slideInHorizontally { it / 6 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 6 } + fadeOut()) },
                            label = "modeLabel",
                        ) { selected ->
                            Text(label, color = if (selected) m.paper else m.ink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
