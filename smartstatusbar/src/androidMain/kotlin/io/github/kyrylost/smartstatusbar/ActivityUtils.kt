package io.github.kyrylost.smartstatusbar

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.Window
import androidx.compose.ui.window.DialogWindowProvider

internal fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

/**
 * The window [this] view is drawn in: the dialog's own window inside a `Dialog` or a bottom sheet,
 * otherwise the Activity's window.
 */
internal fun View.findWindow(): Window? =
    (parent as? DialogWindowProvider)?.window ?: context.findActivity()?.window
