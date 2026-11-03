package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitShape
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** How big an avatar is: [Small] beside a line of text, [Medium] by itself, [Large] on a row, [Hero] on the turn card. */
enum class OrbitAvatarSize(internal val size: Dp) {
    Small(28.dp),
    Medium(40.dp),
    Large(48.dp),
    Hero(60.dp),
}

private val OrbitAvatarSize.style: TextStyle
    @Composable @ReadOnlyComposable
    get() = when (this) {
        OrbitAvatarSize.Small -> MaterialTheme.typography.labelSmall
        OrbitAvatarSize.Medium -> MaterialTheme.typography.labelLarge
        OrbitAvatarSize.Large -> MaterialTheme.typography.titleSmall
        OrbitAvatarSize.Hero -> MaterialTheme.typography.titleLarge
    }

private const val ICON_FRACTION = 0.56f
private val RingWidth = 3.dp

private val MemberShapes = listOf(
    OrbitShape.Cookie,
    OrbitShape.Clover,
    OrbitShape.Sunny,
    OrbitShape.Burst,
    OrbitShape.Flower,
    OrbitShape.Cookie12,
)

/** The shape of the member at [index] of a space: with their colour, it tells people apart without relying on colour. */
fun memberShape(index: Int): OrbitShape = MemberShapes[index.mod(MemberShapes.size)]

/**
 * A member's photo, or their [initials] until it loads or when there is none, on a solid [accent], usually
 * `OrbitTheme.colors.spaceAccent(…)`, where the text is ink. [shape] is a circle, or [memberShape] on a list. With
 * [icon] it stands for no one in particular, such as a way to add someone, and shows that instead of initials. A [ring]
 * colour draws a border right around the shape, for someone picked.
 */
@Composable
fun OrbitAvatar(
    initials: String,
    accent: Color,
    modifier: Modifier = Modifier,
    photoUrl: String? = null,
    contentColor: Color = Ink,
    shape: Shape = CircleShape,
    size: OrbitAvatarSize = OrbitAvatarSize.Medium,
    icon: DrawableResource? = null,
    ring: Color = Color.Transparent,
) {
    Box(
        modifier = modifier
            .size(size.size)
            .drawBehind {
                if (ring.alpha == 0f) return@drawBehind
                val reach = RingWidth.toPx()
                val outer = Size(this.size.width + reach * 2, this.size.height + reach * 2)
                translate(-reach, -reach) { drawOutline(shape.createOutline(outer, layoutDirection, this), ring) }
            }
            .background(accent, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.fillMaxSize(ICON_FRACTION), tint = contentColor)
        } else {
            Text(text = initials, style = size.style, color = contentColor, maxLines = 1)
        }
        if (photoUrl != null) {
            // Drawn over the initials, which show through while it loads or if it fails.
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(shape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
