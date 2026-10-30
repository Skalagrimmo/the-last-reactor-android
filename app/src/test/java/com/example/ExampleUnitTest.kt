package com.example

import com.example.engine.GameEngine
import com.example.engine.GameMap
import com.example.engine.SoundSynthesizer
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testPlayerMovementAndBounds() {
    val map = GameMap()
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val synth = SoundSynthesizer(scope)
    val engine = GameEngine(map, synth)

    val initialPos = engine.playerPos
    engine.movePlayer(Vector2(1f, 0f))
    engine.update(0.1f)

    assertTrue(engine.playerPos.x > initialPos.x)
  }

  @Test
  fun testSteamDashConsumesSteamAndSetsInvulnerable() {
    val map = GameMap()
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val synth = SoundSynthesizer(scope)
    val engine = GameEngine(map, synth)

    val initialSteam = engine.playerSteam
    engine.triggerSteamDash()

    assertTrue(engine.isDashing)
    assertTrue(engine.playerSteam < initialSteam)
    assertTrue(engine.invulnerableTimer > 0f)
  }

  @Test
  fun testValveTogglingDeactivatesHazardAndOpensDoor() {
    val map = GameMap()
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val synth = SoundSynthesizer(scope)
    val engine = GameEngine(map, synth)

    val northValve = map.valves.find { it.id == "valve_north_pressure" }!!
    val northHazard = map.hazards.find { it.id == "hazard_north_conduit" }!!
    val catwalkDoor = map.doors.find { it.id == "door_catwalk" }!!

    assertFalse(northValve.isTurned)
    assertTrue(northHazard.isActive)
    assertFalse(catwalkDoor.isOpen)

    // Position player next to valve and interact
    engine.playerPos = northValve.pos
    engine.triggerInteract()

    assertTrue(northValve.isTurned)
    assertFalse(northHazard.isActive)
    assertTrue(catwalkDoor.isOpen)
  }

  @Test
  fun testPlayerStatsProgressionCalculations() {
    val stats = PlayerStats(vigorLevel = 3, pressureLevel = 2, potencyLevel = 2)
    assertEquals(150f, stats.maxHealth(), 0.01f) // 100 + 2 * 25
    assertEquals(100f, stats.maxSteam(), 0.01f)  // 80 + 1 * 20
    assertEquals(1.25f, stats.vialPotencyMultiplier(), 0.01f)
  }
}
