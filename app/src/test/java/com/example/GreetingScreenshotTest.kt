package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.EpisodeEntity
import com.example.data.model.ZoneEntity
import com.example.domain.SmartPickResult
import com.example.service.PlaybackState
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.KaamelottNuitTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleEpisode = EpisodeEntity(
        id = "ep1",
        title = "01 - Kaamelott Livre I - Tome 1",
        rawFileName = "01 - Kaamelott Livre I - Tome 1 [b05Scfhi0dU].webm",
        uriString = "content://sample/1",
        durationMs = 2_700_000L,
        listenCount = 1
    )
    val sampleZone = ZoneEntity(
        id = "ep1_zone_0",
        episodeId = "ep1",
        zoneIndex = 0,
        startTimeMs = 0L,
        endTimeMs = 510_000L
    )

    composeTestRule.setContent {
        KaamelottNuitTheme {
            HomeScreen(
                episodes = listOf(sampleEpisode),
                allZones = listOf(sampleZone),
                recentSessions = emptyList(),
                selectedFolderUri = "content://sample",
                isScanning = false,
                selectedTimerMinutes = 30,
                playbackState = PlaybackState(),
                smartPickPreview = SmartPickResult(
                    episode = sampleEpisode,
                    targetZone = sampleZone,
                    startPositionMs = 0L,
                    reason = "Zone la moins écoutée"
                ),
                feedbackMessage = null,
                onFolderSelected = {},
                onRescanFolder = {},
                onMinutesSelected = {},
                onAdjustMinutes = {},
                onLaunchSmartPlay = {},
                onPlayEpisode = { _, _ -> },
                onPlaySpecificZone = { _, _ -> },
                onToggleZoneAvoid = { _, _, _ -> },
                onTogglePlayPause = {},
                onOpenNightMode = {},
                onDismissFeedback = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
