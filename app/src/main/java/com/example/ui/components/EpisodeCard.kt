package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EpisodeEntity
import com.example.data.model.ZoneEntity
import com.example.ui.theme.AvoidRed
import com.example.ui.theme.AvoidRedDim
import com.example.ui.theme.PlayingGreen
import com.example.ui.theme.SophisticatedBg
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedBorderGold
import com.example.ui.theme.SophisticatedBorderGoldActive
import com.example.ui.theme.SophisticatedGold
import com.example.ui.theme.SophisticatedGoldAura
import com.example.ui.theme.SophisticatedGoldDim
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceHighlight
import com.example.ui.theme.SophisticatedSurfaceSubtle
import com.example.ui.theme.SophisticatedTextDark
import com.example.ui.theme.SophisticatedTextMuted
import com.example.ui.theme.SophisticatedTextPrimary
import com.example.ui.theme.SophisticatedTextSecondary
import java.util.Locale

@Composable
fun EpisodeCard(
    episode: EpisodeEntity,
    zones: List<ZoneEntity>,
    isCurrentlyPlaying: Boolean,
    onPlayClick: (startFromBeginning: Boolean) -> Unit,
    onPlayZoneClick: (ZoneEntity) -> Unit,
    onToggleZoneAvoid: (zoneIndex: Int, isAvoided: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SophisticatedSurface)
            .border(
                width = if (isCurrentlyPlaying) 1.5.dp else 1.dp,
                color = if (isCurrentlyPlaying) SophisticatedBorderGoldActive else SophisticatedBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
            .testTag("episode_card_${episode.id.hashCode()}")
    ) {
        // Main Row Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play Icon Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentlyPlaying) SophisticatedGold else SophisticatedGoldAura
                    )
                    .border(
                        1.dp,
                        if (isCurrentlyPlaying) SophisticatedGold else SophisticatedBorderGold,
                        CircleShape
                    )
                    .clickable { onPlayClick(false) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Lire",
                    tint = if (isCurrentlyPlaying) SophisticatedTextDark else SophisticatedGold,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Episode Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    color = SophisticatedTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Listen count badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = null,
                            tint = SophisticatedGoldDim,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${episode.listenCount} écoute(s)",
                            color = SophisticatedGoldDim,
                            fontSize = 11.sp
                        )
                    }

                    // Resume Position if saved
                    if (episode.lastPlaybackPositionMs > 10_000L) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = null,
                                tint = SophisticatedGold,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Reprise : ${formatDuration(episode.lastPlaybackPositionMs)}",
                                color = SophisticatedGold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Duration if known
                    if (episode.durationMs > 0) {
                        Text(
                            text = formatDuration(episode.durationMs),
                            color = SophisticatedTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Expand zones toggle
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SophisticatedSurfaceHighlight)
                    .border(1.dp, SophisticatedBorder, CircleShape)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Afficher les zones",
                    tint = SophisticatedTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expanded Zones List
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SophisticatedBorder)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "ZONES DE LECTURE (~8.5 MIN)",
                    color = SophisticatedTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (zones.isEmpty()) {
                    Text(
                        text = "Zones générées automatiquement au premier démarrage",
                        color = SophisticatedTextMuted,
                        fontSize = 11.sp
                    )
                } else {
                    zones.forEach { zone ->
                        ZoneItemRow(
                            zone = zone,
                            onPlayZone = { onPlayZoneClick(zone) },
                            onToggleAvoid = { onToggleZoneAvoid(zone.zoneIndex, zone.isAvoided) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Replay from beginning button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SophisticatedSurfaceHighlight)
                        .border(1.dp, SophisticatedBorder, RoundedCornerShape(8.dp))
                        .clickable { onPlayClick(true) }
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        tint = SophisticatedTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Recommencer depuis le début (00:00)",
                        color = SophisticatedTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneItemRow(
    zone: ZoneEntity,
    onPlayZone: () -> Unit,
    onToggleAvoid: () -> Unit
) {
    val startStr = formatDuration(zone.startTimeMs)
    val endStr = formatDuration(zone.endTimeMs)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (zone.isAvoided) AvoidRedDim.copy(alpha = 0.35f) else SophisticatedSurfaceSubtle)
            .border(1.dp, if (zone.isAvoided) AvoidRed.copy(alpha = 0.3f) else SophisticatedBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable { onPlayZone() }
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Lire cette zone",
                tint = if (zone.isAvoided) AvoidRed else SophisticatedGold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Zone ${zone.zoneIndex + 1} ($startStr - $endStr)",
                    color = if (zone.isAvoided) AvoidRed else SophisticatedTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${zone.listenCount} écoute(s)",
                    color = SophisticatedTextMuted,
                    fontSize = 10.sp
                )
            }
        }

        // Avoid chip toggle
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (zone.isAvoided) AvoidRedDim else SophisticatedSurfaceHighlight)
                .border(1.dp, if (zone.isAvoided) AvoidRed.copy(alpha = 0.5f) else SophisticatedBorder, RoundedCornerShape(6.dp))
                .clickable { onToggleAvoid() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (zone.isAvoided) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = null,
                        tint = AvoidRed,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    text = if (zone.isAvoided) "Évitée" else "Éviter",
                    color = if (zone.isAvoided) AvoidRed else SophisticatedTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
