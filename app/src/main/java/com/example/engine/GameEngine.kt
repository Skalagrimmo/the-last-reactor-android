package com.example.engine

import android.os.Vibrator
import com.example.model.*
import kotlin.math.*
import kotlin.random.Random

class GameEngine(
  val map: GameMap,
  val soundSynth: SoundSynthesizer,
  private val vibrator: Vibrator? = null
) {
  var playerPos = Vector2(250f, 250f)
  var playerVel = Vector2()
  var playerFacingAngle = 0f
  var playerHealth = 100f
  var playerMaxHealth = 100f
  var playerSteam = 80f
  var playerMaxSteam = 80f
  var playerLevel = 1
  var playerXp = 0
  var playerScrap = 40

  var stats = PlayerStats()
  var equippedWeapon: Weapon = EquipmentRegistry.WEAPONS[0]
  var equippedArmor: Armor = EquipmentRegistry.ARMORS[0]

  val vialInventory = mutableMapOf(
    VialType.CORROSIVE_VITRIOL to 3,
    VialType.PYRETIC_PHLOGISTON to 2,
    VialType.CRYO_QUENCH to 2
  )
  var selectedVial = VialType.CORROSIVE_VITRIOL

  var hasMasterKey = false
  var defeatedBoss = false

  // Combat State
  var isAttacking = false
  var attackTimer = 0f
  var attackCooldown = 0f
  var comboStep = 1

  var isDashing = false
  var dashTimer = 0f
  var dashCooldown = 0f
  val dashDuration = 0.22f
  val dashSpeed = 480f

  var invulnerableTimer = 0f

  // Collections
  val enemies = mutableListOf<Enemy>()
  val projectiles = mutableListOf<Projectile>()
  val particles = mutableListOf<Particle>()
  val lootDrops = mutableListOf<LootDrop>()
  val floatingTexts = mutableListOf<FloatingText>()

  data class FloatingText(
    var text: String,
    var pos: Vector2,
    var colorHex: Long,
    var lifetime: Float = 0f,
    val maxLifetime: Float = 0.9f
  )

  var currentPromptMessage: String? = null
  var promptTimer: Float = 0f

  var isGameOver = false
  var isVictory = false

  init {
    resetLevel()
  }

  fun resetLevel() {
    enemies.clear()
    projectiles.clear()
    particles.clear()
    lootDrops.clear()
    floatingTexts.clear()

    // Spawn Enemies in Sectors
    // 1. Catwalk & North Conduits
    enemies.add(Enemy("skitt_1", EnemyType.SKITTERER, Vector2(480f, 320f)))
    enemies.add(Enemy("skitt_2", EnemyType.SKITTERER, Vector2(550f, 260f)))
    enemies.add(Enemy("vane_1", EnemyType.STEAM_VANE, Vector2(950f, 320f)))

    // 2. Scrapper's Alley
    enemies.add(Enemy("skitt_3", EnemyType.SKITTERER, Vector2(350f, 1100f)))
    enemies.add(Enemy("skitt_4", EnemyType.SKITTERER, Vector2(600f, 1300f)))
    enemies.add(Enemy("skitt_5", EnemyType.SKITTERER, Vector2(420f, 1420f)))
    enemies.add(Enemy("slag_1", EnemyType.SLAG_CORRUPTOR, Vector2(520f, 920f)))

    // 3. Central Sluice Guard
    enemies.add(Enemy("vane_2", EnemyType.STEAM_VANE, Vector2(1200f, 1050f)))
    enemies.add(Enemy("vane_3", EnemyType.STEAM_VANE, Vector2(1380f, 1200f)))
    enemies.add(Enemy("skitt_6", EnemyType.SKITTERER, Vector2(1050f, 1300f)))

    // 4. The Grand Boiler Hall - Overseer Malakor
    enemies.add(
      Enemy(
        "boss_malakor",
        EnemyType.BOSS_OVERSEER,
        Vector2(1400f, 1950f)
      )
    )

    updateDerivedStats()
  }

  fun updateDerivedStats() {
    playerMaxHealth = stats.maxHealth()
    playerMaxSteam = stats.maxSteam() + equippedArmor.steamCapacityBonus
    playerHealth = playerHealth.coerceAtMost(playerMaxHealth)
    playerSteam = playerSteam.coerceAtMost(playerMaxSteam)
  }

  fun showPrompt(msg: String) {
    currentPromptMessage = msg
    promptTimer = 2.5f
  }

  // Primary Input Handlers
  fun movePlayer(inputDir: Vector2) {
    if (isDashing) return
    val moveSpeed = 210f
    playerVel = inputDir.normalized() * moveSpeed
    if (inputDir.length() > 0.1f) {
      playerFacingAngle = atan2(inputDir.y, inputDir.x)
    }
  }

  fun triggerHammerAttack() {
    if (attackCooldown > 0f || isDashing) return
    isAttacking = true
    attackTimer = 0.20f
    attackCooldown = equippedWeapon.attackRate

    soundSynth.playHammerSwing()

    // Compute attack hit area in front of player
    val reach = 68f
    val arcAngle = Math.toRadians(85.0).toFloat()
    val attackDir = Vector2.fromAngle(playerFacingAngle)
    val attackCenter = playerPos + (attackDir * (reach * 0.55f))

    // Particle swing arc
    for (i in -3..3) {
      val angle = playerFacingAngle + (i * 0.22f)
      val pPos = playerPos + Vector2.fromAngle(angle, reach * 0.75f)
      val pVel = Vector2.fromAngle(angle + 0.3f, 40f)
      particles.add(
        Particle(
          pos = pPos,
          vel = pVel,
          colorHex = 0xFFFCD34D, // Gold/sparks
          size = 4f,
          maxLifetime = 0.25f,
          shape = ParticleShape.SPARK
        )
      )
    }

    var hitAny = false
    val damage = (equippedWeapon.damage * (1f + (comboStep - 1) * 0.25f))

    for (enemy in enemies) {
      if (enemy.health <= 0f) continue
      val dist = playerPos.distanceTo(enemy.pos)
      if (dist <= reach + enemy.type.sizeRadius) {
        val angleToEnemy = atan2(enemy.pos.y - playerPos.y, enemy.pos.x - playerPos.x)
        var angleDiff = abs(angleToEnemy - playerFacingAngle)
        if (angleDiff > Math.PI) angleDiff = (2 * Math.PI - angleDiff).toFloat()

        if (angleDiff <= arcAngle / 2f) {
          hitAny = true
          // Apply damage
          val effectiveDefense = if (enemy.corrodedTimer > 0f) enemy.defense * 0.35f else enemy.defense
          val finalDamage = (damage - effectiveDefense).coerceAtLeast(8f)

          enemy.health -= finalDamage
          enemy.state = EnemyState.STUNNED
          enemy.stateTimer = 0.25f

          // Knockback
          val knockDir = (enemy.pos - playerPos).normalized()
          val knockStrength = equippedWeapon.knockback * (if (enemy.type == EnemyType.BOSS_OVERSEER) 0.25f else 1f)
          enemy.vel = knockDir * knockStrength

          // Spawn impact sparks & text
          spawnImpactSparks(enemy.pos, 0xFFF59E0B)
          addFloatingText("-${finalDamage.toInt()}", enemy.pos + Vector2(0f, -20f), 0xFFFBBF24)

          if (enemy.health <= 0f) {
            onEnemyDefeated(enemy)
          }
        }
      }
    }

    if (hitAny) {
      soundSynth.playHammerHit()
      vibrate(40)
      comboStep = if (comboStep >= 3) 1 else comboStep + 1
    } else {
      comboStep = 1
    }
  }

  fun triggerSteamDash() {
    if (dashCooldown > 0f || isDashing || playerSteam < 20f) return
    isDashing = true
    dashTimer = dashDuration
    dashCooldown = 0.85f
    playerSteam -= 20f
    invulnerableTimer = dashDuration + 0.1f

    val dashDir = if (playerVel.length() > 0.1f) playerVel.normalized() else Vector2.fromAngle(playerFacingAngle)
    playerVel = dashDir * dashSpeed

    soundSynth.playSteamDash()
    vibrate(25)

    // Emit billowing steam cloud
    for (i in 0..12) {
      val offset = Vector2(Random.nextFloat() * 24f - 12f, Random.nextFloat() * 24f - 12f)
      val sVel = Vector2(Random.nextFloat() * 40f - 20f, Random.nextFloat() * 40f - 20f)
      particles.add(
        Particle(
          pos = playerPos + offset,
          vel = sVel,
          colorHex = 0xAAFFFFFF,
          size = 14f + Random.nextFloat() * 10f,
          maxLifetime = 0.5f,
          shape = ParticleShape.CLOUD
        )
      )
    }
  }

  fun triggerVialThrow() {
    val count = vialInventory[selectedVial] ?: 0
    if (count <= 0) {
      showPrompt("No ${selectedVial.displayName} left!")
      return
    }
    vialInventory[selectedVial] = count - 1

    val throwDir = Vector2.fromAngle(playerFacingAngle)
    val projSpeed = 380f
    projectiles.add(
      Projectile(
        id = "vial_${System.currentTimeMillis()}",
        pos = playerPos + (throwDir * 20f),
        vel = throwDir * projSpeed,
        isPlayer = true,
        vialType = selectedVial,
        damage = 45f * stats.vialPotencyMultiplier(),
        radius = 10f,
        lifetime = 1.2f
      )
    )

    soundSynth.playHammerSwing()
  }

  fun triggerInteract() {
    // 1. Check nearest valve
    for (valve in map.valves) {
      val dist = playerPos.distanceTo(valve.pos)
      if (dist <= 65f) {
        toggleValve(valve)
        return
      }
    }

    // 2. Check nearest door
    for (door in map.doors) {
      val doorCenter = Vector2(door.pos.x + door.width / 2f, door.pos.y + door.height / 2f)
      val dist = playerPos.distanceTo(doorCenter)
      if (dist <= 90f) {
        if (door.isOpen) {
          showPrompt("${door.label} is already open.")
        } else if (door.requiresKey && !hasMasterKey) {
          showPrompt("Locked! Requires the Master Siphon Key from Overseer Malakor.")
        } else if (door.requiresKey && hasMasterKey) {
          door.isOpen = true
          soundSynth.playValveTurn()
          showPrompt("Unlocked ${door.label} with Master Siphon Key!")
          addFloatingText("VAULT OPENED", doorCenter, 0xFF10B981)
          isVictory = true
        } else {
          showPrompt("${door.label} is locked by hydraulic pressure. Turn connected valves!")
        }
        return
      }
    }

    // 3. Check nearest Lore Tablet
    for (lore in map.loreEntries) {
      // Find sector lore points
      val lorePos = when (lore.id) {
        "lore_catwalk" -> Vector2(380f, 220f)
        "lore_skitterers" -> Vector2(400f, 850f)
        else -> Vector2(1400f, 1600f)
      }
      if (playerPos.distanceTo(lorePos) <= 70f) {
        lore.isDiscovered = true
        showPrompt("Discovered: ${lore.title}")
        addFloatingText("LORE DISCOVERED", lorePos, 0xFF38BDF8)
        soundSynth.playLevelUp()
        return
      }
    }

    showPrompt("Nothing nearby to operate.")
  }

  private fun toggleValve(valve: SteamConduitValve) {
    valve.isTurned = !valve.isTurned
    soundSynth.playValveTurn()
    vibrate(60)

    // Deactivate hazard
    if (valve.controlledHazardId.isNotEmpty()) {
      val hazard = map.hazards.find { it.id == valve.controlledHazardId }
      if (hazard != null) {
        hazard.isActive = !valve.isTurned
      }
    }

    // Open connected bulkhead door
    if (valve.controlledDoorId.isNotEmpty()) {
      val door = map.doors.find { it.id == valve.controlledDoorId }
      if (door != null) {
        door.isOpen = valve.isTurned
      }
    }

    // Arena boss valve relief mechanic
    if (valve.id == "valve_boiler_relief") {
      val boss = enemies.find { it.type == EnemyType.BOSS_OVERSEER }
      if (boss != null && boss.health > 0f) {
        boss.defense = (boss.defense - 6f).coerceAtLeast(4f)
        boss.frozenTimer = 4f
        showPrompt("Core Relief Opened! Malakor's armor vented and chilled!")
        addFloatingText("ARMOR VENTED!", boss.pos, 0xFF38BDF8)
      }
    }

    val stateText = if (valve.isTurned) "OPENED & VENTED" else "CLOSED"
    showPrompt("${valve.label}: $stateText")
    addFloatingText(stateText, valve.pos + Vector2(0f, -25f), 0xFF10B981)

    // Steam burst particles
    for (i in 0..16) {
      val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
      val speed = 60f + Random.nextFloat() * 100f
      particles.add(
        Particle(
          pos = valve.pos,
          vel = Vector2.fromAngle(angle, speed),
          colorHex = 0xFFE2E8F0,
          size = 12f,
          maxLifetime = 0.6f,
          shape = ParticleShape.CLOUD
        )
      )
    }
  }

  private fun onEnemyDefeated(enemy: Enemy) {
    soundSynth.playEnemyHurt()
    spawnImpactSparks(enemy.pos, 0xFFEF4444)
    addFloatingText("+${(enemy.type.maxHealth * 0.4f).toInt()} XP", enemy.pos, 0xFF10B981)

    // Grant XP & Scrap
    val xpGain = (enemy.type.maxHealth * 0.4f).toInt()
    playerXp += xpGain
    val scrapGain = when (enemy.type) {
      EnemyType.SKITTERER -> 8 + Random.nextInt(6)
      EnemyType.STEAM_VANE -> 20 + Random.nextInt(12)
      EnemyType.SLAG_CORRUPTOR -> 45 + Random.nextInt(20)
      EnemyType.BOSS_OVERSEER -> 150
    }
    playerScrap += scrapGain

    // Check level up
    val neededXp = playerLevel * 80
    if (playerXp >= neededXp) {
      playerLevel++
      playerXp -= neededXp
      playerHealth = playerMaxHealth
      soundSynth.playLevelUp()
      showPrompt("LEVEL UP! Warden Level $playerLevel Reached!")
      addFloatingText("LEVEL UP!", playerPos, 0xFFF59E0B)
    }

    // Drops
    lootDrops.add(
      LootDrop(
        id = "scrap_${System.currentTimeMillis()}",
        pos = enemy.pos + Vector2(Random.nextFloat() * 16f - 8f, Random.nextFloat() * 16f - 8f),
        type = LootType.SCRAP,
        value = scrapGain,
        label = "$scrapGain Alchemical Scrap"
      )
    )

    if (Random.nextFloat() < 0.45f) {
      val randomVial = VialType.values().random()
      lootDrops.add(
        LootDrop(
          id = "vial_${System.currentTimeMillis()}",
          pos = enemy.pos,
          type = when (randomVial) {
            VialType.CORROSIVE_VITRIOL -> LootType.VIAL_CORROSIVE
            VialType.PYRETIC_PHLOGISTON -> LootType.VIAL_PYRETIC
            VialType.CRYO_QUENCH -> LootType.VIAL_CRYO
          },
          value = 1,
          label = randomVial.displayName
        )
      )
    }

    // Boss rewards
    if (enemy.type == EnemyType.BOSS_OVERSEER) {
      defeatedBoss = true
      hasMasterKey = true
      soundSynth.playBossPhaseShift()
      showPrompt("OVERSEER MALAKOR CRUSHED! Master Siphon Key Acquired!")
      addFloatingText("MASTER KEY ACQUIRED", enemy.pos, 0xFF8B5CF6)
      lootDrops.add(
        LootDrop(
          id = "key_master",
          pos = enemy.pos,
          type = LootType.KEY_ITEM,
          value = 1,
          label = "Master Siphon Key"
        )
      )
    }
  }

  fun update(dt: Float) {
    if (isGameOver) return

    // 1. Timers
    if (promptTimer > 0f) {
      promptTimer -= dt
      if (promptTimer <= 0f) currentPromptMessage = null
    }

    if (attackCooldown > 0f) attackCooldown -= dt
    if (dashCooldown > 0f) dashCooldown -= dt
    if (invulnerableTimer > 0f) invulnerableTimer -= dt

    if (isAttacking) {
      attackTimer -= dt
      if (attackTimer <= 0f) isAttacking = false
    }

    if (isDashing) {
      dashTimer -= dt
      if (dashTimer <= 0f) isDashing = false
    }

    // Steam Regen
    val steamRegenRate = 18f
    playerSteam = (playerSteam + steamRegenRate * dt).coerceAtMost(playerMaxSteam)

    // 2. Player Movement & Collision
    val newPos = playerPos + (playerVel * dt)
    if (!map.isCollidingWithWalls(Vector2(newPos.x, playerPos.y), 18f)) {
      playerPos = Vector2(newPos.x, playerPos.y)
    }
    if (!map.isCollidingWithWalls(Vector2(playerPos.x, newPos.y), 18f)) {
      playerPos = Vector2(playerPos.x, newPos.y)
    }

    if (!isDashing) {
      // Friction
      playerVel = playerVel * 0.82f
    }

    // 3. Hazard checks on Player
    if (invulnerableTimer <= 0f) {
      val hitHazard = map.getIntersectingHazard(playerPos, 14f)
      if (hitHazard != null) {
        val hazardDmg = 25f * dt
        playerHealth -= hazardDmg
        soundSynth.playPlayerHurt()
        vibrate(30)
        spawnImpactSparks(playerPos, 0xFFEF4444)
        if (playerHealth <= 0f) {
          onPlayerDied()
        }
      }
    }

    // 4. Update Hazards
    for (hazard in map.hazards) {
      if (hazard.frozenTimer > 0f) {
        hazard.frozenTimer -= dt
      }
      if (hazard.isPulsing) {
        hazard.pulseTimer += dt
        if (hazard.pulseTimer >= 3.5f) {
          hazard.pulseTimer = 0f
          hazard.isActive = !hazard.isActive
        }
      }
    }

    // 5. Update Projectiles
    val projIter = projectiles.iterator()
    while (projIter.hasNext()) {
      val proj = projIter.next()
      proj.pos = proj.pos + (proj.vel * dt)
      proj.lifetime -= dt

      // Wall collision
      if (map.isCollidingWithWalls(proj.pos, proj.radius) || proj.lifetime <= 0f) {
        onProjectileImpact(proj, null)
        projIter.remove()
        continue
      }

      // Enemy hit
      if (proj.isPlayer) {
        var hitEnemy: Enemy? = null
        for (enemy in enemies) {
          if (enemy.health > 0f && proj.pos.distanceTo(enemy.pos) <= enemy.type.sizeRadius + proj.radius) {
            hitEnemy = enemy
            break
          }
        }
        if (hitEnemy != null) {
          onProjectileImpact(proj, hitEnemy)
          projIter.remove()
          continue
        }
      } else {
        // Enemy projectile hits player
        if (invulnerableTimer <= 0f && proj.pos.distanceTo(playerPos) <= 18f + proj.radius) {
          val netDamage = (proj.damage - equippedArmor.defense).coerceAtLeast(6f)
          playerHealth -= netDamage
          soundSynth.playPlayerHurt()
          vibrate(50)
          addFloatingText("-${netDamage.toInt()}", playerPos + Vector2(0f, -20f), 0xFFEF4444)
          invulnerableTimer = 0.35f
          if (playerHealth <= 0f) onPlayerDied()
          projIter.remove()
          continue
        }
      }
    }

    // 6. Update Enemies
    for (enemy in enemies) {
      if (enemy.health <= 0f) continue
      updateEnemy(enemy, dt)
    }

    // 7. Update Loot Drops
    val lootIter = lootDrops.iterator()
    while (lootIter.hasNext()) {
      val loot = lootIter.next()
      if (playerPos.distanceTo(loot.pos) <= 35f) {
        collectLoot(loot)
        lootIter.remove()
      }
    }

    // 8. Update Particles
    val partIter = particles.iterator()
    while (partIter.hasNext()) {
      val p = partIter.next()
      p.pos = p.pos + (p.vel * dt)
      p.lifetime += dt
      p.alpha = (1f - (p.lifetime / p.maxLifetime)).coerceIn(0f, 1f)
      if (p.lifetime >= p.maxLifetime) {
        partIter.remove()
      }
    }

    // 9. Update Floating Texts
    val textIter = floatingTexts.iterator()
    while (textIter.hasNext()) {
      val ft = textIter.next()
      ft.pos = ft.pos + Vector2(0f, -25f * dt)
      ft.lifetime += dt
      if (ft.lifetime >= ft.maxLifetime) {
        textIter.remove()
      }
    }
  }

  private fun updateEnemy(enemy: Enemy, dt: Float) {
    // Status effects
    if (enemy.frozenTimer > 0f) enemy.frozenTimer -= dt
    if (enemy.corrodedTimer > 0f) {
      enemy.corrodedTimer -= dt
      enemy.health -= 6f * dt
      if (enemy.health <= 0f) {
        onEnemyDefeated(enemy)
        return
      }
    }
    if (enemy.burningTimer > 0f) {
      enemy.burningTimer -= dt
      enemy.health -= 12f * dt
      if (enemy.health <= 0f) {
        onEnemyDefeated(enemy)
        return
      }
    }

    if (enemy.state == EnemyState.STUNNED) {
      enemy.stateTimer -= dt
      if (enemy.stateTimer <= 0f) enemy.state = EnemyState.PURSUING
      // Apply knockback slide
      enemy.pos = enemy.pos + (enemy.vel * dt)
      enemy.vel = enemy.vel * 0.85f
      return
    }

    val distToPlayer = enemy.pos.distanceTo(playerPos)
    val dirToPlayer = (playerPos - enemy.pos).normalized()
    val speedMod = if (enemy.frozenTimer > 0f) 0.4f else 1f
    val speed = enemy.type.speed * speedMod

    if (enemy.attackCooldown > 0f) enemy.attackCooldown -= dt

    when (enemy.type) {
      EnemyType.SKITTERER -> {
        // Fast swarm melee
        if (distToPlayer <= 320f) {
          enemy.facingAngle = atan2(dirToPlayer.y, dirToPlayer.x)
          // Add erratic jittering
          val jitter = Vector2(-dirToPlayer.y, dirToPlayer.x) * (sin(System.currentTimeMillis() * 0.01f) * 40f)
          val moveVec = (dirToPlayer * speed) + jitter
          val targetPos = enemy.pos + (moveVec * dt)
          if (!map.isCollidingWithWalls(targetPos, enemy.type.sizeRadius)) {
            enemy.pos = targetPos
          }

          // Melee bite
          if (distToPlayer <= 36f && enemy.attackCooldown <= 0f && invulnerableTimer <= 0f) {
            val dmg = (enemy.type.baseDamage - equippedArmor.defense).coerceAtLeast(4f)
            playerHealth -= dmg
            soundSynth.playPlayerHurt()
            vibrate(35)
            invulnerableTimer = 0.35f
            enemy.attackCooldown = 0.9f
            addFloatingText("-${dmg.toInt()}", playerPos + Vector2(0f, -20f), 0xFFEF4444)
            if (playerHealth <= 0f) onPlayerDied()
          }
        }
      }

      EnemyType.STEAM_VANE -> {
        // Ranged turret / keeps distance
        if (distToPlayer <= 420f) {
          enemy.facingAngle = atan2(dirToPlayer.y, dirToPlayer.x)
          // Tactical retreat if too close, approach if too far
          val desiredDist = 180f
          val moveDir = if (distToPlayer < desiredDist - 30f) -dirToPlayer else if (distToPlayer > desiredDist + 30f) dirToPlayer else Vector2()
          val targetPos = enemy.pos + (moveDir * speed * dt)
          if (!map.isCollidingWithWalls(targetPos, enemy.type.sizeRadius)) {
            enemy.pos = targetPos
          }

          // Shoot steam projectile
          if (enemy.attackCooldown <= 0f) {
            enemy.attackCooldown = 2.2f
            soundSynth.playSteamDash()
            projectiles.add(
              Projectile(
                id = "steam_shot_${System.currentTimeMillis()}",
                pos = enemy.pos + (dirToPlayer * 22f),
                vel = dirToPlayer * 240f,
                isPlayer = false,
                damage = enemy.type.baseDamage,
                radius = 12f,
                lifetime = 1.8f
              )
            )
            // Windup burst particles
            spawnImpactSparks(enemy.pos, 0xFFE2E8F0)
          }
        }
      }

      EnemyType.SLAG_CORRUPTOR -> {
        // Heavy bruiser
        if (distToPlayer <= 350f) {
          enemy.facingAngle = atan2(dirToPlayer.y, dirToPlayer.x)
          val targetPos = enemy.pos + (dirToPlayer * speed * dt)
          if (!map.isCollidingWithWalls(targetPos, enemy.type.sizeRadius)) {
            enemy.pos = targetPos
          }
          if (distToPlayer <= 48f && enemy.attackCooldown <= 0f && invulnerableTimer <= 0f) {
            val dmg = (enemy.type.baseDamage - equippedArmor.defense).coerceAtLeast(10f)
            playerHealth -= dmg
            soundSynth.playHammerHit()
            vibrate(60)
            invulnerableTimer = 0.4f
            enemy.attackCooldown = 1.4f
            addFloatingText("-${dmg.toInt()}", playerPos, 0xFFEF4444)
            if (playerHealth <= 0f) onPlayerDied()
          }
        }
      }

      EnemyType.BOSS_OVERSEER -> {
        // Boss 3-phase AI
        val hpPercent = enemy.health / enemy.maxHealth
        enemy.phase = if (hpPercent > 0.65f) 1 else if (hpPercent > 0.30f) 2 else 3

        enemy.facingAngle = atan2(dirToPlayer.y, dirToPlayer.x)
        val bossSpeed = speed * (if (enemy.phase == 3) 1.55f else 1.1f)
        val targetPos = enemy.pos + (dirToPlayer * bossSpeed * dt)
        if (!map.isCollidingWithWalls(targetPos, enemy.type.sizeRadius)) {
          enemy.pos = targetPos
        }

        // Heavy Cleave
        if (distToPlayer <= 62f && enemy.attackCooldown <= 0f && invulnerableTimer <= 0f) {
          val dmg = (enemy.type.baseDamage - equippedArmor.defense).coerceAtLeast(14f)
          playerHealth -= dmg
          soundSynth.playHammerHit()
          vibrate(75)
          invulnerableTimer = 0.5f
          enemy.attackCooldown = if (enemy.phase == 3) 0.8f else 1.3f
          addFloatingText("-${dmg.toInt()}", playerPos, 0xFFEF4444)
          spawnImpactSparks(playerPos, 0xFF7C3AED)
          if (playerHealth <= 0f) onPlayerDied()
        }

        // Phase 2 & 3: Radial steam burst volley
        if (enemy.attackCooldown <= 0.1f && enemy.phase >= 2) {
          enemy.attackCooldown = if (enemy.phase == 3) 2.2f else 3.5f
          soundSynth.playSteamDash()
          val numBolts = if (enemy.phase == 3) 8 else 5
          for (i in 0 until numBolts) {
            val angle = (enemy.facingAngle - 0.7f) + (i * 0.35f)
            projectiles.add(
              Projectile(
                id = "boss_steam_$i",
                pos = enemy.pos,
                vel = Vector2.fromAngle(angle, 220f),
                isPlayer = false,
                damage = 20f,
                radius = 10f,
                lifetime = 2.0f
              )
            )
          }
        }
      }
    }
  }

  private fun onProjectileImpact(proj: Projectile, hitEnemy: Enemy?) {
    if (proj.vialType != null) {
      soundSynth.playVialShatter(proj.vialType)
      spawnImpactSparks(proj.pos, proj.vialType.colorHex)

      // Area of effect damage and status
      val aoeRadius = 90f
      for (enemy in enemies) {
        if (enemy.health <= 0f) continue
        if (proj.pos.distanceTo(enemy.pos) <= aoeRadius) {
          val aoeDmg = proj.damage
          enemy.health -= aoeDmg
          addFloatingText("-${aoeDmg.toInt()}", enemy.pos + Vector2(0f, -22f), proj.vialType.colorHex)

          when (proj.vialType) {
            VialType.CORROSIVE_VITRIOL -> {
              enemy.corrodedTimer = 6f
              addFloatingText("CORRODED!", enemy.pos, 0xFF10B981)
            }
            VialType.PYRETIC_PHLOGISTON -> {
              enemy.burningTimer = 5f
              addFloatingText("IGNITED!", enemy.pos, 0xFFF97316)
            }
            VialType.CRYO_QUENCH -> {
              enemy.frozenTimer = 4.5f
              addFloatingText("FROZEN!", enemy.pos, 0xFF38BDF8)
            }
          }

          if (enemy.health <= 0f) onEnemyDefeated(enemy)
        }
      }

      // Check if Cryo Quench hit a steam hazard to temporarily freeze it!
      if (proj.vialType == VialType.CRYO_QUENCH) {
        for (hazard in map.hazards) {
          if (proj.pos.distanceTo(hazard.pos) <= 100f) {
            hazard.frozenTimer = 6f
            showPrompt("Steam conduit temporarily frozen by Cryo Quench!")
            addFloatingText("HAZARD FROZEN!", hazard.pos, 0xFF38BDF8)
          }
        }
      }
    } else {
      spawnImpactSparks(proj.pos, 0xFFFFFFFF)
    }
  }

  private fun collectLoot(loot: LootDrop) {
    soundSynth.playLevelUp()
    when (loot.type) {
      LootType.SCRAP -> {
        playerScrap += loot.value
        addFloatingText("+${loot.value} Scrap", loot.pos, 0xFFFBBF24)
      }
      LootType.VIAL_CORROSIVE -> {
        vialInventory[VialType.CORROSIVE_VITRIOL] = (vialInventory[VialType.CORROSIVE_VITRIOL] ?: 0) + loot.value
        addFloatingText("+${loot.value} Vitriol", loot.pos, 0xFF10B981)
      }
      LootType.VIAL_PYRETIC -> {
        vialInventory[VialType.PYRETIC_PHLOGISTON] = (vialInventory[VialType.PYRETIC_PHLOGISTON] ?: 0) + loot.value
        addFloatingText("+${loot.value} Phlogiston", loot.pos, 0xFFF97316)
      }
      LootType.VIAL_CRYO -> {
        vialInventory[VialType.CRYO_QUENCH] = (vialInventory[VialType.CRYO_QUENCH] ?: 0) + loot.value
        addFloatingText("+${loot.value} Cryo", loot.pos, 0xFF38BDF8)
      }
      LootType.HEALTH_TINCTURE -> {
        playerHealth = (playerHealth + 40f).coerceAtMost(playerMaxHealth)
        addFloatingText("+40 HP", loot.pos, 0xFFEF4444)
      }
      LootType.KEY_ITEM -> {
        hasMasterKey = true
        addFloatingText("KEY SECURED", loot.pos, 0xFF8B5CF6)
      }
      LootType.STEAM_CORE -> {
        playerSteam = playerMaxSteam
        addFloatingText("STEAM RECHARGED", loot.pos, 0xFF38BDF8)
      }
    }
  }

  private fun spawnImpactSparks(pos: Vector2, colorHex: Long) {
    for (i in 0..8) {
      val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
      val spd = 40f + Random.nextFloat() * 80f
      particles.add(
        Particle(
          pos = pos,
          vel = Vector2.fromAngle(angle, spd),
          colorHex = colorHex,
          size = 3.5f + Random.nextFloat() * 3f,
          maxLifetime = 0.35f,
          shape = ParticleShape.SPARK
        )
      )
    }
  }

  private fun addFloatingText(text: String, pos: Vector2, colorHex: Long) {
    floatingTexts.add(FloatingText(text, pos, colorHex))
  }

  private fun onPlayerDied() {
    isGameOver = true
    soundSynth.playPlayerHurt()
    vibrate(120)
  }

  fun respawnAtCheckpoint() {
    // Find closest checkpoint or default spawn
    val cp = map.checkpoints.minByOrNull { it.distanceTo(playerPos) } ?: Vector2(250f, 250f)
    playerPos = cp
    playerVel = Vector2()
    playerHealth = playerMaxHealth
    playerSteam = playerMaxSteam
    isGameOver = false
    invulnerableTimer = 1.5f
    showPrompt("Respawned at Alchemical Checkpoint.")
  }

  private fun vibrate(durationMs: Long) {
    try {
      vibrator?.vibrate(durationMs)
    } catch (_: Exception) {}
  }
}
