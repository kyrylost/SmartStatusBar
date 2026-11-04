package dev.kyrylost.smartstatusbar

import android.view.Window
import androidx.annotation.MainThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.WeakHashMap

/**
 * What the effects, [SmartStatusBarController]s and [SmartStatusBarDebugOverlay]s of one window share:
 * which effect is active, manual check requests and what the active effect last saw.
 *
 * [active] and [background] are flows rather than snapshot state so that apps without Compose
 * ([SmartStatusBar]) can observe them too.
 *
 * Main thread only.
 */
internal class WindowState {
    /** A [SmartStatusBarEffect] or [SmartStatusBarOverride] in the composition. */
    class Effect(val isOverride: Boolean)

    private val effects = mutableListOf<Effect>()

    // What the window showed before the first effect attached.
    private var original = false

    private val activeFlow = MutableStateFlow<Effect?>(null)

    /**
     * The effect that checks: the most recently attached override, or without overrides the most
     * recently attached effect. The others pause until it leaves.
     */
    val active: StateFlow<Effect?> = activeFlow.asStateFlow()

    private val backgroundFlow = MutableStateFlow<StatusBarBackground?>(null)

    /** What the last check found, or null before the first one and once the last effect has left. */
    val background: StateFlow<StatusBarBackground?> = backgroundFlow.asStateFlow()

    /** [appearance] is the window's `isAppearanceLightStatusBars` right now. */
    fun attach(effect: Effect, appearance: Boolean) {
        if (effects.isEmpty()) original = appearance
        effects += effect
        activeFlow.value = effects.lastOrNull { it.isOverride } ?: effects.last()
    }

    /** Returns the appearance to restore once the last effect has left, otherwise null. */
    fun detach(effect: Effect): Boolean? {
        val wasActive = activeFlow.value === effect
        if (!effects.remove(effect)) return null
        activeFlow.value = effects.lastOrNull { it.isOverride } ?: effects.lastOrNull()
        if (effects.isEmpty()) {
            backgroundFlow.value = null
            return original
        }
        // The effect that resumes checks what is on screen now, whatever its strategy.
        if (wasActive) requestUpdate()
        return null
    }

    /** [lightAppearance] is the `isAppearanceLightStatusBars` the active effect has decided on. */
    fun recordDecision(lightAppearance: Boolean) {
        backgroundFlow.value = if (lightAppearance) StatusBarBackground.Light else StatusBarBackground.Dark
    }

    // Conflated and unbound to any collector: a request made before the effect starts is not lost.
    private val requestChannel: Channel<Unit> = Channel(Channel.CONFLATED)

    /** Manual check requests; collected by the active effect. */
    val requests: Flow<Unit> = requestChannel.receiveAsFlow()

    fun requestUpdate() {
        requestChannel.trySend(Unit)
    }

    /** What the active effect last saw; only published while a [SmartStatusBarDebugOverlay] is attached. */
    var snapshot: DebugSnapshot? by mutableStateOf(null)
        private set

    private var overlays = 0

    val isDebugObserved: Boolean get() = overlays > 0

    fun publish(next: DebugSnapshot) {
        // Publishing makes the overlay redraw, which triggers another check: only publish real changes.
        if (snapshot?.sameAs(next) != true) snapshot = next
    }

    /** Returns the function that detaches the overlay again. */
    fun attachOverlay(): () -> Unit {
        overlays++
        return {
            overlays--
            if (overlays == 0) snapshot = null
        }
    }
}

// Weak keys: a WindowState holds no reference to its window.
private val states = WeakHashMap<Window, WindowState>()

@MainThread
internal fun Window.smartStatusBarState(): WindowState = synchronized(states) {
    states.getOrPut(this) { WindowState() }
}
