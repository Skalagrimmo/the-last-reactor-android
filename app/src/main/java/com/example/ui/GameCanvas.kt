package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.engine.GameEngine
import com.example.model.*
import kotlin.math.*

@Composable
fun GameCanvas(
  engine: GameEngine,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier.fillMaxSize()) {
    val canvasWidth = size.width
    val canvasHeight = size.height

    // 1. Camera calculation (center on player)
    val camX = (engine.playerPos.x - canvasWidth / 2f).coerceIn(0f, engine.map.width - canvasWidth)
    val camY = (engine.playerPos.y - canvasHeight / 2f).coerceIn(0f, engine.map.height - canvasHeight)

    // Background fill
    drawRect(Color(0xFF0F141C))

    // Helper to translate world to screen
    fun toScreen(pos: Vector2): Offset = Offset(pos.x - camX, pos.y - camY)

    // 2. Draw Floor Tiles & Grid
    drawDistrictFloors(this, camX, camY, canvasWidth, canvasHeight, engine.map)

    // 3. Draw Steam Hazards
    drawSteamHazards(this, engine.map.hazards, ::toScreen)

    // 4. Draw Bulkhead Doors
    drawBulkheadDoors(this, engine.map.doors, ::toScreen)

    // 5. Draw Conduit Valves
    drawValves(this, engine.map.valves, ::toScreen)

    // 6. Draw Walls & Machinery
    drawWalls(this, engine.map.walls, camX, camY)

    // 7. Draw Checkpoints & Lore Tablets
    drawCheckpointsAndLore(this, engine.map, ::toScreen)

    // 8. Draw Loot Drops
    drawLootDrops(this, engine.lootDrops, ::toScreen)

    // 9. Draw Enemies
    for (enemy in engine.enemies) {
      if (enemy.health > 0f) {
        drawEnemy(this, enemy, ::toScreen)
      }
    }

    // 10. Draw Player & Steam-Hammer
    drawPlayer(this, engine, ::toScreen)

    // 11. Draw Projectiles
    drawProjectiles(this, engine.projectiles, ::toScreen)

    // 12. Draw Particles
    drawParticles(this, engine.particles, ::toScreen)

    // 13. Draw Floating Combat Text
    drawFloatingTexts(this, engine.floatingTexts, ::toScreen)

    // 14. Atmospheric Dark Vignette
    drawVignette(this, canvasWidth, canvasHeight)
  }
}

private fun drawDistrictFloors(
  drawScope: DrawScope,
  camX: Float,
  camY: Float,
  width: Float,
  height: Float,
  map: com.example.engine.GameMap
) {
  val tileSize = map.tileSize
  val startCol = (camX / tileSize).toInt().coerceAtLeast(0)
  val endCol = ((camX + width) / tileSize).toInt().coerceAtMost(map.cols - 1)
  val startRow = (camY / tileSize).toInt().coerceAtLeast(0)
  val endRow = ((camY + height) / tileSize).toInt().coerceAtMost(map.rows - 1)

  for (col in startCol..endCol) {
    for (row in startRow..endRow) {
      val x = col * tileSize - camX
      val y = row * tileSize - camY

      // Checkerboard industrial slate
      val isAlt = (col + row) % 2 == 0
      val tileColor = if (isAlt) Color(0xFF161C24) else Color(0xFF1B232D)
      drawScope.drawRect(tileColor, Offset(x, y), Size(tileSize, tileSize))

      // Subtle rivets
      drawScope.drawCircle(Color(0xFF263342), radius = 1.5f, center = Offset(x + 4f, y + 4f))
      drawScope.drawCircle(Color(0xFF263342), radius = 1.5f, center = Offset(x + tileSize - 4f, y + tileSize - 4f))

      // Siphon Water / Slag gutter in Central section
      if (col in 19..21 && row in 12..25) {
        drawScope.drawRect(
          Color(0xFF042F2E).copy(alpha = 0.7f),
          Offset(x, y),
          Size(tileSize, tileSize)
        )
      }
    }
  }
}

