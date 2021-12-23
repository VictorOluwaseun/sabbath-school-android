package app.ss.media.playback.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import app.ss.media.playback.PlaybackConnection

val LocalPlaybackConnection = staticCompositionLocalOf<PlaybackConnection> {
    error("No LocalPlaybackConnection provided")
}
