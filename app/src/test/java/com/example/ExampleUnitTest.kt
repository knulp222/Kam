package com.example

import com.example.data.model.EpisodeEntity
import com.example.data.model.ListeningSessionEntity
import com.example.data.model.ZoneEntity
import com.example.domain.AudioScanner
import com.example.domain.SmartEpisodeSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testCleanTitleFormatting() {
    val raw1 = "01 - Kaamelott Livre I - Tome 1 [b05Scfhi0dU].webm"
    val clean1 = AudioScanner.formatCleanTitle(raw1)
    assertEquals("01 - Kaamelott Livre I - Tome 1", clean1)

    val raw2 = "09 - Corvus corone - Kaamelott - Livre V - Episode 1 [bpOWG6SMLP4].webm"
    val clean2 = AudioScanner.formatCleanTitle(raw2)
    assertEquals("09 - Corvus corone - Kaamelott - Livre V - Episode 1", clean2)
  }

  @Test
  fun testSmartSelectorPrioritizesUnheardEpisodeAndZone() {
    val ep1 = EpisodeEntity("ep1", "Livre I - Tome 1", "01.webm", "uri1", 2_000_000L, listenCount = 4)
    val ep2 = EpisodeEntity("ep2", "Livre I - Tome 2", "02.webm", "uri2", 2_000_000L, listenCount = 0)

    val ep1Zones = listOf(
        ZoneEntity("ep1_0", "ep1", 0, 0L, 510_000L, listenCount = 3),
        ZoneEntity("ep1_1", "ep1", 1, 510_000L, 1_020_000L, listenCount = 1)
    )
    val ep2Zones = listOf(
        ZoneEntity("ep2_0", "ep2", 0, 0L, 510_000L, listenCount = 0),
        ZoneEntity("ep2_1", "ep2", 1, 510_000L, 1_020_000L, listenCount = 0)
    )

    val recentSessions = listOf(
        ListeningSessionEntity(1L, "ep1", "Livre I - Tome 1", 0, 0L, 510_000L, 510_000L, System.currentTimeMillis())
    )

    val pick = SmartEpisodeSelector.pickSmartTarget(
        episodes = listOf(ep1, ep2),
        allZones = ep1Zones + ep2Zones,
        recentSessions = recentSessions
    )

    assertNotNull(pick)
    assertEquals("ep2", pick!!.episode.id)
    assertEquals(0, pick.targetZone.zoneIndex)
  }

  @Test
  fun testAvoidZoneIsRespected() {
    val ep1 = EpisodeEntity("ep1", "Livre I - Tome 1", "01.webm", "uri1", 2_000_000L, listenCount = 0)
    val zones = listOf(
        ZoneEntity("ep1_0", "ep1", 0, 0L, 510_000L, listenCount = 0, isAvoided = true),
        ZoneEntity("ep1_1", "ep1", 1, 510_000L, 1_020_000L, listenCount = 0, isAvoided = false)
    )

    val targetZone = SmartEpisodeSelector.pickBestZone(zones)
    assertEquals(1, targetZone.zoneIndex)
  }
}
