package com.example.ui.components

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ProgressTrack
import com.example.ui.theme.SophisticatedBg
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedBorderGold
import com.example.ui.theme.SophisticatedGold
import com.example.ui.theme.SophisticatedGoldAura
import com.example.ui.theme.SophisticatedGoldDim
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceHighlight
import com.example.ui.theme.SophisticatedTextDark
import com.example.ui.theme.SophisticatedTextMuted
import com.example.ui.theme.SophisticatedTextPrimary
import com.example.ui.theme.SophisticatedTextSecondary

@Composable
fun TimerDurationPicker(
    selectedMinutes: Int,
    onMinutesSelected: (Int) -> Unit,
    onAdjustMinutes: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickOptions = listOf(15, 30, 45, 60)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SophisticatedSurface)
            .border(1.dp, SophisticatedBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SophisticatedGoldAura),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = SophisticatedGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "MINUTERIE SOMMEIL",
                        color = SophisticatedTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Arrêt automatique",
                        color = SophisticatedTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Custom +/- controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onAdjustMinutes(-1) },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SophisticatedSurfaceHighlight)
                        .border(1.dp, SophisticatedBorder, CircleShape)
                        .testTag("timer_minus_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Diminuer 1 min",
                        tint = SophisticatedTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .background(SophisticatedGoldAura, RoundedCornerShape(8.dp))
                        .border(1.dp, SophisticatedBorderGold, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${selectedMinutes} min",
                        color = SophisticatedGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                IconButton(
                    onClick = { onAdjustMinutes(1) },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SophisticatedSurfaceHighlight)
                        .border(1.dp, SophisticatedBorder, CircleShape)
                        .testTag("timer_plus_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Augmenter 1 min",
                        tint = SophisticatedTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // High-Precision Interactive Slider from 1 min to 60 min
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = selectedMinutes.coerceIn(1, 60).toFloat(),
                onValueChange = { newValue ->
                    val rounded = kotlin.math.round(newValue).toInt().coerceIn(1, 60)
                    onMinutesSelected(rounded)
                },
                valueRange = 1f..60f,
                colors = SliderDefaults.colors(
                    thumbColor = SophisticatedGold,
                    activeTrackColor = SophisticatedGold,
                    inactiveTrackColor = SophisticatedSurfaceHighlight
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timer_slider")
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "1 min",
                    color = SophisticatedTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "15 min",
                    color = SophisticatedTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "30 min",
                    color = SophisticatedTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "45 min",
                    color = SophisticatedTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "60 min",
                    color = SophisticatedTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Preset Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickOptions.forEach { minutes ->
                val isSelected = selectedMinutes == minutes
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) SophisticatedGold else SophisticatedSurfaceHighlight
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) SophisticatedGold else SophisticatedBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onMinutesSelected(minutes) }
                        .testTag("timer_preset_${minutes}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$minutes m",
                        color = if (isSelected) SophisticatedTextDark else SophisticatedTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Fondu sonore automatique et progressif sur les 60 dernières secondes",
            color = SophisticatedTextMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}
