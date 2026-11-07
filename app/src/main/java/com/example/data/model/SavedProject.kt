package com.example.data.model

data class SavedProject(
    val id: String,
    val name: String,
    val updatedAt: Long,
    val widthM: Double,
    val lengthM: Double,
    val tileSizeMm: Int,
    val wastePercentage: Int,
    val edgeMode: String,
    val selectedEdgeSku: String,
    val selectedCornerSku: String,
    val selectedLedSku: String,
    val includeLedChannel: Boolean,
    val manualEdgeQty: Int? = null,
    val manualCornerQty: Int? = null,
    val manualLedQty: Int? = null,
    val serializedGrid: String // Format: "cellIndex:SKU;cellIndex:SKU"
)
