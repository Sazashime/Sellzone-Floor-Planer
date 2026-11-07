package com.example.data.repository

import com.example.data.model.ProductType
import com.example.data.model.SellzoneProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val updatedCount: Int, val message: String) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

class ProductCatalogRepository {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _products = MutableStateFlow<List<SellzoneProduct>>(createInitialCatalog())
    val products: StateFlow<List<SellzoneProduct>> = _products.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    fun getAllProducts(): List<SellzoneProduct> = _products.value

    fun getFlooringTiles(): List<SellzoneProduct> {
        return _products.value.filter { it.isFlooringTile }
    }

    fun getAccessories(): List<SellzoneProduct> {
        return _products.value.filter { it.isAccessory }
    }

    fun getDeckingProducts(): List<SellzoneProduct> {
        return _products.value.filter { it.isDecking }
    }

    fun getProductBySku(sku: String): SellzoneProduct? {
        return _products.value.firstOrNull { it.sku.equals(sku, ignoreCase = true) }
    }

    fun getCompatibleEdges(): List<SellzoneProduct> {
        return _products.value.filter { it.type == ProductType.EDGE_PROFILE }
    }

    fun getCompatibleCorners(): List<SellzoneProduct> {
        return _products.value.filter { it.type == ProductType.CORNER }
    }

    fun getCompatibleLed(): List<SellzoneProduct> {
        return _products.value.filter { it.type == ProductType.LED_CHANNEL }
    }

