package space.aniroom.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import space.aniroom.app.data.Session
import space.aniroom.app.data.User
import space.aniroom.app.data.api
import space.aniroom.app.ui.components.ButtonKind
import space.aniroom.app.ui.components.MangaButton
import space.aniroom.app.ui.components.Mascot
import space.aniroom.app.ui.components.MascotMood
import space.aniroom.app.ui.theme.DisplayFont
import space.aniroom.app.ui.theme.Kicker
import space.aniroom.app.ui.theme.Manga
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private data class AvatarFilter(val name: String, val build: () -> ColorMatrix)

private val FILTERS = listOf(
    AvatarFilter("Оригинал") { ColorMatrix() },
    AvatarFilter("Манга") {
        ColorMatrix().apply { setSaturation(0f); postConcat(contrastMatrix(1.55f, 0f)) }
    },
    AvatarFilter("Сепия") {
        ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    },
    AvatarFilter("Тёплый") { ColorMatrix().apply { setScale(1.12f, 1.02f, 0.86f, 1f) } },
    AvatarFilter("Холод") { ColorMatrix().apply { setScale(0.88f, 1f, 1.14f, 1f) } },
    AvatarFilter("Неон") { ColorMatrix().apply { setSaturation(1.9f); postConcat(contrastMatrix(1.18f, 0f)) } },
    AvatarFilter("Винтаж") {
        ColorMatrix().apply {
            setSaturation(0.55f)
            postConcat(ColorMatrix().apply { setScale(1.06f, 1f, 0.88f, 1f) })
            postConcat(contrastMatrix(0.86f, 0.06f))
        }
    },
    AvatarFilter("Тушь") {
        ColorMatrix().apply { setSaturation(0f); postConcat(contrastMatrix(2.6f, -0.05f)) }
    },
)

private fun contrastMatrix(contrast: Float, brightness: Float): ColorMatrix {
    val t = (1f - contrast) * 128f + brightness * 255f
    return ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, t,
            0f, contrast, 0f, 0f, t,
            0f, 0f, contrast, 0f, t,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
}

private class EditState(
    val zoom: Float,
    val offset: Offset,
    val rotation: Float,
    val flip: Boolean,
    val filter: Int,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
)

private fun colorFilter(s: EditState): ColorMatrixColorFilter {
    val cm = ColorMatrix().apply { setSaturation(s.saturation) }
    cm.postConcat(FILTERS[s.filter].build())
    cm.postConcat(contrastMatrix(s.contrast, s.brightness))
    return ColorMatrixColorFilter(cm)
}

/** Maps the bitmap into a square viewport of [viewport] px. [offset] is in that viewport's px. */
private fun buildMatrix(bitmap: Bitmap, viewport: Float, s: EditState, offsetScale: Float = 1f): Matrix {
    val w = bitmap.width.toFloat()
    val h = bitmap.height.toFloat()
    val base = viewport / min(w, h)
    return Matrix().apply {
        postTranslate(-w / 2f, -h / 2f)
        if (s.flip) postScale(-1f, 1f)
        postScale(base * s.zoom, base * s.zoom)
        postRotate(s.rotation)
        postTranslate(viewport / 2f + s.offset.x * offsetScale, viewport / 2f + s.offset.y * offsetScale)
    }
}

private suspend fun decodeBitmap(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val raw = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
            ?: return@runCatching null
        val orientation = resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
            ?: ExifInterface.ORIENTATION_NORMAL
        val fix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> fix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> fix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> fix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> fix.postScale(-1f, 1f)
            else -> return@runCatching raw
        }
        Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, fix, true)
    }.getOrNull()
}