private fun drawWalls(
  drawScope: DrawScope,
  walls: List<androidx.compose.ui.geometry.Rect>,
  camX: Float,
  camY: Float
) {
  val wallFill = Color(0xFF232D38)
  val wallTop = Color(0xFF384656)
  val pipeColor = Color(0xFF10B981) // Alchemical glowing pipe

  for (wall in walls) {
    val screenRect = androidx.compose.ui.geometry.Rect(
      wall.left - camX,
      wall.top - camY,
      wall.right - camX,
      wall.bottom - camY
    )
    // Wall shadow/body
    drawScope.drawRect(
      wallFill,
      Offset(screenRect.left, screenRect.top),
      Size(screenRect.width, screenRect.height)
    )
    // Wall top highlight
    drawScope.drawRect(
      wallTop,
      Offset(screenRect.left, screenRect.top),
      Size(screenRect.width, min(6f, screenRect.height))
    )
    drawScope.drawRect(
      Color(0xFF0A0D12),
      Offset(screenRect.left, screenRect.top),
      Size(screenRect.width, screenRect.height),
      style = Stroke(width = 1.5f)
    )

    // Decorative conduit pipes on wider horizontal walls
    if (screenRect.width > 120f) {
      drawScope.drawLine(
        pipeColor,
        Offset(screenRect.left + 15f, screenRect.top + screenRect.height / 2f),
        Offset(screenRect.right - 15f, screenRect.top + screenRect.height / 2f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
      )
    }
  }
}

private fun drawSteamHazards(
  drawScope: DrawScope,
  hazards: List<SteamHazard>,
  toScreen: (Vector2) -> Offset
) {
  val time = System.currentTimeMillis() * 0.006f
  for (h in hazards) {
    if (!h.isActive && h.frozenTimer <= 0f) continue
    val origin = toScreen(h.pos)

    val isFrozen = h.frozenTimer > 0f
    val hazardColor = if (isFrozen) Color(0xFF38BDF8) else Color(0xFFF1F5F9)

    val endOffset = when (h.direction) {
      Direction.RIGHT -> Offset(origin.x + h.length, origin.y)
      Direction.LEFT -> Offset(origin.x - h.length, origin.y)
      Direction.DOWN -> Offset(origin.x, origin.y + h.length)
      Direction.UP -> Offset(origin.x, origin.y - h.length)
    }

    // Pipe nozzle
    drawScope.drawCircle(Color(0xFFB45309), radius = 8f, center = origin)

    if (isFrozen) {
      // Ice block over hazard
      drawScope.drawLine(
        Color(0xCC38BDF8),
        origin,
        endOffset,
        strokeWidth = 24f,
        cap = StrokeCap.Round
      )
    } else {
      // Roaring steam cone with pulsing billows
      val pulseWidth = 22f + sin(time) * 4f
      drawScope.drawLine(
        hazardColor.copy(alpha = 0.55f),
        origin,
        endOffset,
        strokeWidth = pulseWidth,
        cap = StrokeCap.Round
      )
      drawScope.drawLine(
        Color(0xFFCBD5E1).copy(alpha = 0.85f),
        origin,
        endOffset,
        strokeWidth = pulseWidth * 0.5f,
        cap = StrokeCap.Round
      )
    }
  }
}

private fun drawBulkheadDoors(
  drawScope: DrawScope,
  doors: List<BulkheadDoor>,
  toScreen: (Vector2) -> Offset
) {
  for (door in doors) {
    val origin = toScreen(door.pos)
    if (door.isOpen) {
      // Retracted gate frame
      drawScope.drawRect(
        Color(0xFF10B981).copy(alpha = 0.4f),
        origin,
        Size(door.width, door.height),
        style = Stroke(width = 2f)
      )
    } else {
      // Heavy iron reinforced bulkhead
      drawScope.drawRect(
        Color(0xFF475569),
        origin,
        Size(door.width, door.height)
      )
      // Steel crossbars
      drawScope.drawLine(
        Color(0xFF1E293B),
        origin,
        Offset(origin.x + door.width, origin.y + door.height),
        strokeWidth = 3f
      )
      drawScope.drawLine(
        Color(0xFF1E293B),
        Offset(origin.x, origin.y + door.height),
        Offset(origin.x + door.width, origin.y),
        strokeWidth = 3f
      )
      // Lock emblem
      val lockColor = if (door.requiresKey) Color(0xFFDC2626) else Color(0xFFF59E0B)
      drawScope.drawCircle(
        lockColor,
        radius = 7f,
        center = Offset(origin.x + door.width / 2f, origin.y + door.height / 2f)
      )
    }
  }
}

private fun drawValves(
  drawScope: DrawScope,
  valves: List<SteamConduitValve>,
  toScreen: (Vector2) -> Offset
) {
  for (valve in valves) {
    val center = toScreen(valve.pos)
    val brassColor = Color(0xFFD97706)
    val indicatorColor = if (valve.isTurned) Color(0xFF10B981) else Color(0xFFEF4444)

    // Base flange
    drawScope.drawCircle(Color(0xFF334155), radius = 18f, center = center)
    // Valve wheel ring
    drawScope.drawCircle(brassColor, radius = 14f, center = center, style = Stroke(width = 3.5f))
    // 4 spokes
    drawScope.drawLine(brassColor, Offset(center.x - 14f, center.y), Offset(center.x + 14f, center.y), strokeWidth = 2.5f)
    drawScope.drawLine(brassColor, Offset(center.x, center.y - 14f), Offset(center.x, center.y + 14f), strokeWidth = 2.5f)
    // Center dial
    drawScope.drawCircle(indicatorColor, radius = 4.5f, center = center)
  }
}

private fun drawCheckpointsAndLore(
  drawScope: DrawScope,
  map: com.example.engine.GameMap,
  toScreen: (Vector2) -> Offset
) {
  // Checkpoints
  for (cp in map.checkpoints) {
    val center = toScreen(cp)
    drawScope.drawCircle(Color(0xFF10B981).copy(alpha = 0.25f), radius = 24f, center = center)
    drawScope.drawCircle(Color(0xFF10B981), radius = 8f, center = center, style = Stroke(width = 2f))
    drawScope.drawCircle(Color(0xFF34D399), radius = 3.5f, center = center)
  }

  // Lore Tablet positions
  val lorePositions = listOf(
    Vector2(380f, 220f),
    Vector2(400f, 850f),
    Vector2(1400f, 1600f)
  )
  for (pos in lorePositions) {
    val center = toScreen(pos)
    // Pedestal
    drawScope.drawRect(Color(0xFF475569), Offset(center.x - 10f, center.y - 14f), Size(20f, 28f))
    drawScope.drawCircle(Color(0xFF38BDF8), radius = 4f, center = Offset(center.x, center.y - 4f))
  }
}

private fun drawLootDrops(
  drawScope: DrawScope,
  lootDrops: List<LootDrop>,
  toScreen: (Vector2) -> Offset
) {
  val bob = sin(System.currentTimeMillis() * 0.007f) * 3f
  for (loot in lootDrops) {
    val center = toScreen(loot.pos) + Offset(0f, bob)
    when (loot.type) {
      LootType.SCRAP -> {
        // Brass cog
        drawScope.drawCircle(Color(0xFFFBBF24), radius = 7f, center = center)
        drawScope.drawCircle(Color(0xFF78350F), radius = 2.5f, center = center)
      }
      LootType.VIAL_CORROSIVE -> {
        drawScope.drawRect(Color(0xFF10B981), Offset(center.x - 4f, center.y - 6f), Size(8f, 12f))
      }
      LootType.VIAL_PYRETIC -> {
        drawScope.drawRect(Color(0xFFF97316), Offset(center.x - 4f, center.y - 6f), Size(8f, 12f))
      }
      LootType.VIAL_CRYO -> {
        drawScope.drawRect(Color(0xFF38BDF8), Offset(center.x - 4f, center.y - 6f), Size(8f, 12f))
      }
      LootType.KEY_ITEM -> {
        // Gilded key
        drawScope.drawCircle(Color(0xFFC084FC), radius = 8f, center = center)
        drawScope.drawLine(Color(0xFFC084FC), center, Offset(center.x, center.y + 12f), strokeWidth = 3f)
      }
      else -> {
        drawScope.drawCircle(Color(0xFFEF4444), radius = 6f, center = center)
      }
    }
  }
}

private fun drawEnemy(
  drawScope: DrawScope,
  enemy: Enemy,
  toScreen: (Vector2) -> Offset
) {
  val center = toScreen(enemy.pos)
  val radius = enemy.type.sizeRadius
  val angleDeg = Math.toDegrees(enemy.facingAngle.toDouble()).toFloat()

  drawScope.rotate(angleDeg, pivot = center) {
    when (enemy.type) {
      EnemyType.SKITTERER -> {
        // Clockwork spider body
        val color = if (enemy.frozenTimer > 0f) Color(0xFF38BDF8) else Color(enemy.type.colorHex)
        drawCircle(color, radius = radius * 0.75f, center = center)
        // 4 Spidery brass legs
        val legColor = Color(0xFF78350F)
        drawLine(legColor, center, Offset(center.x + radius * 1.1f, center.y - radius * 0.9f), strokeWidth = 2.5f)
        drawLine(legColor, center, Offset(center.x + radius * 1.1f, center.y + radius * 0.9f), strokeWidth = 2.5f)
        drawLine(legColor, center, Offset(center.x - radius * 1.1f, center.y - radius * 0.9f), strokeWidth = 2.5f)
        drawLine(legColor, center, Offset(center.x - radius * 1.1f, center.y + radius * 0.9f), strokeWidth = 2.5f)
        // Glowing red optic eye
        drawCircle(Color(0xFFDC2626), radius = 3.5f, center = Offset(center.x + radius * 0.5f, center.y))
      }

      EnemyType.STEAM_VANE -> {
        // Heavy iron kettle chassis
        val bodyColor = if (enemy.frozenTimer > 0f) Color(0xFF38BDF8) else Color(0xFF64748B)
        drawCircle(bodyColor, radius = radius, center = center)
        drawCircle(Color(0xFF0F172A), radius = radius * 0.5f, center = center)
        // Twin steam chimney pipes on back
        drawCircle(Color(0xFFB45309), radius = 5f, center = Offset(center.x - radius * 0.6f, center.y - 7f))
        drawCircle(Color(0xFFB45309), radius = 5f, center = Offset(center.x - radius * 0.6f, center.y + 7f))
        // Directional steam cannon nozzle
        drawRect(Color(0xFF94A3B8), Offset(center.x, center.y - 4f), Size(radius * 0.9f, 8f))
      }

      EnemyType.SLAG_CORRUPTOR -> {
        // Molten brute
        drawCircle(Color(0xFF451A03), radius = radius, center = center)
        drawCircle(Color(0xFFDC2626), radius = radius * 0.55f, center = center)
        // Molten crackles
        drawLine(Color(0xFFF59E0B), center, Offset(center.x + radius * 0.8f, center.y), strokeWidth = 3f)
      }

      EnemyType.BOSS_OVERSEER -> {
        // Overseer Malakor - Colossal forge engine
        val coreColor = if (enemy.phase == 3) Color(0xFFEF4444) else Color(0xFF7C3AED)
        // Outer iron gear ring
        drawCircle(Color(0xFF1E293B), radius = radius, center = center)
        drawCircle(Color(0xFFD97706), radius = radius * 0.85f, center = center, style = Stroke(width = 4f))
        // Inner glowing alchemical reactor furnace
        drawCircle(coreColor, radius = radius * 0.55f, center = center)
        // Exhaust stacks venting purple steam
        drawCircle(Color(0xFF0F172A), radius = 9f, center = Offset(center.x - radius * 0.6f, center.y - 14f))
        drawCircle(Color(0xFF0F172A), radius = 9f, center = Offset(center.x - radius * 0.6f, center.y + 14f))
        // Great Piston Arm
        drawRect(Color(0xFF94A3B8), Offset(center.x, center.y - 8f), Size(radius * 1.1f, 16f))
      }
    }
  }

  // Health bar above enemy
  val hpBarWidth = radius * 2.2f
  val hpBarHeight = 4f
  val hpPercent = (enemy.health / enemy.maxHealth).coerceIn(0f, 1f)
  val hpBarTop = center.y - radius - 10f

  drawScope.drawRect(
    Color(0xFF0F172A),
    Offset(center.x - hpBarWidth / 2f, hpBarTop),
    Size(hpBarWidth, hpBarHeight)
  )
  drawScope.drawRect(
    if (enemy.type == EnemyType.BOSS_OVERSEER) Color(0xFF8B5CF6) else Color(0xFFEF4444),
    Offset(center.x - hpBarWidth / 2f, hpBarTop),
    Size(hpBarWidth * hpPercent, hpBarHeight)
  )
}

private fun drawPlayer(
  drawScope: DrawScope,
  engine: GameEngine,
  toScreen: (Vector2) -> Offset
) {
  val center = toScreen(engine.playerPos)
  val radius = 18f
  val angleDeg = Math.toDegrees(engine.playerFacingAngle.toDouble()).toFloat()

  // Invulnerability flicker
  if (engine.invulnerableTimer > 0f && (System.currentTimeMillis() / 80) % 2 == 0L) {
    return
  }

  drawScope.rotate(angleDeg, pivot = center) {
    // Boiler Backpack (Steam tank)
    drawCircle(Color(0xFFB45309), radius = 6f, center = Offset(center.x - 12f, center.y))

    // Player body (Alchemical Warden Coat)
    drawCircle(Color(0xFF1E293B), radius = radius, center = center)
    // Leather mantle & cuirass
    drawCircle(Color(0xFF78350F), radius = radius * 0.6f, center = center)
    // Visor glow (Jade emerald)
    drawCircle(Color(0xFF10B981), radius = 3.5f, center = Offset(center.x + radius * 0.6f, center.y))

    // Steam-Hammer
    val hammerOffset = if (engine.isAttacking) {
      // Swing motion
      Offset(center.x + 28f, center.y + 10f)
    } else {
      Offset(center.x + 14f, center.y + 14f)
    }
    // Hammer shaft
    drawLine(Color(0xFF78350F), center, hammerOffset, strokeWidth = 3f)
    // Heavy head block
    val hammerHeadColor = when (engine.equippedWeapon.id) {
      "hammer_heavy" -> Color(0xFF475569)
      "hammer_voltaic" -> Color(0xFF38BDF8)
      else -> Color(0xFFD97706)
    }
    drawRect(hammerHeadColor, Offset(hammerOffset.x - 5f, hammerOffset.y - 10f), Size(10f, 20f))
  }
}

private fun drawProjectiles(
  drawScope: DrawScope,
  projectiles: List<Projectile>,
  toScreen: (Vector2) -> Offset
) {
  for (p in projectiles) {
    val center = toScreen(p.pos)
    if (p.isPlayer && p.vialType != null) {
      // Catalyst Vial
      drawScope.drawCircle(Color(p.vialType.colorHex), radius = p.radius, center = center)
      drawScope.drawCircle(Color(0xFFFFFFFF), radius = p.radius * 0.45f, center = center)
    } else {
      // Steam cannon bolt
      drawScope.drawCircle(Color(0xCCF1F5F9), radius = p.radius, center = center)
      drawScope.drawCircle(Color(0xFFCBD5E1), radius = p.radius * 0.5f, center = center)
    }
  }
}

private fun drawParticles(
  drawScope: DrawScope,
  particles: List<Particle>,
  toScreen: (Vector2) -> Offset
) {
  for (part in particles) {
    val center = toScreen(part.pos)
    val color = Color(part.colorHex).copy(alpha = part.alpha)
    when (part.shape) {
      ParticleShape.SPARK -> {
        drawScope.drawLine(
          color,
          center,
          center + Offset(part.vel.x * 0.05f, part.vel.y * 0.05f),
          strokeWidth = part.size
        )
      }
      ParticleShape.CLOUD -> {
        drawScope.drawCircle(color, radius = part.size, center = center)
      }
      else -> {
        drawScope.drawCircle(color, radius = part.size, center = center)
      }
    }
  }
}

private fun drawFloatingTexts(
  drawScope: DrawScope,
  texts: List<GameEngine.FloatingText>,
  toScreen: (Vector2) -> Offset
) {
  // Draw small indicator markers for floating numbers
  for (ft in texts) {
    val center = toScreen(ft.pos)
    val alpha = (1f - (ft.lifetime / ft.maxLifetime)).coerceIn(0f, 1f)
    drawScope.drawCircle(Color(ft.colorHex).copy(alpha = alpha), radius = 3f, center = center)
  }
}

private fun drawVignette(drawScope: DrawScope, width: Float, height: Float) {
  // Atmospheric corner shadow
  val brush = Brush.radialGradient(
    colors = listOf(Color.Transparent, Color(0x66000000), Color(0xDD090D14)),
    center = Offset(width / 2f, height / 2f),
    radius = max(width, height) * 0.65f
  )
  drawScope.drawRect(brush, Offset.Zero, Size(width, height))
}
