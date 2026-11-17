package space.aniroom.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import space.aniroom.app.data.AnimeChoice
import space.aniroom.app.data.AnimeFullPage
import space.aniroom.app.data.AnimeSearchResult
import space.aniroom.app.data.AnimeTranslation
import space.aniroom.app.data.api
import space.aniroom.app.ui.components.EmptyState
import space.aniroom.app.ui.components.MangaButton
import space.aniroom.app.ui.components.MangaField
import space.aniroom.app.ui.components.MangaPanel
import space.aniroom.app.ui.components.MascotMood
import space.aniroom.app.ui.components.Poster
import space.aniroom.app.ui.components.RoundIcon
import space.aniroom.app.ui.components.SelectChip
import space.aniroom.app.ui.components.SkeletonBox
import space.aniroom.app.ui.components.Tag
import space.aniroom.app.ui.theme.Kicker
import space.aniroom.app.ui.theme.Manga

private val QUALITY_ORDER = listOf(1080, 720, 480, 360)

/** Full-screen catalog: search → title → dub, episode, quality → confirm. */
@Composable
fun AnimePickerDialog(
    initialQuery: String = "",
    confirmText: String = "Смотреть в комнате",
    onDismiss: () -> Unit,
    onConfirm: (AnimeChoice) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        AnimePickerContent(initialQuery, confirmText, onDismiss, onConfirm)
    }
}

@Composable
private fun AnimePickerContent(
    initialQuery: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (AnimeChoice) -> Unit,
) {
    val m = Manga.colors
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<AnimeSearchResult>?>(null) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<AnimeSearchResult?>(null) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) { results = null; searching = false; return@LaunchedEffect }
        searching = true
        delay(350)
        api { animeSearch(q, 24) }
            .onSuccess { results = it.results; searchError = null }
            .onFailure { searchError = it.message }
        searching = false
    }
    LaunchedEffect(Unit) { if (initialQuery.isEmpty()) runCatching { focus.requestFocus() } }

    BackHandler(selected != null) { selected = null }

    Box(Modifier.fillMaxSize().background(m.paper)) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                if (targetState != null) (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
                else (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
            },
            label = "picker",
        ) { current ->
            if (current == null) {
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        RoundIcon(Icons.Rounded.Close, "Закрыть", onDismiss)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("КАТАЛОГ", style = Kicker, color = m.red)
                            Text("Что смотрим?", style = MaterialTheme.typography.headlineSmall, color = m.ink)
                        }
                    }
                    MangaField(
                        value = query, onValueChange = { query = it }, placeholder = "Название аниме…",
                        icon = Icons.Rounded.Search,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                        fieldModifier = Modifier.focusRequester(focus),
                        trailing = { if (searching) CircularProgressIndicator(Modifier.size(18.dp), color = m.red, strokeWidth = 2.dp) },
                    )
                    Spacer(Modifier.height(12.dp))
                    val list = results
                    when {
                        searchError != null && list == null -> EmptyState("Поиск не удался", searchError ?: "", mood = MascotMood.SHOCK)
                        list == null && searching -> LazyVerticalGrid(
                            GridCells.Adaptive(108.dp), contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) { items(9) { SkeletonBox(Modifier.fillMaxWidth().aspectRatio(0.66f)) } }
                        list == null -> EmptyState("Найди тайтл", "Начни вводить название — например, «Магическая битва»", mood = MascotMood.SEARCH)
                        list.isEmpty() -> EmptyState("Ничего не нашлось", "Попробуй другое написание или оригинальное название", mood = MascotMood.SHOCK)
                        else -> LazyVerticalGrid(
                            GridCells.Adaptive(108.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.imePadding(),
                        ) {
                            items(list, key = { it.shikimoriId }) { item ->
                                SearchCard(item, Modifier.animateItem()) { selected = item }
                            }
                        }
                    }
                }
            } else {
                TitleSetup(current, confirmText, onBack = { selected = null }, onConfirm = onConfirm)
            }
        }
    }
}

