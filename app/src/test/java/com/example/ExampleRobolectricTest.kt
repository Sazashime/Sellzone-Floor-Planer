package com.example

import com.example.data.calculator.FloorCalculationEngine
import com.example.data.model.GaragePlan
import com.example.data.repository.ProductCatalogRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRobolectricTest {

    @Test
    fun testAppCalculationsAndCatalog() {
        val repo = ProductCatalogRepository()
        val all = repo.getAllProducts()
        assertEquals(39, all.size)

        val plan = GaragePlan(widthM = 10.0, lengthM = 9.2, tileSizeMm = 400)
        assertEquals(25, plan.cols)
        assertEquals(23, plan.rows)
        assertEquals(575, plan.totalCells)
        assertEquals(92.0, plan.garageAreaM2, 0.001)

        val calculation = FloorCalculationEngine.calculate(plan, repo)
        assertEquals(92.0, calculation.totalAreaM2, 0.001)
        assertEquals(0, calculation.coveredCellsCount)
        assertEquals(575, calculation.uncoveredCellsCount)
    }
}
