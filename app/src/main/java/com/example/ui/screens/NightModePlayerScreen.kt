package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.PlaybackState
import com.example.ui.components.SleepTimerSlider
import com.example.ui.components.formatDuration
import com.example.ui.theme.AvoidRed
import com.example.ui.theme.NightSleepBlack
import com.example.ui.theme.NightSleepTextDim
import com.example.ui.theme.NightSleepTextGlow
import com.example.ui.theme.SoftGold
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun NightModePlayerScreen(
    playbackState: PlaybackState,
    onAddFiveMin: () -> Unit,
    onSetTimerMinutes: (Int) -> Unit,
    onStopSession: () -> Unit,
    onLongPressToggleAvoid: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onExitClick()
    }

    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            delay(2000)
            feedbackMessage = null
        }
    }

    val episode = playbackState.currentEpisode
    val zone = playbackState.currentZone
    val remainingMs = playbackState.sleepTimerRemainingMs
    val currentMinutes = if (remainingMs > 0) {
        ((remainingMs + 30_000L) / (60 * 1000L)).toInt().coerceIn(1, 180)
    } else {
        30
    }
    val isFadingOut = remainingMs in 1..60_000L

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NightSleepBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("night_mode_screen")
    ) {
        // Top Bar (Ultra-dim)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (playbackState.isPlaying) NightSleepTextGlow else NightSleepTextDim,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mode Nuit Kaamelott",
                    color = NightSleepTextDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Discreet exit back to navigation
            IconButton(
                onClick = onExitClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("night_mode_close_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Quitter le mode nuit",
                    tint = NightSleepTextDim,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Center Sleep Display & Interactive Adjuster
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sleep Timer Countdown (Very soft slate/gold)
            Text(
                text = formatCountdown(remainingMs),
                color = if (isFadingOut) SoftGold.copy(alpha = 0.6f) else NightSleepTextGlow,
                fontSize = 48.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp,
                modifier = Modifier.testTag("night_timer_countdown")
            )

            if (isFadingOut) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Fondu sonore en cours…",
                    color = SoftGold.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Episode Title (Subdued)
            Text(
                text = episode?.title ?: "Kaamelott",
                color = NightSleepTextDim,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Current Zone Info
            if (zone != null) {
                Text(
                    text = "Zone ${zone.zoneIndex + 1} (${formatDuration(zone.startTimeMs)} - ${formatDuration(zone.endTimeMs)})${if (zone.isAvoided) " • [ÉVITÉE]" else ""}",
                    color = if (zone.isAvoided) AvoidRed.copy(alpha = 0.6f) else NightSleepTextDim.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Horizontal Roulette / Slider to drag left or right to seamlessly adjust sleep time
            SleepTimerSlider(
                currentMinutes = currentMinutes,
                onMinutesChanged = { newMin ->
                    onSetTimerMinutes(newMin)
                    feedbackMessage = "Minuteur réglé à $newMin min"
                },
                isNightMode = true,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Feedback toast
            AnimatedVisibility(
                visible = feedbackMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(NightSleepTextDim.copy(alpha = 0.25f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = feedbackMessage ?: "",
                        color = NightSleepTextGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bottom Controls Bar (Immediate 1-tap buttons)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Button (1-tap reliable response)
                Surface(
                    onClick = onTogglePlayPause,
                    shape = RoundedCornerShape(14.dp),
                    color = if (playbackState.isPlaying) NightSleepTextDim.copy(alpha = 0.18f) else SoftGold.copy(alpha = 0.25f),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("night_play_pause_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Reprendre",
                            tint = if (playbackState.isPlaying) NightSleepTextDim else SoftGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (playbackState.isPlaying) "Pause" else "Reprendre",
                            color = if (playbackState.isPlaying) NightSleepTextDim else SoftGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Add 5 min button
                Surface(
                    onClick = {
                        onAddFiveMin()
                        feedbackMessage = "+5 minutes ajoutées"
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = NightSleepTextDim.copy(alpha = 0.15f),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("night_add_5_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreTime,
                            contentDescription = "+5 min",
                            tint = NightSleepTextDim,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+5 min",
                            color = NightSleepTextDim,
                            fontSize = 12.sp
                        )
                    }
                }

                // Toggle avoid zone button
                Surface(
                    onClick = {
                        onLongPressToggleAvoid()
                        feedbackMessage = if (zone?.isAvoided == true) "Zone réactivée" else "Zone marquée à éviter"
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (zone?.isAvoided == true) AvoidRed.copy(alpha = 0.2f) else NightSleepTextDim.copy(alpha = 0.15f),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("night_avoid_zone_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Éviter zone",
                            tint = if (zone?.isAvoided == true) AvoidRed else NightSleepTextDim,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (zone?.isAvoided == true) "Évitée" else "Éviter",
                            color = if (zone?.isAvoided == true) AvoidRed else NightSleepTextDim,
                            fontSize = 12.sp
                        )
                    }
                }

                // Stop & Power off
                Surface(
                    onClick = onStopSession,
                    shape = RoundedCornerShape(14.dp),
                    color = NightSleepTextDim.copy(alpha = 0.15f),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("night_stop_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Éteindre",
                            tint = NightSleepTextDim,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Glissez la roulette pour ajuster le temps • 1 clic pour reprendre",
                color = NightSleepTextDim.copy(alpha = 0.5f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatCountdown(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
