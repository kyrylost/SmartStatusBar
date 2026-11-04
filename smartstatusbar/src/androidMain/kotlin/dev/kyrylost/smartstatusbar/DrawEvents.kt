package dev.kyrylost.smartstatusbar

import android.view.View
import android.view.ViewTreeObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

// The ViewTreeObserver is shared by the whole window, so these are the window's draw events, not just this view's.
internal fun View.drawEvents(): Flow<Unit> = callbackFlow {
    val vto = viewTreeObserver
    val listener = ViewTreeObserver.OnPreDrawListener {
        trySend(Unit)
        true
    }
    vto.addOnPreDrawListener(listener)
    awaitClose {
        // The observer captured at the start may be dead by now (the view was reattached): use the current one.
        (if (vto.isAlive) vto else viewTreeObserver).removeOnPreDrawListener(listener)
    }
}.conflate()
