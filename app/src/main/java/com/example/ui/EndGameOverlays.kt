package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.GameEngine

@Composable
fun GameOverDialog(
  engine: GameEngine,
  onRespawn: () -> Unit
) {
  Dialog(onDismissRequest = {}) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFF0F172A),
      border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFEF4444)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("game_over_dialog")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          Icons.Default.Warning,
          contentDescription = "Fallen",
          tint = Color(0xFFEF4444),
          modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          "WARDEN CASUALTY",
          color = Color(0xFFEF4444),
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          "Your boiler has ruptured under catastrophic stress. The alchemical vapors claim another victim.",
          color = Color(0xFF94A3B8),
          fontSize = 12.sp,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onRespawn,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .testTag("respawn_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Text("RESPAWN AT CHECKPOINT", fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    }
  }
}

@Composable
fun VictoryScreen(
  engine: GameEngine,
  onContinueExploring: () -> Unit,
  onReturnTitle: () -> Unit
) {
  Dialog(onDismissRequest = {}) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFF0F172A),
      border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF10B981)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("victory_dialog")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          Icons.Default.CheckCircle,
          contentDescription = "Victory",
          tint = Color(0xFF10B981),
          modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          "CORE VAULT UNLOCKED",
          color = Color(0xFF10B981),
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 1.sp
        )

        Text(
          "Vertical Slice Complete!",
          color = Color(0xFFFBBF24),
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          "Overseer Malakor has been subdued and the runaway steam manifold neutralized. The Lower Foundry is secured.",
          color = Color(0xFFCBD5E1),
          fontSize = 12.sp,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Run Statistics
        Card(
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("• Warden Level: ${engine.playerLevel}", color = Color.White, fontSize = 11.sp)
            Text("• Alchemical Scrap Collected: ${engine.playerScrap}", color = Color(0xFFFBBF24), fontSize = 11.sp)
            Text("• Weapon: ${engine.equippedWeapon.name}", color = Color(0xFF38BDF8), fontSize = 11.sp)
            Text("• Plating: ${engine.equippedArmor.name}", color = Color(0xFF38BDF8), fontSize = 11.sp)
            val loreFound = engine.map.loreEntries.count { it.isDiscovered }
            Text("• Lore Records Uncovered: $loreFound / 3", color = Color(0xFF10B981), fontSize = 11.sp)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onReturnTitle,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("TITLE", fontSize = 11.sp)
          }

          Button(
            onClick = onContinueExploring,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1.5f)
          ) {
            Text("EXPLORE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
