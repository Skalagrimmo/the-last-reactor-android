package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameSaveManager
import com.example.data.GameSaveState
import com.example.engine.GameEngine
import com.example.engine.GameMap
import com.example.engine.SoundSynthesizer
import com.example.model.EquipmentRegistry
import com.example.model.Vector2
import com.example.model.VialType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ScreenState {
  TITLE, PLAYING
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

  private val saveManager = GameSaveManager(application)
  private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  val soundSynth = SoundSynthesizer(viewModelScope)

  val map = GameMap()
  val engine = GameEngine(map, soundSynth, vibrator)
  val worldECS = com.example.ecs.WorldECS(map, soundSynth)

  private val _screenState = MutableStateFlow(ScreenState.TITLE)
  val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

  private val _showTransmutation = MutableStateFlow(false)
  val showTransmutation: StateFlow<Boolean> = _showTransmutation.asStateFlow()

  private val _hasSavedGame = MutableStateFlow(saveManager.hasSavedGame())
  val hasSavedGame: StateFlow<Boolean> = _hasSavedGame.asStateFlow()

  private val _isAudioOn = MutableStateFlow(soundSynth.isAudioEnabled())
  val isAudioOn: StateFlow<Boolean> = _isAudioOn.asStateFlow()

  private val _isOpenGLRenderer = MutableStateFlow(true)
  val isOpenGLRenderer: StateFlow<Boolean> = _isOpenGLRenderer.asStateFlow()

  fun toggleRenderer() {
    _isOpenGLRenderer.value = !_isOpenGLRenderer.value
    val mode = if (_isOpenGLRenderer.value) "OpenGL ES 2.0 (Dynamic Lights)" else "2D Canvas"
    engine.showPrompt("Active Renderer: $mode")
  }

  // Game Loop Job
  private var gameLoopJob: Job? = null

  fun startNewGame() {
    engine.resetLevel()
    worldECS.initWorld()
    engine.playerPos = Vector2(250f, 250f)
    engine.playerHealth = 100f
    engine.playerSteam = 80f
    engine.playerScrap = 40
    engine.playerLevel = 1
    engine.playerXp = 0
    engine.hasMasterKey = false
    engine.defeatedBoss = false
    engine.isVictory = false
    engine.isGameOver = false

    // Reset map
    for (v in map.valves) v.isTurned = false
    for (d in map.doors) d.isOpen = false
    for (h in map.hazards) {
      h.isActive = (h.id == "hazard_north_conduit")
      h.frozenTimer = 0f
    }

    _screenState.value = ScreenState.PLAYING
    startGameLoop()
  }

  fun continueGame() {
    val saved = saveManager.loadGame()
    if (saved != null) {
      engine.resetLevel()
      worldECS.initWorld()
      engine.playerPos = Vector2(saved.playerX, saved.playerY)
      engine.playerHealth = saved.health
      engine.playerLevel = saved.level
      engine.playerXp = saved.xp
      engine.playerScrap = saved.scrap
      engine.stats.vigorLevel = saved.vigorLevel
      engine.stats.pressureLevel = saved.pressureLevel
      engine.stats.potencyLevel = saved.potencyLevel

      engine.equippedWeapon = EquipmentRegistry.WEAPONS.find { it.id == saved.equippedWeaponId }
        ?: EquipmentRegistry.WEAPONS[0]
      engine.equippedArmor = EquipmentRegistry.ARMORS.find { it.id == saved.equippedArmorId }
        ?: EquipmentRegistry.ARMORS[0]

      engine.vialInventory[VialType.CORROSIVE_VITRIOL] = saved.vialVitriolCount
      engine.vialInventory[VialType.PYRETIC_PHLOGISTON] = saved.vialPhlogistonCount
      engine.vialInventory[VialType.CRYO_QUENCH] = saved.vialCryoCount

      engine.hasMasterKey = saved.hasMasterKey
      engine.defeatedBoss = saved.defeatedBoss

      // Restore valves
      for (v in map.valves) {
        v.isTurned = saved.turnedValveIds.contains(v.id)
        if (v.isTurned) {
          map.hazards.find { it.id == v.controlledHazardId }?.let { it.isActive = false }
          map.doors.find { it.id == v.controlledDoorId }?.let { it.isOpen = true }
        }
      }

      // Restore doors
      for (d in map.doors) {
        if (saved.openedDoorIds.contains(d.id)) {
          d.isOpen = true
        }
      }

      // Restore lore
      for (l in map.loreEntries) {
        l.isDiscovered = saved.discoveredLoreIds.contains(l.id)
      }

      engine.updateDerivedStats()
    } else {
      startNewGame()
      return
    }

    _screenState.value = ScreenState.PLAYING
    startGameLoop()
  }

  fun saveCurrentGame() {
    val state = GameSaveState(
      playerX = engine.playerPos.x,
      playerY = engine.playerPos.y,
      health = engine.playerHealth,
      level = engine.playerLevel,
      xp = engine.playerXp,
      scrap = engine.playerScrap,
      vigorLevel = engine.stats.vigorLevel,
      pressureLevel = engine.stats.pressureLevel,
      potencyLevel = engine.stats.potencyLevel,
      equippedWeaponId = engine.equippedWeapon.id,
      equippedArmorId = engine.equippedArmor.id,
      vialVitriolCount = engine.vialInventory[VialType.CORROSIVE_VITRIOL] ?: 0,
      vialPhlogistonCount = engine.vialInventory[VialType.PYRETIC_PHLOGISTON] ?: 0,
      vialCryoCount = engine.vialInventory[VialType.CRYO_QUENCH] ?: 0,
      turnedValveIds = map.valves.filter { it.isTurned }.map { it.id }.toSet(),
      openedDoorIds = map.doors.filter { it.isOpen }.map { it.id }.toSet(),
      defeatedBoss = engine.defeatedBoss,
      hasMasterKey = engine.hasMasterKey,
      discoveredLoreIds = map.loreEntries.filter { it.isDiscovered }.map { it.id }.toSet()
    )
    saveManager.saveGame(state)
    _hasSavedGame.value = true
    engine.showPrompt("Expedition Checkpoint Saved!")
    soundSynth.playLevelUp()
  }

  fun returnToTitle() {
    saveCurrentGame()
    stopGameLoop()
    _screenState.value = ScreenState.TITLE
  }

  fun openTransmutation() {
    _showTransmutation.value = true
  }

  fun closeTransmutation() {
    _showTransmutation.value = false
    engine.updateDerivedStats()
  }

  fun toggleAudio() {
    _isAudioOn.value = soundSynth.toggleAudio()
  }

  fun switchVial() {
    val vials = VialType.values()
    val nextIdx = (engine.selectedVial.ordinal + 1) % vials.size
    engine.selectedVial = vials[nextIdx]
    engine.showPrompt("Equipped: ${engine.selectedVial.displayName}")
    soundSynth.playHammerSwing()
  }

  private fun startGameLoop() {
    stopGameLoop()
    gameLoopJob = viewModelScope.launch {
      var lastTime = System.nanoTime()
      while (true) {
        val now = System.nanoTime()
        val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
        lastTime = now

        if (!_showTransmutation.value && !engine.isVictory && !engine.isGameOver) {
          engine.update(dt)
          worldECS.update(dt)

          // Keep Warden ECS transform and vitals in sync with engine
          worldECS.entityManager.transforms[worldECS.wardenId]?.let {
            it.x = engine.playerPos.x
            it.y = engine.playerPos.y
            it.angle = engine.playerFacingAngle
          }
          worldECS.entityManager.healths[worldECS.wardenId]?.let {
            it.current = engine.playerHealth
            it.max = engine.playerMaxHealth
          }
          worldECS.entityManager.wardens[worldECS.wardenId]?.let {
            it.steam = engine.playerSteam
            it.maxSteam = engine.playerMaxSteam
          }
        }

        delay(16) // ~60 FPS
      }
    }
  }

  private fun stopGameLoop() {
    gameLoopJob?.cancel()
    gameLoopJob = null
  }

  override fun onCleared() {
    super.onCleared()
    stopGameLoop()
  }
}
