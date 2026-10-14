package com.theveloper.pixelplay.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** Shared Haze state. Null means glass/blur is disabled (e.g. "disable blur" setting). */
val LocalHazeState = compositionLocalOf<HazeState?> { null }

/**
 * Frosted-glass backdrop: blurs whatever the app content draws behind this node and
 * overlays a translucent [tint]. No-op when [LocalHazeState] is null or [enabled] is false.
 */
@Composable
fun Modifier.glassBackdrop(
    tint: Color,
    radius: Dp = 24.dp,
    enabled: Boolean = true,
): Modifier {
    val state = LocalHazeState.current ?: return this
    // A Haze effect with radius 0 still repaints the captured backdrop unblurred, so when
    // the glass is off the modifier must not be applied at all.
    if (!enabled) return this
    return this.hazeEffect(state = state) {
        blurRadius = radius
        tints = listOf(HazeTint(tint))
    }
}

/**
 * Glass backdrop bound to a screen-local [state] (the screen's scrolling content is the
 * hazeSource). No-op when [state] is null or the global glass setting is off.
 */
@Composable
fun Modifier.glassBackdropWith(
    state: HazeState?,
    tint: Color,
    radius: Dp = 24.dp,
): Modifier {
    if (state == null || LocalHazeState.current == null) return this
    return this.hazeEffect(state = state) {
        blurRadius = radius
        tints = listOf(HazeTint(tint))
    }
}
