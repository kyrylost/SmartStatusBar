package io.github.kyrylost.smartstatusbar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun PlatformStatusBarEffect(config: SmartStatusBarConfig, isOverride: Boolean) = Unit

private val NoRequest: () -> Unit = {}

@Composable
internal actual fun rememberRequestUpdate(): () -> Unit = NoRequest

@Composable
internal actual fun PlatformDebugOverlay(modifier: Modifier) = Unit

@Composable
internal actual fun platformStatusBarBackground(): StatusBarBackground? = null
