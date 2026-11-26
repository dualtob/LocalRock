package com.kodraliu.localrock.ui.vacuum

import com.kodraliu.localrock.shared.vacuum.map.MapFormat
import com.kodraliu.localrock.shared.vacuum.map.MapZone
import com.kodraliu.localrock.shared.vacuum.map.ParsedMap
import com.kodraliu.localrock.shared.vacuum.map.ZoneKind
import kotlin.test.Test
import kotlin.test.assertEquals

class FlatCellTest {
    // '.' is empty, a digit is a floor cell of that room, 'w' is a Wall cell with no room id.
    // Rows are in grid order (row 0 first); [roomIds] false leaves the map without room ids.
    private fun map(vararg rows: String, roomIds: Boolean = true, eraserZones: List<MapZone> = emptyList()): ParsedMap {
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
            originalGrid = if (roomIds) raw else null, eraserZones = eraserZones,
        )
    }

    @Test
    fun a_wall_cell_is_painted_as_its_room() {
        assertEquals(FlatCell.Room(2), flatCell(map("2w"), 1, 0, showWalls = false))
    }

    @Test
    fun a_wall_cell_with_no_room_is_not_painted() {
        assertEquals(FlatCell.None, flatCell(map(".w."), 1, 0, showWalls = false))
    }

    @Test
    fun a_wall_cell_on_a_map_without_room_ids_is_not_painted() {
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
}
