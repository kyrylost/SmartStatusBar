# Changelog

## 0.1.0

First release.

- `SmartStatusBarEffect`: chooses light or dark status bar icons from the pixels under the status bar, captured with `PixelCopy`, including `SurfaceView` content.
- Update strategies: `Debounce` (default, with a bounded wait for endless animations), `Periodic` and `Manual`.
- `SmartStatusBarOverride`: a screen takes over from the app-wide effect while it is shown, wherever either is declared; the app-wide effect resumes when it leaves.
- `rememberSmartStatusBarController()` for manual checks, addressed to the window.
- `SmartStatusBarDebugOverlay` for tuning and diagnosing.
- `currentStatusBarBackground()`: what the last check found, for content of your own under the status bar.
- `SmartStatusBar.install` and `SmartStatusBar.override` for apps built with Views and Fragments.
- Does nothing on API 23–25.
- Compose Multiplatform: the API is available in common code and does nothing outside Android (iOS, macOS, desktop JVM, js, wasmJs).
- Depends on Compose Multiplatform 1.7 (Jetpack Compose 1.7.1 on Android), Lifecycle 2.8 and Core 1.12 or newer, without forcing a BOM on apps.
