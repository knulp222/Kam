package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.EpisodeEntity
import com.example.ui.theme.AvoidRed
import com.example.ui.theme.AvoidRedDim
import com.example.ui.theme.SophisticatedBorder
import com.example.ui.theme.SophisticatedBorderGold
import com.example.ui.theme.SophisticatedGold
import com.example.ui.theme.SophisticatedGoldAura
import com.example.ui.theme.SophisticatedSurface
import com.example.ui.theme.SophisticatedSurfaceHighlight
import com.example.ui.theme.SophisticatedSurfaceSubtle
import com.example.ui.theme.SophisticatedTextDark
import com.example.ui.theme.SophisticatedTextMuted
import com.example.ui.theme.SophisticatedTextPrimary
import com.example.ui.theme.SophisticatedTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    sheetState: SheetState,
    currentMode: String,
    episodes: List<EpisodeEntity>,
    selectedFolderUri: String?,
    voiceSofteningEnabled: Boolean,
    onModeChanged: (String) -> Unit,
    onToggleVoiceSoftening: (Boolean) -> Unit,
    onResetStats: () -> Unit,
    onToggleEpisodeExcluded: (String, Boolean) -> Unit,
    onChangeFolderClick: () -> Unit,
    onRescanClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val filteredEpisodes = remember(episodes, searchQuery) {
        if (searchQuery.isBlank()) episodes
        else episodes.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val excludedCount = remember(episodes) {
        episodes.count { it.isExcluded }
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = AvoidRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Réinitialiser les écoutes ?",
                        color = SophisticatedTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "Cette action remettra à zéro le nombre d'écoutes de tous les épisodes et de toutes les zones, et effacera l'historique des sessions. La rotation intelligente recommencera depuis le début.",
                    color = SophisticatedTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetStats()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AvoidRed,
                        contentColor = SophisticatedTextPrimary
                    )
                ) {
                    Text("Réinitialiser à zéro", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetConfirmDialog = false }
                ) {
                    Text("Annuler", color = SophisticatedTextSecondary)
                }
            },
            containerColor = SophisticatedSurface,
            shape = RoundedCornerShape(18.dp)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SophisticatedSurface,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(22.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SophisticatedGoldAura)
                            .border(1.dp, SophisticatedBorderGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = SophisticatedGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Paramètres",
                        color = SophisticatedTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(SophisticatedSurfaceHighlight)
                        .border(1.dp, SophisticatedBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fermer",
                        tint = SophisticatedTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: App Mode Selection (Classique vs Aléatoire)
                item {
                    Text(
                        text = "MODE D'INTERFACE",
                        color = SophisticatedGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Standard Mode
                        val isStandard = currentMode != "random"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isStandard) SophisticatedGoldAura else SophisticatedSurfaceSubtle)
                                .border(
                                    1.dp,
                                    if (isStandard) SophisticatedGold else SophisticatedBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onModeChanged("standard") }
                                .padding(12.dp)
                        ) {
                            Column {
                                Icon(
                                    imageVector = Icons.Default.ViewList,
                                    contentDescription = null,
                                    tint = if (isStandard) SophisticatedGold else SophisticatedTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Classique",
                                    color = if (isStandard) SophisticatedGold else SophisticatedTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Parcours intelligent & liste complète",
                                    color = SophisticatedTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Pure Random Mode
                        val isRandom = currentMode == "random"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isRandom) SophisticatedGoldAura else SophisticatedSurfaceSubtle)
                                .border(
                                    1.dp,
                                    if (isRandom) SophisticatedGold else SophisticatedBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onModeChanged("random") }
                                .padding(12.dp)
                        ) {
                            Column {
                                Icon(
                                    imageVector = Icons.Default.Casino,
                                    contentDescription = null,
                                    tint = if (isRandom) SophisticatedGold else SophisticatedTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Aléatoire",
                                    color = if (isRandom) SophisticatedGold else SophisticatedTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bouton unique de zapping instantané",
                                    color = SophisticatedTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Section: Night Audio Processing / Equalizer
                item {
                    Text(
                        text = "CONFORT AUDIO NOCTURNE",
                        color = SophisticatedGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SophisticatedSurfaceSubtle)
                            .border(1.dp, SophisticatedBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(SophisticatedGoldAura),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = SophisticatedGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Voix feutrées & Réduction fanfares",
                                        color = SophisticatedTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Égaliseur en temps réel : adoucit les fréquences agressives et baisse automatiquement les trompettes de générique pour s'endormir sans sursaut.",
                                        color = SophisticatedTextMuted,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = voiceSofteningEnabled,
                                onCheckedChange = onToggleVoiceSoftening,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SophisticatedTextDark,
                                    checkedTrackColor = SophisticatedGold
                                )
                            )
                        }
                    }
                }

                // Section: Reset Stats and History
                item {
                    Text(
                        text = "GESTION ET RÉINITIALISATION",
                        color = SophisticatedGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AvoidRedDim.copy(alpha = 0.2f))
                            .border(1.dp, AvoidRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .clickable { showResetConfirmDialog = true }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AvoidRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = AvoidRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Réinitialiser toutes les écoutes",
                                    color = AvoidRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Remet tous les compteurs d'épisodes et de zones à zéro",
                                    color = SophisticatedTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Section: Folder management
                item {
                    Text(
                        text = "DOSSIER AUDIO",
                        color = SophisticatedGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SophisticatedSurfaceSubtle)
                                .border(1.dp, SophisticatedBorder, RoundedCornerShape(12.dp))
                                .clickable { onChangeFolderClick() }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = SophisticatedGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Changer le dossier",
                                    color = SophisticatedTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SophisticatedSurfaceSubtle)
                                .border(1.dp, SophisticatedBorder, RoundedCornerShape(12.dp))
                                .clickable { onRescanClick() }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = SophisticatedTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Actualiser",
                                    color = SophisticatedTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Section: Excluded Episodes
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ÉPISODES EXCLUS DU HASARD",
                            color = SophisticatedGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (excludedCount > 0) {
                            Text(
                                text = "$excludedCount exclu(s)",
                                color = AvoidRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Les épisodes désactivés ci-dessous ne seront jamais lancés aléatoirement.",
                        color = SophisticatedTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Rechercher un épisode...",
                                color = SophisticatedTextMuted,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = SophisticatedTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Effacer",
                                        tint = SophisticatedTextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SophisticatedSurfaceSubtle,
                            unfocusedContainerColor = SophisticatedSurfaceSubtle,
                            focusedBorderColor = SophisticatedBorderGold,
                            unfocusedBorderColor = SophisticatedBorder,
                            focusedTextColor = SophisticatedTextPrimary,
                            unfocusedTextColor = SophisticatedTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                }

                if (filteredEpisodes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SophisticatedSurfaceSubtle)
                                .border(1.dp, SophisticatedBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (episodes.isEmpty()) "Aucun épisode trouvé" else "Aucun résultat pour cette recherche",
                                color = SophisticatedTextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    items(filteredEpisodes, key = { it.id }) { episode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (episode.isExcluded) AvoidRedDim.copy(alpha = 0.25f) else SophisticatedSurfaceSubtle)
                                .border(
                                    1.dp,
                                    if (episode.isExcluded) AvoidRed.copy(alpha = 0.4f) else SophisticatedBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onToggleEpisodeExcluded(episode.id, !episode.isExcluded)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = episode.title,
                                    color = if (episode.isExcluded) AvoidRed else SophisticatedTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (episode.isExcluded) "Exclu du hasard" else "Inclus dans le hasard",
                                    color = if (episode.isExcluded) AvoidRed.copy(alpha = 0.8f) else SophisticatedTextMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Switch(
                                checked = !episode.isExcluded,
                                onCheckedChange = { isIncluded ->
                                    onToggleEpisodeExcluded(episode.id, !isIncluded)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SophisticatedTextDark,
                                    checkedTrackColor = SophisticatedGold,
                                    uncheckedThumbColor = SophisticatedTextMuted,
                                    uncheckedTrackColor = SophisticatedSurfaceHighlight
                                ),
                                modifier = Modifier.testTag("toggle_exclude_${episode.id.hashCode()}")
                            )
                        }
                    }
                }
            }
        }
    }
}
