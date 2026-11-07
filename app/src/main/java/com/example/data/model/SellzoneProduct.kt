package com.example.data.model

data class SellzoneProduct(
    val sku: String,
    val name: String,
    val priceEur: Double,
    val priceBgn: Double = (priceEur * 1.95583 * 100).toLong() / 100.0,
    val oldPriceEur: Double? = null,
    val imageUrl: String,
    val productUrl: String,
    val widthMm: Int,
    val heightMm: Int,
    val thicknessMm: Int = 18,
    val coverageM2: Double,
    val piecesPerM2: Double,
    val type: ProductType,
    val colorHex: Long,
    val colorName: String,
    val patternName: String = "Стандартна мрежа",
    val isVerified: Boolean = true,
    val description: String = ""
) {
    val isFlooringTile: Boolean
        get() = type == ProductType.STANDARD_TILE ||
                type == ProductType.DESIGN_3D_TILE ||
                type == ProductType.PRO_TILE

    val isAccessory: Boolean
        get() = type == ProductType.EDGE_PROFILE ||
                type == ProductType.CORNER ||
                type == ProductType.LED_CHANNEL

    val isDecking: Boolean
        get() = type == ProductType.DECKING
}
