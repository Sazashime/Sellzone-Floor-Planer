package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EdgeCoverageMode
import com.example.data.model.GaragePlan
import com.example.data.model.ProjectCalculation
import com.example.data.repository.ProductCatalogRepository
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

@Composable
fun AccessoriesConfigSection(
    plan: GaragePlan,
    calculation: ProjectCalculation,
    repository: ProductCatalogRepository,
    onEdgeModeChanged: (EdgeCoverageMode) -> Unit,
    onEdgeSkuSelected: (String) -> Unit,
    onCornerSkuSelected: (String) -> Unit,
    onLedToggled: (Boolean) -> Unit,
    onManualEdgeQtyChanged: (Int) -> Unit,
    onManualCornerQtyChanged: (Int) -> Unit,
    onManualLedQtyChanged: (Int) -> Unit,
    showPricesInBgn: Boolean,
    modifier: Modifier = Modifier
) {
    val edgeProducts = repository.getCompatibleEdges()
    val cornerProducts = repository.getCompatibleCorners()
    val ledProducts = repository.getCompatibleLed()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Selector
        Text(
            "Режим на кантовете и рампите:",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EdgeCoverageMode.values().forEach { mode ->
                val isSelected = plan.edgeMode == mode
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEdgeModeChanged(mode) }
                        .testTag("edge_mode_${mode.name.lowercase()}"),
                    color = if (isSelected) EmeraldGreenContainer else GraphiteSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) EmeraldGreenAccent else GraphiteBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (mode) {
                                EdgeCoverageMode.ENTRANCE_ONLY -> "Вход"
                                EdgeCoverageMode.FULL_PERIMETER -> "Периметър"
                                EdgeCoverageMode.NONE -> "Без"
                                EdgeCoverageMode.CUSTOM -> "Ръчно"
                            },
                            color = if (isSelected) EmeraldGreenAccent else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (plan.edgeMode != EdgeCoverageMode.NONE) {
            // Edge Profiles Palette
            Text(
                "Избор на преходен кант (Sellzone):",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(edgeProducts, key = { it.sku }) { edge ->
                    val isSelected = edge.sku.equals(plan.selectedEdgeSku, ignoreCase = true)
                    Card(
                        modifier = Modifier
                            .clickable { onEdgeSkuSelected(edge.sku) }
                            .testTag("select_edge_${edge.sku.lowercase()}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) EmeraldGreenContainer else GraphiteSurfaceVariant
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) EmeraldGreenAccent else GraphiteBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(edge.colorHex))
                            )
                            Column {
                                Text(
                                    text = edge.sku,
                                    color = if (isSelected) EmeraldGreenAccent else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val priceStr = if (showPricesInBgn) "%.2f лв".format(edge.priceBgn) else "%.2f €".format(edge.priceEur)
                                Text(text = priceStr, color = TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // Edge Quantity Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraphiteSurfaceVariant),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Прави кантове:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Препоръчани за ${if (plan.edgeMode == EdgeCoverageMode.ENTRANCE_ONLY) "входа" else "периметъра"}: ${calculation.edgePiecesRecommended} бр.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val current = plan.manualEdgeQty ?: calculation.edgePiecesRecommended
                                    if (current > 0) onManualEdgeQtyChanged(current - 1)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, "Намали", tint = TextPrimary)
                            }

                            Text(
                                text = "${plan.manualEdgeQty ?: calculation.edgePiecesRecommended}",
                                color = EmeraldGreenAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            IconButton(
                                onClick = {
                                    val current = plan.manualEdgeQty ?: calculation.edgePiecesRecommended
                                    onManualEdgeQtyChanged(current + 1)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, "Увеличи", tint = TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Ъглови преходи (DUa589):", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Препоръчани ъгли: ${calculation.cornerPiecesRecommended} бр.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val current = plan.manualCornerQty ?: calculation.cornerPiecesRecommended
                                    if (current > 0) onManualCornerQtyChanged(current - 1)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, "Намали", tint = TextPrimary)
                            }

                            Text(
                                text = "${plan.manualCornerQty ?: calculation.cornerPiecesRecommended}",
                                color = EmeraldGreenAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            IconButton(
                                onClick = {
                                    val current = plan.manualCornerQty ?: calculation.cornerPiecesRecommended
                                    onManualCornerQtyChanged(current + 1)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, "Увеличи", tint = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // LED Channel System
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GraphiteSurfaceVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Подово LED осветление (DUa631)",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Профили за вграждане на светлинни ивици в пода (1.78 € / бр)",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = plan.includeLedChannel,
                        onCheckedChange = onLedToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldGreenAccent,
                            checkedTrackColor = EmeraldGreenContainer,
                            uncheckedThumbColor = MetallicTitanium,
                            uncheckedTrackColor = GraphiteBorder
                        ),
                        modifier = Modifier.testTag("toggle_led_channel")
                    )
                }

                if (plan.includeLedChannel) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Брой LED канали:",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val cur = plan.manualLedQty ?: calculation.ledPiecesRecommended
                                    if (cur > 0) onManualLedQtyChanged(cur - 1)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Remove, "Намали", tint = TextPrimary)
                            }

                            Text(
                                text = "${plan.manualLedQty ?: calculation.ledPiecesRecommended} бр.",
                                color = WorkshopAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )

                            IconButton(
                                onClick = {
                                    val cur = plan.manualLedQty ?: calculation.ledPiecesRecommended
                                    onManualLedQtyChanged(cur + 1)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Add, "Увеличи", tint = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
