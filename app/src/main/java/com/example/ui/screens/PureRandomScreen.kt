package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.PlaybackState
import com.example.ui.components.SleepTimerSlider
import com.example.ui.components.formatDuration
import com.example.ui.theme.PlayingGreen
import com.example.ui.theme.SophisticatedBg
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedBorderGold
import com.example.ui.theme.SophisticatedBorderGoldActive
import com.example.ui.theme.SophisticatedGold
import com.example.ui.theme.SophisticatedGoldAura
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceHighlight
import com.example.ui.theme.SophisticatedTextDark
import com.example.ui.theme.SophisticatedTextMuted
import com.example.ui.theme.SophisticatedTextPrimary
import com.example.ui.theme.SophisticatedTextSecondary
import java.util.Locale

@Composable
fun PureRandomScreen(
    playbackState: PlaybackState,
    onZapRandomSpot: () -> Unit,
    onSetTimerMinutes: (Int) -> Unit,
    onAddFiveMinutes: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onStopPlayback: () -> Unit,
    onOpenNightMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwitchToStandardMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "random_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (playbackState.isPlaying) 0.6f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "random_pulse_alpha"
    )

    val currentMinutes = if (playbackState.sleepTimerRemainingMs > 0) {
        ((playbackState.sleepTimerRemainingMs + 30_000L) / (60 * 1000L)).toInt().coerceIn(1, 180)
    } else {
        30
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SophisticatedBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("pure_random_screen")
    ) {
        // Decorative cosmic / constellation lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = center
            drawCircle(
                color = SophisticatedBorder.copy(alpha = 0.4f),
                radius = 110.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = SophisticatedBorder.copy(alpha = 0.25f),
                radius = 180.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode switches
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SophisticatedSurfaceHighlight)
                            .border(1.dp, SophisticatedBorder, RoundedCornerShape(12.dp))
                            .clickable { onSwitchToStandardMode() }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("random_switch_to_classic")
                    ) {
                        Text(
                            text = "Classique",
                            color = SophisticatedTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            tint = SophisticatedGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "PUREMENT ALÉATOIRE",
                            color = SophisticatedGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                // Settings
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(SophisticatedSurfaceHighlight)
                        .border(1.dp, SophisticatedBorder, CircleShape)
                        .testTag("random_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Paramètres",
                        tint = SophisticatedGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Giant Central Tap Surface for instant Zapping (1 single tap)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                SophisticatedGoldAura.copy(alpha = pulseAlpha),
                                SophisticatedSurface
                            )
                        )
                    )
                    .border(2.dp, if (playbackState.isPlaying) SophisticatedBorderGoldActive else SophisticatedBorderGold, RoundedCornerShape(24.dp))
                    .clickable { onZapRandomSpot() }
                    .testTag("zap_central_touch_area"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SophisticatedGold)
                            .border(3.dp, SophisticatedGoldAura, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Zapper",
                            tint = SophisticatedTextDark,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (playbackState.isPlaying) "TOUCHER POUR ZAPPER" else "TOUCHER POUR LANCER",
                        color = SophisticatedGold,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Choisit un épisode et un passage au hasard complet",
                        color = SophisticatedTextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    // Track info
                    if (playbackState.currentEpisode != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SophisticatedSurfaceHighlight)
                                .border(1.dp, SophisticatedBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (playbackState.isPlaying) PlayingGreen else SophisticatedTextMuted)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = playbackState.currentEpisode?.title ?: "",
                                        color = SophisticatedTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Position : ${formatDuration(playbackState.currentPositionMs)} / ${formatDuration(playbackState.durationMs)}",
                                    color = SophisticatedTextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Roulette / Sleep Timer Slider directly accessible
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SophisticatedSurface)
                    .border(1.dp, SophisticatedBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                SleepTimerSlider(
                    currentMinutes = currentMinutes,
                    onMinutesChanged = { onSetTimerMinutes(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Button (1-tap direct)
                Surface(
                    onClick = onTogglePlayPause,
                    shape = RoundedCornerShape(14.dp),
                    color = if (playbackState.isPlaying) SophisticatedSurfaceHighlight else SophisticatedGold,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorderGold),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("random_play_pause_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (playbackState.isPlaying) SophisticatedGold else SophisticatedTextDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (playbackState.isPlaying) "Pause" else "Reprendre",
                            color = if (playbackState.isPlaying) SophisticatedGold else SophisticatedTextDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Add 5 min button
                Surface(
                    onClick = onAddFiveMinutes,
                    shape = RoundedCornerShape(14.dp),
                    color = SophisticatedGoldAura,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorderGold),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("random_add_5min_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = SophisticatedGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+5 min",
                            color = SophisticatedGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Night mode button
                Surface(
                    onClick = onOpenNightMode,
                    shape = RoundedCornerShape(14.dp),
                    color = SophisticatedSurfaceHighlight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("random_night_mode_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = SophisticatedGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Mode Nuit",
                            color = SophisticatedTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (playbackState.isPlaying) {
                    Surface(
                        onClick = onStopPlayback,
                        shape = RoundedCornerShape(14.dp),
                        color = SophisticatedSurfaceHighlight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, SophisticatedBorder),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("random_stop_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Arrêter",
                                tint = SophisticatedTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
