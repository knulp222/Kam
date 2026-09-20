package com.example.domain

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.data.model.EpisodeEntity
import com.example.data.model.ZoneEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AudioScanner {

    // Target zone duration: ~8.5 minutes (510 seconds = 510,000 ms)
    const val DEFAULT_ZONE_DURATION_MS = 510_000L
    private val SUPPORTED_EXTENSIONS = listOf("webm", "opus", "m4a", "mp3", "ogg", "aac", "wav")

    suspend fun scanFolder(
        context: Context,
        treeUri: Uri
    ): Pair<List<EpisodeEntity>, List<ZoneEntity>> = withContext(Dispatchers.IO) {
        val rootDir = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext Pair(emptyList(), emptyList())
        val files = rootDir.listFiles()

        val episodes = mutableListOf<EpisodeEntity>()
        val allZones = mutableListOf<ZoneEntity>()

        for (file in files) {
            if (!file.isFile) continue
            val name = file.name ?: continue
            val ext = name.substringAfterLast('.', "").lowercase()
            if (ext !in SUPPORTED_EXTENSIONS) continue

            val fileUri = file.uri
            val durationMs = extractDuration(context, fileUri)
            val cleanTitle = formatCleanTitle(name)

            val episodeId = fileUri.toString()
            val episode = EpisodeEntity(
                id = episodeId,
                title = cleanTitle,
                rawFileName = name,
                uriString = fileUri.toString(),
                durationMs = durationMs
            )
            episodes.add(episode)

            // Generate zones
            val zones = createZonesForEpisode(episodeId, durationMs)
            allZones.addAll(zones)
        }

        // Sort episodes naturally by name / title
        episodes.sortBy { it.title }

        Pair(episodes, allZones)
    }

    fun createZonesForEpisode(episodeId: String, durationMs: Long): List<ZoneEntity> {
        val actualDuration = if (durationMs > 0) durationMs else 1_800_000L // default 30 min if unknown
        val zoneDuration = DEFAULT_ZONE_DURATION_MS
        val numZones = maxOf(1, ((actualDuration + zoneDuration - 1) / zoneDuration).toInt())

        val zones = mutableListOf<ZoneEntity>()
        for (i in 0 until numZones) {
            val startMs = i * zoneDuration
            val endMs = minOf(actualDuration, (i + 1) * zoneDuration)
            zones.add(
                ZoneEntity(
                    id = "${episodeId}_zone_$i",
                    episodeId = episodeId,
                    zoneIndex = i,
                    startTimeMs = startMs,
                    endTimeMs = endMs
                )
            )
        }
        return zones
    }

    private fun extractDuration(context: Context, uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationStr?.toLongOrNull() ?: 0L
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }

    fun formatCleanTitle(rawName: String): String {
        // e.g. "01 - Kaamelott Livre I - Tome 1 [b05Scfhi0dU].webm" -> "Kaamelott Livre I - Tome 1"
        var clean = rawName.substringBeforeLast('.')
        // Remove YouTube ID in brackets like [b05Scfhi0dU]
        clean = clean.replace(Regex("\\[.*?\\]"), "").trim()
        // Remove trailing or leading dashes/spaces
        clean = clean.trim('-', ' ')
        return clean.ifEmpty { rawName }
    }
}
