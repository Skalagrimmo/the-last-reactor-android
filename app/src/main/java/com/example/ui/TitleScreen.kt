package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TitleScreen(
  hasSavedGame: Boolean,
  onNewGame: () -> Unit,
  onContinueGame: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showBriefing by remember { mutableStateOf(false) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(Color(0xFF090D14), Color(0xFF141A23), Color(0xFF0A0E15))
        )
      ),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Emblem icon
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(Color(0xFF1E293B))
          .border(2.dp, Color(0xFF10B981), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.Whatshot,
          contentDescription = "Reactor Core Emblem",
          tint = Color(0xFF10B981),
          modifier = Modifier.size(48.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Game Title
      Text(
        text = "THE LAST REACTOR",
        color = Color(0xFFF1F5F9),
        fontSize = 28.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        textAlign = TextAlign.Center
      )

      Text(
        text = "Gothic Industrial Action-RPG",
        color = Color(0xFFF59E0B),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp
      )

      Spacer(modifier = Modifier.height(32.dp))

      // Action Buttons
      Column(
        modifier = Modifier.widthIn(max = 280.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        if (hasSavedGame) {
          Button(
            onClick = onContinueGame,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("continue_game_button")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("CONTINUE EXPEDITION", fontWeight = FontWeight.Bold, color = Color.White)
          }
        }

        Button(
          onClick = onNewGame,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB45309)),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("new_game_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (hasSavedGame) "START NEW EXPEDITION" else "ENTER THE FOUNDRY", fontWeight = FontWeight.Bold, color = Color.White)
        }

        OutlinedButton(
          onClick = { showBriefing = true },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("briefing_button")
        ) {
          Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("WARDEN BRIEFING & CONTROLS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      Text(
        text = "Fortified City of Aethelspire — Siphon District",
        color = Color(0xFF475569),
        fontSize = 11.sp
      )
    }

    if (showBriefing) {
      AlertDialog(
        onDismissRequest = { showBriefing = false },
        containerColor = Color(0xFF0F172A),
        title = {
          Text("Warden Briefing & Mechanics", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              "THE SITUATION:\nThe ancient Alchemical Reactor at the heart of Aethelspire has ruptured. Scalding steam jets block vital corridors. Corrupted Clockwork Skitterers and Steam Vane Automatons patrol the Foundry under the thrall of Overseer Malakor.",
              color = Color(0xFFE2E8F0),
              fontSize = 11.sp
            )
            Divider(color = Color(0xFF334155))
            Text(
              "CONTROLS:\n• Move: Virtual Joystick (Touch) or WASD / Arrow Keys (Keyboard)\n• Attack: Hammer Button or Space / J\n• Dash: Steam Dash Button or K / Shift (Consumes Steam)\n• Catalyst Vial: Flask Button or L (Corrosive, Pyretic, Cryo)\n• Interact: Wheel Button or E (Operates valves & doors)\n• Forge: Anvil Button (Spend Scrap for weapon & attribute upgrades)",
              color = Color(0xFF94A3B8),
              fontSize = 11.sp
            )
            Divider(color = Color(0xFF334155))
            Text(
              "CRITICAL MECHANIC:\nTurn brass boiler wheels to depressurize scalding steam jets and lower heavy hydraulic gates. Cryo Quench vials temporarily freeze leaking conduits!",
              color = Color(0xFF38BDF8),
              fontSize = 11.sp
            )
          }
        },
        confirmButton = {
          TextButton(onClick = { showBriefing = false }) {
            Text("UNDERSTOOD", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
          }
        }
      )
    }
  }
}
