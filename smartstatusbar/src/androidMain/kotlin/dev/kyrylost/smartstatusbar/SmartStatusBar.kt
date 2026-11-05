package dev.kyrylost.smartstatusbar

import android.app.Activity
import android.os.Build
import android.view.Choreographer
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import androidx.compose.ui.unit.Density
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * SmartStatusBar for apps built with Views (XML layouts, Fragments) rather than Compose. It does
 * what [SmartStatusBarEffect] and [SmartStatusBarOverride] do, and shares their window: effects and
 * overrides from both worlds take turns by the same rules.
 *
 * ```kotlin
 * class MainActivity : AppCompatActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         enableEdgeToEdge()
 *         super.onCreate(savedInstanceState)
 *         setContentView(R.layout.activity_main)
 *         SmartStatusBar.install(this)
 *     }
 * }
 * ```
 *
 * Each call attaches right away and detaches when its lifecycle is destroyed (or on
 * [SmartStatusBarHandle.remove]); checks run while the lifecycle is at least STARTED. Call from the
 * main thread. Like the composables, it does nothing below API 26.
 */
public object SmartStatusBar {

    /** Checks [activity]'s window until the activity is destroyed, like [SmartStatusBarEffect]. */
    @JvmStatic
    @JvmOverloads
    public fun <A> install(
        activity: A,
        config: SmartStatusBarConfig = SmartStatusBarConfig(),
    ): SmartStatusBarHandle where A : Activity, A : LifecycleOwner =
        attach(activity.window, activity, config, isOverride = false)

    /**
     * Checks [window] until [lifecycleOwner] is destroyed, like [SmartStatusBarEffect]. For a single
     * Fragment, pass `requireActivity().window` and its `viewLifecycleOwner`.
     */
    @JvmStatic
    @JvmOverloads
    public fun install(
        window: Window,
        lifecycleOwner: LifecycleOwner,
        config: SmartStatusBarConfig = SmartStatusBarConfig(),
    ): SmartStatusBarHandle = attach(window, lifecycleOwner, config, isOverride = false)

    /**
     * Takes over from [install] with a different [config] until [lifecycleOwner] is destroyed, like
     * [SmartStatusBarOverride]: for a Fragment, pass `requireActivity().window` and its
     * `viewLifecycleOwner`.
     */
    @JvmStatic
    public fun override(
        window: Window,
        lifecycleOwner: LifecycleOwner,
        config: SmartStatusBarConfig,
    ): SmartStatusBarHandle = attach(window, lifecycleOwner, config, isOverride = true)

    private fun attach(
        window: Window,
        owner: LifecycleOwner,
        config: SmartStatusBarConfig,
        isOverride: Boolean,
    ): SmartStatusBarHandle {
        val state = window.smartStatusBarState()
        // PixelCopy of a window needs API 26; below that the library leaves the icons alone.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            return SmartStatusBarHandle(state) {}
        }

        val view = window.decorView
        val insetsController = WindowCompat.getInsetsController(window, view)
        val effect = WindowState.Effect(isOverride)
        state.attach(effect, insetsController.isAppearanceLightStatusBars)

        val host = CheckHost(
            window = window,
            view = view,
            density = Density(view.context),
            statusBarTop = view::statusBarTop,
            insetsChanges = view.statusBarTopChanges(),
            awaitFrame = ::awaitChoreographerFrame,
        )
        val checks = owner.lifecycleScope.launch {
            // Another effect is active in this window (an override, or an effect attached later): pause until it leaves.
            state.active.map { it === effect }.distinctUntilChanged().collectLatest { active ->
                if (active) runChecks(host, state, config, owner.lifecycle)
            }
        }

        var removed = false
        lateinit var observer: LifecycleEventObserver
        val remove = {
            if (!removed) {
                removed = true
                checks.cancel()
                owner.lifecycle.removeObserver(observer)
                state.detach(effect)?.let { insetsController.isAppearanceLightStatusBars = it }
            }
        }
        observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_DESTROY) remove() }
        owner.lifecycle.addObserver(observer)
        return SmartStatusBarHandle(state, remove)
    }
}

/** An effect or override attached with [SmartStatusBar]. */
public class SmartStatusBarHandle internal constructor(
    private val state: WindowState,
    private val onRemove: () -> Unit,
) {
    /**
     * What the active effect of the window last found under the status bar, like
     * [currentStatusBarBackground]: null before the first check and once the last effect has left.
     */
    public val background: StateFlow<StatusBarBackground?> get() = state.background

    /** Requests a check, like [SmartStatusBarController.requestUpdate]. */
    public fun requestUpdate() {
        state.requestUpdate()
    }

    /** Detaches now instead of when the lifecycle is destroyed. Further calls do nothing. */
    public fun remove() {
        onRemove()
    }
}

private fun View.statusBarTop(): Int =
    ViewCompat.getRootWindowInsets(this)?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0

// Insets reach the views through a layout pass, so a change of the status bar height shows up there.
private fun View.statusBarTopChanges(): Flow<Unit> = callbackFlow {
    val listener = ViewTreeObserver.OnGlobalLayoutListener { trySend(statusBarTop()) }
    val vto = viewTreeObserver
    vto.addOnGlobalLayoutListener(listener)
    awaitClose { (if (vto.isAlive) vto else viewTreeObserver).removeOnGlobalLayoutListener(listener) }
}.onStart { emit(statusBarTop()) }.distinctUntilChanged().drop(1).filter { it > 0 }.map { }

private suspend fun awaitChoreographerFrame() {
    // Choreographer instances are per thread: keep the main thread's for the cancellation handler.
    val choreographer = Choreographer.getInstance()
    suspendCancellableCoroutine { cont ->
        val callback = Choreographer.FrameCallback { if (cont.isActive) cont.resume(Unit) }
        choreographer.postFrameCallback(callback)
        cont.invokeOnCancellation { choreographer.removeFrameCallback(callback) }
    }
}
