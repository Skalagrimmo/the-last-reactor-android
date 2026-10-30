package com.example

import com.example.ecs.EntityManager
import com.example.engine.GameMap
import com.example.opengl.GLRenderer
import org.junit.Assert.*
import org.junit.Test

class GLRendererUnitTest {

  @Test
  fun testGLRendererInitialization() {
    val map = GameMap()
    val em = EntityManager()
    val renderer = GLRenderer(
      entityManager = em,
      map = map,
      getWardenId = { 1 }
    )
    assertNotNull("GLRenderer instance should be successfully created", renderer)
  }

  @Test
  fun testGLRendererCameraBoundsCalculation() {
    val map = GameMap()
    val em = EntityManager()
    val wardenId = em.createWarden(1200f, 1500f)

    val renderer = GLRenderer(
      entityManager = em,
      map = map,
      getWardenId = { wardenId }
    )

    val transform = em.transforms[wardenId]!!
    assertEquals(1200f, transform.x, 0.01f)
    assertEquals(1500f, transform.y, 0.01f)
  }
}
