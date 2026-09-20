package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EpisodeEntity
import com.example.data.model.ListeningSessionEntity
import com.example.data.model.ZoneEntity
import com.example.domain.SmartPickResult
import com.example.service.PlaybackState
import com.example.ui.components.EpisodeCard
import com.example.ui.components.RecentSessionsSheet
import com.example.ui.components.SmartPlayCard
import com.example.ui.components.TimerDurationPicker
import com.example.ui.theme.PlayingGreen
import com.example.ui.theme.SophisticatedBg
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedBorderGold
import com.example.ui.theme.SophisticatedGold
import com.example.ui.theme.SophisticatedGoldAura
import com.example.ui.theme.SophisticatedGoldDim
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceHighlight
import com.example.ui.theme.SophisticatedSurfaceSubtle
import com.example.ui.theme.SophisticatedSurfaceTranslucent
import com.example.ui.theme.SophisticatedTextDark
import com.example.ui.theme.SophisticatedTextMuted
import com.example.ui.theme.SophisticatedTextPrimary
import com.example.ui.theme.SophisticatedTextSecondary
import com.example.ui.theme.StatusGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    episodes: List<EpisodeEntity>,
    allZones: List<ZoneEntity>,
    recentSessions: List<ListeningSessionEntity>,
    selectedFolderUri: String?,
    isScanning: Boolean,
    selectedTimerMinutes: Int,
    playbackState: PlaybackState,
    smartPickPreview: SmartPickResult?,
    feedbackMessage: String?,
    onFolderSelected: (Uri) -> Unit,
    onRescanFolder: () -> Unit,
    onMinutesSelected: (Int) -> Unit,
    onAdjustMinutes: (Int) -> Unit,
    onLaunchSmartPlay: () -> Unit,
    onPlayEpisode: (EpisodeEntity, Boolean) -> Unit,
    onPlaySpecificZone: (EpisodeEntity, ZoneEntity) -> Unit,
    onToggleZoneAvoid: (String, Int, Boolean) -> Unit,
    onTogglePlayPause: () -> Unit,
    onOpenNightMode: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHistorySheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            onFolderSelected(uri)
        }
    }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            snackbarHostState.showSnackbar(feedbackMessage)
            onDismissFeedback()
        }
    }

    val zonesByEpisode = remember(allZones) {
        allZones.groupBy { it.episodeId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SophisticatedGoldAura)
                                .border(1.dp, SophisticatedBorderGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nightlight,
                                contentDescription = null,
                                tint = SophisticatedGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Kaamelott Nuit",
                                    color = SophisticatedTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    letterSpacing = 0.3.sp
                                )
                                if (episodes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(StatusGreen, CircleShape)
                                    )
                                }
                            }
                            Text(
                                text = if (episodes.isNotEmpty()) "${episodes.size} ÉPISODES INDEXÉS" else "ÉCOUTE NOCTURNE",
                                color = SophisticatedTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                },
                actions = {
                    // Rescan button
                    if (selectedFolderUri != null) {
                        IconButton(
                            onClick = onRescanFolder,
                            enabled = !isScanning,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SophisticatedSurface)
                                .border(1.dp, SophisticatedBorder, CircleShape)
                                .testTag("rescan_btn")
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = SophisticatedGold,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Resynchroniser",
                                    tint = SophisticatedTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // History button
                    IconButton(
                        onClick = { showHistorySheet = true },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SophisticatedSurface)
                            .border(1.dp, SophisticatedBorder, CircleShape)
                            .testTag("history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historique d'écoutes",
                            tint = if (recentSessions.isNotEmpty()) SophisticatedGold else SophisticatedTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Settings button (includes folder, rescan, exclusions & mode)
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SophisticatedSurfaceHighlight)
                            .border(1.dp, SophisticatedBorder, CircleShape)
                            .testTag("settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Paramètres",
                            tint = SophisticatedGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SophisticatedBg
                )
            )
        },
        bottomBar = {
            // Mini Player Bar if playback active
            AnimatedVisibility(
                visible = playbackState.isPlaying || playbackState.currentEpisode != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                MiniPlayerBar(
                    playbackState = playbackState,
                    onTogglePlayPause = onTogglePlayPause,
                    onOpenNightMode = onOpenNightMode
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SophisticatedBg,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Folder picker banner if no folder yet or empty
            if (selectedFolderUri == null || episodes.isEmpty()) {
                item(key = "folder_prompt") {
                    FolderPromptCard(
                        isScanning = isScanning,
                        onSelectFolderClick = { folderPickerLauncher.launch(null) }
                    )
                }
            }

            // Sleep Timer duration picker
            item(key = "timer_picker") {
                TimerDurationPicker(
                    selectedMinutes = selectedTimerMinutes,
                    onMinutesSelected = onMinutesSelected,
                    onAdjustMinutes = onAdjustMinutes
                )
            }

            // Big Smart Play Button
            item(key = "smart_play") {
                SmartPlayCard(
                    smartPick = smartPickPreview,
                    hasEpisodes = episodes.isNotEmpty(),
                    timerMinutes = selectedTimerMinutes,
                    onLaunchClick = onLaunchSmartPlay
                )
            }

            // Section Header
            if (episodes.isNotEmpty()) {
                item(key = "episodes_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ÉPISODES DISPONIBLES (${episodes.size})",
                            color = SophisticatedTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Text(
                            text = "Opus, M4A, MP3, WebM",
                            color = SophisticatedTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Episodes list
                items(
                    items = episodes,
                    key = { it.id }
                ) { episode ->
                    val isPlaying = playbackState.isPlaying && playbackState.currentEpisode?.id == episode.id
                    val episodeZones = zonesByEpisode[episode.id] ?: emptyList()

                    EpisodeCard(
                        episode = episode,
                        zones = episodeZones,
                        isCurrentlyPlaying = isPlaying,
                        onPlayClick = { fromBeginning -> onPlayEpisode(episode, fromBeginning) },
                        onPlayZoneClick = { zone -> onPlaySpecificZone(episode, zone) },
                        onToggleZoneAvoid = { zoneIndex, isAvoided ->
                            onToggleZoneAvoid(episode.id, zoneIndex, isAvoided)
                        }
                    )
                }
            }
        }
    }

    if (showHistorySheet) {
        RecentSessionsSheet(
            recentSessions = recentSessions,
            onDismiss = { showHistorySheet = false }
        )
    }
}

@Composable
private fun FolderPromptCard(
    isScanning: Boolean,
    onSelectFolderClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SophisticatedSurface)
            .border(1.dp, SophisticatedBorder, RoundedCornerShape(20.dp))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(SophisticatedGoldAura)
                .border(1.dp, SophisticatedBorderGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = SophisticatedGold,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Dossier des épisodes Kaamelott",
            color = SophisticatedTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Sélectionnez le dossier du téléphone contenant vos épisodes audio (WebM, Opus, M4A, MP3).",
            color = SophisticatedTextSecondary,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(SophisticatedGold)
                .clickable(enabled = !isScanning) { onSelectFolderClick() }
                .padding(horizontal = 22.dp, vertical = 12.dp)
                .testTag("select_folder_prompt_btn"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = SophisticatedTextDark,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Analyse en cours…",
                        color = SophisticatedTextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = SophisticatedTextDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Choisir le dossier",
                        color = SophisticatedTextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerBar(
    playbackState: PlaybackState,
    onTogglePlayPause: () -> Unit,
    onOpenNightMode: () -> Unit
) {
    val ep = playbackState.currentEpisode ?: return
    val remainingSec = (playbackState.sleepTimerRemainingMs / 1000).coerceAtLeast(0)
    val remainingFormatted = String.format("%02d:%02d", remainingSec / 60, remainingSec % 60)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SophisticatedSurfaceTranslucent)
            .border(1.dp, SophisticatedBorderGold, RoundedCornerShape(18.dp))
            .clickable { onOpenNightMode() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("mini_player_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SophisticatedGoldAura),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Nightlight,
                        contentDescription = "Ouvrir Mode Nuit",
                        tint = SophisticatedGold,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = ep.title,
                        color = SophisticatedTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = "Minuterie : $remainingFormatted • Mode Nuit",
                        color = SophisticatedGold,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SophisticatedSurfaceHighlight)
                    .border(1.dp, SophisticatedBorder, CircleShape)
                    .testTag("mini_player_toggle")
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playbackState.isPlaying) "Pause" else "Lecture",
                    tint = SophisticatedGold,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
