package com.example.data.model

data class ShoppingItem(
    val product: SellzoneProduct,
    val placedQuantity: Int,
    val reserveQuantity: Int,
    val totalQuantity: Int,
    val unitPriceEur: Double,
    val unitPriceBgn: Double,
    val subtotalEur: Double,
    val subtotalBgn: Double,
    val isAccessory: Boolean = false
)

data class ProjectCalculation(
    val totalAreaM2: Double,
    val coveredCellsCount: Int,
    val uncoveredCellsCount: Int,
    val coveredAreaM2: Double,
    val uncoveredAreaM2: Double,
    val wastePercentage: Int,
    val items: List<ShoppingItem>,
    val tilesByColor: Map<String, Int>,
    val tilesBySku: Map<String, Int>,
    val totalTilesPlaced: Int,
    val totalReserveTiles: Int,
    val totalTilesToOrder: Int,
    val materialCostEur: Double,
    val materialCostBgn: Double,
    val accessoriesCostEur: Double,
    val accessoriesCostBgn: Double,
    val grandTotalEur: Double,
    val grandTotalBgn: Double,
    val edgePiecesRecommended: Int,
    val cornerPiecesRecommended: Int,
    val ledPiecesRecommended: Int
)
