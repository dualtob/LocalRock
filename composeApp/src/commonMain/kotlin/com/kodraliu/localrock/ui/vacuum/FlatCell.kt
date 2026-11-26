package com.kodraliu.localrock.ui.vacuum

import com.kodraliu.localrock.shared.vacuum.map.ParsedMap

/** What one cell of the flat map is painted as. */
internal sealed interface FlatCell {
    /** The floor of room [id], in its colour, texture and carpet; with Walls hidden, a Wall's cell takes its room's. */
    data class Room(val id: Int) : FlatCell

    /** Plain floor, on a map without room ids. */
    data object Floor : FlatCell

    /** A Wall, with Walls shown, on a map that draws no floor-plan shape. */
    data object Wall : FlatCell

    /** Not painted from the cells: empty grid, an erased cell, a hidden Wall with no room, or a room the floor plan paints. */
    data object None : FlatCell
}

/**
 * What the cell at [x], [y] (grid row, not flipped) of the flat map is painted as.
 * [floorPlanRooms] are the rooms painted from their floor-plan shape, so their cells (Walls too) are left to it.
 * With [showWalls], a Wall's cell is a [FlatCell.Wall]; without, it is painted as the room it belongs to.
 * A map that draws any floor-plan shape gets no Wall blocks either way: its floor-plan lines are the Walls.
 * One whose shapes are all erased, or drawn without its floor plan, draws no lines and so gets them.
 */
internal fun flatCell(
    map: ParsedMap,
    x: Int,
    y: Int,
    showWalls: Boolean,
    floorPlanRooms: Set<Int> = emptySet(),
): FlatCell {
    val i = y * map.width + x
    if (map.erasedCells?.get(i) == true) return FlatCell.None
    val gv = map.grid[i].toInt() and 0xff
    if (gv != 127 && gv != 128) return FlatCell.None
    val raw = map.originalGrid
    if (gv == 127) {
        val roomId = raw?.let { flatWallRoom(map.grid, it, map.width, map.height, x, y) }
        return when {
            roomId != null && roomId in floorPlanRooms -> FlatCell.None
            showWalls && floorPlanRooms.isEmpty() -> FlatCell.Wall
            roomId != null -> FlatCell.Room(roomId)
            else -> FlatCell.None
        }
    }
    if (raw == null) return FlatCell.Floor
    val roomId = (raw[i].toInt() ushr 3) and 0x1f
    return if (roomId in floorPlanRooms) FlatCell.None else FlatCell.Room(roomId)
}
