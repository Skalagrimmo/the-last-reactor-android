package com.example

import com.example.ecs.*
import com.example.engine.GameMap
import com.example.engine.SoundSynthesizer
import com.example.model.EnemyType
import com.example.model.Vector2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test

class EcsUnitTest {

  @Test
  fun testEcsPrefabCreationAndComponentAttachment() {
    val em = EntityManager()

    val wardenId = em.createWarden(250f, 250f)
    assertNotNull(em.transforms[wardenId])
    assertNotNull(em.velocities[wardenId])
    assertNotNull(em.renderables[wardenId])
    assertNotNull(em.healths[wardenId])
    assertNotNull(em.wardens[wardenId])
    assertNotNull(em.lights[wardenId])
    assertTrue(em.has<PlayerTag>(wardenId))

    val skittererId = em.createSkitterer(400f, 400f)
    assertNotNull(em.enemies[skittererId])
    assertEquals(EnemyType.SKITTERER, em.enemies[skittererId]!!.type)
    assertNotNull(em.lights[skittererId])
    assertTrue(em.has<EnemyTag>(skittererId))

    val bossId = em.createOverseerBoss(1400f, 1950f)
    assertEquals(EnemyType.BOSS_OVERSEER, em.enemies[bossId]!!.type)
    assertEquals(950f, em.healths[bossId]!!.max, 0.1f)
    assertTrue(em.has<BossTag>(bossId))
  }

  @Test
  fun testGenericComponentOperationsAndFiltering() {
    val em = EntityManager()

    val wardenId = em.createWarden(250f, 250f)
    val skittererId = em.createSkitterer(400f, 400f)
    val steamVaneId = em.createSteamVane(600f, 600f)

    // Verify tag queries
    val playerEntities = em.getEntitiesWith(PlayerTag::class)
    assertEquals(1, playerEntities.size)
    assertEquals(wardenId, playerEntities[0])

    val enemyEntities = em.getEntitiesWith(EnemyTag::class)
    assertEquals(2, enemyEntities.size)
    assertTrue(enemyEntities.contains(skittererId))
    assertTrue(enemyEntities.contains(steamVaneId))

    // Generic get / has / remove
    assertTrue(em.has<TransformComponent>(wardenId))
    val transform = em.get<TransformComponent>(wardenId)
    assertNotNull(transform)
    assertEquals(250f, transform!!.x, 0.01f)

    // Remove WeaponComponent
    assertTrue(em.has<WeaponComponent>(wardenId))
    em.remove<WeaponComponent>(wardenId)
    assertFalse(em.has<WeaponComponent>(wardenId))
    assertNull(em.get<WeaponComponent>(wardenId))
  }

  @Test
  fun testEcsMovementSystemVelocityIntegration() {
    val map = GameMap()
    val em = EntityManager()
    val movement = MovementSystem(map)

    val wardenId = em.createWarden(250f, 250f)
    val vel = em.velocities[wardenId]!!
    vel.vx = 100f
    vel.vy = 0f

    movement.update(em, 0.1f)

    val transform = em.transforms[wardenId]!!
    assertTrue(transform.x > 250f)
  }

  @Test
  fun testEcsCombatSystemHammerAttackAndHealthDeduction() {
    val map = GameMap()
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val synth = SoundSynthesizer(scope)
    val em = EntityManager()
    val combat = CombatSystem(synth)

    val wardenId = em.createWarden(250f, 250f)
    // Place an enemy right in front of Warden (angle = 0 is +X)
    val skittererId = em.createSkitterer(280f, 250f)

    val initialEnemyHealth = em.healths[skittererId]!!.current
    val hit = combat.triggerHammerAttack(em, wardenId, baseDamage = 38f, reach = 60f)

    assertTrue("Hammer swing should connect with enemy in range", hit)
    assertTrue("Enemy health should decrease after being hit", em.healths[skittererId]!!.current < initialEnemyHealth)
  }

  @Test
  fun testEcsCombatSystemDashDeductsSteam() {
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val synth = SoundSynthesizer(scope)
    val em = EntityManager()
    val combat = CombatSystem(synth)

    val wardenId = em.createWarden(250f, 250f)
    val initialSteam = em.wardens[wardenId]!!.steam

    val dashed = combat.triggerSteamDash(em, wardenId, Vector2(1f, 0f))
    assertTrue(dashed)
    assertTrue(em.wardens[wardenId]!!.isDashing)
    assertTrue(em.wardens[wardenId]!!.steam < initialSteam)
    assertTrue(em.healths[wardenId]!!.invulnerableTimer > 0f)
  }
}
