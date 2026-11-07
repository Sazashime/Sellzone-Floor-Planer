package com.example.data.model

import kotlin.math.ceil

enum class EdgeCoverageMode(val labelBg: String) {
    ENTRANCE_ONLY("Само вход на гараж (предна страна)"),
    FULL_PERIMETER("Пълен периметър (4-те страни)"),
    NONE("Без кантове"),
    CUSTOM("Ръчно зададени бройки")
}

data class GaragePlan(
    val widthM: Double = 10.0,
    val lengthM: Double = 9.2,
    val tileSizeMm: Int = 400,
    val grid: Map<Int, String> = emptyMap(), // key = row * cols + col, value = sku
    val wastePercentage: Int = 5,
    val edgeMode: EdgeCoverageMode = EdgeCoverageMode.ENTRANCE_ONLY,
    val selectedEdgeSku: String = "DUa587",
    val selectedCornerSku: String = "DUa589",
    val selectedLedSku: String = "DUa631",
    val manualEdgeQty: Int? = null,
    val manualCornerQty: Int? = null,
    val manualLedQty: Int? = null,
    val includeLedChannel: Boolean = false
) {
    val cols: Int
        get() = maxOf(1, ceil((widthM * 1000.0) / tileSizeMm).toInt())

    val rows: Int
        get() = maxOf(1, ceil((lengthM * 1000.0) / tileSizeMm).toInt())

    val totalCells: Int
        get() = cols * rows

    val garageAreaM2: Double
        get() = (widthM * lengthM * 100).toLong() / 100.0

    val tileAreaM2: Double
        get() = (tileSizeMm / 1000.0) * (tileSizeMm / 1000.0)

    val perimeterM: Double
        get() = 2 * (widthM + lengthM)

    fun getCellSku(col: Int, row: Int): String? {
        if (col !in 0 until cols || row !in 0 until rows) return null
        return grid[row * cols + col]
    }

    fun setCellSku(col: Int, row: Int, sku: String?): GaragePlan {
        if (col !in 0 until cols || row !in 0 until rows) return this
        val key = row * cols + col
        val newGrid = grid.toMutableMap()
        if (sku == null) {
            newGrid.remove(key)
        } else {
            newGrid[key] = sku
        }
        return copy(grid = newGrid)
    }

    fun batchSetCells(cellUpdates: Map<Int, String?>): GaragePlan {
        val newGrid = grid.toMutableMap()
        for ((key, sku) in cellUpdates) {
            val r = key / cols
            val c = key % cols
            if (c in 0 until cols && r in 0 until rows) {
                if (sku == null) {
                    newGrid.remove(key)
                } else {
                    newGrid[key] = sku
                }
            }
        }
        return copy(grid = newGrid)
    }

    fun clearGrid(): GaragePlan {
        return copy(grid = emptyMap())
    }
}
