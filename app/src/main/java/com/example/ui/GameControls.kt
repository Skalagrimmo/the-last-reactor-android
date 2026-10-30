package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameEngine
import com.example.model.Vector2
import com.example.model.VialType
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun GameControls(
  engine: GameEngine,
  onMove: (Vector2) -> Unit,
  onAttack: () -> Unit,
  onDash: () -> Unit,
  onThrowVial: () -> Unit,
  onInteract: () -> Unit,
  onSwitchVial: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp)
  ) {
    // 1. Bottom-Left Virtual Joystick
    VirtualJoystick(
      onMove = onMove,
      modifier = Modifier
        .align(Alignment.BottomStart)
        .testTag("virtual_joystick")
    )

    // 2. Bottom-Right Action Buttons
    Row(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(bottom = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.Bottom
    ) {
      // Column 1: Vial Throw & Interact
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Interact Button (Operate valve / door)
        Button(
          onClick = onInteract,
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
          contentPadding = PaddingValues(0.dp),
          modifier = Modifier
            .size(48.dp)
            .border(2.dp, Color(0xFFFDE68A), CircleShape)
            .testTag("interact_button")
        ) {
          Icon(
            Icons.Default.TouchApp,
            contentDescription = "Interact",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
        }

        // Vial Throw Button (with vial switcher badge)
        Box {
          val currentVial = engine.selectedVial
          val count = engine.vialInventory[currentVial] ?: 0
          Button(
            onClick = onThrowVial,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(currentVial.colorHex)),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
              .size(54.dp)
              .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
              .testTag("vial_button")
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                Icons.Default.Science,
                contentDescription = "Vial",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = "$count",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Small switch badge
          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .offset(x = 4.dp, y = (-4).dp)
              .size(20.dp)
              .clip(CircleShape)
              .background(Color(0xFF0F172A))
              .border(1.dp, Color.White, CircleShape)
              .clickable { onSwitchVial() },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Default.Sync,
              contentDescription = "Switch Vial",
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(12.dp)
            )
          }
        }
      }

      // Column 2: Dash & Attack
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Steam Dash Button
        val canDash = engine.dashCooldown <= 0f && engine.playerSteam >= 20f
        Button(
          onClick = onDash,
          enabled = canDash,
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF0284C7),
            disabledContainerColor = Color(0xFF334155)
          ),
          contentPadding = PaddingValues(0.dp),
          modifier = Modifier
            .size(54.dp)
            .border(2.dp, Color(0xFF38BDF8), CircleShape)
            .testTag("dash_button")
        ) {
          Icon(
            Icons.Default.FastForward,
            contentDescription = "Steam Dash",
            tint = if (canDash) Color.White else Color(0xFF64748B),
            modifier = Modifier.size(24.dp)
          )
        }

        // Main Steam-Hammer Attack Button
        Button(
          onClick = onAttack,
          shape = CircleShape,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
          contentPadding = PaddingValues(0.dp),
          modifier = Modifier
            .size(68.dp)
            .border(3.dp, Color(0xFFFBBF24), CircleShape)
            .testTag("attack_button")
        ) {
          Icon(
            Icons.Default.FlashOn,
            contentDescription = "Hammer Strike",
            tint = Color(0xFFFEF3C7),
            modifier = Modifier.size(32.dp)
          )
        }
      }
    }
  }
}

@Composable
fun VirtualJoystick(
  onMove: (Vector2) -> Unit,
  modifier: Modifier = Modifier
) {
  val baseRadius = 65f
  val knobRadius = 25f
  var knobOffset by remember { mutableStateOf(Offset.Zero) }

  Box(
    modifier = modifier
      .size(130.dp)
      .clip(CircleShape)
      .background(Color(0x770F172A))
      .border(2.dp, Color(0xFF475569), CircleShape)
      .pointerInput(Unit) {
        detectDragGestures(
          onDragStart = { offset ->
            val center = Offset(size.width / 2f, size.height / 2f)
            val dragVector = offset - center
            val dist = dragVector.getDistance()
            val clampedDist = dist.coerceAtMost(baseRadius - knobRadius)
            val normal = if (dist > 0.001f) dragVector / dist else Offset.Zero
            knobOffset = normal * clampedDist
            onMove(Vector2(normal.x, normal.y))
          },
          onDrag = { change, dragAmount ->
            change.consume()
            val newOffset = knobOffset + dragAmount
            val dist = newOffset.getDistance()
            val clampedDist = dist.coerceAtMost(baseRadius - knobRadius)
            val normal = if (dist > 0.001f) newOffset / dist else Offset.Zero
            knobOffset = normal * clampedDist
            val strength = (clampedDist / (baseRadius - knobRadius)).coerceIn(0f, 1f)
            onMove(Vector2(normal.x * strength, normal.y * strength))
          },
          onDragEnd = {
            knobOffset = Offset.Zero
            onMove(Vector2(0f, 0f))
          },
          onDragCancel = {
            knobOffset = Offset.Zero
            onMove(Vector2(0f, 0f))
          }
        )
      },
    contentAlignment = Alignment.Center
  ) {
    // Inner Knob
    Box(
      modifier = Modifier
        .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
        .size(50.dp)
        .clip(CircleShape)
        .background(Color(0xFF334155))
        .border(2.dp, Color(0xFF94A3B8), CircleShape)
    )
  }
}
