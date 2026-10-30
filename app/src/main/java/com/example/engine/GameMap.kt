package com.example.engine

import androidx.compose.ui.geometry.Rect
import com.example.model.*

data class Tile(
  val col: Int,
  val row: Int,
  val isWall: Boolean,
  val type: TileType
)

enum class TileType {
  FLOOR_SLATE,
  FLOOR_GRATE,
  WALL_MASONRY,
  WALL_PIPES,
  LAVA_TRENCH,
  WATER_SLUICE
}

data class MapObject(
  val id: String,
  val bounds: Rect,
  val isSolid: Boolean,
  val name: String
)

class GameMap {
  val tileSize = 60f
  val cols = 40
  val rows = 40
  val width = cols * tileSize
  val height = rows * tileSize

  val walls = mutableListOf<Rect>()
  val valves = mutableListOf<SteamConduitValve>()
  val doors = mutableListOf<BulkheadDoor>()
  val hazards = mutableListOf<SteamHazard>()
  val loreEntries = mutableListOf<LoreEntry>()
  val checkpoints = mutableListOf<Vector2>()

  init {
    buildDistrict()
  }

  private fun buildDistrict() {
    // 1. Boundary walls
    walls.add(Rect(0f, 0f, width, tileSize)) // Top
    walls.add(Rect(0f, height - tileSize, width, height)) // Bottom
    walls.add(Rect(0f, 0f, tileSize, height)) // Left
    walls.add(Rect(width - tileSize, 0f, width, height)) // Right

    // 2. Interior Dividers & Zones
    // Horizontal barrier dividing North (Catwalk/Conduits) from Central
    walls.add(Rect(tileSize, 700f, 750f, 760f))
    walls.add(Rect(1050f, 700f, 1600f, 760f))
    walls.add(Rect(1850f, 700f, width - tileSize, 760f))

    // Vertical barrier dividing Scrapper Yard from Central Sluice
    walls.add(Rect(750f, 700f, 810f, 1500f))

    // Barrier between Central Sluice and Alchemical Sanctum
    walls.add(Rect(1600f, tileSize, 1660f, 1100f))
    walls.add(Rect(1600f, 1280f, 1660f, 1500f))

    // Wall protecting the Grand Boiler Hall (South entrance)
    walls.add(Rect(tileSize, 1500f, 1150f, 1560f))
    // Siphon Gate gap at 1150..1350
    walls.add(Rect(1350f, 1500f, width - tileSize, 1560f))

    // Pillar obstacles inside the Boiler Hall
    walls.add(Rect(1100f, 1750f, 1220f, 1870f))
    walls.add(Rect(1550f, 1750f, 1670f, 1870f))
    walls.add(Rect(1100f, 2050f, 1220f, 2170f))
    walls.add(Rect(1550f, 2050f, 1670f, 2170f))

    // Machinery blocks in the Foundry
    walls.add(Rect(300f, 300f, 440f, 440f))
    walls.add(Rect(350f, 950f, 550f, 1050f))
    walls.add(Rect(400f, 1200f, 520f, 1380f))
    walls.add(Rect(1100f, 350f, 1350f, 430f))

    // 3. Steam Hazards (Scalding jets blocking passageways)
    // North Conduit jet (blocks passage to central corridor unless Valve 1 is turned)
    hazards.add(
      SteamHazard(
        id = "hazard_north_conduit",
        pos = Vector2(750f, 710f),
        length = 300f,
        direction = Direction.RIGHT,
        isActive = true
      )
    )

    // Boiler Hall side-vent jets (pulse periodically during combat)
    hazards.add(
      SteamHazard(
        id = "hazard_boiler_west",
        pos = Vector2(980f, 1700f),
        length = 200f,
        direction = Direction.DOWN,
        isActive = false,
        isPulsing = true
      )
    )

    hazards.add(
      SteamHazard(
        id = "hazard_boiler_east",
        pos = Vector2(1750f, 1700f),
        length = 200f,
        direction = Direction.DOWN,
        isActive = false,
        isPulsing = true
      )
    )

    // 4. Interactive Steam Conduit Valves
    // Valve 1: North Conduit Wheel -> Deactivates north hazard jet & opens catwalk bypass
    valves.add(
      SteamConduitValve(
        id = "valve_north_pressure",
        pos = Vector2(620f, 220f),
        isTurned = false,
        controlledHazardId = "hazard_north_conduit",
        controlledDoorId = "door_catwalk",
        label = "Primary North Boiler Wheel"
      )
    )

    // Valve 2: Hydraulic Siphon Wheel -> Lowers the massive Bulkhead Gate into the Boiler Hall
    valves.add(
      SteamConduitValve(
        id = "valve_siphon_sluice",
        pos = Vector2(1480f, 1020f),
        isTurned = false,
        controlledHazardId = "",
        controlledDoorId = "door_boiler_hall",
        label = "Hydraulic Siphon Wheel"
      )
    )

    // Valve 3: Boiler Core Relief Valve (inside Boss arena) -> Venting cools Overseer Malakor's shield
    valves.add(
      SteamConduitValve(
        id = "valve_boiler_relief",
        pos = Vector2(1380f, 1680f),
        isTurned = false,
        controlledHazardId = "",
        controlledDoorId = "",
        label = "Reactor Relief Cock"
      )
    )

    // 5. Bulkhead Doors
    // Door 1: Catwalk gate
    doors.add(
      BulkheadDoor(
        id = "door_catwalk",
        pos = Vector2(1600f, 1100f),
        width = 60f,
        height = 180f,
        isOpen = false,
        label = "Sanctum Air-Lock"
      )
    )

    // Door 2: Grand Siphon Bulkhead (blocks entrance to Overseer Malakor)
    doors.add(
      BulkheadDoor(
        id = "door_boiler_hall",
        pos = Vector2(1150f, 1500f),
        width = 200f,
        height = 60f,
        isOpen = false,
        label = "Grand Siphon Bulkhead"
      )
    )

    // Door 3: Core Breach Vault Door (Requires Master Siphon Key from boss)
    doors.add(
      BulkheadDoor(
        id = "door_core_vault",
        pos = Vector2(1300f, 2260f),
        width = 180f,
        height = 60f,
        isOpen = false,
        requiresKey = true,
        label = "Reactor Core Vault Door"
      )
    )

    // 6. Checkpoints
    checkpoints.add(Vector2(250f, 250f)) // Spawn / Catwalk Overlook
    checkpoints.add(Vector2(1850f, 500f)) // Alchemical Sanctum Checkpoint
    checkpoints.add(Vector2(1250f, 1420f)) // Pre-Boss Sluice Checkpoint

    // 7. Atmospheric Lore Scrolls
    loreEntries.add(
      LoreEntry(
        id = "lore_catwalk",
        title = "Warden Log #44: The Initial Tremor",
        author = "Arch-Warden Vane",
        excerpt = "The subterranean pressure has climbed past 400 atmospheres. The alchemical regulator needles snapped an hour ago. If the lower bypass valves are not turned manually, the steam manifolds will rupture the entire Upper Spire."
      )
    )
    loreEntries.add(
      LoreEntry(
        id = "lore_skitterers",
        title = "Maintenance Memo: Clockwork Vermin",
        author = "Master Machinist Orin",
        excerpt = "Do not venture into Scrapper's Alley unarmored. The clockwork maintenance spiders have been corrupted by the green catalytic leaks. They mistake flesh for foreign slag."
      )
    )
    loreEntries.add(
      LoreEntry(
        id = "lore_malakor",
        title = "Decree of Overseer Malakor",
        author = "Overseer Malakor",
        excerpt = "I will not shut down the core. The Great Alchemical Reactor is not failing—it is awakening! The slag singing through the conduits is the divine blood of industrial immortality!"
      )
    )
  }

