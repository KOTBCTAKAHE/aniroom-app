package space.aniroom.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.aniroom.app.ui.theme.BodyFont
import space.aniroom.app.ui.theme.Manga

@Composable
fun MangaField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "",
    icon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 5,
    trailing: (@Composable () -> Unit)? = null,
    fieldModifier: Modifier = Modifier,
) {
    val m = Manga.colors
    var focused by remember { mutableStateOf(false) }
    val shadow by animateDpAsState(if (focused) 4.dp else 2.dp, label = "fieldShadow")
    Column(modifier) {
        if (label != null) {
            Text(label, color = m.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.padding(top = 6.dp))
        }
        MangaPanel(Modifier.fillMaxWidth(), color = m.paper2, radius = 14.dp, shadow = shadow) {
            Row(
                Modifier.heightIn(min = 52.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(icon, null, tint = if (focused) m.red else m.muted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, color = m.muted, fontSize = 16.sp, fontFamily = BodyFont, maxLines = 1)
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = singleLine,
                        maxLines = maxLines,
                        textStyle = TextStyle(color = m.ink, fontSize = 16.sp, fontFamily = BodyFont),
                        cursorBrush = SolidColor(m.red),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        visualTransformation = visualTransformation,
                        modifier = fieldModifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                    )
                }
                if (trailing != null) trailing()
            }
        }
    }
}
