package com.orbit.app.core

import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

actual fun deviceMoneyLocale(): MoneyLocale = DecimalFormatSymbols.getInstance(Locale.getDefault()).let {
    MoneyLocale(it.decimalSeparator.toString(), it.groupingSeparator.toString())
}

actual fun formatEuro(cents: Long): String = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
    currency = Currency.getInstance("EUR")
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}.format(BigDecimal.valueOf(cents, 2))
