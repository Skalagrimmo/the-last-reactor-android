package com.example.model

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector2(val x: Float = 0f, val y: Float = 0f) {
  operator fun plus(other: Vector2) = Vector2(x + other.x, y + other.y)
  operator fun minus(other: Vector2) = Vector2(x - other.x, y - other.y)
  operator fun unaryMinus() = Vector2(-x, -y)
  operator fun times(scale: Float) = Vector2(x * scale, y * scale)
  operator fun div(scale: Float) = if (scale != 0f) Vector2(x / scale, y / scale) else Vector2()

  fun length(): Float = sqrt(x * x + y * y)
  fun normalized(): Vector2 {
    val len = length()
    return if (len > 0.0001f) Vector2(x / len, y / len) else Vector2(0f, 0f)
  }
  fun distanceTo(other: Vector2): Float = (this - other).length()

  companion object {
    fun fromAngle(angleRad: Float, length: Float = 1f): Vector2 {
      return Vector2(cos(angleRad) * length, sin(angleRad) * length)
    }
  }
}

enum class Direction {
  UP, DOWN, LEFT, RIGHT
}

enum class VialType(val displayName: String, val colorHex: Long, val description: String) {
  CORROSIVE_VITRIOL(
    "Corrosive Vitriol",
    0xFF10B981,
    "Dissolves clockwork armor plating, causing vulnerable status and damage over time."
  ),
  PYRETIC_PHLOGISTON(
    "Pyretic Phlogiston",
    0xFFF97316,
    "Alchemical fire that combusts enemy boiler valves in an incendiary shockwave."
  ),
  CRYO_QUENCH(
    "Cryo Quench",
    0xFF38BDF8,
    "Rapid endothermic flash freezing enemies, slowing them by 60% and temporarily plugging steam leaks."
  )
}

data class Weapon(
  val id: String,
  val name: String,
  val damage: Float,
  val attackRate: Float,
  val knockback: Float,
  val description: String,
  val costScrap: Int = 0
)

data class Armor(
  val id: String,
  val name: String,
  val defense: Float,
  val steamCapacityBonus: Float,
  val description: String,
  val costScrap: Int = 0
)

object EquipmentRegistry {
  val WEAPONS = listOf(
    Weapon(
      id = "hammer_standard",
      name = "Piston Steam-Hammer",
      damage = 38f,
      attackRate = 0.40f,
      knockback = 180f,
      description = "Standard issue Warden striking maul driven by pneumatic pressure.",
      costScrap = 0
    ),
    Weapon(
      id = "hammer_heavy",
      name = "Sledge-Pounder of the Deep Flue",
      damage = 72f,
      attackRate = 0.65f,
      knockback = 340f,
      description = "Massive cast-iron crusher that creates ground-shattering seismic tremors.",
      costScrap = 120
    ),
    Weapon(
      id = "hammer_voltaic",
      name = "Voltaic Arc-Maul",
      damage = 54f,
      attackRate = 0.32f,
      knockback = 210f,
      description = "Fitted with galvanic capacitor coils that spark upon impact.",
      costScrap = 220
    )
  )

  val ARMORS = listOf(
    Armor(
      id = "armor_gambeson",
      name = "Warden Leather Gambeson",
      defense = 4f,
      steamCapacityBonus = 0f,
      description = "Oiled leather garment reinforced with brass rivet studs.",
      costScrap = 0
    ),
    Armor(
      id = "armor_cuirass",
      name = "Foundry Plate Cuirass",
      defense = 14f,
      steamCapacityBonus = 25f,
      description = "Cold-rolled plate armor with internal insulation against slag splashes.",
      costScrap = 100
    ),
    Armor(
      id = "armor_exoskeleton",
      name = "Pressurized Hazmat Exoskeleton",
      defense = 24f,
      steamCapacityBonus = 50f,
      description = "High-pressure sealed pneumatic suit built for emergency core dampening.",
      costScrap = 200
    )
  )
}

