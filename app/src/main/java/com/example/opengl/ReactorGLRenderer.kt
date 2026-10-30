package com.example.opengl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.ecs.EntityManager
import com.example.ecs.ShapeType
import com.example.engine.GameMap
import com.example.model.Direction
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

class ReactorGLRenderer(
  private val entityManager: EntityManager,
  private val map: GameMap,
  private val getWardenId: () -> Int
) : GLSurfaceView.Renderer {

  private var surfaceWidth = 1f
  private var surfaceHeight = 1f

  private val projectionMatrix = FloatArray(16)
  private val viewMatrix = FloatArray(16)
  private val mvpMatrix = FloatArray(16)
  private val modelMatrix = FloatArray(16)

  private var programId = 0
  private var aPositionLoc = 0
  private var uColorLoc = 0
  private var uMvpMatrixLoc = 0
  private var uModelMatrixLoc = 0
  private var uAmbientLoc = 0
  private var uNumLightsLoc = 0
  private var uLightPosLoc = 0
  private var uLightColorLoc = 0
  private var uLightRadiusLoc = 0
  private var uLightIntensityLoc = 0

  // Quad Vertex Buffer (Unit Quad centered at 0,0)
  private lateinit var quadVertexBuffer: FloatBuffer
  private lateinit var quadIndexBuffer: ShortBuffer

  private val maxLights = 8
  private val lightPosArray = FloatArray(maxLights * 2)
  private val lightColorArray = FloatArray(maxLights * 3)
  private val lightRadiusArray = FloatArray(maxLights)
  private val lightIntensityArray = FloatArray(maxLights)

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    GLES20.glClearColor(0.035f, 0.051f, 0.078f, 1.0f)
    GLES20.glEnable(GLES20.GL_BLEND)
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

    initShaders()
    initBuffers()
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    surfaceWidth = width.toFloat()
    surfaceHeight = height.toFloat()
    GLES20.glViewport(0, 0, width, height)
  }

  override fun onDrawFrame(gl: GL10?) {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    if (programId == 0) return

    val wardenId = getWardenId()
    val wardenTransform = entityManager.transforms[wardenId]
    val wardenX = wardenTransform?.x ?: 250f
    val wardenY = wardenTransform?.y ?: 250f

    // Camera following Warden
    val camX = (wardenX - surfaceWidth / 2f).coerceIn(0f, map.width - surfaceWidth)
    val camY = (wardenY - surfaceHeight / 2f).coerceIn(0f, map.height - surfaceHeight)

    Matrix.orthoM(projectionMatrix, 0, camX, camX + surfaceWidth, camY + surfaceHeight, camY, -1f, 1f)
    Matrix.setIdentityM(viewMatrix, 0)

    GLES20.glUseProgram(programId)

    // Gather and upload dynamic point lights
    uploadPointLights()

    // 1. Draw Floor Tiles
    drawDistrictFloors(camX, camY)

    // 2. Draw Walls & Machinery
    drawWalls()

    // 3. Draw Steam Hazards
    drawSteamHazards()

    // 4. Draw Bulkhead Doors & Valves
    drawDoorsAndValves()

    // 5. Draw ECS Entities (Warden, Skitterers, Automatons, Boss)
    drawEcsEntities()
  }

  private fun uploadPointLights() {
    var lightCount = 0
    val snapshot = entityManager.getActiveEntitiesSnapshot()
    for (id in snapshot) {
      val light = entityManager.lights[id] ?: continue
      val transform = entityManager.transforms[id] ?: continue

      if (lightCount < maxLights) {
        val idx2 = lightCount * 2
        val idx3 = lightCount * 3

        lightPosArray[idx2] = transform.x
        lightPosArray[idx2 + 1] = transform.y

        lightColorArray[idx3] = light.colorR
        lightColorArray[idx3 + 1] = light.colorG
        lightColorArray[idx3 + 2] = light.colorB

        lightRadiusArray[lightCount] = light.radius
        lightIntensityArray[lightCount] = light.intensity

        lightCount++
      }
    }

    GLES20.glUniform3f(uAmbientLoc, 0.22f, 0.26f, 0.32f) // Gothic industrial ambient tone
    GLES20.glUniform1i(uNumLightsLoc, lightCount)
    if (lightCount > 0) {
      GLES20.glUniform2fv(uLightPosLoc, lightCount, lightPosArray, 0)
      GLES20.glUniform3fv(uLightColorLoc, lightCount, lightColorArray, 0)
      GLES20.glUniform1fv(uLightRadiusLoc, lightCount, lightRadiusArray, 0)
      GLES20.glUniform1fv(uLightIntensityLoc, lightCount, lightIntensityArray, 0)
    }
  }

  private fun drawQuad(x: Float, y: Float, width: Float, height: Float, r: Float, g: Float, b: Float, a: Float, angle: Float = 0f) {
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.translateM(modelMatrix, 0, x, y, 0f)
    if (angle != 0f) {
      Matrix.rotateM(modelMatrix, 0, Math.toDegrees(angle.toDouble()).toFloat(), 0f, 0f, 1f)
    }
    Matrix.scaleM(modelMatrix, 0, width, height, 1f)

    Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0)
    GLES20.glUniformMatrix4fv(uMvpMatrixLoc, 1, false, mvpMatrix, 0)
    GLES20.glUniformMatrix4fv(uModelMatrixLoc, 1, false, modelMatrix, 0)
    GLES20.glUniform4f(uColorLoc, r, g, b, a)

    quadVertexBuffer.position(0)
    GLES20.glVertexAttribPointer(aPositionLoc, 2, GLES20.GL_FLOAT, false, 0, quadVertexBuffer)
    GLES20.glEnableVertexAttribArray(aPositionLoc)

    quadIndexBuffer.position(0)
    GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, quadIndexBuffer)
  }

  private fun drawDistrictFloors(camX: Float, camY: Float) {
    val tileSize = map.tileSize
    val startCol = (camX / tileSize).toInt().coerceAtLeast(0)
    val endCol = ((camX + surfaceWidth) / tileSize).toInt().coerceAtMost(map.cols - 1)
    val startRow = (camY / tileSize).toInt().coerceAtLeast(0)
    val endRow = ((camY + surfaceHeight) / tileSize).toInt().coerceAtMost(map.rows - 1)

    for (col in startCol..endCol) {
      for (row in startRow..endRow) {
        val x = col * tileSize + tileSize / 2f
        val y = row * tileSize + tileSize / 2f
        val isAlt = (col + row) % 2 == 0
        if (isAlt) {
          drawQuad(x, y, tileSize - 1f, tileSize - 1f, 0.086f, 0.110f, 0.141f, 1f)
        } else {
          drawQuad(x, y, tileSize - 1f, tileSize - 1f, 0.105f, 0.137f, 0.176f, 1f)
        }
      }
    }
  }

  private fun drawWalls() {
    for (wall in map.walls) {
      val cx = wall.left + wall.width / 2f
      val cy = wall.top + wall.height / 2f
      drawQuad(cx, cy, wall.width, wall.height, 0.137f, 0.176f, 0.220f, 1f)
    }
  }

  private fun drawSteamHazards() {
    for (hazard in map.hazards) {
      if (!hazard.isActive && hazard.frozenTimer <= 0f) continue
      val isFrozen = hazard.frozenTimer > 0f
      val r = if (isFrozen) 0.22f else 0.88f
      val g = if (isFrozen) 0.74f else 0.92f
      val b = if (isFrozen) 0.97f else 0.98f

      val len = hazard.length
      val thickness = 22f
      val (cx, cy, w, h) = when (hazard.direction) {
        Direction.RIGHT -> listOf(hazard.pos.x + len / 2f, hazard.pos.y, len, thickness)
        Direction.LEFT -> listOf(hazard.pos.x - len / 2f, hazard.pos.y, len, thickness)
        Direction.DOWN -> listOf(hazard.pos.x, hazard.pos.y + len / 2f, thickness, len)
        Direction.UP -> listOf(hazard.pos.x, hazard.pos.y - len / 2f, thickness, len)
      }
      drawQuad(cx, cy, w, h, r, g, b, 0.75f)
    }
  }

  private fun drawDoorsAndValves() {
    // Bulkhead doors
    for (door in map.doors) {
      val cx = door.pos.x + door.width / 2f
      val cy = door.pos.y + door.height / 2f
      if (door.isOpen) {
        drawQuad(cx, cy, door.width, door.height, 0.06f, 0.72f, 0.50f, 0.35f)
      } else {
        val r = if (door.requiresKey) 0.86f else 0.96f
        val g = if (door.requiresKey) 0.15f else 0.62f
        val b = if (door.requiresKey) 0.15f else 0.04f
        drawQuad(cx, cy, door.width, door.height, r, g, b, 0.95f)
      }
    }

    // Valves
    for (valve in map.valves) {
      val vr = if (valve.isTurned) 0.06f else 0.93f
      val vg = if (valve.isTurned) 0.72f else 0.26f
      val vb = if (valve.isTurned) 0.50f else 0.26f
      drawQuad(valve.pos.x, valve.pos.y, 28f, 28f, 0.85f, 0.46f, 0.02f, 1f)
      drawQuad(valve.pos.x, valve.pos.y, 14f, 14f, vr, vg, vb, 1f)
    }
  }

  private fun drawEcsEntities() {
    val snapshot = entityManager.getActiveEntitiesSnapshot()
    for (id in snapshot) {
      val transform = entityManager.transforms[id] ?: continue
      val render = entityManager.renderables[id] ?: continue
      if (!render.visible) continue

      val size = render.size
      when (render.shape) {
        ShapeType.WARDEN -> {
          // Warden Body
          drawQuad(transform.x, transform.y, size * 1.8f, size * 1.8f, render.colorR, render.colorG, render.colorB, render.alpha, transform.angle)
          // Visor slit (Emerald)
          val visorDist = size * 0.55f
          val vx = transform.x + cos(transform.angle) * visorDist
          val vy = transform.y + sin(transform.angle) * visorDist
          drawQuad(vx, vy, 8f, 8f, 0.06f, 0.85f, 0.50f, 1f)
        }
        ShapeType.SKITTERER -> {
          // Arachnid body with sharp angle
          drawQuad(transform.x, transform.y, size * 1.4f, size * 1.4f, render.colorR, render.colorG, render.colorB, render.alpha, transform.angle)
          // Red Optic Eye
          val eyeDist = size * 0.45f
          drawQuad(transform.x + cos(transform.angle) * eyeDist, transform.y + sin(transform.angle) * eyeDist, 6f, 6f, 0.95f, 0.15f, 0.15f, 1f)
        }
        ShapeType.STEAM_VANE -> {
          // Heavy kettle turret
          drawQuad(transform.x, transform.y, size * 1.8f, size * 1.8f, render.colorR, render.colorG, render.colorB, render.alpha, transform.angle)
          // Brass boiler nozzle
          val nozDist = size * 0.7f
          drawQuad(transform.x + cos(transform.angle) * nozDist, transform.y + sin(transform.angle) * nozDist, 10f, 10f, 0.95f, 0.65f, 0.10f, 1f)
        }
        ShapeType.BOSS_OVERSEER -> {
          // Colossal Forge Sovereign
          drawQuad(transform.x, transform.y, size * 2f, size * 2f, render.colorR, render.colorG, render.colorB, render.alpha, transform.angle)
          // Core Furnace
          drawQuad(transform.x, transform.y, size * 0.9f, size * 0.9f, 0.93f, 0.26f, 0.26f, 1f)
        }
        else -> {
          drawQuad(transform.x, transform.y, size, size, render.colorR, render.colorG, render.colorB, render.alpha, transform.angle)
        }
      }
    }
  }

  private fun initBuffers() {
    // 1x1 Quad centered at (0,0)
    val vertices = floatArrayOf(
      -0.5f,  0.5f,
      -0.5f, -0.5f,
       0.5f, -0.5f,
       0.5f,  0.5f
    )
    val indices = shortArrayOf(0, 1, 2, 0, 2, 3)

    quadVertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
      .order(ByteOrder.nativeOrder())
      .asFloatBuffer()
      .put(vertices)
    quadVertexBuffer.position(0)

    quadIndexBuffer = ByteBuffer.allocateDirect(indices.size * 2)
      .order(ByteOrder.nativeOrder())
      .asShortBuffer()
      .put(indices)
    quadIndexBuffer.position(0)
  }

  private fun initShaders() {
    val vertexShaderCode = """
      uniform mat4 u_MvpMatrix;
      uniform mat4 u_ModelMatrix;
      attribute vec4 a_Position;
      varying vec2 v_WorldPos;

      void main() {
        vec4 worldPos = u_ModelMatrix * a_Position;
        v_WorldPos = worldPos.xy;
        gl_Position = u_MvpMatrix * a_Position;
      }
    """.trimIndent()

    val fragmentShaderCode = """
      precision mediump float;
      varying vec2 v_WorldPos;
      uniform vec4 u_Color;

      uniform vec3 u_AmbientColor;
      uniform int u_NumLights;
      uniform vec2 u_LightPos[8];
      uniform vec3 u_LightColor[8];
      uniform float u_LightRadius[8];
      uniform float u_LightIntensity[8];

      void main() {
        vec3 lightAccum = u_AmbientColor;

        for (int i = 0; i < 8; i++) {
          if (i >= u_NumLights) break;
          float dist = distance(v_WorldPos, u_LightPos[i]);
          float radius = u_LightRadius[i];
          if (dist < radius) {
            float atten = (1.0 - (dist / radius)) * u_LightIntensity[i];
            lightAccum += u_LightColor[i] * atten;
          }
        }

        lightAccum = clamp(lightAccum, 0.0, 1.4);
        gl_FragColor = vec4(u_Color.rgb * lightAccum, u_Color.a);
      }
    """.trimIndent()

    val vShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
    val fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

    programId = GLES20.glCreateProgram().also {
      GLES20.glAttachShader(it, vShader)
      GLES20.glAttachShader(it, fShader)
      GLES20.glLinkProgram(it)
    }

    aPositionLoc = GLES20.glGetAttribLocation(programId, "a_Position")
    uColorLoc = GLES20.glGetUniformLocation(programId, "u_Color")
    uMvpMatrixLoc = GLES20.glGetUniformLocation(programId, "u_MvpMatrix")
    uModelMatrixLoc = GLES20.glGetUniformLocation(programId, "u_ModelMatrix")
    uAmbientLoc = GLES20.glGetUniformLocation(programId, "u_AmbientColor")
    uNumLightsLoc = GLES20.glGetUniformLocation(programId, "u_NumLights")
    uLightPosLoc = GLES20.glGetUniformLocation(programId, "u_LightPos")
    uLightColorLoc = GLES20.glGetUniformLocation(programId, "u_LightColor")
    uLightRadiusLoc = GLES20.glGetUniformLocation(programId, "u_LightRadius")
    uLightIntensityLoc = GLES20.glGetUniformLocation(programId, "u_LightIntensity")
  }

  private fun loadShader(type: Int, shaderCode: String): Int {
    return GLES20.glCreateShader(type).also { shader ->
      GLES20.glShaderSource(shader, shaderCode)
      GLES20.glCompileShader(shader)
    }
  }
}
