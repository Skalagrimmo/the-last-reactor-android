package com.example.ecs

import com.example.engine.GameMap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min

/**
 * PhysicsSystem handles movement integration and AABB collision detection/resolution
 * for the Alchemical Warden and enemy entities.
 */
class PhysicsSystem(private val map: GameMap) {

  fun update(em: EntityManager, dt: Float) {
    val snapshot = em.getActiveEntitiesSnapshot()

    // 1. Movement Integration and Map Wall AABB Collisions
    for (id in snapshot) {
      val transform = em.transforms[id] ?: continue
      val velocity = em.velocities[id] ?: continue
      val physics = em.physics[id] ?: continue

      if (physics.isStatic) continue

      val isDashing = em.wardens[id]?.isDashing == true

      // Try moving along X axis
      val targetX = transform.x + velocity.vx * dt
      val aabbX = physics.getAABB(targetX, transform.y)
      if (!isCollidingWithMapAABB(aabbX)) {
        transform.x = targetX
      } else {
        velocity.vx = 0f
      }

      // Try moving along Y axis
      val targetY = transform.y + velocity.vy * dt
      val aabbY = physics.getAABB(transform.x, targetY)
      if (!isCollidingWithMapAABB(aabbY)) {
        transform.y = targetY
      } else {
        velocity.vy = 0f
      }

      // Update rotation angle when moving
      val speedSq = velocity.vx * velocity.vx + velocity.vy * velocity.vy
      if (speedSq > 10f) {
        transform.angle = atan2(velocity.vy, velocity.vx)
      }

      // Velocity damping / drag
      if (!isDashing) {
        velocity.vx *= velocity.drag
        velocity.vy *= velocity.drag
      }
    }

    // 2. Entity-to-Entity AABB Collision Detection and Separation
    val dynamicEntities = snapshot.filter { id ->
      val physics = em.physics[id]
      physics != null && !physics.isStatic && !physics.isTrigger && em.transforms.containsKey(id)
    }

    for (i in 0 until dynamicEntities.size) {
      val idA = dynamicEntities[i]
      val transformA = em.transforms[idA] ?: continue
      val physicsA = em.physics[idA] ?: continue
      val aabbA = physicsA.getAABB(transformA.x, transformA.y)

      for (j in (i + 1) until dynamicEntities.size) {
        val idB = dynamicEntities[j]
        val transformB = em.transforms[idB] ?: continue
        val physicsB = em.physics[idB] ?: continue
        val aabbB = physicsB.getAABB(transformB.x, transformB.y)

        if (aabbA.overlaps(aabbB)) {
          resolveAABBOverlap(transformA, physicsA, transformB, physicsB)
        }
      }
    }
  }

  private fun isCollidingWithMapAABB(box: AABB): Boolean {
    // Check perimeter and interior walls
    for (wall in map.walls) {
      val wallBox = AABB(wall.left, wall.top, wall.right, wall.bottom)
      if (box.overlaps(wallBox)) return true
    }

    // Check closed bulkhead doors
    for (door in map.doors) {
      if (!door.isOpen) {
        val doorBox = AABB(door.pos.x, door.pos.y, door.pos.x + door.width, door.pos.y + door.height)
        if (box.overlaps(doorBox)) return true
      }
    }

    return false
  }

  private fun resolveAABBOverlap(
    tA: TransformComponent,
    pA: PhysicsComponent,
    tB: TransformComponent,
    pB: PhysicsComponent
  ) {
    val aabbA = pA.getAABB(tA.x, tA.y)
    val aabbB = pB.getAABB(tB.x, tB.y)

    // Calculate overlap distance on each axis
    val overlapX1 = aabbA.maxX - aabbB.minX
    val overlapX2 = aabbB.maxX - aabbA.minX
    val overlapX = if (overlapX1 < overlapX2) overlapX1 else -overlapX2

    val overlapY1 = aabbA.maxY - aabbB.minY
    val overlapY2 = aabbB.maxY - aabbA.minY
    val overlapY = if (overlapY1 < overlapY2) overlapY1 else -overlapY2

    // Resolve along the minimum penetration axis
    if (abs(overlapX) < abs(overlapY)) {
      val pushA = overlapX * 0.5f
      val pushB = overlapX * 0.5f
      tA.x -= pushA
      tB.x += pushB
    } else {
      val pushA = overlapY * 0.5f
      val pushB = overlapY * 0.5f
      tA.y -= pushA
      tB.y += pushB
    }
  }
}
