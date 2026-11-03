package com.orbit.app.feature.item.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.orbit.app.core.designsystem.component.OrbitFilterChip
import com.orbit.app.core.designsystem.component.OrbitTextField
import com.orbit.app.core.designsystem.component.OrbitTonalButton
import com.orbit.app.core.designsystem.theme.OrbitSpacing
import com.orbit.app.feature.item.CustomDuration
import com.orbit.app.feature.item.DurationUnit
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.duration_amount
import org.jetbrains.compose.resources.stringResource

/** So many minutes, hours or days, and the button named [addText] that uses them. */
@Composable
internal fun CustomDurationFields(
    custom: CustomDuration,
    onAmountChange: (String) -> Unit,
    onUnitSelect: (DurationUnit) -> Unit,
    addText: String,
    onAdd: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(OrbitSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OrbitTextField(
            value = custom.amount,
            onValueChange = onAmountChange,
            placeholder = stringResource(Res.string.duration_amount),
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        for (unit in DurationUnit.entries) {
            OrbitFilterChip(
                label = stringResource(unit.label),
                selected = unit == custom.unit,
                onClick = { onUnitSelect(unit) },
            )
        }
    }
    OrbitTonalButton(addText, onAdd, Modifier.fillMaxWidth(), enabled = custom.length != null)
}
