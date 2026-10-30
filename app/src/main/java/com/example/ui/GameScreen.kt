package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import com.example.model.Vector2
import com.example.opengl.ReactorGameView

@Composable
fun GameScreen(
  viewModel: GameViewModel,
  modifier: Modifier = Modifier
) {
  val engine = viewModel.engine
  val showTransmutation by viewModel.showTransmutation.collectAsState()
  val isAudioOn by viewModel.isAudioOn.collectAsState()
  val isOpenGLRenderer by viewModel.isOpenGLRenderer.collectAsState()

  val focusRequester = remember { FocusRequester() }
  var keyMoveVector by remember { mutableStateOf(Vector2()) }

  // Keyboard navigation tracking
  val pressedKeys = remember { mutableSetOf<Key>() }

  fun updateKeyMovement() {
    var dx = 0f
    var dy = 0f
    if (pressedKeys.contains(Key.W) || pressedKeys.contains(Key.DirectionUp)) dy -= 1f
    if (pressedKeys.contains(Key.S) || pressedKeys.contains(Key.DirectionDown)) dy += 1f
    if (pressedKeys.contains(Key.A) || pressedKeys.contains(Key.DirectionLeft)) dx -= 1f
    if (pressedKeys.contains(Key.D) || pressedKeys.contains(Key.DirectionRight)) dx += 1f
    keyMoveVector = Vector2(dx, dy)
    engine.movePlayer(keyMoveVector)
    viewModel.worldECS.setWardenVelocity(keyMoveVector)
  }

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  BackHandler {
    if (showTransmutation) {
      viewModel.closeTransmutation()
    } else {
      viewModel.returnToTitle()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .focusRequester(focusRequester)
      .focusable()
      .onKeyEvent { event ->
        val isDown = event.type == KeyEventType.KeyDown
        when (event.key) {
          Key.W, Key.DirectionUp,
          Key.S, Key.DirectionDown,
          Key.A, Key.DirectionLeft,
          Key.D, Key.DirectionRight -> {
            if (isDown) pressedKeys.add(event.key) else pressedKeys.remove(event.key)
            updateKeyMovement()
            true
          }
          Key.Spacebar, Key.J -> {
            if (isDown) {
              engine.triggerHammerAttack()
              viewModel.worldECS.triggerWardenAttack()
            }
            true
          }
          Key.K, Key.ShiftLeft, Key.ShiftRight -> {
            if (isDown) {
              engine.triggerSteamDash()
              viewModel.worldECS.triggerWardenDash()
            }
            true
          }
          Key.L, Key.Q -> {
            if (isDown) engine.triggerVialThrow()
            true
          }
          Key.E -> {
            if (isDown) engine.triggerInteract()
            true
          }
          Key.Tab, Key.I -> {
            if (isDown) {
              if (showTransmutation) viewModel.closeTransmutation() else viewModel.openTransmutation()
            }
            true
          }
          Key.Escape -> {
            if (isDown) viewModel.returnToTitle()
            true
          }
          else -> false
        }
      }
  ) {
    // 1. Hardware-Accelerated OpenGL ES 2.0 or 2D Canvas Renderer
    if (isOpenGLRenderer) {
      ReactorGameView(
        entityManager = viewModel.worldECS.entityManager,
        map = viewModel.map,
        getWardenId = { viewModel.worldECS.wardenId }
      )
    } else {
      GameCanvas(engine = engine)
    }

    // 2. HUD Overlay
    GameHUD(
      engine = engine,
      onOpenTransmutation = { viewModel.openTransmutation() },
      onSaveGame = { viewModel.saveCurrentGame() },
      onToggleAudio = { viewModel.toggleAudio() },
      onToggleRenderer = { viewModel.toggleRenderer() },
      isAudioOn = isAudioOn,
      isOpenGL = isOpenGLRenderer,
      modifier = Modifier
        .statusBarsPadding()
        .padding(top = 4.dp)
    )

    // 3. Touch Virtual Controls
    GameControls(
      engine = engine,
      onMove = { dir ->
        engine.movePlayer(dir)
        viewModel.worldECS.setWardenVelocity(dir)
      },
      onAttack = {
        engine.triggerHammerAttack()
        viewModel.worldECS.triggerWardenAttack()
      },
      onDash = {
        engine.triggerSteamDash()
        viewModel.worldECS.triggerWardenDash()
      },
      onThrowVial = { engine.triggerVialThrow() },
      onInteract = { engine.triggerInteract() },
      onSwitchVial = { viewModel.switchVial() },
      modifier = Modifier
        .navigationBarsPadding()
        .padding(bottom = 12.dp)
    )

    // 4. Overlays & Dialogs
    if (showTransmutation) {
      TransmutationDialog(
        engine = engine,
        onDismiss = { viewModel.closeTransmutation() }
      )
    }

    if (engine.isGameOver) {
      GameOverDialog(
        engine = engine,
        onRespawn = { engine.respawnAtCheckpoint() }
      )
    }

    if (engine.isVictory) {
      VictoryScreen(
        engine = engine,
        onContinueExploring = { engine.isVictory = false },
        onReturnTitle = { viewModel.returnToTitle() }
      )
    }
  }
}