    suspend fun refreshPricesFromSellzone(): Result<Int> = withContext(Dispatchers.IO) {
        _syncStatus.value = SyncStatus.Syncing
        try {
            val request = Request.Builder()
                .url("https://sellzone.bg/avtochasti-i-aksesoari/nastilki-za-garazhi?limit=100")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = "Грешка при връзка със Sellzone (HTTP ${response.code})"
                _syncStatus.value = SyncStatus.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val html = response.body?.string() ?: ""
            if (html.isEmpty()) {
                val err = "Получен е празен отговор от Sellzone"
                _syncStatus.value = SyncStatus.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val updatedPriceMap = parsePricesFromHtml(html)
            if (updatedPriceMap.isEmpty()) {
                val err = "Не са намерени продукти в HTML отговора"
                _syncStatus.value = SyncStatus.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            var updatedCount = 0
            val currentList = _products.value.toMutableList()
            for (i in currentList.indices) {
                val product = currentList[i]
                val newPrice = updatedPriceMap[product.sku.uppercase()]
                if (newPrice != null && newPrice > 0.0) {
                    if (newPrice != product.priceEur) {
                        updatedCount++
                    }
                    val updatedBgn = (newPrice * 1.95583 * 100).toLong() / 100.0
                    currentList[i] = product.copy(
                        priceEur = newPrice,
                        priceBgn = updatedBgn,
                        isVerified = true
                    )
                }
            }

            _products.value = currentList
            _lastSyncTimestamp.value = System.currentTimeMillis()
            val msg = "Цените са обновени успешно на живо от Sellzone.bg! ($updatedCount промени)"
            _syncStatus.value = SyncStatus.Success(updatedCount, msg)
            Result.success(updatedCount)
        } catch (e: Exception) {
            val err = "Неуспешна синхронизация (${e.localizedMessage ?: "Офлайн"}). Използва се надежден локален кеш."
            _syncStatus.value = SyncStatus.Error(err)
            Result.failure(e)
        }
    }

    private fun parsePricesFromHtml(html: String): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        val itemPattern = Pattern.compile(
            "<div class=\"product-layout[^\"]*\"[\\s\\S]*?class=\"custom-product-model\">([^<]+)<[\\s\\S]*?<p class=\"price\">([\\s\\S]*?)</p>",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = itemPattern.matcher(html)
        while (matcher.find()) {
            val skuRaw = matcher.group(1).trim().uppercase()
            val priceBlock = matcher.group(2)

            val priceNewMatcher = Pattern.compile("class=\"price-new\">([0-9.,]+)€", Pattern.CASE_INSENSITIVE).matcher(priceBlock)
            val regularMatcher = Pattern.compile("([0-9.,]+)€", Pattern.CASE_INSENSITIVE).matcher(priceBlock)

            val priceStr = if (priceNewMatcher.find()) {
                priceNewMatcher.group(1)
            } else if (regularMatcher.find()) {
                regularMatcher.group(1)
            } else {
                null
            }

            val priceVal = priceStr?.replace(",", ".")?.toDoubleOrNull()
            if (priceVal != null && skuRaw.isNotEmpty()) {
                map[skuRaw] = priceVal
            }
        }
        return map
    }

    private fun createInitialCatalog(): List<SellzoneProduct> {
        return listOf(
            // --- Standard 400x400 mm PVC Garage Tiles ---
            SellzoneProduct(
                sku = "DUa583",
                name = "PVC модулна подова настилка за гараж – черна квадратна плочка DUa583",
                priceEur = 2.96,
                oldPriceEur = 5.99,
                imageUrl = "https://sellzone.bg/image/cache/DUa583/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-modulna-podova-nastilka-za-garazh-cherna-kvadratna-plochka-dua583",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.STANDARD_TILE,
                colorHex = 0xFF222428,
                colorName = "Черен",
                patternName = "Стандартна мрежа",
                description = "Високоякостен PVC материал, устойчив на масла, химикали и тежест от автомобили."
            ),
            SellzoneProduct(
                sku = "DUa584",
                name = "PVC модулна подова настилка за гараж – сива квадратна плочка DUa584",
                priceEur = 2.96,
                oldPriceEur = 5.99,
                imageUrl = "https://sellzone.bg/image/cache/DUa584/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-modulna-podova-nastilka-za-garazh-siva-kvadratna-plochka-dua584",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.STANDARD_TILE,
                colorHex = 0xFF64748B,
                colorName = "Сив",
                patternName = "Стандартна мрежа"
            ),
            SellzoneProduct(
                sku = "DUa585",
                name = "PVC модулна подова настилка за гараж – жълта квадратна плочка DUa585",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa585/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-modulna-podova-nastilka-za-garazh-zhalta-kvadratna-plochka-dua585",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.STANDARD_TILE,
                colorHex = 0xFFEAB308,
                colorName = "Жълт",
                patternName = "Стандартна мрежа"
            ),
            SellzoneProduct(
                sku = "DUa586",
                name = "PVC модулна подова настилка за гараж – червена квадратна плочка DUa586",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa586/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-modulna-podova-nastilka-za-garazh-chervena-kvadratna-plochka-dua586",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.STANDARD_TILE,
                colorHex = 0xFFDC2626,
                colorName = "Червен",
                patternName = "Стандартна мрежа"
            ),
            SellzoneProduct(
                sku = "DUa590",
                name = "PVC модулна подова настилка за гараж и сервиз – светло сива - DUa590",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa590/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-modulna-podova-nastilka-za-garazh-i-serviz-svetlo-siva-dua590",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.STANDARD_TILE,
                colorHex = 0xFF94A3B8,
                colorName = "Светло сив",
                patternName = "Стандартна мрежа"
            ),
            SellzoneProduct(
                sku = "DUa591",
                name = "PVC модулна подова настилка за гараж – бяла квадратна плочка DUa591",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa591/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-modulna-podova-nastilka-za-garazh-byala-kvadratna-plochka-dua591",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.STANDARD_TILE,
                colorHex = 0xFFF8FAFC,
                colorName = "Бял",
                patternName = "Стандартна мрежа"
            ),
            SellzoneProduct(
                sku = "DUa592",
                name = "PVC модулна подова настилка за гараж и работилница – оранжева DUa592",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa592/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-modulna-podova-nastilka-za-garazh-i-rabotilnitsa-oranzheva-dua592",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.STANDARD_TILE,
                colorHex = 0xFFF97316,
                colorName = "Оранжев",
                patternName = "Стандартна мрежа"
            ),
            SellzoneProduct(
                sku = "DUa593",
                name = "PVC модулна подова настилка за гараж – светло зелена - DUa593",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa593/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-modulna-podova-nastilka-za-garazh-svetlo-zelena-dua593",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.STANDARD_TILE,
                colorHex = 0xFF4ADE80,
                colorName = "Светло зелен",
                patternName = "Стандартна мрежа"
            ),
            SellzoneProduct(
                sku = "DUa594",
                name = "PVC модулна подова настилка за гараж – ярко зелена - DUa594",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa594/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-modulna-podova-nastilka-za-garazh-yarko-zelena-dua594",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.STANDARD_TILE,
                colorHex = 0xFF10B981,
                colorName = "Ярко зелен",
                patternName = "Стандартна мрежа"
            ),
            SellzoneProduct(
                sku = "DUa595",
                name = "PVC модулна подова настилка за гараж шоурум – тъмно зелена - DUa595",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa595/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-modulna-podova-nastilka-za-garazh-shourum-tamno-zelena-dua595",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.STANDARD_TILE,
                colorHex = 0xFF166534,
                colorName = "Тъмно зелен",
                patternName = "Стандартна мрежа"
            ),
            SellzoneProduct(
                sku = "DUa596",
                name = "PVC модулна подова настилка за гараж работилница – синя тюркоаз - DUa596",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa596/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-modulna-podova-nastilka-za-garazh-rabotilnitsa-sinya-tyurkoaz-dua596",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.STANDARD_TILE,
                colorHex = 0xFF06B6D4,
                colorName = "Синя тюркоаз",
                patternName = "Стандартна мрежа"
            ),
            SellzoneProduct(
                sku = "DUa597",
                name = "PVC модулна подова настилка за гараж и студиа – светло синя - DUa597",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa597/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-modulna-podova-nastilka-za-garazh-i-studia-svetlo-sinya-dua597",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.STANDARD_TILE,
                colorHex = 0xFF38BDF8,
                colorName = "Светло син",
                patternName = "Стандартна мрежа"
            ),
            SellzoneProduct(
                sku = "DUa598",
                name = "PVC модулна подова настилка за гараж и сервиз– синя DUa598",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa598/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-modulna-podova-nastilka-za-garazh-i-serviz-sinya-dua598",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.STANDARD_TILE,
                colorHex = 0xFF2563EB,
                colorName = "Син",
                patternName = "Стандартна мрежа"
            ),
            SellzoneProduct(
                sku = "DUa599",
                name = "PVC модулна подова гаражна настилка – лилава - DUa599",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa599/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-modulna-podova-garazhna-nastilka-lilava-dua599",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.STANDARD_TILE,
                colorHex = 0xFF8B5CF6,
                colorName = "Лилав",
                patternName = "Стандартна мрежа"
            ),
            SellzoneProduct(
                sku = "DUa600",
                name = "PVC модулна подова настилка за гараж – розова - DUa600",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa600/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-modulna-podova-nastilka-za-garazh-rozova-dua600",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.STANDARD_TILE,
                colorHex = 0xFFEC4899,
                colorName = "Розов",
                patternName = "Стандартна мрежа"
            ),

            // --- 3D Design 400x400 mm Garage Tiles ---
            SellzoneProduct(
                sku = "DUa639",
                name = "PVC плочка за модулен под за гаражи – черен 3D дизайн - DUa639",
                priceEur = 3.88,
                imageUrl = "https://sellzone.bg/image/cache/DUa639/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-plochka-za-modulen-pod-za-garazhi-cheren-3d-dizayn-dua639",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.DESIGN_3D_TILE,
                colorHex = 0xFF1E2229,
                colorName = "Черен 3D",
                patternName = "3D Модулен дизайн"
            ),
            SellzoneProduct(
                sku = "DUa640",
                name = "PVC плочка за модулен под за гаражи – сив 3D дизайн - DUa640",
                priceEur = 3.88,
                imageUrl = "https://sellzone.bg/image/cache/DUa640/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-plochka-za-modulen-pod-za-garazhi-siv-3d-dizayn-dua640",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.DESIGN_3D_TILE,
                colorHex = 0xFF64748B,
                colorName = "Сив 3D",
                patternName = "3D Модулен дизайн"
            ),
            SellzoneProduct(
                sku = "DUa641",
                name = "PVC плочка за модулен под за гаражи – син 3D дизайн - DUa641",
                priceEur = 3.88,
                imageUrl = "https://sellzone.bg/image/cache/DUa641/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-plochka-za-modulen-pod-za-garazhi-sin-3d-dizayn-dua641",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.DESIGN_3D_TILE,
                colorHex = 0xFF1D4ED8,
                colorName = "Син 3D",
                patternName = "3D Модулен дизайн"
            ),
            SellzoneProduct(
                sku = "DUa642",
                name = "PVC плочка за модулен под за гаражи – червен 3D дизайн - DUa642",
                priceEur = 3.88,
                imageUrl = "https://sellzone.bg/image/cache/DUa642/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-plochka-za-modulen-pod-za-garazhi-cherven-3d-dizayn-dua642",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.DESIGN_3D_TILE,
                colorHex = 0xFFB91C1C,
                colorName = "Червен 3D",
                patternName = "3D Модулен дизайн"
            ),
            SellzoneProduct(
                sku = "DUa643",
                name = "PVC плочка за модулен под за гаражи – зелен 3D дизайн – DUa643",
                priceEur = 3.88,
                imageUrl = "https://sellzone.bg/image/cache/DUa643/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-plochka-za-modulen-pod-za-garazhi-zelen-3d-dizayn-dua643",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.DESIGN_3D_TILE,
                colorHex = 0xFF059669,
                colorName = "Зелен 3D",
                patternName = "3D Модулен дизайн"
            ),
            SellzoneProduct(
                sku = "DUa644",
                name = "PVC плочка за модулен под за гаражи – жълт 3D дизайн – DUa644",
                priceEur = 3.88,
                imageUrl = "https://sellzone.bg/image/cache/DUa644/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-plochka-za-modulen-pod-za-garazhi-zhalt-3d-dizayn-dua644",
                widthMm = 400,
                heightMm = 400,
                thicknessMm = 18,
                coverageM2 = 0.16,
                piecesPerM2 = 6.25,
                type = ProductType.DESIGN_3D_TILE,
                colorHex = 0xFFCA8A04,
                colorName = "Жълт 3D",
                patternName = "3D Модулен дизайн"
            ),

            // --- Heavy-Duty Professional 500x500 mm Garage Tile ---
            SellzoneProduct(
                sku = "DUa653",
                name = "PVC подова настилка – противоплъзгаща и устойчива, сив цвят – DUa653",
                priceEur = 13.80,
                imageUrl = "https://sellzone.bg/image/cache/DUa653/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/pvc-podova-nastilka-protivoplazgashta-i-ustoychiva-siv-tsvyat-dua653",
                widthMm = 500,
                heightMm = 500,
                thicknessMm = 20,
                coverageM2 = 0.25,
                piecesPerM2 = 4.0,
                type = ProductType.PRO_TILE,
                colorHex = 0xFF475569,
                colorName = "Професионален сив 50x50",
                patternName = "Противоплъзгаща текстура",
                description = "Издръжлива плочка за индустриални натоварвания. 4 броя покриват 1 кв.м."
            ),

            // --- Accessories: Edges, Corners, LED Channels ---
            SellzoneProduct(
                sku = "DUa587",
                name = "Кант/край за страна на PVC модулна подова настилка за гараж – DUa587/8",
                priceEur = 1.17,
                imageUrl = "https://sellzone.bg/image/cache/DUa587/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/kant-kray-za-strana-na-pvc-modulna-podova-nastilka-za-garazh-dua587-8",
                widthMm = 400,
                heightMm = 60,
                thicknessMm = 18,
                coverageM2 = 0.024,
                piecesPerM2 = 0.0,
                type = ProductType.EDGE_PROFILE,
                colorHex = 0xFF2D3748,
                colorName = "Черен кант",
                description = "Преходен кант с наклонена рампа за плавно преминаване на гуми. Мъжки/женски закопчавания."
            ),
            SellzoneProduct(
                sku = "DUa589",
                name = "Ъгъл/край/кант за ръб на PVC модулна подова настилка за гараж – DUa589",
                priceEur = 1.53,
                imageUrl = "https://sellzone.bg/image/cache/DUa589/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/agal-kray-kant-za-rab-na-pvc-modulna-podova-nastilka-za-garazh-dua589",
                widthMm = 60,
                heightMm = 60,
                thicknessMm = 18,
                coverageM2 = 0.0036,
                piecesPerM2 = 0.0,
                type = ProductType.CORNER,
                colorHex = 0xFF2D3748,
                colorName = "Черен ъгъл",
                description = "Ъглов свързващ елемент за кантове при 90 градусов завой."
            ),
            SellzoneProduct(
                sku = "DUa631",
                name = "Канал и елементи за LED осветление в модулна подова настилка – DUa631 (аксесоари DUa632–DUa638)",
                priceEur = 1.78,
                imageUrl = "https://sellzone.bg/image/cache/DUa631/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/kanal-i-elementi-za-led-osvetlenie-v-modulna-podova-nastilka-dua631-i-aksesoari-dua632dua638",
                widthMm = 400,
                heightMm = 50,
                thicknessMm = 18,
                coverageM2 = 0.02,
                piecesPerM2 = 0.0,
                type = ProductType.LED_CHANNEL,
                colorHex = 0xFFF1F5F9,
                colorName = "LED алуминиев канал",
                description = "Специализиран профил за вграждане на подово LED осветление и акцентни светлинни линии в гаража."
            ),
            SellzoneProduct(
                sku = "DUa601",
                name = "Сив кант / край / ъгъл за страна на PVC модулна подова настилка - DUa601/2/3",
                priceEur = 1.53,
                imageUrl = "https://sellzone.bg/image/cache/DUa601/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/siv-kant-kray-agal-za-strana-na-pvc-modulna-podova-nastilka-dua601-2-3",
                widthMm = 400,
                heightMm = 60,
                thicknessMm = 18,
                coverageM2 = 0.024,
                piecesPerM2 = 0.0,
                type = ProductType.EDGE_PROFILE,
                colorHex = 0xFF64748B,
                colorName = "Сив кант"
            ),
            SellzoneProduct(
                sku = "DUa604",
                name = "Светло сив кант / край / ъгъл за страна на PVC модулна подова настилка – DUa604 / DUa605 / DUa606",
                priceEur = 1.53,
                imageUrl = "https://sellzone.bg/image/cache/DUa604/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/svetlo-siv-kant-kray-agal-za-strana-na-pvc-modulna-podova-nastilka-dua604-dua605-dua606",
                widthMm = 400,
                heightMm = 60,
                thicknessMm = 18,
                coverageM2 = 0.024,
                piecesPerM2 = 0.0,
                type = ProductType.EDGE_PROFILE,
                colorHex = 0xFF94A3B8,
                colorName = "Светло сив кант"
            ),
            SellzoneProduct(
                sku = "DUa607",
                name = "Бял кант / край / ъгъл за страна на PVC модулна подова настилка - DUa607/8/9",
                priceEur = 1.53,
                imageUrl = "https://sellzone.bg/image/cache/DUa607/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/byal-kant-kray-agal-za-strana-na-pvc-modulna-podova-nastilka-dua607-8-9",
                widthMm = 400,
                heightMm = 60,
                thicknessMm = 18,
                coverageM2 = 0.024,
                piecesPerM2 = 0.0,
                type = ProductType.EDGE_PROFILE,
                colorHex = 0xFFF8FAFC,
                colorName = "Бял кант"
            ),
            SellzoneProduct(
                sku = "DUa610",
                name = "Червен кант / край / ъгъл за страна на PVC модулна подова настилка – DUa610 / DUa611 / DUa612",
                priceEur = 1.53,
                imageUrl = "https://sellzone.bg/image/cache/DUa610/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/cherven-kant-kray-agal-za-strana-na-pvc-modulna-podova-nastilka-dua610-dua611-dua612",
                widthMm = 400,
                heightMm = 60,
                thicknessMm = 18,
                coverageM2 = 0.024,
                piecesPerM2 = 0.0,
                type = ProductType.EDGE_PROFILE,
                colorHex = 0xFFDC2626,
                colorName = "Червен кант"
            ),
            SellzoneProduct(
                sku = "DUa613",
                name = "Оранжев кант / край / ъгъл за страна на PVC модулна подова настилка - DUa613/14/15",
                priceEur = 1.53,
                imageUrl = "https://sellzone.bg/image/cache/DUa613/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/oranzhev-kant-kray-agal-za-strana-na-pvc-modulna-podova-nastilka-dua613-14-15",
                widthMm = 400,
                heightMm = 60,
                thicknessMm = 18,
                coverageM2 = 0.024,
                piecesPerM2 = 0.0,
                type = ProductType.EDGE_PROFILE,
                colorHex = 0xFFF97316,
                colorName = "Оранжев кант"
            ),
            SellzoneProduct(
                sku = "DUa616",
                name = "Жълт кант / край / ъгъл за страна на PVC модулна подова настилка - DUa616/17/18",
                priceEur = 1.53,
                imageUrl = "https://sellzone.bg/image/cache/DUa616/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/zhalt-kant-kray-agal-za-strana-na-pvc-modulna-podova-nastilka-dua616-17-18",
                widthMm = 400,
                heightMm = 60,
                thicknessMm = 18,
                coverageM2 = 0.024,
                piecesPerM2 = 0.0,
                type = ProductType.EDGE_PROFILE,
                colorHex = 0xFFEAB308,
                colorName = "Жълт кант"
            ),
            SellzoneProduct(
                sku = "DUa619",
                name = "Тъмно зелен кант / край / ъгъл за страна на PVC модулна подова настилка - DUa619/20/21",
                priceEur = 1.53,
                imageUrl = "https://sellzone.bg/image/cache/DUa619/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/tamno-zelen-kant-kray-agal-za-strana-na-pvc-modulna-podova-nastilka-dua619-20-21",
                widthMm = 400,
                heightMm = 60,
                thicknessMm = 18,
                coverageM2 = 0.024,
                piecesPerM2 = 0.0,
                type = ProductType.EDGE_PROFILE,
                colorHex = 0xFF166534,
                colorName = "Тъмно зелен кант"
            ),
            SellzoneProduct(
                sku = "DUa622",
                name = "Синя тюркоаз кант / край / ъгъл за страна на PVC модулна подова настилка – DUa622/623/624",
                priceEur = 1.53,
                imageUrl = "https://sellzone.bg/image/cache/DUa622/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/sinya-tyurkoaz-kant-kray-agal-za-strana-na-pvc-modulna-podova-nastilka-dua622-623-624",
                widthMm = 400,
                heightMm = 60,
                thicknessMm = 18,
                coverageM2 = 0.024,
                piecesPerM2 = 0.0,
                type = ProductType.EDGE_PROFILE,
                colorHex = 0xFF06B6D4,
                colorName = "Тюркоаз кант"
            ),
            SellzoneProduct(
                sku = "DUa625",
                name = "Светло син кант / край / ъгъл за страна на PVC модулна подова настилка – DUa625 / DUa626 / DUa627",
                priceEur = 1.53,
                imageUrl = "https://sellzone.bg/image/cache/DUa625/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/svetlo-sin-kant-kray-agal-za-strana-na-pvc-modulna-podova-nastilka-dua625-dua626-dua627",
                widthMm = 400,
                heightMm = 60,
                thicknessMm = 18,
                coverageM2 = 0.024,
                piecesPerM2 = 0.0,
                type = ProductType.EDGE_PROFILE,
                colorHex = 0xFF38BDF8,
                colorName = "Светло син кант"
            ),
            SellzoneProduct(
                sku = "DUa628",
                name = "Тъмно син кант / край / ъгъл за страна на PVC модулна подова настилка – DUa628 / DUa629 / DUa630",
                priceEur = 1.53,
                imageUrl = "https://sellzone.bg/image/cache/DUa628/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/tamno-sin-kant-kray-agal-za-strana-na-pvc-modulna-podova-nastilka-dua628-dua629-dua630",
                widthMm = 400,
                heightMm = 60,
                thicknessMm = 18,
                coverageM2 = 0.024,
                piecesPerM2 = 0.0,
                type = ProductType.EDGE_PROFILE,
                colorHex = 0xFF1E3A8A,
                colorName = "Тъмно син кант"
            ),

            // --- Decking Category (Preserved in database, separated from garage tiles) ---
            SellzoneProduct(
                sku = "DUa649",
                name = "Плочки за декинг на тераса, двор и градина – модулен под Антрацит – DUa649",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa649/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/plochki-za-deking-na-terasa-dvor-i-gradina-modulen-pod-antratsit-dua649",
                widthMm = 300,
                heightMm = 300,
                thicknessMm = 20,
                coverageM2 = 0.09,
                piecesPerM2 = 11.11,
                type = ProductType.DECKING,
                colorHex = 0xFF334155,
                colorName = "Антрацит декинг",
                description = "Градински и терасен декинг модул (не се използва за гараж)."
            ),
            SellzoneProduct(
                sku = "DUa650",
                name = "Плочки за декинг на тераса, двор и градина – модулен под в кафяво – DUa650",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa650/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/plochki-za-deking-na-terasa-dvor-i-gradina-modulen-pod-v-kafyavo-dua650",
                widthMm = 300,
                heightMm = 300,
                thicknessMm = 20,
                coverageM2 = 0.09,
                piecesPerM2 = 11.11,
                type = ProductType.DECKING,
                colorHex = 0xFF78350F,
                colorName = "Кафяв декинг",
                description = "Градински и терасен декинг модул."
            ),
            SellzoneProduct(
                sku = "DUa651",
                name = "Плочки за декинг с изкуствена трева за тераса, двор и градина – модулен под Зелен – DUa651",
                priceEur = 3.93,
                imageUrl = "https://sellzone.bg/image/cache/DUa651/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/plochki-za-deking-s-izkustvena-treva-za-terasa-dvor-i-gradina-modulen-pod-zelen-dua651",
                widthMm = 300,
                heightMm = 300,
                thicknessMm = 25,
                coverageM2 = 0.09,
                piecesPerM2 = 11.11,
                type = ProductType.DECKING,
                colorHex = 0xFF15803D,
                colorName = "Декинг трева",
                description = "Декинг с изкуствена трева за открити площи."
            ),
            SellzoneProduct(
                sku = "DUa652",
                name = "Плочки за декинг на тераса, двор и градина – модулен под с визия на дърво – DUa652",
                priceEur = 3.06,
                imageUrl = "https://sellzone.bg/image/cache/DUa652/1200 1-228x228.jpg",
                productUrl = "https://sellzone.bg/plochki-za-deking-na-terasa-dvor-i-gradina-modulen-pod-s-viziya-na-darvo-dua652",
                widthMm = 300,
                heightMm = 300,
                thicknessMm = 20,
                coverageM2 = 0.09,
                piecesPerM2 = 11.11,
                type = ProductType.DECKING,
                colorHex = 0xFF92400E,
                colorName = "Декинг дърво",
                description = "Декинг с визия на естествено дърво."
            )
        )
    }
}
