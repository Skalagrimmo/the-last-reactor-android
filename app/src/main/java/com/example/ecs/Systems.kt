package com.example.ecs

import com.example.engine.GameMap
import com.example.engine.SoundSynthesizer
import com.example.model.EnemyState
import com.example.model.EnemyType
import com.example.model.Vector2
import kotlin.math.*
import kotlin.random.Random

class MovementSystem(private val map: GameMap) {
  fun update(em: EntityManager, dt: Float) {
    for (id in em.activeEntities) {
      val transform = em.transforms[id] ?: continue
      val velocity = em.velocities[id] ?: continue
      val collider = em.colliders[id]

      val nextX = transform.x + velocity.vx * dt
      val nextY = transform.y + velocity.vy * dt
      val radius = collider?.radius ?: 16f

      // Check X axis collision
      if (!map.isCollidingWithWalls(Vector2(nextX, transform.y), radius)) {
        transform.x = nextX
      } else {
        velocity.vx = 0f
      }

      // Check Y axis collision
      if (!map.isCollidingWithWalls(Vector2(transform.x, nextY), radius)) {
        transform.y = nextY
      } else {
        velocity.vy = 0f
      }

      // Update facing angle if moving
      val speedSq = velocity.vx * velocity.vx + velocity.vy * velocity.vy
      if (speedSq > 10f) {
        transform.angle = atan2(velocity.vy, velocity.vx)
      }

      // Apply drag
      val isWardenDashing = em.wardens[id]?.isDashing == true
      if (!isWardenDashing) {
        velocity.vx *= velocity.drag
        velocity.vy *= velocity.drag
      }
    }
  }
}

class EnemyAISystem(
  private val map: GameMap,
  private val soundSynth: SoundSynthesizer
) {
  fun update(em: EntityManager, wardenId: EntityId, dt: Float) {
    val wardenTransform = em.transforms[wardenId] ?: return
    val wardenPos = Vector2(wardenTransform.x, wardenTransform.y)
    val wardenHealth = em.healths[wardenId]

    for (id in em.activeEntities) {
      val ai = em.enemies[id] ?: continue
      val transform = em.transforms[id] ?: continue
      val velocity = em.velocities[id] ?: continue
      val health = em.healths[id] ?: continue

      if (health.current <= 0f) continue

      if (ai.frozenTimer > 0f) ai.frozenTimer -= dt
      if (ai.attackCooldown > 0f) ai.attackCooldown -= dt

      val enemyPos = Vector2(transform.x, transform.y)
      val distToWarden = enemyPos.distanceTo(wardenPos)
      val dirToWarden = (wardenPos - enemyPos).normalized()
      val speedMod = if (ai.frozenTimer > 0f) 0.4f else 1f
      val speed = ai.type.speed * speedMod

      when (ai.type) {
        EnemyType.SKITTERER -> {
          if (distToWarden <= 300f) {
            transform.angle = atan2(dirToWarden.y, dirToWarden.x)
            val jitter = Vector2(-dirToWarden.y, dirToWarden.x) * (sin(System.currentTimeMillis() * 0.01f) * 35f)
            val move = (dirToWarden * speed) + jitter
            velocity.vx = move.x
            velocity.vy = move.y

            // Melee bite
            if (distToWarden <= ai.attackRange && ai.attackCooldown <= 0f && (wardenHealth?.invulnerableTimer ?: 0f) <= 0f) {
              val dmg = (ai.type.baseDamage - (wardenHealth?.defense ?: 0f)).coerceAtLeast(4f)
              wardenHealth?.current = (wardenHealth?.current ?: 0f) - dmg
              wardenHealth?.invulnerableTimer = 0.35f
              ai.attackCooldown = 0.9f
              soundSynth.playPlayerHurt()
            }
          }
        }

        EnemyType.STEAM_VANE -> {
          if (distToWarden <= 400f) {
            transform.angle = atan2(dirToWarden.y, dirToWarden.x)
            val desiredDist = 180f
            val moveDir = if (distToWarden < desiredDist - 30f) -dirToWarden else if (distToWarden > desiredDist + 30f) dirToWarden else Vector2()
            val move = moveDir * speed
            velocity.vx = move.x
            velocity.vy = move.y

            if (ai.attackCooldown <= 0f) {
              ai.attackCooldown = 2.4f
              soundSynth.playSteamDash()
              // Ranged steam projectile damage check
              if ((wardenHealth?.invulnerableTimer ?: 0f) <= 0f && distToWarden <= 260f) {
                val dmg = (ai.type.baseDamage - (wardenHealth?.defense ?: 0f)).coerceAtLeast(6f)
                wardenHealth?.current = (wardenHealth?.current ?: 0f) - dmg
                wardenHealth?.invulnerableTimer = 0.4f
                soundSynth.playPlayerHurt()
              }
            }
          }
        }

        EnemyType.SLAG_CORRUPTOR -> {
          if (distToWarden <= 340f) {
            transform.angle = atan2(dirToWarden.y, dirToWarden.x)
            val move = dirToWarden * speed
            velocity.vx = move.x
            velocity.vy = move.y

            if (distToWarden <= ai.attackRange && ai.attackCooldown <= 0f && (wardenHealth?.invulnerableTimer ?: 0f) <= 0f) {
              val dmg = (ai.type.baseDamage - (wardenHealth?.defense ?: 0f)).coerceAtLeast(10f)
              wardenHealth?.current = (wardenHealth?.current ?: 0f) - dmg
              wardenHealth?.invulnerableTimer = 0.45f
              ai.attackCooldown = 1.3f
              soundSynth.playHammerHit()
            }
          }
        }

        EnemyType.BOSS_OVERSEER -> {
          val hpPercent = health.current / health.max
          ai.phase = if (hpPercent > 0.65f) 1 else if (hpPercent > 0.30f) 2 else 3

          transform.angle = atan2(dirToWarden.y, dirToWarden.x)
          val bossSpeed = speed * (if (ai.phase == 3) 1.5f else 1.1f)
          val move = dirToWarden * bossSpeed
          velocity.vx = move.x
          velocity.vy = move.y

          if (distToWarden <= ai.attackRange && ai.attackCooldown <= 0f && (wardenHealth?.invulnerableTimer ?: 0f) <= 0f) {
            val dmg = (ai.type.baseDamage - (wardenHealth?.defense ?: 0f)).coerceAtLeast(14f)
            wardenHealth?.current = (wardenHealth?.current ?: 0f) - dmg
            wardenHealth?.invulnerableTimer = 0.5f
            ai.attackCooldown = if (ai.phase == 3) 0.85f else 1.3f
            soundSynth.playHammerHit()
          }
        }
      }
    }
  }
}

