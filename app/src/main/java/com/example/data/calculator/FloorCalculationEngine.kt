package com.example.data.calculator

import com.example.data.model.EdgeCoverageMode
import com.example.data.model.GaragePlan
import com.example.data.model.ProjectCalculation
import com.example.data.model.ShoppingItem
import com.example.data.repository.ProductCatalogRepository
import kotlin.math.ceil

object FloorCalculationEngine {

    fun calculate(plan: GaragePlan, repository: ProductCatalogRepository): ProjectCalculation {
        val totalAreaM2 = plan.garageAreaM2
        val tileAreaM2 = plan.tileAreaM2
        val totalCells = plan.totalCells

        // 1. Count placed tiles by SKU
        val placedTilesBySku = mutableMapOf<String, Int>()
        for ((_, sku) in plan.grid) {
            placedTilesBySku[sku] = (placedTilesBySku[sku] ?: 0) + 1
        }

        val totalPlacedTiles = placedTilesBySku.values.sum()
        val uncoveredCells = maxOf(0, totalCells - totalPlacedTiles)
        val coveredAreaM2 = (totalPlacedTiles * tileAreaM2 * 100).toLong() / 100.0
        val uncoveredAreaM2 = maxOf(0.0, ((totalAreaM2 - coveredAreaM2) * 100).toLong() / 100.0)

        // 2. Count by Color
        val tilesByColor = mutableMapOf<String, Int>()
        for ((sku, count) in placedTilesBySku) {
            val prod = repository.getProductBySku(sku)
            val colorName = prod?.colorName ?: "Неизвестен"
            tilesByColor[colorName] = (tilesByColor[colorName] ?: 0) + count
        }

        // 3. Build Shopping Items for Tiles
        val shoppingItems = mutableListOf<ShoppingItem>()
        var totalReserveTiles = 0
        var totalTilesToOrder = 0
        var materialCostEur = 0.0
        var materialCostBgn = 0.0

        for ((sku, placedCount) in placedTilesBySku) {
            val product = repository.getProductBySku(sku) ?: continue
            val reserveCount = if (plan.wastePercentage > 0) {
                ceil(placedCount * (plan.wastePercentage / 100.0)).toInt()
            } else {
                0
            }
            val totalQty = placedCount + reserveCount
            val subtotalEur = (totalQty * product.priceEur * 100).toLong() / 100.0
            val subtotalBgn = (totalQty * product.priceBgn * 100).toLong() / 100.0

            totalReserveTiles += reserveCount
            totalTilesToOrder += totalQty
            materialCostEur += subtotalEur
            materialCostBgn += subtotalBgn

            shoppingItems.add(
                ShoppingItem(
                    product = product,
                    placedQuantity = placedCount,
                    reserveQuantity = reserveCount,
                    totalQuantity = totalQty,
                    unitPriceEur = product.priceEur,
                    unitPriceBgn = product.priceBgn,
                    subtotalEur = subtotalEur,
                    subtotalBgn = subtotalBgn,
                    isAccessory = false
                )
            )
        }

        // 4. Calculate Recommended Accessories
        val edgePiecesRecommended: Int
        val cornerPiecesRecommended: Int

        when (plan.edgeMode) {
            EdgeCoverageMode.ENTRANCE_ONLY -> {
                // Front width divided by tile size
                edgePiecesRecommended = plan.manualEdgeQty ?: plan.cols
                cornerPiecesRecommended = plan.manualCornerQty ?: 2
            }
            EdgeCoverageMode.FULL_PERIMETER -> {
                // Total perimeter minus corner sections
                val perimeterCount = ceil((plan.perimeterM * 1000.0) / plan.tileSizeMm).toInt()
                edgePiecesRecommended = plan.manualEdgeQty ?: maxOf(0, perimeterCount - 4)
                cornerPiecesRecommended = plan.manualCornerQty ?: 4
            }
            EdgeCoverageMode.NONE -> {
                edgePiecesRecommended = 0
                cornerPiecesRecommended = 0
            }
            EdgeCoverageMode.CUSTOM -> {
                edgePiecesRecommended = plan.manualEdgeQty ?: 0
                cornerPiecesRecommended = plan.manualCornerQty ?: 0
            }
        }

        val ledPiecesRecommended = if (plan.includeLedChannel) {
            plan.manualLedQty ?: plan.cols
        } else {
            0
        }

        // 5. Add Accessories to Shopping List
        var accessoriesCostEur = 0.0
        var accessoriesCostBgn = 0.0

        if (edgePiecesRecommended > 0) {
            val edgeProduct = repository.getProductBySku(plan.selectedEdgeSku)
                ?: repository.getProductBySku("DUa587")
            if (edgeProduct != null) {
                val subEur = (edgePiecesRecommended * edgeProduct.priceEur * 100).toLong() / 100.0
                val subBgn = (edgePiecesRecommended * edgeProduct.priceBgn * 100).toLong() / 100.0
                accessoriesCostEur += subEur
                accessoriesCostBgn += subBgn
                shoppingItems.add(
                    ShoppingItem(
                        product = edgeProduct,
                        placedQuantity = edgePiecesRecommended,
                        reserveQuantity = 0,
                        totalQuantity = edgePiecesRecommended,
                        unitPriceEur = edgeProduct.priceEur,
                        unitPriceBgn = edgeProduct.priceBgn,
                        subtotalEur = subEur,
                        subtotalBgn = subBgn,
                        isAccessory = true
                    )
                )
            }
        }

        if (cornerPiecesRecommended > 0) {
            val cornerProduct = repository.getProductBySku(plan.selectedCornerSku)
                ?: repository.getProductBySku("DUa589")
            if (cornerProduct != null) {
                val subEur = (cornerPiecesRecommended * cornerProduct.priceEur * 100).toLong() / 100.0
                val subBgn = (cornerPiecesRecommended * cornerProduct.priceBgn * 100).toLong() / 100.0
                accessoriesCostEur += subEur
                accessoriesCostBgn += subBgn
                shoppingItems.add(
                    ShoppingItem(
                        product = cornerProduct,
                        placedQuantity = cornerPiecesRecommended,
                        reserveQuantity = 0,
                        totalQuantity = cornerPiecesRecommended,
                        unitPriceEur = cornerProduct.priceEur,
                        unitPriceBgn = cornerProduct.priceBgn,
                        subtotalEur = subEur,
                        subtotalBgn = subBgn,
                        isAccessory = true
                    )
                )
            }
        }

        if (ledPiecesRecommended > 0) {
            val ledProduct = repository.getProductBySku(plan.selectedLedSku)
                ?: repository.getProductBySku("DUa631")
            if (ledProduct != null) {
                val subEur = (ledPiecesRecommended * ledProduct.priceEur * 100).toLong() / 100.0
                val subBgn = (ledPiecesRecommended * ledProduct.priceBgn * 100).toLong() / 100.0
                accessoriesCostEur += subEur
                accessoriesCostBgn += subBgn
                shoppingItems.add(
                    ShoppingItem(
                        product = ledProduct,
                        placedQuantity = ledPiecesRecommended,
                        reserveQuantity = 0,
                        totalQuantity = ledPiecesRecommended,
                        unitPriceEur = ledProduct.priceEur,
                        unitPriceBgn = ledProduct.priceBgn,
                        subtotalEur = subEur,
                        subtotalBgn = subBgn,
                        isAccessory = true
                    )
                )
            }
        }

        val grandTotalEur = ((materialCostEur + accessoriesCostEur) * 100).toLong() / 100.0
        val grandTotalBgn = ((materialCostBgn + accessoriesCostBgn) * 100).toLong() / 100.0

        return ProjectCalculation(
            totalAreaM2 = totalAreaM2,
            coveredCellsCount = totalPlacedTiles,
            uncoveredCellsCount = uncoveredCells,
            coveredAreaM2 = coveredAreaM2,
            uncoveredAreaM2 = uncoveredAreaM2,
            wastePercentage = plan.wastePercentage,
            items = shoppingItems,
            tilesByColor = tilesByColor,
            tilesBySku = placedTilesBySku,
            totalTilesPlaced = totalPlacedTiles,
            totalReserveTiles = totalReserveTiles,
            totalTilesToOrder = totalTilesToOrder,
            materialCostEur = materialCostEur,
            materialCostBgn = materialCostBgn,
            accessoriesCostEur = accessoriesCostEur,
            accessoriesCostBgn = accessoriesCostBgn,
            grandTotalEur = grandTotalEur,
            grandTotalBgn = grandTotalBgn,
            edgePiecesRecommended = edgePiecesRecommended,
            cornerPiecesRecommended = cornerPiecesRecommended,
            ledPiecesRecommended = ledPiecesRecommended
        )
    }
}
