package com.orbit.app.core.share

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

/** Shares through the iOS share sheet, shown over whatever is on screen. */
class IosTextSharer : TextSharer {
    override fun share(text: String) {
        val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        topViewController()?.presentViewController(sheet, animated = true, completion = null)
    }

    private fun topViewController(): UIViewController? {
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (top?.presentedViewController != null) {
            top = top.presentedViewController
        }
        return top
    }
}
