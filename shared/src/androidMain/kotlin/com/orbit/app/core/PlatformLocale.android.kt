package com.orbit.app.core

import java.util.Locale

actual fun deviceLanguageCode(): String? = Locale.getDefault().language
