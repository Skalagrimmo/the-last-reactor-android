package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameEngine
import com.example.model.VialType

@Composable
fun GameHUD(
  engine: GameEngine,
  onOpenTransmutation: () -> Unit,
  onSaveGame: () -> Unit,
  onToggleAudio: () -> Unit,
  onToggleRenderer: () -> Unit,
  isAudioOn: Boolean,
  isOpenGL: Boolean,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    // Top Row: Health, Steam, Scrap & Menu Buttons
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Vital Bars
      Column(modifier = Modifier.widthIn(max = 200.dp)) {
        // Health Bar
        val hpPercent = (engine.playerHealth / engine.playerMaxHealth).coerceIn(0f, 1f)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.Favorite,
            contentDescription = "Health",
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${engine.playerHealth.toInt()}/${engine.playerMaxHealth.toInt()}",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF475569), RoundedCornerShape(5.dp))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(hpPercent)
              .fillMaxHeight()
              .background(Color(0xFFDC2626))
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Steam Pressure Gauge
        val steamPercent = (engine.playerSteam / engine.playerMaxSteam).coerceIn(0f, 1f)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Default.Air,
            contentDescription = "Steam Pressure",
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Steam: ${engine.playerSteam.toInt()} PSI",
            color = Color(0xFF94A3B8),
            fontSize = 10.sp
          )
        }
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(steamPercent)
              .fillMaxHeight()
              .background(Color(0xFF0284C7))
          )
        }
      }

      // Middle: Scrap Currency & Level
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .background(Color(0xCC0F172A), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFB45309), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Icon(
            Icons.Default.Settings,
            contentDescription = "Alchemical Scrap",
            tint = Color(0xFFFBBF24),
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "${engine.playerScrap} Scrap",
            color = Color(0xFFFDE68A),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Lv.${engine.playerLevel}",
            color = Color(0xFF10B981),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Right: Utility Buttons (Forge, Save, Audio)
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Forge / Transmutation
        IconButton(
          onClick = onOpenTransmutation,
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xCC1E293B))
            .testTag("forge_button")
        ) {
          Icon(
            Icons.Default.Build,
            contentDescription = "Transmutation Forge",
            tint = Color(0xFFF59E0B),
            modifier = Modifier.size(18.dp)
          )
        }

        // Save Button
        IconButton(
          onClick = onSaveGame,
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xCC1E293B))
            .testTag("save_button")
        ) {
          Icon(
            Icons.Default.Save,
            contentDescription = "Save Game",
            tint = Color(0xFF10B981),
            modifier = Modifier.size(18.dp)
          )
        }

        // Audio Toggle
        IconButton(
          onClick = onToggleAudio,
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xCC1E293B))
            .testTag("audio_button")
        ) {
          Icon(
            if (isAudioOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            contentDescription = "Toggle Audio",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp)
          )
        }

        // Renderer Mode Toggle (OpenGL ES 2.0 / 2D Canvas)
        IconButton(
          onClick = onToggleRenderer,
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isOpenGL) Color(0xCC0284C7) else Color(0xCC1E293B))
            .testTag("renderer_button")
        ) {
          Icon(
            Icons.Default.Speed,
            contentDescription = "Toggle Renderer",
            tint = if (isOpenGL) Color(0xFFE0F2FE) else Color(0xFF94A3B8),
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Objective Banner
    val objectiveText = when {
      engine.isVictory -> "VICTORY: The Reactor Core Vault has been breached!"
      engine.defeatedBoss -> "Return to the Core Vault with the Master Siphon Key!"
      engine.map.doors.find { it.id == "door_boiler_hall" }?.isOpen == true ->
        "Confront Overseer Malakor in the Siphon Boiler Hall!"
      engine.map.valves.find { it.id == "valve_siphon_sluice" }?.isTurned == true ->
        "Hydraulic gate lowering. Proceed south toward the Boiler Hall!"
      engine.map.valves.find { it.id == "valve_north_pressure" }?.isTurned == true ->
        "North Conduit vented! Infiltrate the Siphon Sluice."
      else -> "Turn the North Boiler Wheel to depressurize scalding steam jets."
    }

    Surface(
      color = Color(0xDD090D14),
      shape = RoundedCornerShape(6.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Default.Flag,
          contentDescription = "Objective",
          tint = Color(0xFFF59E0B),
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = objectiveText,
          color = Color(0xFFE2E8F0),
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }

    // Active Prompt Toast
    if (engine.currentPromptMessage != null) {
      Spacer(modifier = Modifier.height(4.dp))
      Surface(
        color = Color(0xEE1E293B),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
        modifier = Modifier.align(Alignment.CenterHorizontally)
      ) {
        Text(
          text = engine.currentPromptMessage!!,
          color = Color(0xFF6EE7B7),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
      }
    }
  }
}
