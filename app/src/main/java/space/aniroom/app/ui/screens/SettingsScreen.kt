package space.aniroom.app.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import space.aniroom.app.BuildConfig
import space.aniroom.app.data.AnimeListEntry
import space.aniroom.app.data.BlockedUser
import space.aniroom.app.data.CatalogItem
import space.aniroom.app.data.FavoriteTitle
import space.aniroom.app.data.Session
import space.aniroom.app.data.ThemeMode
import space.aniroom.app.data.UpdateState
import space.aniroom.app.data.Updater
import space.aniroom.app.data.User
import space.aniroom.app.data.UserHub
import space.aniroom.app.data.api
import space.aniroom.app.data.obj
import space.aniroom.app.ui.components.ButtonKind
import space.aniroom.app.ui.components.CoverSwatch
import space.aniroom.app.ui.components.FrameSample
import space.aniroom.app.ui.components.MangaButton
import space.aniroom.app.ui.components.MangaField
import space.aniroom.app.ui.components.MangaPanel
import space.aniroom.app.ui.components.NICK_COLORS
import space.aniroom.app.ui.components.NickText
import space.aniroom.app.ui.components.PROFILE_COVERS
import space.aniroom.app.ui.components.Poster
import space.aniroom.app.ui.components.ProgressLine
import space.aniroom.app.ui.components.RoundIcon
import space.aniroom.app.ui.components.SelectChip
import space.aniroom.app.ui.components.SkeletonBox
import space.aniroom.app.ui.components.Tag
import space.aniroom.app.ui.components.UserAvatar
import space.aniroom.app.ui.components.bubbleExtras
import space.aniroom.app.ui.components.bubbleSkin
import space.aniroom.app.ui.components.siteNickColor
import space.aniroom.app.ui.theme.Kicker
import space.aniroom.app.ui.theme.Manga
import space.aniroom.app.util.parseInstant
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Section(val title: String, val hint: String, val icon: ImageVector) {
    PROFILE("Профиль", "Аватар, ник, цвет, о себе, обложка", Icons.Rounded.Person),
    WARDROBE("Гардероб", "Рамки, облачка, эффекты ника, титулы", Icons.Rounded.Checkroom),
    LOOK("Внешний вид", "Тема, Material You, размер чата", Icons.Rounded.Palette),
    PLAYER("Плеер", "Качество, жесты, картинка в картинке", Icons.Rounded.Movie),
    PRIVACY("Приватность", "Кто что видит, чёрный список", Icons.Rounded.Shield),
    NOTIFY("Уведомления", "Тихие часы, новые серии", Icons.Rounded.NotificationsActive),
    ACCOUNT("Аккаунт", "Пароль, сеансы, удаление", Icons.Rounded.Key),
    UPDATES("Обновления", "Версия ${BuildConfig.VERSION_NAME}", Icons.Rounded.SystemUpdate),
}

private suspend fun savePrefs(payload: JsonObject): String? {
    var error: String? = null
    api { updatePreferences(payload) }.onSuccess { Session.setUser(it) }.onFailure { error = it.message }
    return error
}

@Composable
fun SettingsScreen(onBack: () -> Unit, initial: String? = null) {
    val m = Manga.colors
    var section by rememberSaveable { mutableStateOf<Section?>(initial?.let { runCatching { Section.valueOf(it) }.getOrNull() }) }
    BackHandler(section != null && initial == null) { section = null }
    AnimatedContent(
        section,
        transitionSpec = {
            if (targetState != null) (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 6 } + fadeOut())
            else (slideInHorizontally { -it / 6 } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
        },
        label = "settings",
        modifier = Modifier.fillMaxSize().background(m.paper),
    ) { current ->
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", { if (current != null && initial == null) section = null else onBack() })
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(if (current == null) "ПРИЛОЖЕНИЕ" else "НАСТРОЙКИ", style = Kicker, color = m.red)
                    Text(current?.title ?: "Настройки", style = MaterialTheme.typography.headlineMedium, color = m.ink)
                }
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (current) {
                    null -> SectionList { section = it }
                    Section.PROFILE -> ProfileSection()
                    Section.WARDROBE -> WardrobeSection()
                    Section.LOOK -> LookSection()
                    Section.PLAYER -> PlayerSection()
                    Section.PRIVACY -> PrivacySection()
                    Section.NOTIFY -> NotifySection()
                    Section.ACCOUNT -> AccountSection()
                    Section.UPDATES -> UpdatesSection()
                }
            }
        }
    }
}

