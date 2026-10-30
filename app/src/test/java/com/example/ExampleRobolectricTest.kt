package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameSaveManager
import com.example.data.GameSaveState
import com.example.engine.GameEngine
import com.example.engine.GameMap
import com.example.engine.SoundSynthesizer
import com.example.model.Vector2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("The Last Reactor", appName)
  }

  @Test
  fun `save and load game state correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val saveManager = GameSaveManager(context)

    val testState = GameSaveState(
      playerX = 850f,
      playerY = 1200f,
      health = 85f,
      level = 3,
      scrap = 120,
      turnedValveIds = setOf("valve_north_pressure"),
      openedDoorIds = setOf("door_catwalk"),
      defeatedBoss = false
    )

    saveManager.saveGame(testState)
    assertTrue(saveManager.hasSavedGame())

    val loaded = saveManager.loadGame()
    assertNotNull(loaded)
    assertEquals(850f, loaded!!.playerX, 0.1f)
    assertEquals(1200f, loaded.playerY, 0.1f)
    assertEquals(3, loaded.level)
    assertEquals(120, loaded.scrap)
    assertTrue(loaded.turnedValveIds.contains("valve_north_pressure"))
    assertTrue(loaded.openedDoorIds.contains("door_catwalk"))
  }
}