private fun render(bitmap: Bitmap, viewportPx: Float, s: EditState, out: Int = 640): ByteArray {
    val result = Bitmap.createBitmap(out, out, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(result)
    canvas.drawColor(android.graphics.Color.rgb(241, 236, 225))
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { colorFilter = colorFilter(s) }
    canvas.drawBitmap(bitmap, buildMatrix(bitmap, out.toFloat(), s, out / viewportPx), paint)
    return ByteArrayOutputStream().use { stream ->
        result.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        result.recycle()
        stream.toByteArray()
    }
}

/** Native avatar editor: pinch/drag/rotate, flip, filters, light/contrast/saturation, then upload. */
@Composable
fun AvatarEditorDialog(uri: Uri, onDismiss: () -> Unit, onSaved: (User) -> Unit) {
    val context = LocalContext.current
    val m = Manga.colors
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    val mime = remember(uri) { context.contentResolver.getType(uri).orEmpty() }
    val isAnimated = mime.contains("gif") || mime.startsWith("video/")

    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val rotation = remember { Animatable(0f) }
    var freeRotation by remember { mutableFloatStateOf(0f) }
    var flip by remember { mutableStateOf(false) }
    var filter by remember { mutableIntStateOf(0) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var touching by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var viewportPx by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(uri) {
        if (isAnimated) return@LaunchedEffect
        bitmap = decodeBitmap(context, uri)
        failed = bitmap == null
    }

    fun state() = EditState(zoom, offset, rotation.value + freeRotation, flip, filter, brightness, contrast, saturation)

    fun upload(bytes: ByteArray, name: String, type: String) {
        busy = true
        error = null
        scope.launch {
            val part = MultipartBody.Part.createFormData("file", name, bytes.toRequestBody(type.toMediaTypeOrNull()))
            api { uploadAvatar(part) }
                .onSuccess { Session.setUser(it); onSaved(it) }
                .onFailure { error = it.message }
            busy = false
        }
    }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(
            Modifier.fillMaxSize().background(Color(0xFF0F0E0D)).statusBarsPadding().navigationBarsPadding(),
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).clickable(enabled = !busy, onClick = onDismiss), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, "Отмена", tint = Color.White)
                }
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text("РЕДАКТОР", style = Kicker, color = m.red)
                    Text("Новый аватар", color = Color.White, fontFamily = DisplayFont, fontSize = 16.sp)
                }
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(m.red).border(2.dp, Color.Black, CircleShape)
                        .clickable(enabled = !busy && (bitmap != null || isAnimated)) {
                            if (isAnimated) {
                                scope.launch {
                                    val bytes = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                                    if (bytes != null) upload(bytes, "avatar.${mime.substringAfter('/')}", mime) else error = "Файл не читается"
                                }
                            } else bitmap?.let { bmp ->
                                val snapshot = state()
                                busy = true
                                scope.launch {
                                    val bytes = withContext(Dispatchers.Default) { render(bmp, viewportPx, snapshot) }
                                    upload(bytes, "avatar.jpg", "image/jpeg")
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                    else Icon(Icons.Rounded.Check, "Сохранить", tint = Color.White)
                }
            }

            if (isAnimated) {
                Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Mascot(MascotMood.HAPPY, size = 120.dp)
                    Spacer(Modifier.height(16.dp))
                    Text("Анимированный аватар", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("GIF и видео загружаются как есть — сервер сам обрежет их по кругу.", color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 6.dp))
                    error?.let { Text(it, color = m.red, modifier = Modifier.padding(top = 12.dp)) }
                }
                return@Column
            }

            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                val side = min(maxWidth.value, maxHeight.value).dp
                val bmp = bitmap
                Box(
                    Modifier
                        .size(side)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoomChange, rotationChange ->
                                touching = true
                                zoom = (zoom * zoomChange).coerceIn(1f, 8f)
                                val limit = viewportPx * 0.5f * zoom
                                offset = Offset((offset.x + pan.x).coerceIn(-limit, limit), (offset.y + pan.y).coerceIn(-limit, limit))
                                freeRotation += rotationChange
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { touching = true; tryAwaitRelease(); touching = false },
                                onDoubleTap = { zoom = if (zoom > 1.5f) 1f else 2f; offset = Offset.Zero },
                            )
                        },
                ) {
                    if (bmp == null) {
                        if (failed) Text("Не удалось открыть изображение", color = Color.White, modifier = Modifier.align(Alignment.Center))
                        else CircularProgressIndicator(Modifier.align(Alignment.Center), color = m.red)
                    } else {
                        val s = state()
                        val paint = remember(s.filter, s.brightness, s.contrast, s.saturation) {
                            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { colorFilter = colorFilter(s) }
                        }
                        Canvas(Modifier.fillMaxSize()) {
                            viewportPx = size.width
                            drawIntoCanvas { it.nativeCanvas.drawBitmap(bmp, buildMatrix(bmp, size.width, s), paint) }
                            val radius = size.width / 2f - 6.dp.toPx()
                            val hole = Path().apply {
                                fillType = PathFillType.EvenOdd
                                addRect(Rect(0f, 0f, size.width, size.height))
                                addOval(Rect(center, radius))
                            }
                            drawPath(hole, Color.Black.copy(alpha = 0.62f))
                            drawCircle(Color.White, radius, center, style = Stroke(2.dp.toPx()))
                            if (touching) {
                                val third = size.width / 3f
                                for (i in 1..2) {
                                    drawLine(Color.White.copy(alpha = 0.35f), Offset(third * i, 0f), Offset(third * i, size.height), 1f)
                                    drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, third * i), Offset(size.width, third * i), 1f)
                                }
                            }
                        }
                    }
                }
            }

            Column(Modifier.fillMaxWidth().background(m.paper).padding(top = 12.dp).verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ToolButton(Icons.Rounded.RotateLeft, "−90°") { scope.launch { rotation.animateTo(rotation.value - 90f, spring(dampingRatio = 0.7f)) } }
                    ToolButton(Icons.Rounded.RotateRight, "+90°") { scope.launch { rotation.animateTo(rotation.value + 90f, spring(dampingRatio = 0.7f)) } }
                    ToolButton(Icons.Rounded.Flip, "Зеркало") { flip = !flip }
                    ToolButton(Icons.Rounded.RestartAlt, "Сброс") {
                        zoom = 1f; offset = Offset.Zero; freeRotation = 0f; flip = false; filter = 0
                        brightness = 0f; contrast = 1f; saturation = 1f
                        scope.launch { rotation.snapTo(0f) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val bmp = bitmap
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(FILTERS) { index, f ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { filter = index }) {
                            Box(
                                Modifier.size(58.dp).clip(CircleShape).background(m.paper3)
                                    .border(if (filter == index) 3.dp else 2.dp, if (filter == index) m.red else m.ink, CircleShape),
                            ) {
                                if (bmp != null) {
                                    val thumbPaint = remember(index) {
                                        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                                            colorFilter = ColorMatrixColorFilter(f.build())
                                        }
                                    }
                                    Canvas(Modifier.fillMaxSize()) {
                                        drawIntoCanvas { it.nativeCanvas.drawBitmap(bmp, buildMatrix(bmp, size.width, EditState(1f, Offset.Zero, 0f, false, 0, 0f, 1f, 1f)), thumbPaint) }
                                    }
                                }
                            }
                            Text(f.name, color = if (filter == index) m.red else m.ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                AdjustSlider("Яркость", brightness, -0.4f..0.4f, { brightness = it }) { "${(it * 250).roundToInt()}" }
                AdjustSlider("Контраст", contrast, 0.6f..1.6f, { contrast = it }) { "${((it - 1f) * 100).roundToInt()}" }
                AdjustSlider("Насыщенность", saturation, 0f..2f, { saturation = it }) { "${((it - 1f) * 100).roundToInt()}" }
                Text(
                    "Щипок — масштаб, два пальца — поворот, двойной тап — приблизить.",
                    color = m.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                error?.let { Text(it, color = m.red, modifier = Modifier.padding(horizontal = 16.dp)) }
                MangaButton(
                    "Сохранить аватар",
                    onClick = {
                        val b = bitmap ?: return@MangaButton
                        val snapshot = state()
                        busy = true
                        scope.launch {
                            val bytes = withContext(Dispatchers.Default) { render(b, viewportPx, snapshot) }
                            upload(bytes, "avatar.jpg", "image/jpeg")
                        }
                    },
                    loading = busy,
                    enabled = bitmap != null,
                    kind = ButtonKind.RED,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val m = Manga.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(CircleShape).clickable(onClick = onClick).padding(6.dp)) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(m.paper2).border(2.dp, m.ink, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = m.ink)
        }
        Text(label, color = m.ink, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun AdjustSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, format: (Float) -> String) {
    val m = Manga.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = m.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(110.dp))
        Slider(
            value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = m.red, activeTrackColor = m.red, inactiveTrackColor = m.paper3),
        )
        Text(format(value), color = m.muted, fontSize = 12.sp, modifier = Modifier.width(40.dp).padding(start = 6.dp))
    }
}

@Suppress("unused")
private fun Modifier.square() = this.aspectRatio(1f)
