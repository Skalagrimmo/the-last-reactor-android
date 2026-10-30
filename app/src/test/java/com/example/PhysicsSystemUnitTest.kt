package com.example

import com.example.ecs.*
import com.example.engine.GameMap
import org.junit.Assert.*
import org.junit.Test

class PhysicsSystemUnitTest {

  @Test
  fun testAABBOverlapDetection() {
    val box1 = AABB(10f, 10f, 40f, 40f)
    val box2 = AABB(30f, 30f, 60f, 60f)
    val box3 = AABB(100f, 100f, 130f, 130f)

    assertTrue("box1 and box2 should overlap", box1.overlaps(box2))
    assertTrue("box2 and box1 should overlap", box2.overlaps(box1))
    assertFalse("box1 and box3 should not overlap", box1.overlaps(box3))
  }

  @Test
  fun testPhysicsMovementIntegration() {
    val map = GameMap()
    val em = EntityManager()
    val physicsSystem = PhysicsSystem(map)

    val wardenId = em.createWarden(250f, 250f)
    val velocity = em.velocities[wardenId]!!
    velocity.vx = 80f
    velocity.vy = 0f

    physicsSystem.update(em, 0.1f)

    val transform = em.transforms[wardenId]!!
    assertTrue("Warden X position should advance with velocity", transform.x > 250f)
    assertEquals("Warden Y position should remain unchanged", 250f, transform.y, 0.01f)
  }

  @Test
  fun testPhysicsWallCollisionPreventsClipping() {
    val map = GameMap()
    val em = EntityManager()
    val physicsSystem = PhysicsSystem(map)

    // Place Warden right next to Left border wall (tileSize is 60f, wall is 0..60f)
    val wardenId = em.createWarden(65f, 250f)
    val velocity = em.velocities[wardenId]!!
    // Drive westward directly into the wall
    velocity.vx = -300f
    velocity.vy = 0f

    physicsSystem.update(em, 0.1f)

    val transform = em.transforms[wardenId]!!
    val physics = em.physics[wardenId]!!
    val aabb = physics.getAABB(transform.x, transform.y)

    assertTrue("Entity should not penetrate beyond wall boundary (minX >= 60f)", aabb.minX >= 60f || transform.x >= 60f)
    assertEquals("Velocity towards wall should be zeroed out", 0f, velocity.vx, 0.01f)
  }

  @Test
  fun testEntityToEntityAABBCollisionSeparation() {
    val map = GameMap()
    val em = EntityManager()
    val physicsSystem = PhysicsSystem(map)

    // Place Warden and Skitterer overlapping at the same location
    val wardenId = em.createWarden(300f, 300f)
    val skittererId = em.createSkitterer(305f, 300f)

    val initialDistance = em.transforms[skittererId]!!.x - em.transforms[wardenId]!!.x
    assertTrue("Initial separation is within collision overlap", initialDistance < 30f)

    physicsSystem.update(em, 0.01f)

    val finalDistance = em.transforms[skittererId]!!.x - em.transforms[wardenId]!!.x
    assertTrue("Entities should be pushed apart to resolve overlap", finalDistance > initialDistance)
  }
}
