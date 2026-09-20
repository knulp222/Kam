package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.SettingsSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NightModePlayerScreen
import com.example.ui.screens.PureRandomScreen
import com.example.ui.theme.KaamelottNuitTheme

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KaamelottNuitTheme {
                NotificationPermissionEffect()

                val isNightModeActive by viewModel.isNightModeActive.collectAsStateWithLifecycle()
                val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
                val episodes by viewModel.allEpisodes.collectAsStateWithLifecycle()
                val allZones by viewModel.allZones.collectAsStateWithLifecycle()
                val recentSessions by viewModel.recentSessions.collectAsStateWithLifecycle()
                val selectedFolderUri by viewModel.selectedFolderUri.collectAsStateWithLifecycle()
                val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
                val selectedTimerMinutes by viewModel.selectedTimerMinutes.collectAsStateWithLifecycle()
                val smartPickPreview by viewModel.smartPickPreview.collectAsStateWithLifecycle()
                val feedbackMessage by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()
                val appMode by viewModel.appMode.collectAsStateWithLifecycle()
                val voiceSofteningEnabled by viewModel.voiceSofteningEnabled.collectAsStateWithLifecycle()

                var showSettingsSheet by remember { mutableStateOf(false) }
                val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                val folderPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri: Uri? ->
                    if (uri != null) {
                        viewModel.onFolderSelected(uri)
                    }
                }

                // Settings modal sheet
                if (showSettingsSheet) {
                    SettingsSheet(
                        sheetState = settingsSheetState,
                        currentMode = appMode,
                        episodes = episodes,
                        selectedFolderUri = selectedFolderUri,
                        voiceSofteningEnabled = voiceSofteningEnabled,
                        onModeChanged = { newMode ->
                            viewModel.setAppMode(newMode)
                        },
                        onToggleVoiceSoftening = { enabled ->
                            viewModel.setVoiceSofteningEnabled(enabled)
                        },
                        onResetStats = {
                            viewModel.resetAllListeningStats()
                        },
                        onToggleEpisodeExcluded = { epId, isExcluded ->
                            viewModel.toggleEpisodeExcluded(epId, isExcluded)
                        },
                        onChangeFolderClick = {
                            folderPickerLauncher.launch(null)
                        },
                        onRescanClick = {
                            viewModel.rescanFolder()
                        },
                        onDismiss = { showSettingsSheet = false }
                    )
                }

                AnimatedContent(
                    targetState = Pair(isNightModeActive, appMode),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { (nightModeActive, currentMode) ->
                    if (nightModeActive) {
                        NightModePlayerScreen(
                            playbackState = playbackState,
                            onAddFiveMin = { viewModel.addFiveMinutesToSleepTimer() },
                            onSetTimerMinutes = { min -> viewModel.setSleepTimerDirect(min) },
                            onStopSession = { viewModel.stopAndExitSession() },
                            onLongPressToggleAvoid = { viewModel.toggleCurrentZoneAvoid() },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onExitClick = { viewModel.exitNightMode() },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else when (currentMode) {
                        "random" -> {
                            PureRandomScreen(
                                playbackState = playbackState,
                                onZapRandomSpot = { viewModel.launchPureRandomZap() },
                                onSetTimerMinutes = { min -> viewModel.setSleepTimerDirect(min) },
                                onAddFiveMinutes = { viewModel.addFiveMinutesToSleepTimer() },
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onStopPlayback = { viewModel.stopAndExitSession() },
                                onOpenNightMode = { viewModel.openNightMode() },
                                onOpenSettings = { showSettingsSheet = true },
                                onSwitchToStandardMode = { viewModel.setAppMode("standard") },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            HomeScreen(
                                episodes = episodes,
                                allZones = allZones,
                                recentSessions = recentSessions,
                                selectedFolderUri = selectedFolderUri,
                                isScanning = isScanning,
                                selectedTimerMinutes = selectedTimerMinutes,
                                playbackState = playbackState,
                                smartPickPreview = smartPickPreview,
                                feedbackMessage = feedbackMessage,
                                onFolderSelected = { uri -> viewModel.onFolderSelected(uri) },
                                onRescanFolder = { viewModel.rescanFolder() },
                                onMinutesSelected = { min -> viewModel.setTimerMinutes(min) },
                                onAdjustMinutes = { delta -> viewModel.adjustTimerMinutes(delta) },
                                onLaunchSmartPlay = { viewModel.launchSmartPlay() },
                                onPlayEpisode = { episode, fromBeginning ->
                                    viewModel.playEpisodeDirect(episode, fromBeginning)
                                },
                                onPlaySpecificZone = { episode, zone ->
                                    viewModel.playEpisodeDirect(episode, specificZone = zone)
                                },
                                onToggleZoneAvoid = { epId, zoneIdx, isAvoided ->
                                    viewModel.toggleZoneAvoidForEpisode(epId, zoneIdx, isAvoided)
                                },
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onOpenNightMode = { viewModel.openNightMode() },
                                onOpenSettings = { showSettingsSheet = true },
                                onDismissFeedback = { viewModel.clearFeedbackMessage() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationPermissionEffect() {
    val context = androidx.compose.ui.platform.LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            )
        }
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasPermission = isGranted
        }
        LaunchedEffect(Unit) {
            if (!hasPermission) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