class CombatSystem(
  private val soundSynth: SoundSynthesizer
) {
  fun update(em: EntityManager, wardenId: EntityId, dt: Float) {
    val warden = em.wardens[wardenId] ?: return
    val health = em.healths[wardenId]

    // Timers
    if (health != null && health.invulnerableTimer > 0f) {
      health.invulnerableTimer -= dt
    }

    if (warden.attackCooldown > 0f) warden.attackCooldown -= dt
    if (warden.dashCooldown > 0f) warden.dashCooldown -= dt

    if (warden.isAttacking) {
      warden.attackTimer -= dt
      if (warden.attackTimer <= 0f) warden.isAttacking = false
    }

    if (warden.isDashing) {
      warden.dashTimer -= dt
      if (warden.dashTimer <= 0f) warden.isDashing = false
    }

    // Steam passive recharge
    warden.steam = (warden.steam + 18f * dt).coerceAtMost(warden.maxSteam)
  }

  fun triggerHammerAttack(em: EntityManager, wardenId: EntityId, baseDamage: Float, reach: Float = 68f): Boolean {
    val warden = em.wardens[wardenId] ?: return false
    val transform = em.transforms[wardenId] ?: return false
    if (warden.attackCooldown > 0f || warden.isDashing) return false

    warden.isAttacking = true
    warden.attackTimer = 0.20f
    warden.attackCooldown = 0.40f

    soundSynth.playHammerSwing()

    val wardenPos = Vector2(transform.x, transform.y)
    var hitAny = false

    for (id in em.activeEntities) {
      if (id == wardenId) continue
      val enemyAI = em.enemies[id] ?: continue
      val enemyTransform = em.transforms[id] ?: continue
      val enemyHealth = em.healths[id] ?: continue
      val enemyCollider = em.colliders[id] ?: continue

      if (enemyHealth.current <= 0f) continue

      val enemyPos = Vector2(enemyTransform.x, enemyTransform.y)
      val dist = wardenPos.distanceTo(enemyPos)
      if (dist <= reach + enemyCollider.radius) {
        val angleToEnemy = atan2(enemyPos.y - wardenPos.y, enemyPos.x - wardenPos.x)
        var angleDiff = abs(angleToEnemy - transform.angle)
        if (angleDiff > Math.PI) angleDiff = (2 * Math.PI - angleDiff).toFloat()

        if (angleDiff <= Math.toRadians(45.0)) {
          hitAny = true
          val netDamage = (baseDamage * (1f + (warden.comboStep - 1) * 0.25f) - enemyHealth.defense).coerceAtLeast(8f)
          enemyHealth.current -= netDamage

          // Knockback
          val knockDir = (enemyPos - wardenPos).normalized()
          val vel = em.velocities[id]
          if (vel != null) {
            val knockStrength = if (enemyAI.type == EnemyType.BOSS_OVERSEER) 60f else 220f
            vel.vx = knockDir.x * knockStrength
            vel.vy = knockDir.y * knockStrength
          }
        }
      }
    }

    if (hitAny) {
      soundSynth.playHammerHit()
      warden.comboStep = if (warden.comboStep >= 3) 1 else warden.comboStep + 1
    } else {
      warden.comboStep = 1
    }

    return hitAny
  }

  fun triggerSteamDash(em: EntityManager, wardenId: EntityId, inputVel: Vector2): Boolean {
    val warden = em.wardens[wardenId] ?: return false
    val transform = em.transforms[wardenId] ?: return false
    val velocity = em.velocities[wardenId] ?: return false
    val health = em.healths[wardenId] ?: return false

    if (warden.dashCooldown > 0f || warden.isDashing || warden.steam < 20f) return false

    warden.isDashing = true
    warden.dashTimer = 0.22f
    warden.dashCooldown = 0.85f
    warden.steam -= 20f
    health.invulnerableTimer = 0.30f

    val dashDir = if (inputVel.length() > 0.1f) inputVel.normalized() else Vector2.fromAngle(transform.angle)
    velocity.vx = dashDir.x * 480f
    velocity.vy = dashDir.y * 480f

    soundSynth.playSteamDash()
    return true
  }
}
