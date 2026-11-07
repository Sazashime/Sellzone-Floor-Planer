package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.data.model.GaragePlan
import com.example.data.model.ProductType
import com.example.data.repository.ProductCatalogRepository
import com.example.ui.theme.EmeraldGreenAccent
import com.example.ui.theme.GraphiteBackground
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurfaceVariant
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@Composable
fun GarageGridCanvas(
    plan: GaragePlan,
    catalogRepository: ProductCatalogRepository,
    activeTool: DesignTool,
    activeSku: String,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    onPanChanged: (Float, Float) -> Unit,
    onCellClicked: (col: Int, row: Int) -> Unit,
    onRectPainted: (startCol: Int, startRow: Int, endCol: Int, endRow: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragStartCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragCurrentCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isDraggingCanvas by remember { mutableStateOf(false) }

    // Pre-calculate base cell size in px
    val baseCellSizePx = 36f * zoomScale
    val gridWidthPx = plan.cols * baseCellSizePx
    val gridHeightPx = plan.rows * baseCellSizePx

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GraphiteBackground)
            .pointerInput(plan, activeTool, zoomScale, panOffsetX, panOffsetY) {
                detectTapGestures { offset ->
                    val localX = offset.x - panOffsetX
                    val localY = offset.y - panOffsetY
                    val col = floor(localX / baseCellSizePx).toInt()
                    val row = floor(localY / baseCellSizePx).toInt()
                    if (col in 0 until plan.cols && row in 0 until plan.rows) {
                        onCellClicked(col, row)
                    }
                }
            }
            .pointerInput(plan, activeTool, zoomScale, panOffsetX, panOffsetY) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val localX = offset.x - panOffsetX
                        val localY = offset.y - panOffsetY
                        val col = floor(localX / baseCellSizePx).toInt()
                        val row = floor(localY / baseCellSizePx).toInt()
                        if (col in 0 until plan.cols && row in 0 until plan.rows) {
                            if (activeTool == DesignTool.RECTANGLE) {
                                dragStartCell = Pair(col, row)
                                dragCurrentCell = Pair(col, row)
                                isDraggingCanvas = false
                            } else if (activeTool == DesignTool.PEN || activeTool == DesignTool.ERASER) {
                                onCellClicked(col, row)
                                isDraggingCanvas = false
                            } else {
                                isDraggingCanvas = true
                            }
                        } else {
                            isDraggingCanvas = true
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (isDraggingCanvas) {
                            onPanChanged(panOffsetX + dragAmount.x, panOffsetY + dragAmount.y)
                        } else if (activeTool == DesignTool.RECTANGLE) {
                            val localX = change.position.x - panOffsetX
                            val localY = change.position.y - panOffsetY
                            val col = floor(localX / baseCellSizePx).toInt().coerceIn(0, plan.cols - 1)
                            val row = floor(localY / baseCellSizePx).toInt().coerceIn(0, plan.rows - 1)
                            dragCurrentCell = Pair(col, row)
                        } else if (activeTool == DesignTool.PEN || activeTool == DesignTool.ERASER) {
                            val localX = change.position.x - panOffsetX
                            val localY = change.position.y - panOffsetY
                            val col = floor(localX / baseCellSizePx).toInt()
                            val row = floor(localY / baseCellSizePx).toInt()
                            if (col in 0 until plan.cols && row in 0 until plan.rows) {
                                onCellClicked(col, row)
                            }
                        }
                    },
                    onDragEnd = {
                        if (activeTool == DesignTool.RECTANGLE && dragStartCell != null && dragCurrentCell != null) {
                            val start = dragStartCell!!
                            val end = dragCurrentCell!!
                            val minCol = min(start.first, end.first)
                            val maxCol = max(start.first, end.first)
                            val minRow = min(start.second, end.second)
                            val maxRow = max(start.second, end.second)
                            onRectPainted(minCol, minRow, maxCol, maxRow)
                        }
                        dragStartCell = null
                        dragCurrentCell = null
                        isDraggingCanvas = false
                    },
                    onDragCancel = {
                        dragStartCell = null
                        dragCurrentCell = null
                        isDraggingCanvas = false
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // 1. Draw outer boundary and garage perimeter border
            val startX = panOffsetX
            val startY = panOffsetY

            // Draw concrete floor under the grid
            drawRect(
                color = Color(0xFF16191E),
                topLeft = Offset(startX, startY),
                size = Size(gridWidthPx, gridHeightPx)
            )

            // Draw concrete texture grid
            val strokeGrid = Stroke(width = 1f)
            val subColor = Color(0xFF262C36)
            for (c in 0..plan.cols) {
                val x = startX + c * baseCellSizePx
                drawLine(
                    color = subColor,
                    start = Offset(x, startY),
                    end = Offset(x, startY + gridHeightPx),
                    strokeWidth = 1f
                )
            }
            for (r in 0..plan.rows) {
                val y = startY + r * baseCellSizePx
                drawLine(
                    color = subColor,
                    start = Offset(startX, y),
                    end = Offset(startX + gridWidthPx, y),
                    strokeWidth = 1f
                )
            }

            // 2. Draw placed tiles
            val tileGap = max(1f, 1.5f * zoomScale)
            val innerSize = max(2f, baseCellSizePx - tileGap)

            for (r in 0 until plan.rows) {
                for (c in 0 until plan.cols) {
                    val sku = plan.getCellSku(c, r) ?: continue
                    val product = catalogRepository.getProductBySku(sku)
                    val tileColor = if (product != null) Color(product.colorHex) else Color(0xFF64748B)

                    val cellLeft = startX + c * baseCellSizePx + tileGap / 2f
                    val cellTop = startY + r * baseCellSizePx + tileGap / 2f

                    // Base tile body
                    drawRect(
                        color = tileColor,
                        topLeft = Offset(cellLeft, cellTop),
                        size = Size(innerSize, innerSize)
                    )

                    // Characteristic Sellzone tile mesh texture (vented slots / interlocking pattern)
                    if (innerSize >= 14f) {
                        val ribColor = Color.Black.copy(alpha = 0.28f)
                        val highlightColor = Color.White.copy(alpha = 0.15f)

                        // Outer border bevel
                        drawLine(
                            color = highlightColor,
                            start = Offset(cellLeft, cellTop),
                            end = Offset(cellLeft + innerSize, cellTop),
                            strokeWidth = 1.2f
                        )
                        drawLine(
                            color = highlightColor,
                            start = Offset(cellLeft, cellTop),
                            end = Offset(cellLeft, cellTop + innerSize),
                            strokeWidth = 1.2f
                        )
                        drawLine(
                            color = ribColor,
                            start = Offset(cellLeft, cellTop + innerSize),
                            end = Offset(cellLeft + innerSize, cellTop + innerSize),
                            strokeWidth = 1.5f
                        )
                        drawLine(
                            color = ribColor,
                            start = Offset(cellLeft + innerSize, cellTop),
                            end = Offset(cellLeft + innerSize, cellTop + innerSize),
                            strokeWidth = 1.5f
                        )

                        // If 3D tile, draw diamond cross ribs
                        if (product?.type == ProductType.DESIGN_3D_TILE) {
                            val midX = cellLeft + innerSize / 2f
                            val midY = cellTop + innerSize / 2f
                            val path = Path().apply {
                                moveTo(midX, cellTop + 2f)
                                lineTo(cellLeft + innerSize - 2f, midY)
                                lineTo(midX, cellTop + innerSize - 2f)
                                lineTo(cellLeft + 2f, midY)
                                close()
                            }
                            drawPath(path, color = highlightColor, style = Stroke(width = 1.5f))
                        } else if (innerSize >= 22f) {
                            // Modular grill / rib grid
                            val step = innerSize / 4f
                            for (i in 1..3) {
                                drawLine(
                                    color = ribColor,
                                    start = Offset(cellLeft + i * step, cellTop + 2f),
                                    end = Offset(cellLeft + i * step, cellTop + innerSize - 2f),
                                    strokeWidth = 1f
                                )
                                drawLine(
                                    color = ribColor,
                                    start = Offset(cellLeft + 2f, cellTop + i * step),
                                    end = Offset(cellLeft + innerSize - 2f, cellTop + i * step),
                                    strokeWidth = 1f
                                )
                            }
                        }
                    }
                }
            }

            // 3. Draw active drag selection rectangle (for RECTANGLE tool)
            if (activeTool == DesignTool.RECTANGLE && dragStartCell != null && dragCurrentCell != null) {
                val s = dragStartCell!!
                val e = dragCurrentCell!!
                val minC = min(s.first, e.first)
                val maxC = max(s.first, e.first)
                val minR = min(s.second, e.second)
                val maxR = max(s.second, e.second)

                val rectLeft = startX + minC * baseCellSizePx
                val rectTop = startY + minR * baseCellSizePx
                val rectWidth = (maxC - minC + 1) * baseCellSizePx
                val rectHeight = (maxR - minR + 1) * baseCellSizePx

                drawRect(
                    color = EmeraldGreenAccent.copy(alpha = 0.25f),
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(rectWidth, rectHeight)
                )
                drawRect(
                    color = EmeraldGreenAccent,
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(rectWidth, rectHeight),
                    style = Stroke(width = 2.5f)
                )
            }

            // 4. Draw outer perimeter boundary accent
            drawRect(
                color = EmeraldGreenAccent.copy(alpha = 0.7f),
                topLeft = Offset(startX, startY),
                size = Size(gridWidthPx, gridHeightPx),
                style = Stroke(width = 2f)
            )

            // 5. Draw entrance ramp indicator if enabled
            val entranceThickness = 6f * zoomScale
            drawRect(
                color = Color(0xFFFFB300),
                topLeft = Offset(startX, startY - entranceThickness - 2f),
                size = Size(gridWidthPx, entranceThickness)
            )
        }
    }
}
