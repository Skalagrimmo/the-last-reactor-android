package com.example.ecs

import com.example.engine.GameMap
import com.example.engine.SoundSynthesizer
import com.example.model.Vector2

class WorldECS(
  val map: GameMap,
  val soundSynth: SoundSynthesizer
) {
  val entityManager = EntityManager()
  val physicsSystem = PhysicsSystem(map)
  val movementSystem = MovementSystem(map)
  val enemyAiSystem = EnemyAISystem(map, soundSynth)
  val combatSystem = CombatSystem(soundSynth)

  var wardenId: EntityId = 0

  init {
    initWorld()
  }

  fun initWorld() {
    entityManager.clear()

    // 1. Spawn Alchemical Warden
    wardenId = entityManager.createWarden(250f, 250f)

    // 2. Spawn Enemies
    // Catwalk & North Conduits
    entityManager.createSkitterer(480f, 320f)
    entityManager.createSkitterer(550f, 260f)
    entityManager.createSteamVane(950f, 320f)

    // Scrapper's Alley
    entityManager.createSkitterer(350f, 1100f)
    entityManager.createSkitterer(600f, 1300f)
    entityManager.createSkitterer(420f, 1420f)

    // Central Sluice
    entityManager.createSteamVane(1200f, 1050f)
    entityManager.createSteamVane(1380f, 1200f)

    // Siphon Boiler Hall - Overseer Malakor
    entityManager.createOverseerBoss(1400f, 1950f)

    // 3. Ambient Reactor Lights (Flue & Conduits)
    entityManager.createPointLight(1400f, 2200f, 0.10f, 0.95f, 0.45f, 420f, 1.2f)
    entityManager.createPointLight(620f, 220f, 0.95f, 0.65f, 0.10f, 160f, 0.9f)
    entityManager.createPointLight(1480f, 1020f, 0.95f, 0.65f, 0.10f, 160f, 0.9f)
  }

  fun update(dt: Float) {
    physicsSystem.update(entityManager, dt)
    enemyAiSystem.update(entityManager, wardenId, dt)
    combatSystem.update(entityManager, wardenId, dt)
  }

  fun setWardenVelocity(dir: Vector2) {
    val vel = entityManager.velocities[wardenId] ?: return
    val warden = entityManager.wardens[wardenId] ?: return
    if (warden.isDashing) return

    val maxSpd = vel.maxSpeed
    vel.vx = dir.x * maxSpd
    vel.vy = dir.y * maxSpd
  }

  fun triggerWardenAttack(): Boolean {
    return combatSystem.triggerHammerAttack(entityManager, wardenId, baseDamage = 38f)
  }

  fun triggerWardenDash(): Boolean {
    val vel = entityManager.velocities[wardenId] ?: return false
    return combatSystem.triggerSteamDash(entityManager, wardenId, Vector2(vel.vx, vel.vy))
  }
}