@Composable
private fun SearchCard(item: AnimeSearchResult, modifier: Modifier, onClick: () -> Unit) {
    val m = Manga.colors
    Column(modifier) {
        MangaPanel(Modifier.fillMaxWidth().aspectRatio(0.68f), radius = 12.dp, shadow = 3.dp, onClick = onClick) {
            Poster(item.poster, Modifier.fillMaxSize(), radius = 0.dp)
            item.scoreText?.let { score ->
                Row(
                    Modifier.align(Alignment.TopStart).padding(6.dp).clip(RoundedCornerShape(8.dp)).background(m.yellow).padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Star, null, tint = m.onYellow, modifier = Modifier.size(12.dp))
                    Text(score, color = m.onYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(item.title, color = m.ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(listOfNotNull(item.year?.toString(), item.kind).joinToString(" · "), color = m.muted, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun TitleSetup(item: AnimeSearchResult, confirmText: String, onBack: () -> Unit, onConfirm: (AnimeChoice) -> Unit) {
    val m = Manga.colors
    var page by remember { mutableStateOf<AnimeFullPage?>(null) }
    var translations by remember { mutableStateOf<List<AnimeTranslation>?>(null) }
    var seriesCount by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var translation by remember { mutableStateOf<AnimeTranslation?>(null) }
    var episode by remember { mutableIntStateOf(1) }
    var qualities by remember { mutableStateOf<List<Int>>(emptyList()) }
    var quality by remember { mutableStateOf<Int?>(null) }
    var skip by remember { mutableStateOf<List<List<Double>>>(emptyList()) }
    var linkLoading by remember { mutableStateOf(false) }
    var typeFilter by remember { mutableStateOf("voice") }

    LaunchedEffect(item.shikimoriId) {
        api { animeTitle(item.shikimoriId) }.onSuccess { page = it }
        api { animeTranslations(item.shikimoriId) }
            .onSuccess { t ->
                translations = t.translations
                seriesCount = t.seriesCount
                if (t.translations.none { it.type?.contains("voice") == true }) typeFilter = "all"
            }
            .onFailure { error = it.message; translations = emptyList() }
    }
    val isMovie = Regex("movie|фильм", RegexOption.IGNORE_CASE).containsMatchIn(page?.type ?: item.kind ?: "")
    LaunchedEffect(translation?.id, episode, isMovie) {
        val t = translation ?: return@LaunchedEffect
        linkLoading = true
        delay(250)
        api { animeLink(item.shikimoriId, t.id, if (isMovie) 0 else episode) }
            .onSuccess { link ->
                val list = link.qualities.ifEmpty { QUALITY_ORDER.filter { it <= link.maxQuality } }.sortedDescending()
                qualities = list
                if (quality == null || quality !in list) quality = list.firstOrNull { it <= 720 } ?: list.firstOrNull() ?: link.maxQuality
                skip = link.skipSegments
                error = null
            }
            .onFailure { error = it.message; qualities = emptyList() }
        linkLoading = false
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Box(Modifier.fillMaxWidth().height(260.dp)) {
                    Poster(page?.picture ?: item.poster, Modifier.fillMaxSize(), radius = 0.dp)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, m.paper.copy(alpha = 0.6f), m.paper))))
                    RoundIcon(
                        Icons.AutoMirrored.Rounded.ArrowBack, "Назад", onBack,
                        modifier = Modifier.statusBarsPadding().padding(16.dp),
                    )
                    Row(Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
                        MangaPanel(Modifier.width(96.dp).aspectRatio(0.68f), radius = 10.dp, shadow = 3.dp) {
                            Poster(item.poster, Modifier.fillMaxSize(), radius = 0.dp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f).padding(bottom = 4.dp)) {
                            Text(page?.title ?: item.title, style = MaterialTheme.typography.titleLarge, color = m.ink, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            val meta = listOfNotNull(page?.type ?: item.kind, item.year?.toString(), page?.episodes?.let { "$it эп." }, page?.score?.let { "★ $it" })
                            Text(meta.joinToString(" · "), color = m.muted, fontSize = 13.sp)
                        }
                    }
                }
            }
            val desc = page?.description ?: item.description
            if (!desc.isNullOrBlank()) item {
                var expanded by remember { mutableStateOf(false) }
                Text(
                    desc, color = m.inkSoft, fontSize = 14.sp, lineHeight = 20.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(16.dp).clickable { expanded = !expanded },
                )
            }
            if (item.genres.isNotEmpty() || page?.genres?.isNotEmpty() == true) item {
                FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (page?.genres?.takeIf { it.isNotEmpty() } ?: item.genres).take(8).forEach { Tag(it, color = m.paper3, textColor = m.ink) }
                }
            }
            item {
                Text("ОЗВУЧКА", style = Kicker, color = m.red, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp))
            }
            val list = translations
            if (list == null) {
                item { Column(Modifier.padding(horizontal = 16.dp)) { repeat(4) { SkeletonBox(Modifier.fillMaxWidth().height(52.dp).padding(bottom = 8.dp)) } } }
            } else if (list.isEmpty()) {
                item { EmptyState("Озвучек нет", error ?: "Для этого тайтла пока нет источников", mood = MascotMood.SHOCK) }
            } else {
                item {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectChip("Озвучка", typeFilter == "voice", { typeFilter = "voice" })
                        SelectChip("Субтитры", typeFilter == "subtitles", { typeFilter = "subtitles" })
                        SelectChip("Все", typeFilter == "all", { typeFilter = "all" })
                    }
                }
                val filtered = list.filter { typeFilter == "all" || (it.type ?: "").contains(typeFilter) }.ifEmpty { list }
                items(filtered, key = { it.id }) { t ->
                    val active = translation?.id == t.id
                    MangaPanel(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).animateItem(),
                        color = if (active) m.ink else m.paper2, radius = 12.dp, shadow = if (active) 0.dp else 3.dp,
                        onClick = { translation = t },
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(t.name ?: "Озвучка", color = if (active) m.paper else m.ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            t.seriesRange?.let { Text(it, color = if (active) m.paper.copy(alpha = 0.7f) else m.muted, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(translation != null) {
            MangaPanel(Modifier.fillMaxWidth(), color = m.paper2, radius = 0.dp, shadow = 0.dp) {
                Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                    if (!isMovie) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Серия", color = m.ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            RoundIcon(Icons.Rounded.Remove, "Меньше", { if (episode > 1) episode-- }, size = 36.dp)
                            AnimatedContent(episode, label = "ep", modifier = Modifier.width(64.dp)) { e ->
                                Text("$e", color = m.ink, fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                            RoundIcon(Icons.Rounded.Add, "Больше", { if (seriesCount <= 0 || episode < seriesCount) episode++ }, size = 36.dp)
                        }
                        if (seriesCount > 0) Text("Доступно серий: $seriesCount", color = m.muted, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Качество", color = m.ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (linkLoading) CircularProgressIndicator(Modifier.size(18.dp), color = m.red, strokeWidth = 2.dp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        qualities.forEach { q -> SelectChip("${q}p", quality == q, { quality = q }) }
                        if (qualities.isEmpty() && !linkLoading) Text(error ?: "Источник недоступен", color = m.red, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    MangaButton(
                        confirmText,
                        onClick = {
                            val t = translation ?: return@MangaButton
                            val q = quality ?: return@MangaButton
                            onConfirm(
                                AnimeChoice(
                                    shikimoriId = item.shikimoriId,
                                    title = page?.title ?: item.title,
                                    poster = page?.picture ?: item.poster,
                                    translationId = t.id,
                                    translationName = t.name ?: "Озвучка",
                                    quality = q,
                                    episode = if (isMovie) 0 else episode,
                                    episodesTotal = seriesCount,
                                    skipSegments = skip,
                                ),
                            )
                        },
                        icon = Icons.Rounded.PlayArrow,
                        enabled = quality != null && !linkLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
