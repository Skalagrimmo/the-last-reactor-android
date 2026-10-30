package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

data class GameSaveState(
  val playerX: Float = 250f,
  val playerY: Float = 250f,
  val health: Float = 100f,
  val level: Int = 1,
  val xp: Int = 0,
  val scrap: Int = 40,
  val vigorLevel: Int = 1,
  val pressureLevel: Int = 1,
  val potencyLevel: Int = 1,
  val equippedWeaponId: String = "hammer_standard",
  val equippedArmorId: String = "armor_gambeson",
  val vialVitriolCount: Int = 3,
  val vialPhlogistonCount: Int = 2,
  val vialCryoCount: Int = 2,
  val turnedValveIds: Set<String> = emptySet(),
  val openedDoorIds: Set<String> = emptySet(),
  val defeatedBoss: Boolean = false,
  val hasMasterKey: Boolean = false,
  val discoveredLoreIds: Set<String> = emptySet(),
  val lastSavedTimeMs: Long = System.currentTimeMillis()
)

class GameSaveManager(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("the_last_reactor_save", Context.MODE_PRIVATE)

  private val KEY_SAVE_DATA = "save_game_json"

  fun hasSavedGame(): Boolean {
    return prefs.contains(KEY_SAVE_DATA)
  }

  fun saveGame(state: GameSaveState) {
    try {
      val json = JSONObject().apply {
        put("playerX", state.playerX.toDouble())
        put("playerY", state.playerY.toDouble())
        put("health", state.health.toDouble())
        put("level", state.level)
        put("xp", state.xp)
        put("scrap", state.scrap)
        put("vigorLevel", state.vigorLevel)
        put("pressureLevel", state.pressureLevel)
        put("potencyLevel", state.potencyLevel)
        put("equippedWeaponId", state.equippedWeaponId)
        put("equippedArmorId", state.equippedArmorId)
        put("vialVitriolCount", state.vialVitriolCount)
        put("vialPhlogistonCount", state.vialPhlogistonCount)
        put("vialCryoCount", state.vialCryoCount)
        put("defeatedBoss", state.defeatedBoss)
        put("hasMasterKey", state.hasMasterKey)
        put("lastSavedTimeMs", System.currentTimeMillis())

        val valvesArray = JSONArray()
        state.turnedValveIds.forEach { valvesArray.put(it) }
        put("turnedValveIds", valvesArray)

        val doorsArray = JSONArray()
        state.openedDoorIds.forEach { doorsArray.put(it) }
        put("openedDoorIds", doorsArray)

        val loreArray = JSONArray()
        state.discoveredLoreIds.forEach { loreArray.put(it) }
        put("discoveredLoreIds", loreArray)
      }

      prefs.edit().putString(KEY_SAVE_DATA, json.toString()).apply()
    } catch (_: Exception) {
      // Fallback safe save
    }
  }

  fun loadGame(): GameSaveState? {
    val raw = prefs.getString(KEY_SAVE_DATA, null) ?: return null
    return try {
      val json = JSONObject(raw)

      val turnedValves = mutableSetOf<String>()
      val valvesArray = json.optJSONArray("turnedValveIds")
      if (valvesArray != null) {
        for (i in 0 until valvesArray.length()) {
          turnedValves.add(valvesArray.getString(i))
        }
      }

      val openedDoors = mutableSetOf<String>()
      val doorsArray = json.optJSONArray("openedDoorIds")
      if (doorsArray != null) {
        for (i in 0 until doorsArray.length()) {
          openedDoors.add(doorsArray.getString(i))
        }
      }

      val loreIds = mutableSetOf<String>()
      val loreArray = json.optJSONArray("discoveredLoreIds")
      if (loreArray != null) {
        for (i in 0 until loreArray.length()) {
          loreIds.add(loreArray.getString(i))
        }
      }

      GameSaveState(
        playerX = json.optDouble("playerX", 250.0).toFloat(),
        playerY = json.optDouble("playerY", 250.0).toFloat(),
        health = json.optDouble("health", 100.0).toFloat(),
        level = json.optInt("level", 1),
        xp = json.optInt("xp", 0),
        scrap = json.optInt("scrap", 40),
        vigorLevel = json.optInt("vigorLevel", 1),
        pressureLevel = json.optInt("pressureLevel", 1),
        potencyLevel = json.optInt("potencyLevel", 1),
        equippedWeaponId = json.optString("equippedWeaponId", "hammer_standard"),
        equippedArmorId = json.optString("equippedArmorId", "armor_gambeson"),
        vialVitriolCount = json.optInt("vialVitriolCount", 3),
        vialPhlogistonCount = json.optInt("vialPhlogistonCount", 2),
        vialCryoCount = json.optInt("vialCryoCount", 2),
        turnedValveIds = turnedValves,
        openedDoorIds = openedDoors,
        defeatedBoss = json.optBoolean("defeatedBoss", false),
        hasMasterKey = json.optBoolean("hasMasterKey", false),
        discoveredLoreIds = loreIds,
        lastSavedTimeMs = json.optLong("lastSavedTimeMs", System.currentTimeMillis())
      )
    } catch (_: Exception) {
      null
    }
  }

  fun clearSave() {
    prefs.edit().remove(KEY_SAVE_DATA).apply()
  }
}
