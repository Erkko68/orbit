package com.orbit.app.feature.item.ui

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import orbit.shared.generated.resources.item_date_format
import orbit.shared.generated.resources.item_month_1
import orbit.shared.generated.resources.item_month_10
import orbit.shared.generated.resources.item_month_11
import orbit.shared.generated.resources.item_month_12
import orbit.shared.generated.resources.item_month_2
import orbit.shared.generated.resources.item_month_3
import orbit.shared.generated.resources.item_month_4
import orbit.shared.generated.resources.item_month_5
import orbit.shared.generated.resources.item_month_6
import orbit.shared.generated.resources.item_month_7
import orbit.shared.generated.resources.item_month_8
import orbit.shared.generated.resources.item_month_9
import orbit.shared.generated.resources.Res
import org.jetbrains.compose.resources.stringResource

/** Month names and date order come from resources, so translations can follow the device language. */
@Composable
internal fun itemDateLabel(date: LocalDate): String {
    val month = when (date.month.ordinal + 1) {
        1 -> Res.string.item_month_1
        2 -> Res.string.item_month_2
        3 -> Res.string.item_month_3
        4 -> Res.string.item_month_4
        5 -> Res.string.item_month_5
        6 -> Res.string.item_month_6
        7 -> Res.string.item_month_7
        8 -> Res.string.item_month_8
        9 -> Res.string.item_month_9
        10 -> Res.string.item_month_10
        11 -> Res.string.item_month_11
        12 -> Res.string.item_month_12
        else -> error("Invalid month")
    }
    return stringResource(Res.string.item_date_format, stringResource(month), date.day, date.year)
}