enum class EnemyType(
  val displayName: String,
  val maxHealth: Float,
  val speed: Float,
  val baseDamage: Float,
  val defense: Float,
  val sizeRadius: Float,
  val colorHex: Long
) {
  SKITTERER(
    displayName = "Clockwork Skitterer",
    maxHealth = 65f,
    speed = 150f,
    baseDamage = 14f,
    defense = 2f,
    sizeRadius = 18f,
    colorHex = 0xFFD97706
  ),
  STEAM_VANE(
    displayName = "Steam Vane Automaton",
    maxHealth = 140f,
    speed = 70f,
    baseDamage = 22f,
    defense = 10f,
    sizeRadius = 26f,
    colorHex = 0xFF94A3B8
  ),
  SLAG_CORRUPTOR(
    displayName = "Slag Corruptor",
    maxHealth = 260f,
    speed = 85f,
    baseDamage = 32f,
    defense = 16f,
    sizeRadius = 32f,
    colorHex = 0xFFDC2626
  ),
  BOSS_OVERSEER(
    displayName = "Overseer Malakor",
    maxHealth = 950f,
    speed = 95f,
    baseDamage = 45f,
    defense = 20f,
    sizeRadius = 45f,
    colorHex = 0xFF7C3AED
  )
}

enum class EnemyState {
  IDLE, PURSUING, ATTACKING, WINDUP, RECOVERING, STUNNED
}

data class Enemy(
  val id: String,
  val type: EnemyType,
  var pos: Vector2,
  var vel: Vector2 = Vector2(),
  var health: Float = type.maxHealth,
  var maxHealth: Float = type.maxHealth,
  var defense: Float = type.defense,
  var state: EnemyState = EnemyState.IDLE,
  var stateTimer: Float = 0f,
  var attackCooldown: Float = 0f,
  var facingAngle: Float = 0f,
  var corrodedTimer: Float = 0f,
  var frozenTimer: Float = 0f,
  var burningTimer: Float = 0f,
  var phase: Int = 1,
  var homePos: Vector2 = pos
)

data class SteamConduitValve(
  val id: String,
  val pos: Vector2,
  var isTurned: Boolean = false,
  val controlledHazardId: String,
  val controlledDoorId: String,
  val label: String
)

data class BulkheadDoor(
  val id: String,
  val pos: Vector2,
  val width: Float,
  val height: Float,
  var isOpen: Boolean = false,
  val requiresKey: Boolean = false,
  val label: String
)

data class SteamHazard(
  val id: String,
  val pos: Vector2,
  val length: Float,
  val direction: Direction,
  var isActive: Boolean = true,
  var frozenTimer: Float = 0f,
  var isPulsing: Boolean = false,
  var pulseTimer: Float = 0f
)

enum class LootType {
  SCRAP,
  VIAL_CORROSIVE,
  VIAL_PYRETIC,
  VIAL_CRYO,
  HEALTH_TINCTURE,
  STEAM_CORE,
  KEY_ITEM
}

data class LootDrop(
  val id: String,
  var pos: Vector2,
  val type: LootType,
  val value: Int = 1,
  val label: String
)

data class Projectile(
  val id: String,
  var pos: Vector2,
  var vel: Vector2,
  val isPlayer: Boolean,
  val vialType: VialType? = null,
  val damage: Float,
  val radius: Float = 8f,
  var lifetime: Float = 3f,
  val penetrates: Boolean = false
)

enum class ParticleShape {
  CIRCLE, SPARK, CLOUD, RUNE
}

data class Particle(
  var pos: Vector2,
  var vel: Vector2,
  val colorHex: Long,
  var size: Float,
  var alpha: Float = 1f,
  var lifetime: Float = 0f,
  val maxLifetime: Float = 0.5f,
  val shape: ParticleShape = ParticleShape.CIRCLE
)

data class LoreEntry(
  val id: String,
  val title: String,
  val author: String,
  val excerpt: String,
  var isDiscovered: Boolean = false
)

data class PlayerStats(
  var vigorLevel: Int = 1,
  var pressureLevel: Int = 1,
  var potencyLevel: Int = 1
) {
  fun maxHealth(): Float = 100f + (vigorLevel - 1) * 25f
  fun maxSteam(): Float = 80f + (pressureLevel - 1) * 20f
  fun vialPotencyMultiplier(): Float = 1f + (potencyLevel - 1) * 0.25f
  fun upgradeCost(level: Int): Int = 30 + level * 25
}