  fun isCollidingWithWalls(pos: Vector2, radius: Float): Boolean {
    val playerRect = Rect(pos.x - radius, pos.y - radius, pos.x + radius, pos.y + radius)
    for (wall in walls) {
      if (wall.overlaps(playerRect)) return true
    }
    for (door in doors) {
      if (!door.isOpen) {
        val doorRect = Rect(door.pos.x, door.pos.y, door.pos.x + door.width, door.pos.y + door.height)
        if (doorRect.overlaps(playerRect)) return true
      }
    }
    return false
  }

  fun getIntersectingHazard(pos: Vector2, radius: Float): SteamHazard? {
    val pRect = Rect(pos.x - radius, pos.y - radius, pos.x + radius, pos.y + radius)
    for (hazard in hazards) {
      if (hazard.isActive && hazard.frozenTimer <= 0f) {
        val hazardRect = when (hazard.direction) {
          Direction.RIGHT -> Rect(hazard.pos.x, hazard.pos.y - 18f, hazard.pos.x + hazard.length, hazard.pos.y + 18f)
          Direction.LEFT -> Rect(hazard.pos.x - hazard.length, hazard.pos.y - 18f, hazard.pos.x, hazard.pos.y + 18f)
          Direction.DOWN -> Rect(hazard.pos.x - 18f, hazard.pos.y, hazard.pos.x + 18f, hazard.pos.y + hazard.length)
          Direction.UP -> Rect(hazard.pos.x - 18f, hazard.pos.y - hazard.length, hazard.pos.x + 18f, hazard.pos.y)
        }
        if (hazardRect.overlaps(pRect)) return hazard
      }
    }
    return null
  }
}
