package com.example.ecs

import com.example.model.EnemyState
import com.example.model.EnemyType

typealias EntityId = Int

/**
 * Base marker interface for all ECS components.
 */
interface Component

/**
 * Interface for data-holding components that store mutable simulation state.
 */
interface DataComponent : Component

/**
 * Interface for lightweight tagging components to categorize entities.
 */
interface TagComponent : Component

// --- Tag Components ---
class PlayerTag : TagComponent
class EnemyTag : TagComponent
class BossTag : TagComponent
class LightEmitterTag : TagComponent

// --- Data Components ---

data class TransformComponent(
  var x: Float = 0f,
  var y: Float = 0f,
  var angle: Float = 0f,
  var scale: Float = 1f
) : DataComponent

data class VelocityComponent(
  var vx: Float = 0f,
  var vy: Float = 0f,
  var drag: Float = 0.85f,
  var maxSpeed: Float = 220f
) : DataComponent

enum class ShapeType {
  WARDEN,
  SKITTERER,
  STEAM_VANE,
  SLAG_CORRUPTOR,
  BOSS_OVERSEER,
  PROJECTILE,
  PARTICLE,
  VALVE,
  DOOR,
  QUAD
}

data class RenderComponent(
  var shape: ShapeType,
  var colorR: Float,
  var colorG: Float,
  var colorB: Float,
  var alpha: Float = 1f,
  var size: Float = 20f,
  var layer: Int = 0,
  var visible: Boolean = true
) : DataComponent

data class HealthComponent(
  var current: Float = 100f,
  var max: Float = 100f,
  var defense: Float = 0f,
  var invulnerableTimer: Float = 0f
) : DataComponent

data class ColliderComponent(
  var radius: Float = 18f,
  var isTrigger: Boolean = false
) : DataComponent

data class WardenComponent(
  var steam: Float = 80f,
  var maxSteam: Float = 80f,
  var comboStep: Int = 1,
  var attackCooldown: Float = 0f,
  var dashCooldown: Float = 0f,
  var isAttacking: Boolean = false,
  var isDashing: Boolean = false,
  var dashTimer: Float = 0f,
  var attackTimer: Float = 0f
) : DataComponent

data class EnemyAIComponent(
  var type: EnemyType,
  var state: EnemyState = EnemyState.IDLE,
  var stateTimer: Float = 0f,
  var attackCooldown: Float = 0f,
  var attackRange: Float = 45f,
  var homeX: Float = 0f,
  var homeY: Float = 0f,
  var frozenTimer: Float = 0f,
  var corrodedTimer: Float = 0f,
  var burningTimer: Float = 0f,
  var phase: Int = 1
) : DataComponent

data class LightComponent(
  var colorR: Float,
  var colorG: Float,
  var colorB: Float,
  var radius: Float,
  var intensity: Float = 1f
) : DataComponent

data class WeaponComponent(
  var weaponId: String = "hammer_standard",
  var damage: Float = 38f,
  var attackRate: Float = 0.40f,
  var knockback: Float = 180f
) : DataComponent

data class AABB(
  val minX: Float,
  val minY: Float,
  val maxX: Float,
  val maxY: Float
) {
  val width: Float get() = maxX - minX
  val height: Float get() = maxY - minY
  val centerX: Float get() = (minX + maxX) / 2f
  val centerY: Float get() = (minY + maxY) / 2f

  fun overlaps(other: AABB): Boolean {
    return this.minX < other.maxX && this.maxX > other.minX &&
           this.minY < other.maxY && this.maxY > other.minY
  }
}

data class PhysicsComponent(
  var width: Float = 32f,
  var height: Float = 32f,
  var isStatic: Boolean = false,
  var isTrigger: Boolean = false,
  var mass: Float = 1.0f
) : DataComponent {
  fun getAABB(x: Float, y: Float): AABB {
    val halfW = width / 2f
    val halfH = height / 2f
    return AABB(x - halfW, y - halfH, x + halfW, y + halfH)
  }
}
