package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ProductType
import com.example.data.model.SellzoneProduct
import com.example.ui.theme.EmeraldGreenAccent
import com.example.ui.theme.EmeraldGreenContainer
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceVariant
import com.example.ui.theme.MetallicTitanium
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WorkshopAmber

enum class CatalogFilter(val labelBg: String) {
    ALL_TILES("Всички плочки"),
    STANDARD_400("Стандартни 40x40"),
    DESIGN_3D("3D Дизайн"),
    PRO_500("Професионални 50x50"),
    ACCESSORIES("Кантове и ъгли"),
    DECKING("Декинг (не-гаражни)")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogSelector(
    products: List<SellzoneProduct>,
    selectedSku: String,
    onProductSelected: (SellzoneProduct) -> Unit,
    showPricesInBgn: Boolean,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(CatalogFilter.ALL_TILES) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredProducts = remember(products, activeFilter, searchQuery) {
        products.filter { product ->
            val matchesFilter = when (activeFilter) {
                CatalogFilter.ALL_TILES -> product.isFlooringTile
                CatalogFilter.STANDARD_400 -> product.type == ProductType.STANDARD_TILE
                CatalogFilter.DESIGN_3D -> product.type == ProductType.DESIGN_3D_TILE
                CatalogFilter.PRO_500 -> product.type == ProductType.PRO_TILE
                CatalogFilter.ACCESSORIES -> product.isAccessory
                CatalogFilter.DECKING -> product.isDecking
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                product.name.contains(searchQuery, ignoreCase = true) ||
                        product.sku.contains(searchQuery, ignoreCase = true) ||
                        product.colorName.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    Column(modifier = modifier) {
        // Filter chips
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CatalogFilter.values().forEach { filter ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { activeFilter = filter },
                    label = { Text(filter.labelBg, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldGreenContainer,
                        selectedLabelColor = EmeraldGreenAccent,
                        containerColor = GraphiteSurfaceVariant,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        // Product list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredProducts, key = { it.sku }) { product ->
                ProductCard(
                    product = product,
                    isSelected = product.sku.equals(selectedSku, ignoreCase = true),
                    showPricesInBgn = showPricesInBgn,
                    onSelect = { onProductSelected(product) }
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: SellzoneProduct,
    isSelected: Boolean,
    showPricesInBgn: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tileColor = Color(product.colorHex)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable { onSelect() }
            .testTag("product_card_${product.sku.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) GraphiteSurfaceVariant else GraphiteSurface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) EmeraldGreenAccent else GraphiteBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Swatch / Real Image preview
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tileColor)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (product.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreenAccent)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Избран",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = product.sku,
                            color = EmeraldGreenAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (product.isDecking) {
                        Surface(
                            color = Color(0xFF78350F),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ДЕКИНГ",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "${product.widthMm}×${product.heightMm} мм",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = product.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val priceMain = if (showPricesInBgn) "%.2f лв".format(product.priceBgn) else "%.2f €".format(product.priceEur)
                    val priceSub = if (showPricesInBgn) "%.2f €".format(product.priceEur) else "%.2f лв".format(product.priceBgn)

                    Text(
                        text = priceMain,
                        color = EmeraldGreenAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "($priceSub / бр)",
                        color = TextMuted,
                        fontSize = 11.sp
                    )

                    if (product.oldPriceEur != null) {
                        Text(
                            text = "%.2f €".format(product.oldPriceEur),
                            color = TextMuted,
                            fontSize = 11.sp,
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        )
                    }
                }
            }

            // Direct link to Sellzone
            IconButton(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(product.productUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // ignore
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = "Отвори в Sellzone.bg",
                    tint = MetallicTitanium,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
