package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.more_count
import org.jetbrains.compose.resources.stringResource

/**
 * The members of a group as overlapping avatars, one per entry of [initials]. Past [max] of them the
 * rest are counted in a last avatar, such as "+3".
 */
@Composable
fun OrbitAvatarStack(initials: List<String>, modifier: Modifier = Modifier, max: Int = 4) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(-OrbitSpacing.sm)) {
        initials.take(max).forEachIndexed { index, memberInitials ->
            OrbitAvatar(memberInitials, OrbitTheme.colors.spaceAccent(index))
        }
        val hidden = initials.size - max
        if (hidden > 0) {
            OrbitAvatar(stringResource(Res.string.more_count, hidden), MaterialTheme.colorScheme.outline)
        }
    }
}
