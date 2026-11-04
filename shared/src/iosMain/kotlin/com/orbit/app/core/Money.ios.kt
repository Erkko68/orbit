package com.orbit.app.core

import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSLocale
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle
import platform.Foundation.currentLocale

actual fun deviceMoneyLocale(): MoneyLocale = NSNumberFormatter().let {
    it.locale = NSLocale.currentLocale
    MoneyLocale(requireNotNull(it.decimalSeparator), requireNotNull(it.groupingSeparator))
}

actual fun formatEuro(cents: Long): String {
    val decimal = NSDecimalNumber(string = cents.toString()).decimalNumberByDividingBy(NSDecimalNumber(string = "100"))
    return requireNotNull(NSNumberFormatter().apply {
        locale = NSLocale.currentLocale
        numberStyle = NSNumberFormatterCurrencyStyle
        currencyCode = "EUR"
        minimumFractionDigits = 2u
        maximumFractionDigits = 2u
    }.stringFromNumber(decimal))
}
