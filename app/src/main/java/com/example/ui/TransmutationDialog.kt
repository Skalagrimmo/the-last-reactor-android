package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.GameEngine
import com.example.model.EquipmentRegistry
import com.example.model.LoreEntry

enum class TransmuteTab {
  EQUIPMENT, ATTRIBUTES, LORE
}

@Composable
fun TransmutationDialog(
  engine: GameEngine,
  onDismiss: () -> Unit
) {
  var currentTab by remember { mutableStateOf(TransmuteTab.EQUIPMENT) }
  var scrap by remember { mutableStateOf(engine.playerScrap) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFF0F172A),
      border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFB45309)),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 580.dp)
        .testTag("transmutation_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.Build,
              contentDescription = "Transmutation Bench",
              tint = Color(0xFFF59E0B),
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Alchemical Forge & Codex",
              color = Color.White,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
          }
        }

        // Scrap Balance Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Available Scrap:", color = Color(0xFF94A3B8), fontSize = 13.sp)
          Text(
            "$scrap Alchemical Scrap",
            color = Color(0xFFFBBF24),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TabButton(
            text = "Equipment",
            isSelected = currentTab == TransmuteTab.EQUIPMENT,
            onClick = { currentTab = TransmuteTab.EQUIPMENT },
            modifier = Modifier.weight(1f)
          )
          TabButton(
            text = "Attributes",
            isSelected = currentTab == TransmuteTab.ATTRIBUTES,
            onClick = { currentTab = TransmuteTab.ATTRIBUTES },
            modifier = Modifier.weight(1f)
          )
          TabButton(
            text = "Lore Codex",
            isSelected = currentTab == TransmuteTab.LORE,
            onClick = { currentTab = TransmuteTab.LORE },
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        when (currentTab) {
          TransmuteTab.EQUIPMENT -> {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              item {
                Text(
                  "Weapons (Steam-Hammers)",
                  color = Color(0xFFF59E0B),
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
              }
              items(EquipmentRegistry.WEAPONS) { weapon ->
                val isEquipped = engine.equippedWeapon.id == weapon.id
                val canAfford = scrap >= weapon.costScrap

                Card(
                  colors = CardDefaults.cardColors(
                    containerColor = if (isEquipped) Color(0xFF1E3A5F) else Color(0xFF1E293B)
                  ),
                  border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isEquipped) Color(0xFF38BDF8) else Color(0xFF334155)
                  )
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        weapon.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                      )
                      Text(
                        "Dmg: ${weapon.damage.toInt()} | Rate: ${weapon.attackRate}s | Knockback: ${weapon.knockback.toInt()}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                      )
                      Text(
                        weapon.description,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                      )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isEquipped) {
                      Text(
                        "EQUIPPED",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                      )
                    } else {
                      Button(
                        onClick = {
                          if (canAfford) {
                            scrap -= weapon.costScrap
                            engine.playerScrap = scrap
                            engine.equippedWeapon = weapon
                            engine.soundSynth.playLevelUp()
                          }
                        },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                      ) {
                        Text(
                          if (weapon.costScrap > 0) "${weapon.costScrap} Scrap" else "Equip",
                          fontSize = 11.sp
                        )
                      }
                    }
                  }
                }
              }

              item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  "Armor Plating",
                  color = Color(0xFFF59E0B),
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
              }
              items(EquipmentRegistry.ARMORS) { armor ->
                val isEquipped = engine.equippedArmor.id == armor.id
                val canAfford = scrap >= armor.costScrap

                Card(
                  colors = CardDefaults.cardColors(
                    containerColor = if (isEquipped) Color(0xFF1E3A5F) else Color(0xFF1E293B)
                  ),
                  border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isEquipped) Color(0xFF38BDF8) else Color(0xFF334155)
                  )
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        armor.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                      )
                      Text(
                        "Def: +${armor.defense.toInt()} | Steam Capacity: +${armor.steamCapacityBonus.toInt()}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                      )
                      Text(
                        armor.description,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                      )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isEquipped) {
                      Text(
                        "EQUIPPED",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                      )
                    } else {
                      Button(
                        onClick = {
                          if (canAfford) {
                            scrap -= armor.costScrap
                            engine.playerScrap = scrap
                            engine.equippedArmor = armor
                            engine.updateDerivedStats()
                            engine.soundSynth.playLevelUp()
                          }
                        },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                      ) {
                        Text(
                          if (armor.costScrap > 0) "${armor.costScrap} Scrap" else "Equip",
                          fontSize = 11.sp
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          TransmuteTab.ATTRIBUTES -> {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              item {
                AttributeRow(
                  name = "Vigor (Constitution)",
                  description = "Increases maximum Warden health by 25 HP per rank.",
                  level = engine.stats.vigorLevel,
                  currentVal = "${engine.stats.maxHealth().toInt()} HP",
                  cost = engine.stats.upgradeCost(engine.stats.vigorLevel),
                  canAfford = scrap >= engine.stats.upgradeCost(engine.stats.vigorLevel),
                  onUpgrade = {
                    val cost = engine.stats.upgradeCost(engine.stats.vigorLevel)
                    if (scrap >= cost) {
                      scrap -= cost
                      engine.playerScrap = scrap
                      engine.stats.vigorLevel++
                      engine.updateDerivedStats()
                      engine.soundSynth.playLevelUp()
                    }
                  }
                )
              }

              item {
                AttributeRow(
                  name = "Steam Pressure",
                  description = "Increases steam boiler reservoir and enables faster dash recovery.",
                  level = engine.stats.pressureLevel,
                  currentVal = "${engine.stats.maxSteam().toInt()} PSI",
                  cost = engine.stats.upgradeCost(engine.stats.pressureLevel),
                  canAfford = scrap >= engine.stats.upgradeCost(engine.stats.pressureLevel),
                  onUpgrade = {
                    val cost = engine.stats.upgradeCost(engine.stats.pressureLevel)
                    if (scrap >= cost) {
                      scrap -= cost
                      engine.playerScrap = scrap
                      engine.stats.pressureLevel++
                      engine.updateDerivedStats()
                      engine.soundSynth.playLevelUp()
                    }
                  }
                )
              }

              item {
                AttributeRow(
                  name = "Alchemical Potency",
                  description = "Boosts damage and elemental effect duration of all Catalyst Vials.",
                  level = engine.stats.potencyLevel,
                  currentVal = "+${((engine.stats.vialPotencyMultiplier() - 1f) * 100).toInt()}% Potency",
                  cost = engine.stats.upgradeCost(engine.stats.potencyLevel),
                  canAfford = scrap >= engine.stats.upgradeCost(engine.stats.potencyLevel),
                  onUpgrade = {
                    val cost = engine.stats.upgradeCost(engine.stats.potencyLevel)
                    if (scrap >= cost) {
                      scrap -= cost
                      engine.playerScrap = scrap
                      engine.stats.potencyLevel++
                      engine.soundSynth.playLevelUp()
                    }
                  }
                )
              }
            }
          }

          TransmuteTab.LORE -> {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxSize()
            ) {
              items(engine.map.loreEntries) { lore ->
                Card(
                  colors = CardDefaults.cardColors(
                    containerColor = if (lore.isDiscovered) Color(0xFF1E293B) else Color(0xFF0F172A)
                  ),
                  border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (lore.isDiscovered) Color(0xFF0284C7) else Color(0xFF334155)
                  )
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    if (lore.isDiscovered) {
                      Text(
                        lore.title,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                      )
                      Text(
                        "Author: ${lore.author}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                      )
                      Spacer(modifier = Modifier.height(4.dp))
                      Text(
                        lore.excerpt,
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                      )
                    } else {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                          Icons.Default.Lock,
                          contentDescription = "Undiscovered Lore",
                          tint = Color(0xFF64748B),
                          modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                          "Encrypted Warden Log (Undiscovered in District)",
                          color = Color(0xFF64748B),
                          fontSize = 12.sp
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TabButton(
  text: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Button(
    onClick = onClick,
    shape = RoundedCornerShape(8.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = if (isSelected) Color(0xFFB45309) else Color(0xFF1E293B)
    ),
    contentPadding = PaddingValues(vertical = 6.dp),
    modifier = modifier
  ) {
    Text(
      text = text,
      color = if (isSelected) Color.White else Color(0xFF94A3B8),
      fontSize = 12.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
    )
  }
}

@Composable
private fun AttributeRow(
  name: String,
  description: String,
  level: Int,
  currentVal: String,
  cost: Int,
  canAfford: Boolean,
  onUpgrade: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text("Rank $level | Current: $currentVal", color = Color(0xFF10B981), fontSize = 11.sp)
        Text(description, color = Color(0xFF94A3B8), fontSize = 10.sp)
      }

      Spacer(modifier = Modifier.width(8.dp))

      Button(
        onClick = onUpgrade,
        enabled = canAfford,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text("$cost Scrap", fontSize = 11.sp)
      }
    }
  }
}
