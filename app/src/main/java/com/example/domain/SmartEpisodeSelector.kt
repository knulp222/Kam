package com.example.domain

import com.example.data.model.EpisodeEntity
import com.example.data.model.ListeningSessionEntity
import com.example.data.model.ZoneEntity
import kotlin.random.Random

data class SmartPickResult(
    val episode: EpisodeEntity,
    val targetZone: ZoneEntity,
    val startPositionMs: Long,
    val reason: String
)

object SmartEpisodeSelector {

    /**
     * Strict Rotation & Intelligent Picking:
     * 1. Finds all non-excluded episodes.
     * 2. Finds the absolute minimum listenCount among all available episodes.
     *    (Guarantees full cycle: every episode is heard before any is repeated).
     * 3. Among least-listened candidates, excludes those in recent sessions if possible.
     * 4. Among candidates, chooses the one with oldest lastListenedTimestamp (shuffled tie-break).
     * 5. Inside the episode, picks the zone with the lowest listenCount (randomized tie-break among unplayed zones).
     * 6. Applies a varied starting offset so audio never restarts at the exact same second.
     */
    fun pickSmartTarget(
        episodes: List<EpisodeEntity>,
        allZones: List<ZoneEntity>,
        recentSessions: List<ListeningSessionEntity>
    ): SmartPickResult? {
        val availableEpisodes = episodes.filter { !it.isExcluded }
        if (availableEpisodes.isEmpty()) return null

        val recentEpisodeIds = recentSessions.take(6).map { it.episodeId }
        val zonesByEpisode = allZones.groupBy { it.episodeId }

        // Find strictly minimum listen count among all available episodes
        val minEpisodeListens = availableEpisodes.minOf { it.listenCount }
        val minListenedEpisodes = availableEpisodes.filter { it.listenCount == minEpisodeListens }

        // Among min-listened episodes, filter out ones listened in recent sessions if alternatives exist
        val nonRecentMinEpisodes = minListenedEpisodes.filter { it.id !in recentEpisodeIds }
        val pool = if (nonRecentMinEpisodes.isNotEmpty()) nonRecentMinEpisodes else minListenedEpisodes

        // Shuffle to break ties randomly when lastListenedTimestamp is identical (e.g. 0L)
        val selectedEpisode = pool.shuffled().minByOrNull { it.lastListenedTimestamp }
            ?: availableEpisodes.random()

        // Get zones for this episode
        val episodeZones = zonesByEpisode[selectedEpisode.id]?.takeIf { it.isNotEmpty() }
            ?: AudioScanner.createZonesForEpisode(selectedEpisode.id, selectedEpisode.durationMs)

        val targetZone = pickBestZone(episodeZones)

        // Varied starting offset:
        // Instead of always starting at exactly targetZone.startTimeMs (e.g. 0s),
        // add a dynamic variance of 10s-35s within the zone if duration permits
        val zoneDuration = maxOf(10_000L, targetZone.endTimeMs - targetZone.startTimeMs)
        val maxOffset = minOf(35_000L, zoneDuration / 4)
        val startOffset = if (maxOffset > 5_000L) Random.nextLong(0L, maxOffset) else 0L
        val variedStartMs = (targetZone.startTimeMs + startOffset).coerceAtMost(maxOf(0L, selectedEpisode.durationMs - 5_000L))

        val reason = if (targetZone.listenCount == 0) {
            "Passage inédit (~${targetZone.zoneIndex * 8} min)"
        } else if (minEpisodeListens == 0) {
            "Épisode non écouté"
        } else {
            "Rotation intelligente (Zone la moins écoutée)"
        }

        return SmartPickResult(
            episode = selectedEpisode,
            targetZone = targetZone,
            startPositionMs = variedStartMs,
            reason = reason
        )
    }

