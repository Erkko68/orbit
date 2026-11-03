package com.orbit.app.core

import platform.Foundation.NSBundle

actual fun deviceLanguageCode(): String? = NSBundle.mainBundle.preferredLocalizations.firstOrNull() as? String
