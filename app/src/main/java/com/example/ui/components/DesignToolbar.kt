package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.BorderOuter
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HorizontalSplit
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.WarningRed
import com.example.ui.theme.WorkshopAmber

@Composable
fun DesignToolbar(
    activeTool: DesignTool,
    onToolSelected: (DesignTool) -> Unit,
    activeProduct: SellzoneProduct,
    allTileProducts: List<SellzoneProduct>,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onFillAll: () -> Unit,
    onApplyCheckerboard: (primarySku: String, secondarySku: String) -> Unit,
    onApplyBorderPattern: (borderSku: String, fillSku: String) -> Unit,
    onApplyParkingBay: (accentSku: String, centerSku: String, outerSku: String) -> Unit,
    onApplyStripes: (sku1: String, sku2: String, isHorizontal: Boolean) -> Unit,
    onClearFloor: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCheckerboardDialog by remember { mutableStateOf(false) }
    var showBorderDialog by remember { mutableStateOf(false) }
    var showParkingBayDialog by remember { mutableStateOf(false) }
    var showStripesDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Quick tools row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GraphiteSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolButton(
                icon = Icons.Default.Brush,
                label = "Плочка",
                isSelected = activeTool == DesignTool.PEN,
                onClick = { onToolSelected(DesignTool.PEN) },
                testTag = "tool_pen"
            )

            ToolButton(
                icon = Icons.Default.CropSquare,
                label = "Зона",
                isSelected = activeTool == DesignTool.RECTANGLE,
                onClick = { onToolSelected(DesignTool.RECTANGLE) },
                testTag = "tool_rect"
            )

            ToolButton(
                icon = Icons.Default.HorizontalSplit,
                label = "Ред",
                isSelected = activeTool == DesignTool.ROW,
                onClick = { onToolSelected(DesignTool.ROW) },
                testTag = "tool_row"
            )

            ToolButton(
                icon = Icons.Default.VerticalSplit,
                label = "Колона",
                isSelected = activeTool == DesignTool.COLUMN,
                onClick = { onToolSelected(DesignTool.COLUMN) },
                testTag = "tool_col"
            )

            ToolButton(
                icon = Icons.Default.FormatColorFill,
                label = "Запълни",
                isSelected = false,
                onClick = onFillAll,
                testTag = "tool_fill_all"
            )

            ToolButton(
                icon = Icons.Default.GridView,
                label = "Шахмат",
                isSelected = false,
                onClick = { showCheckerboardDialog = true },
                testTag = "tool_checkerboard"
            )

            ToolButton(
                icon = Icons.Default.BorderOuter,
                label = "Рамка",
                isSelected = false,
                onClick = { showBorderDialog = true },
                testTag = "tool_border"
            )

            ToolButton(
                icon = Icons.Default.DirectionsCar,
                label = "Паркомясто",
                isSelected = false,
                onClick = { showParkingBayDialog = true },
                testTag = "tool_parking_bay"
            )

            ToolButton(
                icon = Icons.Default.BorderAll,
                label = "Ленти",
                isSelected = false,
                onClick = { showStripesDialog = true },
                testTag = "tool_stripes"
            )

            ToolButton(
                icon = Icons.Default.Delete,
                label = "Гума",
                isSelected = activeTool == DesignTool.ERASER,
                onClick = { onToolSelected(DesignTool.ERASER) },
                tint = WarningRed,
                testTag = "tool_eraser"
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Undo / Redo
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.testTag("action_undo")
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Отмени",
                    tint = if (canUndo) TextPrimary else TextMuted
                )
            }

            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.testTag("action_redo")
            ) {
                Icon(
                    imageVector = Icons.Default.Redo,
                    contentDescription = "Повтори",
                    tint = if (canRedo) TextPrimary else TextMuted
                )
            }
        }
    }

    // --- Pattern Dialogs ---

    // 1. Checkerboard Dialog
    if (showCheckerboardDialog) {
        var secondarySku by remember {
            mutableStateOf(
                allTileProducts.firstOrNull { it.sku != activeProduct.sku }?.sku ?: "DUa584"
            )
        }
        AlertDialog(
            onDismissRequest = { showCheckerboardDialog = false },
            title = { Text("Шахматен модел", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        "Редуване на две плочки през една по целия под на гаража:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Цвят 1: ${activeProduct.name} (${activeProduct.sku})", color = EmeraldGreenAccent, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Изберете Цвят 2:", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allTileProducts.take(12).forEach { prod ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(prod.colorHex))
                                    .border(
                                        width = if (secondarySku == prod.sku) 2.dp else 1.dp,
                                        color = if (secondarySku == prod.sku) EmeraldGreenAccent else GraphiteBorder,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { secondarySku = prod.sku }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApplyCheckerboard(activeProduct.sku, secondarySku)
                        showCheckerboardDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenAccent, contentColor = Color.Black)
                ) {
                    Text("Приложи шахмат")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckerboardDialog = false }) {
                    Text("Отказ", color = TextSecondary)
                }
            },
            containerColor = GraphiteSurface
        )
    }

    // 2. Border Dialog
    if (showBorderDialog) {
        var innerSku by remember {
            mutableStateOf(
                allTileProducts.firstOrNull { it.sku != activeProduct.sku }?.sku ?: "DUa584"
            )
        }
        AlertDialog(
            onDismissRequest = { showBorderDialog = false },
            title = { Text("Периметърна рамка", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        "Очертава външния кант на пода с активната плочка, а вътрешността с втори избран цвят:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Рамка: ${activeProduct.name} (${activeProduct.sku})", color = EmeraldGreenAccent, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Вътрешен цвят:", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allTileProducts.take(12).forEach { prod ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(prod.colorHex))
                                    .border(
                                        width = if (innerSku == prod.sku) 2.dp else 1.dp,
                                        color = if (innerSku == prod.sku) EmeraldGreenAccent else GraphiteBorder,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { innerSku = prod.sku }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApplyBorderPattern(activeProduct.sku, innerSku)
                        showBorderDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenAccent, contentColor = Color.Black)
                ) {
                    Text("Приложи рамка")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBorderDialog = false }) {
                    Text("Отказ", color = TextSecondary)
                }
            },
            containerColor = GraphiteSurface
        )
    }

    // 3. Parking Bay Dialog
    if (showParkingBayDialog) {
        var outerSku by remember { mutableStateOf("DUa590") }
        var centerSku by remember { mutableStateOf("DUa584") }
        AlertDialog(
            onDismissRequest = { showParkingBayDialog = false },
            title = { Text("Автомобилно паркомясто", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        "Генерира спортна композиция: очертано паркомясто с акцентна линия (${activeProduct.name}), шахматен или плътен център и светъл външен под.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApplyParkingBay(activeProduct.sku, centerSku, outerSku)
                        showParkingBayDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenAccent, contentColor = Color.Black)
                ) {
                    Text("Генерирай паркомясто")
                }
            },
            dismissButton = {
                TextButton(onClick = { showParkingBayDialog = false }) {
                    Text("Отказ", color = TextSecondary)
                }
            },
            containerColor = GraphiteSurface
        )
    }

    // 4. Alternating Stripes Dialog
    if (showStripesDialog) {
        var isHorizontal by remember { mutableStateOf(false) }
        var secondSku by remember {
            mutableStateOf(allTileProducts.firstOrNull { it.sku != activeProduct.sku }?.sku ?: "DUa584")
        }
        AlertDialog(
            onDismissRequest = { showStripesDialog = false },
            title = { Text("Редуващи се ленти", color = TextPrimary) },
            text = {
                Column {
                    Text("Посока на лентите:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { isHorizontal = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isHorizontal) EmeraldGreenAccent else GraphiteSurfaceVariant,
                                contentColor = if (!isHorizontal) Color.Black else TextPrimary
                            )
                        ) {
                            Text("Вертикални")
                        }
                        Button(
                            onClick = { isHorizontal = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isHorizontal) EmeraldGreenAccent else GraphiteSurfaceVariant,
                                contentColor = if (isHorizontal) Color.Black else TextPrimary
                            )
                        ) {
                            Text("Хоризонтални")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApplyStripes(activeProduct.sku, secondSku, isHorizontal)
                        showStripesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenAccent, contentColor = Color.Black)
                ) {
                    Text("Приложи ленти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStripesDialog = false }) {
                    Text("Отказ", color = TextSecondary)
                }
            },
            containerColor = GraphiteSurface
        )
    }
}

@Composable
fun ToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tint: Color = TextPrimary,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag(testTag),
        color = if (isSelected) EmeraldGreenContainer else GraphiteSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) EmeraldGreenAccent else GraphiteBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) EmeraldGreenAccent else tint,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = if (isSelected) EmeraldGreenAccent else tint,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
