package com.orbit.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.orbit.app.core.designsystem.theme.Ink
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.core.designsystem.theme.OrbitTheme
import kotlin.math.abs
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private const val NUMBERS = 100
private const val DIGITS = 2
private const val AWAY_SCALE = 0.75f
private const val AWAY_ALPHA = 0.55f
private val NumberHeight = 40.dp
private val WheelWidth = 60.dp
private val WheelHeight = NumberHeight * 3

/**
 * A length of time on two small wheels, from 0 to 99 hours and 0 to 99 minutes, such as how long something takes. A
 * wheel scrolls and snaps to a number. Tapping a number goes to it and opens the keyboard, and what is typed moves the
 * wheel. The numbers around the one picked are smaller and fainter. What is picked is the caller's state. [hoursLabel] and [minutesLabel] name the wheels.
 */
@Composable
fun OrbitTimeWheel(
    hours: Int,
    minutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    hoursLabel: String,
    minutesLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
        Wheel(hours, hoursLabel, onHoursChange)
        Text(
            text = ":",
            modifier = Modifier.height(WheelHeight).wrapContentHeight(),
            style = OrbitTheme.data.large,
        )
        Wheel(minutes, minutesLabel, onMinutesChange)
    }
}

/**
 * The numbers lie over the one field of the wheel, in the band in the middle. At rest the field shows the number picked,
 * and the numbers take over while they move. So the wheel gets every drag and the keyboard stays up as it turns.
 */
@Composable
private fun Wheel(value: Int, label: String, onValueChange: (Int) -> Unit) {
    val pager = rememberPagerState(initialPage = value) { NUMBERS }
    val scope = rememberCoroutineScope()
    val requester = remember { FocusRequester() }
    val onSettled by rememberUpdatedState(onValueChange)
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.drop(1).collect { onSettled(it) }
    }
    LaunchedEffect(value) {
        if (value != pager.settledPage) pager.scrollToPage(value)
    }
    val turning = pager.isScrollInProgress
    Column(Modifier.width(WheelWidth), verticalArrangement = Arrangement.spacedBy(OrbitSpacing.xs)) {
        Box(Modifier.fillMaxWidth().height(WheelHeight), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(NumberHeight)
                    .background(OrbitTheme.accent.vivid, MaterialTheme.shapes.medium),
            )
            WheelField(
                number = pager.currentPage,
                label = label,
                onValueChange = onValueChange,
                modifier = Modifier.focusRequester(requester).alpha(if (turning) 0f else 1f),
            )
            VerticalPager(
                state = pager,
                modifier = Modifier.fillMaxSize().nestedScroll(StopAtTheEnds).clearAndSetSemantics {},
                contentPadding = PaddingValues(vertical = NumberHeight),
                pageSize = PageSize.Fixed(NumberHeight),
                flingBehavior = PagerDefaults.flingBehavior(pager, pagerSnapDistance = PagerSnapDistance.atMost(NUMBERS)),
            ) { number ->
                val picked = number == pager.currentPage
                Box(
                    modifier = Modifier.fillMaxSize().clickable(interactionSource = null, indication = null) {
                        scope.launch { pager.animateScrollToPage(number) }
                        requester.requestFocus()
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = number.twoDigits(),
                        modifier = Modifier.graphicsLayer {
                            val away = abs(number - pager.currentPage - pager.currentPageOffsetFraction).coerceAtMost(1f)
                            scaleX = lerp(1f, AWAY_SCALE, away)
                            scaleY = scaleX
                            alpha = if (picked && !turning) 0f else lerp(1f, AWAY_ALPHA, away)
                        },
                        style = OrbitTheme.data.large,
                        color = if (picked) Ink else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Always [number], with the cursor after it: each digit typed comes in on the right and pushes the first one out. */
@Composable
private fun WheelField(number: Int, label: String, onValueChange: (Int) -> Unit, modifier: Modifier) {
    val focus = LocalFocusManager.current
    val text = number.twoDigits()
    BasicTextField(
        value = TextFieldValue(text, TextRange(text.length)),
        onValueChange = { typed ->
            val typedNumber = typed.text.filter(Char::isDigit).takeLast(DIGITS).toIntOrNull() ?: 0
            if (typedNumber != number) onValueChange(typedNumber)
        },
        modifier = modifier.fillMaxWidth().semantics { contentDescription = label },
        textStyle = OrbitTheme.data.large.copy(color = Ink, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(Ink),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        singleLine = true,
    )
}

private fun Int.twoDigits() = toString().padStart(DIGITS, '0')

/** A wheel at its first or last number stops there, and the drag does not go on to move the sheet around it. */
private val StopAtTheEnds = object : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) = available

    override suspend fun onPostFling(consumed: Velocity, available: Velocity) = available
}
