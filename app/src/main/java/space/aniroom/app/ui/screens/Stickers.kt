package space.aniroom.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import space.aniroom.app.R
import space.aniroom.app.data.Api
import space.aniroom.app.data.Sticker
import space.aniroom.app.data.StickerStore
import space.aniroom.app.ui.components.ButtonKind
import space.aniroom.app.ui.components.Dialogish
import space.aniroom.app.ui.components.MangaButton
import space.aniroom.app.ui.components.MangaField
import space.aniroom.app.ui.components.SkeletonBox
import space.aniroom.app.ui.theme.Manga
import space.aniroom.app.util.rememberHaptic

/** Picks a file and asks for a name, then uploads it to the user's pack. */
@Composable
fun rememberStickerUploader(): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> pending = uri }
    pending?.let { uri ->
        var name by remember(uri) { mutableStateOf("") }
        Dialogish("Новый стикер", onDismiss = { pending = null }) {
            AsyncImage(
                model = uri, contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.size(120.dp).align(Alignment.CenterHorizontally).clip(RoundedCornerShape(14.dp)).background(Manga.colors.paper3),
            )
            Spacer(Modifier.height(12.dp))
            MangaField(name, { name = it.take(40) }, placeholder = "Название (необязательно)", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            MangaButton("Загрузить", {
                val target = uri
                pending = null
                scope.launch {
                    val error = StickerStore.upload(context, target, name)
                    Toast.makeText(context, error ?: "Стикер добавлен", Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier.fillMaxWidth())
        }
    }
    return { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
}

/** One sticker tile; long press opens rename / delete. */
@Composable
fun StickerTile(sticker: Sticker, size: Dp, onClick: () -> Unit) {
    val m = Manga.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = rememberHaptic()
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Box {
        AsyncImage(
            model = Api.media(sticker.image), contentDescription = sticker.name, contentScale = ContentScale.Fit,
            modifier = Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(m.paper2)
                .combinedClickable(onClick = onClick, onLongClick = { haptic(true); menu = true }).padding(4.dp),
        )
        DropdownMenu(menu, onDismissRequest = { menu = false }) {
            if (sticker.name.isNotBlank()) Text(sticker.name, color = m.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
            DropdownMenuItem(text = { Text("Переименовать") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { menu = false; renaming = true })
            DropdownMenuItem(text = { Text("Удалить", color = m.red) }, leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = m.red) }, onClick = { menu = false; confirmDelete = true })
        }
    }
    if (renaming) {
        var name by remember { mutableStateOf(sticker.name) }
        Dialogish("Название стикера", onDismiss = { renaming = false }) {
            MangaField(name, { name = it.take(40) }, placeholder = "Например, «шок»", modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            MangaButton("Сохранить", {
                renaming = false
                scope.launch { StickerStore.rename(sticker, name)?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } }
            }, modifier = Modifier.fillMaxWidth())
        }
    }
    if (confirmDelete) {
        Dialogish("Удалить стикер?", onDismiss = { confirmDelete = false }) {
            AsyncImage(model = Api.media(sticker.image), contentDescription = null, modifier = Modifier.size(90.dp).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MangaButton("Отмена", { confirmDelete = false }, kind = ButtonKind.PAPER, modifier = Modifier.weight(1f))
                MangaButton("Удалить", {
                    confirmDelete = false
                    scope.launch { StickerStore.delete(sticker)?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } }
                }, icon = Icons.Rounded.Delete, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AddStickerTile(size: Dp, onClick: () -> Unit) {
    val m = Manga.colors
    val uploading by StickerStore.uploading.collectAsState()
    Box(
        Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(m.yellow).border(2.dp, m.ink, RoundedCornerShape(12.dp))
            .combinedClickable(enabled = !uploading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (uploading) CircularProgressIndicator(Modifier.size(24.dp), color = m.ink, strokeWidth = 2.5.dp)
        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(R.drawable.lc_image_plus), "Добавить стикер", tint = m.onYellow, modifier = Modifier.size(24.dp))
            Text("Свой", color = m.onYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Chat tray: your pack + an «add» tile. Tap sends, long press manages. */
@Composable
fun StickerTrayContent(onPick: (Sticker) -> Unit) {
    val m = Manga.colors
    val stickers by StickerStore.stickers.collectAsState()
    val upload = rememberStickerUploader()
    LaunchedEffect(Unit) { StickerStore.load() }
    Column(Modifier.fillMaxWidth().background(m.paper3)) {
        LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(96.dp)) {
            item(key = "add") { AddStickerTile(72.dp, upload) }
            val list = stickers
            if (list == null) items(5) { SkeletonBox(Modifier.size(72.dp)) }
            else items(list, key = { it.id }) { s -> StickerTile(s, 72.dp) { onPick(s) } }
        }
        if (stickers?.isEmpty() == true) Text(
            "Загрузи картинку, GIF или короткое видео — и отправляй в чат. Долгое нажатие — переименовать или удалить.",
            color = m.muted, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
        )
    }
}
