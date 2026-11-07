package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.model.ProjectCalculation
import com.example.data.model.ShoppingItem
import com.example.ui.theme.EmeraldGreenAccent
import com.example.ui.theme.EmeraldGreenContainer
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceElevated
import com.example.ui.theme.GraphiteSurfaceVariant
import com.example.ui.theme.MetallicTitanium
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WorkshopAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShoppingListView(
    calculation: ProjectCalculation,
    wastePercentage: Int,
    onWastePercentageChanged: (Int) -> Unit,
    showPricesInBgn: Boolean,
    onToggleCurrency: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- Header & Live Summary Card ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GraphiteSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Калкулация на проекта",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Currency switcher button
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onToggleCurrency() },
                            color = EmeraldGreenContainer,
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreenAccent)
                        ) {
                            Text(
                                text = if (showPricesInBgn) "Валута: BGN (лв)" else "Валута: EUR (€)",
                                color = EmeraldGreenAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Grid stats
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Обща площ:", color = TextMuted, fontSize = 11.sp)
                            Text("${calculation.totalAreaM2} м²", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Покрита площ:", color = TextMuted, fontSize = 11.sp)
                            Text("${calculation.coveredAreaM2} м² (${calculation.coveredCellsCount} бр.)", color = EmeraldGreenAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Непокрита площ:", color = TextMuted, fontSize = 11.sp)
                            Text("${calculation.uncoveredAreaM2} м²", color = if (calculation.uncoveredCellsCount > 0) WorkshopAmber else TextSecondary, fontSize = 14.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = GraphiteBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Waste percentage configuration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Резерв за рязане / фира:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$wastePercentage%",
                            color = WorkshopAmber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Slider(
                        value = wastePercentage.toFloat(),
                        onValueChange = { onWastePercentageChanged(it.toInt()) },
                        valueRange = 0f..20f,
                        steps = 19,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldGreenAccent,
                            activeTrackColor = EmeraldGreenAccent,
                            inactiveTrackColor = GraphiteBorder
                        ),
                        modifier = Modifier.testTag("slider_waste_percentage")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Плочки в дизайна: ${calculation.totalTilesPlaced} бр.", color = TextMuted, fontSize = 11.sp)
                        Text("+ Резерв: ${calculation.totalReserveTiles} бр.", color = WorkshopAmber, fontSize = 11.sp)
                        Text("Общо за покупка: ${calculation.totalTilesToOrder} бр.", color = EmeraldGreenAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // --- Breakdown by Color ---
        if (calculation.tilesByColor.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = GraphiteSurface)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Разпределение по цветове:", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            calculation.tilesByColor.forEach { (colorName, count) ->
                                Surface(
                                    color = GraphiteSurfaceElevated,
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
                                ) {
                                    Text(
                                        text = "$colorName: $count бр.",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Shopping List Items ---
        item {
            Text(
                text = "Детайлен списък за поръчка от Sellzone.bg:",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (calculation.items.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GraphiteSurface)
                ) {
                    Text(
                        text = "Няма поставени плочки на пода. Използвайте инструментите за рисуване, за да оцветите гаража.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(calculation.items, key = { it.product.sku + "_" + it.isAccessory }) { item ->
                ShoppingItemCard(item = item, showPricesInBgn = showPricesInBgn)
            }
        }

        // --- Grand Total & Action Buttons ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GraphiteSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreenAccent)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Стойност плочки (с включен резерв):", color = TextSecondary, fontSize = 12.sp)
                        val matStr = if (showPricesInBgn) "%.2f лв".format(calculation.materialCostBgn) else "%.2f €".format(calculation.materialCostEur)
                        Text(matStr, color = TextPrimary, fontSize = 12.sp)
                    }

                    if (calculation.accessoriesCostEur > 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Стойност кантове и аксесоари:", color = TextSecondary, fontSize = 12.sp)
                            val accStr = if (showPricesInBgn) "%.2f лв".format(calculation.accessoriesCostBgn) else "%.2f €".format(calculation.accessoriesCostEur)
                            Text(accStr, color = TextPrimary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = GraphiteBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ОБЩО ПРОЕКТ:",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            val grandMain = if (showPricesInBgn) "%.2f лв".format(calculation.grandTotalBgn) else "%.2f €".format(calculation.grandTotalEur)
                            val grandSub = if (showPricesInBgn) "%.2f €".format(calculation.grandTotalEur) else "%.2f лв".format(calculation.grandTotalBgn)

                            Text(
                                text = grandMain,
                                color = EmeraldGreenAccent,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "($grandSub)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Actions: Copy, Share, Export
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val text = generateShoppingListText(calculation, showPricesInBgn)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Sellzone Shopping List", text)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Списъкът за поръчка е копиран в клипборда!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldGreenAccent,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_copy_shopping_list")
                        ) {
                            Icon(Icons.Default.ContentCopy, "Копирай", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Копирай списък", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val text = generateShoppingListText(calculation, showPricesInBgn)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, text)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Сподели проект Sellzone")
                                context.startActivity(shareIntent)
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreenAccent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreenAccent),
                            modifier = Modifier.testTag("btn_share_shopping_list")
                        ) {
                            Icon(Icons.Default.Share, "Сподели", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShoppingItemCard(
    item: ShoppingItem,
    showPricesInBgn: Boolean
) {
    val context = LocalContext.current
    val prod = item.product
    val unitPriceStr = if (showPricesInBgn) "%.2f лв".format(item.unitPriceBgn) else "%.2f €".format(item.unitPriceEur)
    val subtotalStr = if (showPricesInBgn) "%.2f лв".format(item.subtotalBgn) else "%.2f €".format(item.subtotalEur)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(prod.colorHex))
            ) {
                if (prod.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = prod.imageUrl,
                        contentDescription = prod.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = prod.sku,
                            color = EmeraldGreenAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = if (item.isAccessory) "Аксесоар" else "${prod.widthMm}×${prod.heightMm} мм",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = prod.name,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (item.isAccessory) {
                                "Количество: ${item.totalQuantity} бр."
                            } else {
                                "В проект: ${item.placedQuantity} + Резерв: ${item.reserveQuantity} = ${item.totalQuantity} бр."
                            },
                            color = if (item.reserveQuantity > 0) WorkshopAmber else TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Ед. цена: $unitPriceStr",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = subtotalStr,
                        color = EmeraldGreenAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Direct link to Sellzone
            IconButton(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(prod.productUrl))
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
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

fun generateShoppingListText(calculation: ProjectCalculation, showInBgn: Boolean): String {
    val sb = StringBuilder()
    sb.appendLine("===========================================")
    sb.appendLine("SELLZONE GARAGE FLOOR PLANNER PRO - СПИСЪК ЗА ПОРЪЧКА")
    sb.appendLine("===========================================")
    sb.appendLine("Обща площ на гаража: ${calculation.totalAreaM2} м²")
    sb.appendLine("Покрита площ: ${calculation.coveredAreaM2} м² (${calculation.coveredCellsCount} плочки)")
    sb.appendLine("Резерв за рязане: ${calculation.wastePercentage}% (+${calculation.totalReserveTiles} бр.)")
    sb.appendLine("Общо плочки за поръчка: ${calculation.totalTilesToOrder} бр.")
    sb.appendLine("-------------------------------------------")
    sb.appendLine("АРТИКУЛИ ЗА ЗАКУПУВАНЕ:")

    for (item in calculation.items) {
        val prod = item.product
        val priceStr = if (showInBgn) "%.2f лв".format(item.unitPriceBgn) else "%.2f €".format(item.unitPriceEur)
        val subStr = if (showInBgn) "%.2f лв".format(item.subtotalBgn) else "%.2f €".format(item.subtotalEur)

        sb.appendLine("• SKU: ${prod.sku} - ${prod.name}")
        if (item.isAccessory) {
            sb.appendLine("  Количество: ${item.totalQuantity} бр. | Ед. цена: $priceStr | Сума: $subStr")
        } else {
            sb.appendLine("  В проект: ${item.placedQuantity} бр. | Резерв: ${item.reserveQuantity} бр. | Общо: ${item.totalQuantity} бр.")
            sb.appendLine("  Ед. цена: $priceStr | Сума: $subStr")
        }
        sb.appendLine("  Линк: ${prod.productUrl}")
        sb.appendLine()
    }

    sb.appendLine("-------------------------------------------")
    val totalStr = if (showInBgn) "%.2f лв".format(calculation.grandTotalBgn) else "%.2f €".format(calculation.grandTotalEur)
    sb.appendLine("ОБЩА КРАЙНА СТОЙНОСТ: $totalStr")
    sb.appendLine("Актуални цени директно от: https://sellzone.bg")
    sb.appendLine("===========================================")
    return sb.toString()
}
