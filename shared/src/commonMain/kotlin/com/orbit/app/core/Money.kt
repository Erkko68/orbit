package com.orbit.app.core

/** Regional separators come from the device, independently of the app's translated language. */
data class MoneyLocale(val decimal: String, val grouping: String)

expect fun deviceMoneyLocale(): MoneyLocale

/** Euros until space currencies arrive (#111). Native formatters receive exact decimals, never Double. */
expect fun formatEuro(cents: Long): String

const val EURO = "€"

/** Whether the device writes the euro sign before an amount (€12.40) or after it (12,40 €), as [formatEuro] does. */
fun euroBeforeAmount(formatted: String = formatEuro(0)): Boolean = formatted.trimStart().startsWith(EURO)

/** Plain editing value, without currency or grouping, so it can be safely edited and parsed again. */
fun moneyInput(cents: Long, locale: MoneyLocale): String {
    require(cents >= 0)
    return "${cents / 100}${locale.decimal}${(cents % 100).toString().padStart(2, '0')}"
}

/** Rejects malformed grouping, fractions of a cent, signs and overflow rather than rounding money. */
fun parseMoney(text: String, locale: MoneyLocale): Long? {
    val parts = text.trim().split(locale.decimal)
    if (parts.size > 2) return null
    val whole = parts[0]
    val groups = if (locale.grouping.isNotEmpty()) whole.split(locale.grouping) else listOf(whole)
    if (groups.size > 1 && (groups.first().length !in 1..3 || groups.drop(1).any { it.length != 3 })) return null
    val digits = groups.joinToString("")
    val fraction = parts.getOrNull(1).orEmpty()
    if (digits.isEmpty() || digits.any { it.digitToIntOrNull() == null } || fraction.length > 2 || fraction.any { it.digitToIntOrNull() == null }) return null
    return (digits + fraction.padEnd(2, '0')).map { it.digitToInt() }.joinToString("").toLongOrNull()
}