@Composable
private fun SectionList(open: (Section) -> Unit) {
    val m = Manga.colors
    val user by Session.user.collectAsState()
    user?.let { u ->
        MangaPanel(Modifier.fillMaxWidth(), color = m.red, shadow = 4.dp, onClick = { open(Section.PROFILE) }) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(u.avatar, u.username, size = 56.dp, color = u.nicknameColor, frame = u.game?.frame)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(u.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(u.email, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Color.White)
            }
        }
    }
    val update by Updater.state.collectAsState()
    Section.entries.forEachIndexed { i, s ->
        val anim by animateFloatAsState(1f, spring(dampingRatio = 0.6f), label = "s$i")
        MangaPanel(Modifier.fillMaxWidth().scale(anim), shadow = 3.dp, onClick = { open(s) }) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(m.paper3).border(2.dp, m.ink, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    Icon(s.icon, null, tint = m.ink)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title, color = m.ink, fontWeight = FontWeight.Bold)
                    Text(s.hint, color = m.muted, fontSize = 12.sp)
                }
                if (s == Section.UPDATES && update is UpdateState.Available) Tag("новая!", color = m.red, textColor = Color.White)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = m.ink)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    MangaButton("Выйти из аккаунта", { Session.signOut() }, kind = ButtonKind.PAPER, icon = Icons.AutoMirrored.Rounded.Logout, modifier = Modifier.fillMaxWidth())
    Text("AniRoom для Android · ${BuildConfig.VERSION_NAME}", color = m.muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}

// ------------------------------------------------------------------------------------- building blocks

@Composable
private fun Card(title: String? = null, hint: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val m = Manga.colors
    MangaPanel(Modifier.fillMaxWidth(), shadow = 3.dp) {
        Column(Modifier.padding(14.dp)) {
            if (title != null) Text(title.uppercase(), style = Kicker, color = m.red)
            if (hint != null) Text(hint, color = m.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
            content()
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val m = Manga.colors
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = m.ink, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, color = m.muted, fontSize = 12.sp)
        }
        Switch(
            checked = checked, onCheckedChange = onChange, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = m.red, checkedThumbColor = Color.White, uncheckedTrackColor = m.paper3, uncheckedBorderColor = m.ink, uncheckedThumbColor = m.ink),
        )
    }
}

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, value: T, onChange: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        options.forEach { (key, label) -> SelectChip(label, value == key, { onChange(key) }) }
    }
}

