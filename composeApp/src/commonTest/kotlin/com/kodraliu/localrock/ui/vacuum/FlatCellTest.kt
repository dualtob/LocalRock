package com.kodraliu.localrock.ui.vacuum

import com.kodraliu.localrock.shared.vacuum.map.FloorPlanPoint
import com.kodraliu.localrock.shared.vacuum.map.FloorPlanZone
import com.kodraliu.localrock.shared.vacuum.map.MapFormat
import com.kodraliu.localrock.shared.vacuum.map.MapZone
import com.kodraliu.localrock.shared.vacuum.map.ParsedMap
import com.kodraliu.localrock.shared.vacuum.map.ZoneKind
import kotlin.test.Test
import kotlin.test.assertEquals

class FlatCellTest {
    // '.' is empty, a digit is a floor cell of that room, 'w' is a Wall's cell, with no room id.
    // Rows are in grid order (row 0 first); [roomIds] false leaves the map without room ids.
    // [zoneLines] are its floor plan (block 55): give one to make a map with a floor plan.
    private fun map(
        vararg rows: String,
        roomIds: Boolean = true,
        eraserZones: List<MapZone> = emptyList(),
        zoneLines: List<FloorPlanZone> = emptyList(),
    ): ParsedMap {
        val h = rows.size
        val w = rows[0].length
        val grid = ByteArray(w * h)
        val raw = ByteArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val c = rows[y][x]
            val i = y * w + x
            when {
                c.isDigit() -> { grid[i] = 128.toByte(); raw[i] = (((c - '0') shl 3) or 7).toByte() }
                c == 'w' -> { grid[i] = 127.toByte(); raw[i] = 1 }
            }
        }
        return ParsedMap(
            width = w, height = h, grid = grid, rooms = emptyList(), resolution = 0.05f, format = MapFormat.LEGACY,
            originalGrid = if (roomIds) raw else null, eraserZones = eraserZones, zoneLines = zoneLines,
        )
    }

    // A floor-plan shape for room 1 (the maps below need not hold that room).
    private val floorPlan = listOf(FloorPlanZone(1, listOf(FloorPlanPoint(0, 0, 1), FloorPlanPoint(1, 0, 1), FloorPlanPoint(1, 1, 1), FloorPlanPoint(0, 1, 1))))

    @Test
    fun a_wall_is_painted_as_its_room() {
        assertEquals(FlatCell.Room(2), flatCell(map("2w"), 1, 0, showWalls = false))
    }

    @Test
    fun a_wall_with_no_room_is_not_painted() {
        assertEquals(FlatCell.None, flatCell(map(".w."), 1, 0, showWalls = false))
    }

    @Test
    fun a_wall_on_a_map_without_room_ids_is_not_painted() {
        assertEquals(FlatCell.None, flatCell(map("2w", roomIds = false), 1, 0, showWalls = false))
    }

    @Test
    fun a_floor_cell_is_its_room_or_plain_floor_without_room_ids() {
        assertEquals(FlatCell.Room(3), flatCell(map("3"), 0, 0, showWalls = false))
        assertEquals(FlatCell.Floor, flatCell(map("3", roomIds = false), 0, 0, showWalls = false))
    }

    @Test
    fun an_empty_cell_is_not_painted() {
        assertEquals(FlatCell.None, flatCell(map("1."), 1, 0, showWalls = false))
        assertEquals(FlatCell.None, flatCell(map("1.", roomIds = false), 1, 0, showWalls = false))
    }

    @Test
    fun an_erased_cell_is_not_painted() {
        // The eraser covers the first two cells (0 to 100 mm); the third is left.
        val erased = listOf(MapZone.rect(0, 0, 100, 50, ZoneKind.ERASER))
        val m = map("1w1", eraserZones = erased)
        assertEquals(FlatCell.None, flatCell(m, 0, 0, showWalls = false))
        assertEquals(FlatCell.None, flatCell(m, 1, 0, showWalls = false))
        assertEquals(FlatCell.Room(1), flatCell(m, 2, 0, showWalls = false))
    }

    @Test
    fun a_room_with_a_floor_plan_shape_is_left_to_the_floor_plan() {
        val m = map("1w2")
        assertEquals(FlatCell.None, flatCell(m, 0, 0, showWalls = false, floorPlanRooms = setOf(1)))
        assertEquals(FlatCell.None, flatCell(m, 1, 0, showWalls = false, floorPlanRooms = setOf(1)))
        assertEquals(FlatCell.Room(2), flatCell(m, 2, 0, showWalls = false, floorPlanRooms = setOf(1)))
    }

    @Test
    fun a_wall_is_drawn_when_walls_are_shown() {
        assertEquals(FlatCell.Wall, flatCell(map("2w"), 1, 0, showWalls = true))
        assertEquals(FlatCell.Wall, flatCell(map(".w."), 1, 0, showWalls = true))
    }

    @Test
    fun a_wall_on_a_map_without_room_ids_is_drawn_when_walls_are_shown() {
        assertEquals(FlatCell.Wall, flatCell(map("2w", roomIds = false), 1, 0, showWalls = true))
    }

    @Test
    fun floor_cells_do_not_change_when_walls_are_shown() {
        assertEquals(FlatCell.Room(3), flatCell(map("3"), 0, 0, showWalls = true))
        assertEquals(FlatCell.Floor, flatCell(map("3", roomIds = false), 0, 0, showWalls = true))
    }

    @Test
    fun empty_and_erased_cells_are_not_painted_when_walls_are_shown() {
        val erased = listOf(MapZone.rect(0, 0, 100, 50, ZoneKind.ERASER))
        val m = map("1w1.", eraserZones = erased)
        assertEquals(FlatCell.None, flatCell(m, 0, 0, showWalls = true))
        assertEquals(FlatCell.None, flatCell(m, 1, 0, showWalls = true))
        assertEquals(FlatCell.None, flatCell(m, 3, 0, showWalls = true))
        assertEquals(FlatCell.None, flatCell(map("1.", roomIds = false), 1, 0, showWalls = true))
    }

    @Test
    fun a_room_with_a_floor_plan_shape_gets_no_wall_when_walls_are_shown() {
        val m = map("1w2")
        assertEquals(FlatCell.None, flatCell(m, 0, 0, showWalls = true, floorPlanRooms = setOf(1)))
        assertEquals(FlatCell.None, flatCell(m, 1, 0, showWalls = true, floorPlanRooms = setOf(1)))
        assertEquals(FlatCell.Room(2), flatCell(m, 2, 0, showWalls = true, floorPlanRooms = setOf(1)))
    }

    @Test
    fun a_map_with_a_floor_plan_gets_no_walls_when_walls_are_shown() {
        // Its floor-plan lines are the Walls: a Wall keeps its room, or is not painted.
        assertEquals(FlatCell.Room(2), flatCell(map("2w2", zoneLines = floorPlan), 1, 0, showWalls = true, floorPlanRooms = setOf(1)))
        assertEquals(FlatCell.None, flatCell(map(".w.", zoneLines = floorPlan), 1, 0, showWalls = true, floorPlanRooms = setOf(1)))
    }

    @Test
    fun a_map_with_a_floor_plan_gets_no_walls_when_walls_are_hidden() {
        assertEquals(FlatCell.Room(2), flatCell(map("2w2", zoneLines = floorPlan), 1, 0, showWalls = false, floorPlanRooms = setOf(1)))
        assertEquals(FlatCell.None, flatCell(map(".w.", zoneLines = floorPlan), 1, 0, showWalls = false, floorPlanRooms = setOf(1)))
    }

    @Test
    fun a_map_whose_floor_plan_rooms_are_all_erased_gets_walls() {
        // Its one floor-plan room is erased, so no floor-plan shape or line is drawn and the Walls must show.
        val m = map("1w2", eraserZones = listOf(MapZone.rect(0, 0, 50, 50, ZoneKind.ERASER)), zoneLines = floorPlan)
        val floorPlanRooms = m.floorPlanShapes().mapTo(HashSet()) { it.segment }
        assertEquals(FlatCell.Wall, flatCell(m, 1, 0, showWalls = true, floorPlanRooms = floorPlanRooms))
    }

    @Test
    fun a_map_with_a_floor_plan_shown_as_the_scan_gets_its_walls_and_rooms_from_the_cells() {
        // The Scan leaves the Floor Plan out, so no room is left to it: the Walls show and each room keeps its own cells.
        val m = map("1w2", zoneLines = floorPlan)
        val floorPlanRooms = m.floorPlanShapes(floorPlan = false).mapTo(HashSet()) { it.segment }
        assertEquals(FlatCell.Wall, flatCell(m, 1, 0, showWalls = true, floorPlanRooms = floorPlanRooms))
        assertEquals(FlatCell.Room(1), flatCell(m, 0, 0, showWalls = true, floorPlanRooms = floorPlanRooms))
    }
}
