package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NightSleepTextDim
import com.example.ui.theme.NightSleepTextGlow
import com.example.ui.theme.SoftGold
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSlider(
    currentMinutes: Int,
    onMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isNightMode: Boolean = false,
    minMinutes: Int = 1,
    maxMinutes: Int = 180,
    quickPresets: List<Int> = listOf(15, 30, 45, 60, 90)
) {
    val activeColor = if (isNightMode) SoftGold else MaterialTheme.colorScheme.primary
    val inactiveTrackColor = if (isNightMode) NightSleepTextDim.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isNightMode) NightSleepTextGlow else MaterialTheme.colorScheme.onSurface
    val subTextColor = if (isNightMode) NightSleepTextDim else MaterialTheme.colorScheme.onSurfaceVariant

    var sliderValue by remember(currentMinutes) {
        mutableFloatStateOf(currentMinutes.coerceIn(minMinutes, maxMinutes).toFloat())
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row: Header & Value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = activeColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Minuteur de sommeil",
                    color = subTextColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "${sliderValue.roundToInt()} min",
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.testTag("sleep_timer_slider_label")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Slider with - / + buttons on sides
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minus button (-5 min)
            IconButton(
                onClick = {
                    val newVal = (sliderValue.roundToInt() - 5).coerceIn(minMinutes, maxMinutes)
                    sliderValue = newVal.toFloat()
                    onMinutesChanged(newVal)
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(inactiveTrackColor.copy(alpha = 0.4f))
                    .testTag("timer_minus_5_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Moins 5 minutes",
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Main continuous drag slider
            Slider(
                value = sliderValue,
                onValueChange = { newFloat ->
                    sliderValue = newFloat
                },
                onValueChangeFinished = {
                    onMinutesChanged(sliderValue.roundToInt())
                },
                valueRange = minMinutes.toFloat()..maxMinutes.toFloat(),
                steps = 0,
                colors = SliderDefaults.colors(
                    thumbColor = activeColor,
                    activeTrackColor = activeColor,
                    inactiveTrackColor = inactiveTrackColor
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("sleep_timer_slider_bar")
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Plus button (+5 min)
            IconButton(
                onClick = {
                    val newVal = (sliderValue.roundToInt() + 5).coerceIn(minMinutes, maxMinutes)
                    sliderValue = newVal.toFloat()
                    onMinutesChanged(newVal)
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(inactiveTrackColor.copy(alpha = 0.4f))
                    .testTag("timer_plus_5_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Plus 5 minutes",
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Quick Preset Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            quickPresets.forEach { preset ->
                val isSelected = sliderValue.roundToInt() == preset
                Surface(
                    onClick = {
                        sliderValue = preset.toFloat()
                        onMinutesChanged(preset)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) activeColor.copy(alpha = 0.25f) else Color.Transparent,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = "${preset}m",
                        color = if (isSelected) activeColor else subTextColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
