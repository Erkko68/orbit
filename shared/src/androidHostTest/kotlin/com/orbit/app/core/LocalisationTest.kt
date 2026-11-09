package com.orbit.app.core

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import com.orbit.app.core.designsystem.ComposeTest
import com.orbit.app.core.designsystem.theme.OrbitTheme
import com.orbit.app.feature.item.ui.itemDateLabel
import kotlinx.datetime.LocalDate
import orbit.shared.generated.resources.Res
import orbit.shared.generated.resources.duration_days
import orbit.shared.generated.resources.sign_in
import orbit.shared.generated.resources.expenses_receives
import orbit.shared.generated.resources.expenses_pays
import orbit.shared.generated.resources.expenses_even
import orbit.shared.generated.resources.expenses_who_pays
import orbit.shared.generated.resources.expenses_who_receives
import orbit.shared.generated.resources.expenses_settlement_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class LocalisationTest : ComposeTest() {
    @Test
    @Config(qualifiers = "en-rUS")
    fun english() = assertLanguage(
        "Sign in",
        "1 day",
        "2 days",
        "en",
        "Jan",
        listOf("To receive", "To pay", "Settled up", "Who pays", "Who receives", "Reimbursement")
    )

    @Test
    @Config(qualifiers = "es-rES")
    fun spanish() = assertLanguage(
        "Iniciar sesión",
        "1 día",
        "2 días",
        "es",
        "ene",
        listOf("Por recibir", "Por pagar", "Al día", "Quién paga", "Quién recibe", "Reembolso")
    )

    @Test
    @Config(qualifiers = "ca-rES")
    fun catalan() = assertLanguage(
        "Inicia la sessió",
        "1 dia",
        "2 dies",
        "ca",
        "gen",
        listOf("Per rebre", "Per pagar", "Al dia", "Qui paga", "Qui rep", "Reemborsament")
    )

    @Test
    @Config(qualifiers = "it-rIT")
    fun italian() = assertLanguage(
        "Accedi",
        "1 giorno",
        "2 giorni",
        "it",
        "gen",
        listOf("Da ricevere", "Da pagare", "In pari", "Chi paga", "Chi riceve", "Rimborso")
    )

    @Test
    @Config(qualifiers = "fr-rFR")
    fun unsupportedLanguageUsesEnglish() = assertLanguage(
        "Sign in",
        "1 day",
        "2 days",
        "en",
        "janv",
        listOf("To receive", "To pay", "Settled up", "Who pays", "Who receives", "Reimbursement")
    )

    private fun assertLanguage(
        action: String,
        one: String,
        two: String,
        language: String,
        month: String,
        expenses: List<String>
    ) =
        runComposeUiTest {
            assertEquals(language, deviceLanguage())
            setContent {
                OrbitTheme {
                    Column {
                        Text(stringResource(Res.string.sign_in))
                        Text(pluralStringResource(Res.plurals.duration_days, 1, 1))
                        Text(pluralStringResource(Res.plurals.duration_days, 2, 2))
                        Text(itemDateLabel(LocalDate(2026, 1, 12)))
                        Text(stringResource(Res.string.expenses_receives))
                        Text(stringResource(Res.string.expenses_pays))
                        Text(stringResource(Res.string.expenses_even))
                        Text(stringResource(Res.string.expenses_who_pays))
                        Text(stringResource(Res.string.expenses_who_receives))
                        Text(stringResource(Res.string.expenses_settlement_title))
                    }
                }
            }
            onNodeWithText(action).assertIsDisplayed()
            onNodeWithText(one).assertIsDisplayed()
            onNodeWithText(two).assertIsDisplayed()
            expenses.forEach { onNodeWithText(it).assertIsDisplayed() }
            // Punctuation and abbreviations vary with the OS's locale data; the date and month must not.
            onNodeWithText("2026", substring = true)
                .assertIsDisplayed()
                .assertTextContains("12", substring = true)
                .assertTextContains(month, substring = true)
        }
}