    /**
     * Pick a random episode with strict least-listened preference and varied start
     */
    fun pickRandomEpisode(
        episodes: List<EpisodeEntity>,
        allZones: List<ZoneEntity>
    ): SmartPickResult? {
        val availableEpisodes = episodes.filter { !it.isExcluded }
        if (availableEpisodes.isEmpty()) return null

        val minEpisodeListens = availableEpisodes.minOf { it.listenCount }
        val pool = availableEpisodes.filter { it.listenCount == minEpisodeListens }
        val randomEpisode = (if (pool.isNotEmpty()) pool else availableEpisodes).random()

        val zonesByEpisode = allZones.groupBy { it.episodeId }
        val episodeZones = zonesByEpisode[randomEpisode.id]?.takeIf { it.isNotEmpty() }
            ?: AudioScanner.createZonesForEpisode(randomEpisode.id, randomEpisode.durationMs)

        val targetZone = pickBestZone(episodeZones)

        val zoneDuration = maxOf(10_000L, targetZone.endTimeMs - targetZone.startTimeMs)
        val maxOffset = minOf(45_000L, zoneDuration / 3)
        val startOffset = if (maxOffset > 5_000L) Random.nextLong(0L, maxOffset) else 0L
        val variedStartMs = (targetZone.startTimeMs + startOffset).coerceAtMost(maxOf(0L, randomEpisode.durationMs - 5_000L))

        return SmartPickResult(
            episode = randomEpisode,
            targetZone = targetZone,
            startPositionMs = variedStartMs,
            reason = "Lancement Express Aléatoire"
        )
    }

    /**
     * Pure Random Zapping mode:
     * Jumps to a completely random spot in any available non-excluded episode.
     * Avoids current episode if multiple episodes exist.
     */
    fun pickPureRandomSpot(
        episodes: List<EpisodeEntity>,
        allZones: List<ZoneEntity>,
        currentEpisodeId: String? = null
    ): SmartPickResult? {
        val availableEpisodes = episodes.filter { !it.isExcluded }
        if (availableEpisodes.isEmpty()) return null

        val candidates = if (availableEpisodes.size > 1 && currentEpisodeId != null) {
            availableEpisodes.filter { it.id != currentEpisodeId }
        } else {
            availableEpisodes
        }

        val chosenEpisode = candidates.random()
        val duration = if (chosenEpisode.durationMs > 15_000L) chosenEpisode.durationMs else 500_000L

        // Pick a completely random position between 5% and 85% of duration
        val minPos = minOf(10_000L, (duration * 0.05).toLong())
        val maxPos = maxOf(minPos + 5_000L, (duration * 0.85).toLong())
        val randomPos = Random.nextLong(minPos, maxPos)

        val zonesByEpisode = allZones.groupBy { it.episodeId }
        val episodeZones = zonesByEpisode[chosenEpisode.id]?.takeIf { it.isNotEmpty() }
            ?: AudioScanner.createZonesForEpisode(chosenEpisode.id, duration)

        val targetZone = findZoneForPosition(episodeZones, randomPos)
            ?: (episodeZones.firstOrNull() ?: ZoneEntity("tmp", chosenEpisode.id, 0, 0L, duration))

        return SmartPickResult(
            episode = chosenEpisode,
            targetZone = targetZone,
            startPositionMs = randomPos,
            reason = "Zapping Purement Aléatoire"
        )
    }

    /**
     * Finds the least listened zone, properly randomizing ties so Zone 1 is not always favoured.
     */
    fun pickBestZone(zones: List<ZoneEntity>): ZoneEntity {
        if (zones.isEmpty()) {
            return ZoneEntity(
                id = "temp",
                episodeId = "",
                zoneIndex = 0,
                startTimeMs = 0L,
                endTimeMs = AudioScanner.DEFAULT_ZONE_DURATION_MS
            )
        }

        // 1. Filter out avoided zones if possible
        val nonAvoidedZones = zones.filter { !it.isAvoided }
        val candidateZones = if (nonAvoidedZones.isNotEmpty()) nonAvoidedZones else zones

        // 2. Find zones with the minimum listen count
        val minListens = candidateZones.minOf { it.listenCount }
        val leastListenedZones = candidateZones.filter { it.listenCount == minListens }

        // 3. Shuffle first to avoid default index 0 bias on equal timestamps
        return leastListenedZones.shuffled().minByOrNull { it.lastListenedTimestamp }
            ?: leastListenedZones.random()
    }

    fun findZoneForPosition(zones: List<ZoneEntity>, positionMs: Long): ZoneEntity? {
        return zones.find { positionMs in it.startTimeMs until it.endTimeMs }
            ?: zones.lastOrNull()
    }
}
