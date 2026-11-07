package com.example

import com.example.data.calculator.FloorCalculationEngine
import com.example.data.model.EdgeCoverageMode
import com.example.data.model.GaragePlan
import com.example.data.repository.ProductCatalogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FloorCalculationEngineTest {

    private val repository = ProductCatalogRepository()

    @Test
    fun testStandardGarageCalculations_10x9_2m_400mm() {
        // Width: 10.0m, Length: 9.2m, 400mm tiles
        val plan = GaragePlan(
            widthM = 10.0,
            lengthM = 9.2,
            tileSizeMm = 400,
            wastePercentage = 5,
            edgeMode = EdgeCoverageMode.ENTRANCE_ONLY
        )

        assertEquals(25, plan.cols)
        assertEquals(23, plan.rows)
        assertEquals(575, plan.totalCells)
        assertEquals(92.0, plan.garageAreaM2, 0.001)
        assertEquals(38.4, plan.perimeterM, 0.001)

        // Fill all with DUa583 (2.96 €)
        val filledGrid = mutableMapOf<Int, String>()
        for (i in 0 until plan.totalCells) {
            filledGrid[i] = "DUa583"
        }
        val filledPlan = plan.copy(grid = filledGrid)

        val calculation = FloorCalculationEngine.calculate(filledPlan, repository)

        assertEquals(92.0, calculation.totalAreaM2, 0.001)
        assertEquals(575, calculation.coveredCellsCount)
        assertEquals(0, calculation.uncoveredCellsCount)
        assertEquals(92.0, calculation.coveredAreaM2, 0.001)

        // Waste: ceil(575 * 0.05) = ceil(28.75) = 29
        assertEquals(29, calculation.totalReserveTiles)
        // Total to order = 575 + 29 = 604
        assertEquals(604, calculation.totalTilesToOrder)

        // Material cost = 604 * 2.96 = 1787.84 €
        assertEquals(1787.84, calculation.materialCostEur, 0.01)

        // Entrance accessories: 25 straight edges + 2 corners
        assertEquals(25, calculation.edgePiecesRecommended)
        assertEquals(2, calculation.cornerPiecesRecommended)
    }

    @Test
    fun testProfessionalGarageCalculations_10x9_2m_500mm() {
        // Width: 10.0m, Length: 9.2m, 500mm tiles (DUa653)
        val plan = GaragePlan(
            widthM = 10.0,
            lengthM = 9.2,
            tileSizeMm = 500,
            wastePercentage = 5
        )

        assertEquals(20, plan.cols)
        assertEquals(19, plan.rows) // ceil(9200 / 500) = 19
        assertEquals(380, plan.totalCells)

        // DUa653 price = 13.80 €
        val proProduct = repository.getProductBySku("DUa653")
        assertNotNull(proProduct)
        assertEquals(13.80, proProduct!!.priceEur, 0.001)
        assertEquals(0.25, proProduct.coverageM2, 0.001)
        assertEquals(4.0, proProduct.piecesPerM2, 0.001)
    }

    @Test
    fun testMixedColorCalculations() {
        val plan = GaragePlan(
            widthM = 4.0,
            lengthM = 3.0,
            tileSizeMm = 400,
            wastePercentage = 5
        )
        // cols = 10, rows = 8, total = 80 cells
        val grid = mutableMapOf<Int, String>()
        for (i in 0 until 80) {
            grid[i] = if (i % 2 == 0) "DUa583" else "DUa584" // 40 Black, 40 Grey
        }

        val calculation = FloorCalculationEngine.calculate(plan.copy(grid = grid), repository)
        assertEquals(40, calculation.tilesBySku["DUa583"])
        assertEquals(40, calculation.tilesBySku["DUa584"])

        // Waste for each: ceil(40 * 0.05) = ceil(2.0) = 2
        // Total for each: 42
        assertEquals(4, calculation.totalReserveTiles)
        assertEquals(84, calculation.totalTilesToOrder)
    }

    @Test
    fun testCatalogSeparation() {
        val all = repository.getAllProducts()
        assertEquals(39, all.size)

        val tiles = repository.getFlooringTiles()
        // Decking products should be excluded from flooring tiles
        assertTrue(tiles.none { it.isDecking })

        val decking = repository.getDeckingProducts()
        assertEquals(4, decking.size)

        val accessories = repository.getAccessories()
        assertTrue(accessories.isNotEmpty())
        assertTrue(accessories.all { it.isAccessory })
    }
}