@Composable
private fun Feedback(text: String?) {
    AnimatedVisibility(text != null) {
        Text(text ?: "", color = Manga.colors.red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
    }
}

// ------------------------------------------------------------------------------------- profile

@Composable
private fun ProfileSection() {
    val m = Manga.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user by Session.user.collectAsState()
    val u = user ?: return
    var editUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var favorites by remember { mutableStateOf<List<FavoriteTitle>?>(null) }

    LaunchedEffect(u.id) { api { publicProfile(u.id) }.onSuccess { favorites = it.favorites } }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) editUri = uri }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) editUri = cameraUri }

    Card("Аватар", "Фото с обработкой прямо в приложении, GIF и видео — как есть.") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(u.avatar, u.username, size = 76.dp, color = u.nicknameColor, frame = u.game?.frame)
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MangaButton("Галерея", { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }, compact = true, icon = Icons.Rounded.Image)
                MangaButton("Камера", {
                    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                    val file = File(dir, "shot-${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                    cameraUri = uri
                    camera.launch(uri)
                }, compact = true, kind = ButtonKind.PAPER, icon = Icons.Rounded.CameraAlt)
            }
        }
    }

    UsernameCard(u)
    NickColorCard(u)
    BioCard(u)

    Card("Обложка профиля", "Фон шапки на твоей странице.") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
            PROFILE_COVERS.forEach { (id, label) ->
                val active = (u.profileCover ?: "default") == id
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp).clickable {
                    if (!active) scope.launch {
                        api { updateProfile(obj("profile_cover" to id)) }.onSuccess { Session.setUser(it) }.onFailure { message = it.message }
                    }
                }) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1.4f).clip(RoundedCornerShape(10.dp))
                            .border(if (active) 3.dp else 2.dp, if (active) m.red else m.ink, RoundedCornerShape(10.dp)),
                    ) {
                        CoverSwatch(id, Modifier.fillMaxSize())
                        if (active) Box(Modifier.align(Alignment.Center).size(24.dp).clip(CircleShape).background(m.red), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(label, color = m.ink, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        Feedback(message)
    }

    FavoritesCard(u, favorites) { favorites = it }
    ShikimoriCard()

    editUri?.let { uri ->
        AvatarEditorDialog(uri, onDismiss = { editUri = null }, onSaved = { editUri = null; message = null })
    }
}

@Composable
private fun UsernameCard(u: User) {
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(u.username) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val next = parseInstant(u.usernameChangedAt)?.plusSeconds(14 * 86400L)
    val locked = next != null && next.isAfter(Instant.now())
    Card("Ник", if (locked) "Снова сменить можно с ${DateTimeFormatter.ofPattern("d MMMM", Locale("ru")).format(next!!.atZone(ZoneId.systemDefault()))}" else "Меняется раз в 14 дней.") {
        if (!editing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("@${u.username}", color = Manga.colors.ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                MangaButton("Изменить", { name = u.username; editing = true }, compact = true, kind = ButtonKind.PAPER, enabled = !locked)
            }
        } else {
            MangaField(name, { name = it.take(24) }, placeholder = "Новый ник", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            MangaField(password, { password = it }, placeholder = "Пароль для подтверждения", visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
            Feedback(error)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MangaButton("Отмена", { editing = false; error = null }, compact = true, kind = ButtonKind.PAPER)
                MangaButton("Сменить ник", {
                    busy = true
                    scope.launch {
                        api { changeUsername(obj("username" to name.trim(), "password" to password)) }
                            .onSuccess { Session.setUser(it); editing = false; password = "" }
                            .onFailure { error = it.message }
                        busy = false
                    }
                }, compact = true, loading = busy, enabled = password.isNotEmpty() && name.trim().length >= 3 && name.trim() != u.username)
            }
        }
    }
}

@Composable
private fun NickColorCard(u: User) {
    val m = Manga.colors
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    val current = u.nicknameColor?.uppercase()
    Card("Цвет ника", "Как ник выглядит в чате и на профиле.") {
        MangaPanel(Modifier.fillMaxWidth().padding(vertical = 6.dp), color = m.paper3, shadow = 0.dp, border = 1.5.dp) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(u.avatar, u.username, size = 32.dp, color = u.nicknameColor)
                Spacer(Modifier.width(10.dp))
                NickText(u.username, siteNickColor(u.username, u.nicknameColor), u.game?.nickFx, fontSize = 16.sp)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            (listOf<String?>(null) + NICK_COLORS).forEach { hex ->
                val selected = current == hex
                val s by animateFloatAsState(if (selected) 1.15f else 1f, spring(dampingRatio = 0.4f), label = "nick")
                Box(
                    Modifier.size(38.dp).scale(s).clip(CircleShape)
                        .background(if (hex == null) m.paper2 else Color(android.graphics.Color.parseColor(hex)))
                        .border(if (selected) 3.dp else 2.dp, m.ink, CircleShape)
                        .clickable {
                            scope.launch { savePrefs(obj("nickname_color" to hex))?.let { error = it } }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (hex == null) Text("A", color = m.muted, fontWeight = FontWeight.Bold)
                    if (selected) Icon(Icons.Rounded.Check, null, tint = m.ink, modifier = Modifier.size(18.dp))
                }
            }
        }
        Feedback(error)
    }
}

@Composable
private fun BioCard(u: User) {
    val scope = rememberCoroutineScope()
    var bio by remember(u.bio) { mutableStateOf(u.bio.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val dirty = bio.trim() != u.bio.orEmpty().trim()
    Card("О себе", "Пара строк: что смотришь, что любишь. До 300 символов.") {
        MangaField(bio, { bio = it.take(300) }, placeholder = "Пересматриваю «Монолог фармацевта» и ищу компанию на онгоинги", singleLine = false, maxLines = 5, modifier = Modifier.fillMaxWidth())
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${bio.length}/300", color = if (bio.length > 280) Manga.colors.red else Manga.colors.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            AnimatedVisibility(dirty) {
                MangaButton("Сохранить", {
                    busy = true
                    scope.launch {
                        api { updateProfile(obj("bio" to bio.trim().ifEmpty { null })) }.onSuccess { Session.setUser(it) }.onFailure { error = it.message }
                        busy = false
                    }
                }, compact = true, loading = busy)
            }
        }
        Feedback(error)
    }
}

@Composable
private fun FavoritesCard(u: User, favorites: List<FavoriteTitle>?, onChange: (List<FavoriteTitle>) -> Unit) {
    val m = Manga.colors
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf<List<AnimeListEntry>?>(null) }
    var query by remember { mutableStateOf("") }
    val favs = favorites.orEmpty()
    LaunchedEffect(picking) {
        if (picking && entries == null) entries = api { userList(u.id, null, 0, 200) }.getOrNull()?.entries ?: emptyList()
    }
    fun apply(next: List<FavoriteTitle>) = scope.launch {
        api { updateProfile(obj("favorite_titles" to JsonArray(next.map { JsonPrimitive(it.shikimoriId) }))) }
            .onSuccess { Session.setUser(it); onChange(next) }
    }
    Card("Любимые тайтлы", "До трёх тайтлов из твоего списка — они встанут на витрину профиля.") {
        if (favorites == null) SkeletonBox(Modifier.fillMaxWidth().height(110.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
            favs.forEach { f ->
                Box(Modifier.weight(1f)) {
                    Column {
                        Poster(f.poster, Modifier.fillMaxWidth().aspectRatio(0.68f), radius = 10.dp)
                        Text(f.title, color = m.ink, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Box(
                        Modifier.align(Alignment.TopEnd).offset(4.dp, (-4).dp).size(24.dp).clip(CircleShape).background(m.ink)
                            .clickable { apply(favs.filterNot { it.shikimoriId == f.shikimoriId }) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Close, "Убрать", tint = m.paper, modifier = Modifier.size(14.dp)) }
                }
            }
            if (favs.size < 3) Box(
                Modifier.weight(1f).aspectRatio(0.68f).clip(RoundedCornerShape(10.dp)).border(2.dp, m.ink, RoundedCornerShape(10.dp)).clickable { picking = !picking },
                contentAlignment = Alignment.Center,
            ) { Icon(if (picking) Icons.Rounded.Close else Icons.Rounded.Add, "Добавить", tint = m.ink) }
            repeat((2 - favs.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
        }
        AnimatedVisibility(picking) {
            Column(Modifier.padding(top = 10.dp)) {
                MangaField(query, { query = it }, placeholder = "Найти в своём списке", modifier = Modifier.fillMaxWidth())
                val list = entries
                when {
                    list == null -> CircularProgressIndicator(Modifier.padding(12.dp), color = m.red)
                    list.isEmpty() -> Text("Список пока пуст — добавь тайтлы в «Мой список».", color = m.muted, modifier = Modifier.padding(8.dp))
                    else -> list.filter { e -> favs.none { it.shikimoriId == e.shikimoriId } && (query.isBlank() || e.title.contains(query.trim(), true)) }.take(25).forEach { e ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                                val next = (favs + FavoriteTitle(e.shikimoriId, e.title, e.poster, e.status)).take(3)
                                apply(next)
                                if (next.size >= 3) picking = false
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Poster(e.poster, Modifier.width(34.dp).aspectRatio(0.7f), radius = 6.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(e.title, color = m.ink, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Rounded.Add, null, tint = m.red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShikimoriCard() {
    val scope = rememberCoroutineScope()
    var nick by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    Card("Импорт из Шикимори", "Перенеси свой список с shikimori.one по нику.") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MangaField(nick, { nick = it.take(40) }, placeholder = "Ник на Шикимори", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            MangaButton("Импорт", {
                busy = true
                scope.launch {
                    api { importShikimori(obj("nickname" to nick.trim())) }
                        .onSuccess { result = "Готово: +${it.added}, обновлено ${it.updated}, всего ${it.total}" }
                        .onFailure { result = it.message }
                    busy = false
                }
            }, compact = true, loading = busy, enabled = nick.isNotBlank(), icon = Icons.Rounded.Download)
        }
        result?.let { Text(it, color = Manga.colors.inkSoft, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
    }
}

// ------------------------------------------------------------------------------------- wardrobe

private val WARDROBE_TABS = listOf("titles" to "Титул", "frames" to "Рамка", "bubbles" to "Облачко", "nick_fx" to "Ник")

@Composable
private fun WardrobeSection() {
    val m = Manga.colors
    val scope = rememberCoroutineScope()
    val user by Session.user.collectAsState()
    val game by UserHub.game.collectAsState()
    var tab by rememberSaveable { mutableStateOf("titles") }
    var busy by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { UserHub.refreshGame() }
    val u = user ?: return
    val g = game
    if (g == null) {
        SkeletonBox(Modifier.fillMaxWidth().height(160.dp))
        return
    }
    val eq = g.equipped
    val color = siteNickColor(u.username, u.nicknameColor)

    // live preview
    MangaPanel(Modifier.fillMaxWidth(), color = m.paper2, shadow = 4.dp) {
        Column(Modifier.padding(16.dp)) {
            Text("ТАК ТЕБЯ ВИДЯТ", style = Kicker, color = m.red)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Top) {
                UserAvatar(u.avatar, u.username, size = 52.dp, color = u.nicknameColor, frame = eq.frame)
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NickText(u.username, color, eq.nickFx, fontSize = 15.sp)
                        Spacer(Modifier.width(6.dp))
                        Tag("ур. ${g.level}", color = m.yellow, textColor = m.onYellow)
                    }
                    g.cosmetics?.title?.let { Text(it, color = m.red, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(6.dp))
                    val skin = bubbleSkin(eq.bubble, false, m, RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp))
                    Box(
                        Modifier.clip(skin.shape).background(skin.background)
                            .then(if (!skin.dashed) Modifier.border(2.dp, m.ink, skin.shape) else Modifier)
                            .bubbleExtras(skin, skin.text).padding(horizontal = 12.dp, vertical = 8.dp),
                    ) { Text("Ещё одну серию? 👀", color = skin.text) }
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WARDROBE_TABS.forEach { (key, label) -> SelectChip(label, tab == key, { tab = key }) }
    }
    val items: List<CatalogItem> = when (tab) {
        "titles" -> g.catalog.titles
        "frames" -> g.catalog.frames
        "bubbles" -> g.catalog.bubbles
        else -> g.catalog.nickFx
    }
    val current = when (tab) {
        "titles" -> eq.titleKey ?: ""
        "frames" -> eq.frame
        "bubbles" -> eq.bubble
        else -> eq.nickFx
    }
    if (items.isEmpty()) Text("Пока нечего надеть — уровни открывают новые вещи.", color = m.muted)
    items.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { item ->
                val active = current == item.key
                MangaPanel(
                    Modifier.weight(1f),
                    color = if (active) m.yellow else if (item.unlocked) m.paper2 else m.paper3,
                    shadow = if (item.unlocked) 3.dp else 1.dp,
                    onClick = if (!item.unlocked || busy != null) null else ({
                        val field = when (tab) { "titles" -> "title_key"; "frames" -> "frame"; "bubbles" -> "bubble"; else -> "nick_fx" }
                        val value: String? = if (tab == "titles" && active) null else item.key
                        busy = item.key
                        error = null
                        scope.launch {
                            api { updateCosmetics(obj(field to value)) }
                                .onSuccess { UserHub.setGame(it); api { me() }.onSuccess { me -> Session.setUser(me) } }
                                .onFailure { error = it.message }
                            busy = null
                        }
                    }),
                ) {
                    Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.height(54.dp), contentAlignment = Alignment.Center) {
                            when (tab) {
                                "titles" -> Tag(item.name, color = if (item.rarity == "legendary") m.yellow else m.paper3, textColor = m.ink)
                                "frames" -> FrameSample(item.key, 38.dp)
                                "bubbles" -> {
                                    val skin = bubbleSkin(item.key, false, m, RoundedCornerShape(14.dp))
                                    Box(Modifier.clip(skin.shape).background(skin.background).then(if (!skin.dashed) Modifier.border(2.dp, m.ink, skin.shape) else Modifier).bubbleExtras(skin, skin.text).padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text("Привет!", color = skin.text, fontSize = 12.sp)
                                    }
                                }
                                else -> NickText(u.username, color, item.key, fontSize = 15.sp)
                            }
                        }
                        Text(
                            if (tab == "titles") (if (item.fromBadge) "за достижение" else "${item.level} уровень") else item.name,
                            color = m.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                        )
                        when {
                            busy == item.key -> CircularProgressIndicator(Modifier.size(14.dp), color = m.red, strokeWidth = 2.dp)
                            active -> Text("надето", color = m.red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            !item.unlocked -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lock, null, tint = m.muted, modifier = Modifier.size(12.dp))
                                Text(" ${item.level} ур.", color = m.muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    Feedback(error)
    Text("Уровень ${g.level}: ${g.intoLevel}/${g.levelNeed} XP до следующего", color = m.muted, fontSize = 12.sp)
    ProgressLine(g.progress.toFloat(), Modifier.fillMaxWidth())
}

// ------------------------------------------------------------------------------------- look

@Composable
private fun LookSection() {
    val prefs by Session.appPrefs.collectAsState()
    Card("Тема", "«Авто» сама переключается днём и ночью.") {
        Segmented(
            listOf(ThemeMode.SYSTEM to "Авто", ThemeMode.LIGHT to "Бумага", ThemeMode.SEPIA to "Сепия", ThemeMode.DARK to "Тушь"),
            prefs.theme,
        ) { mode -> Session.updatePrefs { it.copy(theme = mode) }; UserHub.syncPrefs() }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Card("Material You", "Только на Android: акцентный цвет берётся из твоих обоев вместо красного.") {
            SwitchRow("Цвета из обоев", null, prefs.materialYou) { v -> Session.updatePrefs { it.copy(materialYou = v) } }
        }
    }
    Card("Размер текста в чате", "Только у тебя, остальные видят свой размер.") {
        Segmented(listOf("sm" to "Мелкий", "md" to "Обычный", "lg" to "Крупный"), prefs.chatFont) { v -> Session.updatePrefs { it.copy(chatFont = v) }; UserHub.syncPrefs() }
        SwitchRow("Сообщения поверх видео", "В полноэкранном режиме", prefs.chatToasts) { v -> Session.updatePrefs { it.copy(chatToasts = v) }; UserHub.syncPrefs() }
        SwitchRow("Вибрация", "Тактильный отклик на нажатия и реакции", prefs.haptics) { v -> Session.updatePrefs { it.copy(haptics = v) } }
        SwitchRow("Звук новых сообщений", "Тихий «тук-тук», когда пишут другие", prefs.chatSound) { v ->
            Session.updatePrefs { it.copy(chatSound = v) }
            UserHub.syncPrefs()
            if (v) space.aniroom.app.util.Sounds.chat()
        }
        SwitchRow("Звук достижений", "Короткий сигнал и анимация при новой награде", prefs.achievementSound) { v ->
            Session.updatePrefs { it.copy(achievementSound = v) }
            UserHub.syncPrefs()
            if (v) space.aniroom.app.util.Sounds.play("rare", "rare")
        }
        val demoScope = rememberCoroutineScope()
        MangaButton("Показать все анимации наград", { demoScope.launch { playAwardsDemo() } }, kind = ButtonKind.YELLOW, compact = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    }
}

// ------------------------------------------------------------------------------------- player

@Composable
private fun PlayerSection() {
    val scope = rememberCoroutineScope()
    val prefs by Session.appPrefs.collectAsState()
    val user by Session.user.collectAsState()
    var dub by remember { mutableStateOf(prefs.preferredDub) }
    Card("Качество по умолчанию", "«Авто» подстраивается под сеть. Сохраняется в аккаунте.") {
        Segmented(listOf("auto" to "Авто", "1080" to "1080p", "720" to "720p", "480" to "480p", "360" to "360p"), user?.qualityMode ?: prefs.qualityMode) { v ->
            Session.updatePrefs { it.copy(qualityMode = v) }
            scope.launch { savePrefs(obj("quality_mode" to v)) }
        }
        SwitchRow("Экономия трафика", "Не выше 480p — для мобильного интернета", user?.dataSaver ?: prefs.dataSaver) { v ->
            Session.updatePrefs { it.copy(dataSaver = v) }
            scope.launch { savePrefs(obj("data_saver" to v)) }
        }
    }
    Card("Просмотр") {
        SwitchRow("Пропускать опенинг сам", "Когда ты хост — прыжок через опенинг за всех", prefs.autoSkipOpening) { v -> Session.updatePrefs { it.copy(autoSkipOpening = v) }; UserHub.syncPrefs() }
        SwitchRow("Реакции поверх видео", "Летящие 🔥 и 😂 от остальных зрителей", prefs.reactions) { v -> Session.updatePrefs { it.copy(reactions = v) }; UserHub.syncPrefs() }
        SwitchRow("Картинка в картинке", "Свернул приложение — видео продолжается в окошке", prefs.autoPip) { v -> Session.updatePrefs { it.copy(autoPip = v) } }
        SwitchRow("Подсветка вокруг видео", "В полном экране чёрные поля светятся цветами кадра, как в YouTube", prefs.ambient) { v -> Session.updatePrefs { it.copy(ambient = v) } }
        Text("Картинка в полном экране", color = Manga.colors.ink, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Segmented(listOf("fit" to "Вписать", "fill" to "Заполнить", "stretch" to "Растянуть"), prefs.videoFit) { v -> Session.updatePrefs { it.copy(videoFit = v) } }
        SwitchRow("Жесты в плеере", "Свайп слева — яркость, справа — громкость (в полноэкранном)", prefs.gestures) { v -> Session.updatePrefs { it.copy(gestures = v) } }
    }
    Card("Любимая озвучка", "При выборе тайтла она будет отмечена первой.") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MangaField(dub, { dub = it.take(60) }, placeholder = "Например, AniLibria", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            MangaButton("ОК", { Session.updatePrefs { it.copy(preferredDub = dub.trim()) }; UserHub.syncPrefs() }, compact = true, enabled = dub.trim() != prefs.preferredDub)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
            listOf("AniLibria", "AniDUB", "Studio Band", "SHIZA Project", "AniMedia", "JAM", "Dream Cast").forEach { s ->
                Tag(s, Modifier.clickable { dub = s; Session.updatePrefs { it.copy(preferredDub = s) }; UserHub.syncPrefs() }, color = Manga.colors.paper3, textColor = Manga.colors.ink)
            }
        }
    }
}

// ------------------------------------------------------------------------------------- privacy

private val VIS = listOf("everyone" to "Все", "friends" to "Друзья", "nobody" to "Никто")

@Composable
private fun PrivacySection() {
    val m = Manga.colors
    val scope = rememberCoroutineScope()
    val user by Session.user.collectAsState()
    val u = user ?: return
    var blocked by remember { mutableStateOf<List<BlockedUser>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { blocked = api { blockedUsers() }.getOrDefault(emptyList()) }
    fun save(vararg pairs: Pair<String, Any?>) = scope.launch { error = savePrefs(obj(*pairs)) }

    Card("Кто видит мой список", "«Мой список» и статистика просмотров.") {
        Segmented(VIS, u.listVisibility ?: if (u.statsPublic) "everyone" else "friends") { save("list_visibility" to it) }
    }
    Card("Кто видит, что я смотрю", "Статус «в комнате» и название тайтла.") {
        Segmented(VIS, u.statusVisibility ?: "friends") { save("status_visibility" to it) }
    }
    Card {
        SwitchRow("Друзья могут звать в комнаты", "Выключи, если приглашения отвлекают", u.allowInvites) { save("allow_invites" to it) }
        SwitchRow("Принимать заявки в друзья", "Иначе добавить тебя смогут только по твоей заявке", u.allowFriendRequests) { save("allow_friend_requests" to it) }
        SwitchRow("Показывать мои достижения в комнате", "«🏆 ник получил…» видят остальные", u.achievementsPublic) { save("achievements_public" to it) }
        Feedback(error)
    }
    Card("Чёрный список", "Заблокированные не могут писать тебе заявки и звать в комнаты.") {
        val list = blocked
        when {
            list == null -> CircularProgressIndicator(Modifier.size(20.dp), color = m.red)
            list.isEmpty() -> Text("Никого. Мирная глава.", color = m.muted)
            else -> list.forEach { b ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(b.avatar, b.username, size = 34.dp, color = b.nicknameColor)
                    Spacer(Modifier.width(10.dp))
                    Text(b.username, color = m.ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    MangaButton("Разблок.", {
                        scope.launch { api { unblockUser(b.id) }.onSuccess { blocked = blocked?.filterNot { it.id == b.id } } }
                    }, compact = true, kind = ButtonKind.PAPER)
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------- notifications

@Composable
private fun NotifySection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user by Session.user.collectAsState()
    val prefs by Session.appPrefs.collectAsState()
    val u = user ?: return
    var error by remember { mutableStateOf<String?>(null) }
    var granted by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    fun save(vararg pairs: Pair<String, Any?>) = scope.launch { error = savePrefs(obj(*pairs)) }
    val tz = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000

    Card("Системные уведомления", "Приглашения и друзья приходят на телефон, пока приложение работает в фоне.") {
        if (!granted) MangaButton("Разрешить уведомления", { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }, kind = ButtonKind.YELLOW, modifier = Modifier.fillMaxWidth())
        else Text("Разрешены ✓", color = Manga.colors.green, fontWeight = FontWeight.Bold)
    }
    Card("О чём сообщать") {
        SwitchRow("Друг начал смотреть", "Когда друг заходит в комнату", u.notifyFriendWatching) { save("notify_friend_watching" to it) }
        SwitchRow("Вышла новая серия", "Для тайтлов со статусом «Смотрю»", u.notifyNewEpisodes) { save("notify_new_episodes" to it) }
        SwitchRow("Подсказки из буфера обмена", "Скопировал ссылку на комнату — приложение предложит войти", prefs.clipboardHints) { v -> Session.updatePrefs { it.copy(clipboardHints = v) } }
    }
    Card("Тишина") {
        SwitchRow("Не беспокоить", "Уведомления копятся, но не всплывают", u.doNotDisturb) { save("do_not_disturb" to it) }
        val quietOn = !u.dndFrom.isNullOrBlank() && !u.dndTo.isNullOrBlank()
        SwitchRow("Тихие часы", if (quietOn) "Каждый день с ${u.dndFrom} до ${u.dndTo}" else "Например, с 23:00 до 9:00", quietOn) { on ->
            if (on) save("dnd_from" to "23:00", "dnd_to" to "09:00", "tz_offset" to tz) else save("dnd_from" to "", "dnd_to" to "")
        }
        if (quietOn) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            fun pick(current: String, onPicked: (String) -> Unit) {
                val (h, mm) = current.split(":").map { it.toIntOrNull() ?: 0 }.let { (it.getOrElse(0) { 23 }) to (it.getOrElse(1) { 0 }) }
                TimePickerDialog(context, { _, hour, minute -> onPicked(String.format(Locale.US, "%02d:%02d", hour, minute)) }, h, mm, true).show()
            }
            MangaButton("с ${u.dndFrom}", { pick(u.dndFrom!!) { t -> if (t != u.dndTo) save("dnd_from" to t, "dnd_to" to u.dndTo, "tz_offset" to tz) } }, compact = true, kind = ButtonKind.PAPER, icon = Icons.Rounded.BrightnessMedium)
            MangaButton("до ${u.dndTo}", { pick(u.dndTo!!) { t -> if (t != u.dndFrom) save("dnd_from" to u.dndFrom, "dnd_to" to t, "tz_offset" to tz) } }, compact = true, kind = ButtonKind.PAPER, icon = Icons.Rounded.BrightnessMedium)
        }
        Feedback(error)
    }
}

// ------------------------------------------------------------------------------------- account

@Composable
private fun AccountSection() {
    val m = Manga.colors
    val scope = rememberCoroutineScope()
    val user by Session.user.collectAsState()
    val u = user ?: return
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var pwBusy by remember { mutableStateOf(false) }
    var pwMsg by remember { mutableStateOf<String?>(null) }
    var sessionsMsg by remember { mutableStateOf<String?>(null) }
    var deleteOpen by remember { mutableStateOf(false) }
    var confirmName by remember { mutableStateOf("") }
    var deletePassword by remember { mutableStateOf("") }
    var deleteMsg by remember { mutableStateOf<String?>(null) }

    Card("Пароль", "После смены другие устройства выйдут из аккаунта.") {
        MangaField(current, { current = it }, placeholder = "Текущий пароль", visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        MangaField(next, { next = it }, placeholder = "Новый пароль (от 8 символов)", visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        MangaField(repeat, { repeat = it }, placeholder = "Повтори новый пароль", visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Feedback(pwMsg)
        Spacer(Modifier.height(10.dp))
        MangaButton("Сменить пароль", {
            if (next != repeat) { pwMsg = "Пароли не совпадают"; return@MangaButton }
            pwBusy = true
            scope.launch {
                api { changePassword(obj("current_password" to current, "new_password" to next)) }
                    .onSuccess { Session.signIn(it.accessToken, it.user); pwMsg = "Пароль изменён"; current = ""; next = ""; repeat = "" }
                    .onFailure { pwMsg = it.message }
                pwBusy = false
            }
        }, loading = pwBusy, enabled = current.isNotEmpty() && next.length >= 8, modifier = Modifier.fillMaxWidth())
    }
    Card("Сеансы", "Выйдет на всех устройствах, кроме этого.") {
        MangaButton("Выйти везде", {
            scope.launch {
                api { logoutAll() }.onSuccess { Session.signIn(it.accessToken, it.user); sessionsMsg = "Остальные устройства отключены" }.onFailure { sessionsMsg = it.message }
            }
        }, kind = ButtonKind.PAPER, icon = Icons.AutoMirrored.Rounded.Logout, modifier = Modifier.fillMaxWidth())
        sessionsMsg?.let { Text(it, color = m.inkSoft, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
    }
    Card("Удаление аккаунта", "Профиль, список и история исчезнут навсегда.") {
        if (!deleteOpen) MangaButton("Удалить аккаунт", { deleteOpen = true }, kind = ButtonKind.PAPER, icon = Icons.Rounded.Delete, modifier = Modifier.fillMaxWidth())
        else {
            Text("Введи ник «${u.username}» и пароль:", color = m.ink)
            Spacer(Modifier.height(8.dp))
            MangaField(confirmName, { confirmName = it }, placeholder = u.username, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            MangaField(deletePassword, { deletePassword = it }, placeholder = "Пароль", visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Feedback(deleteMsg)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MangaButton("Отмена", { deleteOpen = false }, kind = ButtonKind.PAPER, compact = true)
                MangaButton("Удалить навсегда", {
                    scope.launch {
                        api { deleteAccount(obj("password" to deletePassword, "confirm" to confirmName)) }
                            .onSuccess { Session.signOut("Аккаунт удалён") }
                            .onFailure { deleteMsg = it.message }
                    }
                }, compact = true, enabled = confirmName == u.username && deletePassword.isNotEmpty(), icon = Icons.Rounded.Delete)
            }
        }
    }
}

// ------------------------------------------------------------------------------------- updates

@Composable
private fun UpdatesSection() {
    val m = Manga.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by Updater.state.collectAsState()
    LaunchedEffect(Unit) { Updater.check(force = true) }
    Card("Версия", "Сейчас установлена сборка ${Updater.currentVersion}.") {
        Text("AniRoom ${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold, color = m.ink)
        Spacer(Modifier.height(10.dp))
        UpdateContent(state, onDownload = { s -> scope.launch { Updater.download(context, s) } }, onInstall = { Updater.install(context, it.file) })
        Spacer(Modifier.height(10.dp))
        MangaButton("Проверить ещё раз", { scope.launch { Updater.check(force = true) } }, kind = ButtonKind.PAPER, compact = true, enabled = state !is UpdateState.Checking && state !is UpdateState.Downloading)
    }
    Card("Как это работает", null) {
        Text(
            "Каждая новая сборка с GitHub прилетает прямо в приложение: проверка раз в несколько часов, загрузка с прогрессом и установка поверх — без потери входа.",
            color = m.inkSoft, fontSize = 13.sp,
        )
    }
}

@Composable
fun UpdateContent(state: UpdateState, onDownload: (UpdateState.Available) -> Unit, onInstall: (UpdateState.Ready) -> Unit) {
    val m = Manga.colors
    when (state) {
        UpdateState.Idle, UpdateState.UpToDate -> Text("У тебя последняя версия ✓", color = m.green, fontWeight = FontWeight.Bold)
        UpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), color = m.red, strokeWidth = 2.dp)
            Text("  Проверяю…", color = m.muted)
        }
        is UpdateState.Available -> Column {
            Text("Доступна сборка ${state.version}", color = m.red, fontWeight = FontWeight.Black, fontSize = 16.sp)
            if (state.notes.isNotBlank()) Text(state.notes, color = m.inkSoft, fontSize = 13.sp, maxLines = 8, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 6.dp))
            MangaButton("Скачать и установить · ${state.sizeBytes / 1024 / 1024 + 1} МБ", { onDownload(state) }, icon = Icons.Rounded.Download, modifier = Modifier.fillMaxWidth())
        }
        is UpdateState.Downloading -> Column {
            Text("Загрузка… ${(state.progress * 100).toInt()}%", color = m.ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            ProgressLine(state.progress, Modifier.fillMaxWidth())
        }
        is UpdateState.Ready -> MangaButton("Установить сборку ${state.version}", { onInstall(state) }, kind = ButtonKind.YELLOW, modifier = Modifier.fillMaxWidth())
        is UpdateState.Failed -> Text(state.message, color = m.red)
    }
}

@Suppress("unused")
private val divider: @Composable () -> Unit = { HorizontalDivider() }

private suspend fun playAwardsDemo() {
    fun id() = space.aniroom.app.data.GameEvents.nextId()
    val E = space.aniroom.app.data.GameEvents
    E.emit(space.aniroom.app.data.GameEvent.Xp(id(), 25, "Серия досмотрена", 0.62f, 7))
    kotlinx.coroutines.delay(900)
    E.emit(space.aniroom.app.data.GameEvent.Quest(id(), "Посмотри 3 серии с друзьями", 60))
    kotlinx.coroutines.delay(900)
    E.emit(space.aniroom.app.data.GameEvent.Streak(id(), 5, 15))
    kotlinx.coroutines.delay(1200)
    E.emit(space.aniroom.app.data.GameEvent.Achievement(id(), "first_session", "Первый сеанс", "Посмотри первую серию вместе", "common", ""))
    E.emit(space.aniroom.app.data.GameEvent.Achievement(id(), "night_duo", "Ночной дуэт", "Смотри с другом после полуночи", "rare", ""))
    E.emit(space.aniroom.app.data.GameEvent.Achievement(id(), "hundred_episodes", "Сотня серий", "100 серий в комнатах", "legendary", ""))
    E.emit(space.aniroom.app.data.GameEvent.Achievement(id(), "rezero_season", "Re:Zero — сезон 2", "", "rare", "rezero-shard-gold", season = true, completedSeasons = 2, totalSeasons = 4))
    kotlinx.coroutines.delay(2000)
    E.emit(space.aniroom.app.data.GameEvent.Moment(id(), space.aniroom.app.data.MomentPayload("all_gasped", "shock", listOf("Аня", "Кирилл", "Ты"))))
}
