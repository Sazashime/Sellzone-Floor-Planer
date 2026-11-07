package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.calculator.FloorCalculationEngine
import com.example.data.model.EdgeCoverageMode
import com.example.data.model.GaragePlan
import com.example.data.model.ProjectCalculation
import com.example.data.model.SavedProject
import com.example.data.model.SellzoneProduct
import com.example.data.repository.ProductCatalogRepository
import com.example.data.repository.ProjectRepository
import com.example.data.repository.SyncStatus
import com.example.ui.components.DesignTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.min

class PlannerViewModel(application: Application) : AndroidViewModel(application) {

    val catalogRepository = ProductCatalogRepository()
    val projectRepository = ProjectRepository(application)

    private val _plan = MutableStateFlow(createInitialPlan())
    val plan: StateFlow<GaragePlan> = _plan.asStateFlow()

    private val _activeTool = MutableStateFlow(DesignTool.PEN)
    val activeTool: StateFlow<DesignTool> = _activeTool.asStateFlow()

    private val _activeSku = MutableStateFlow("DUa583") // Standard Black PVC tile
    val activeSku: StateFlow<String> = _activeSku.asStateFlow()

    private val _zoomScale = MutableStateFlow(1.0f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _panOffsetX = MutableStateFlow(30f)
    val panOffsetX: StateFlow<Float> = _panOffsetX.asStateFlow()

    private val _panOffsetY = MutableStateFlow(30f)
    val panOffsetY: StateFlow<Float> = _panOffsetY.asStateFlow()

    private val _showPricesInBgn = MutableStateFlow(false) // Default EUR as requested
    val showPricesInBgn: StateFlow<Boolean> = _showPricesInBgn.asStateFlow()

    private val _projectName = MutableStateFlow("Моят гараж 10x9.2m")
    val projectName: StateFlow<String> = _projectName.asStateFlow()

    private val _currentProjectId = MutableStateFlow<String?>("demo_sport_garage_10x9.2")
    val currentProjectId: StateFlow<String?> = _currentProjectId.asStateFlow()

    // Undo / Redo history stacks
    private val undoStack = mutableListOf<GaragePlan>()
    private val redoStack = mutableListOf<GaragePlan>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Real-time calculation derived from plan & repository
    val calculation: StateFlow<ProjectCalculation> = combine(
        _plan,
        catalogRepository.products
    ) { currentPlan, _ ->
        FloorCalculationEngine.calculate(currentPlan, catalogRepository)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        FloorCalculationEngine.calculate(_plan.value, catalogRepository)
    )

    val activeProduct: StateFlow<SellzoneProduct> = combine(
        _activeSku,
        catalogRepository.products
    ) { sku, _ ->
        catalogRepository.getProductBySku(sku) ?: catalogRepository.getFlooringTiles().first()
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        catalogRepository.getProductBySku("DUa583") ?: catalogRepository.getAllProducts().first()
    )

    init {
        // Initial setup
        updateUndoRedoState()
    }

    private fun pushHistory(previousPlan: GaragePlan) {
        undoStack.add(previousPlan)
        if (undoStack.size > 50) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateUndoRedoState()
    }

    private fun updateUndoRedoState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(_plan.value)
            _plan.value = previous
            updateUndoRedoState()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(_plan.value)
            _plan.value = next
            updateUndoRedoState()
        }
    }

    fun setActiveTool(tool: DesignTool) {
        _activeTool.value = tool
    }

    fun setActiveProduct(product: SellzoneProduct) {
        _activeSku.value = product.sku
        if (_activeTool.value == DesignTool.ERASER) {
            _activeTool.value = DesignTool.PEN
        }
    }

    fun toggleCurrency() {
        _showPricesInBgn.value = !_showPricesInBgn.value
    }

    fun setPanOffset(x: Float, y: Float) {
        _panOffsetX.value = x
        _panOffsetY.value = y
    }

    fun zoomIn() {
        _zoomScale.value = minOf(2.5f, _zoomScale.value + 0.15f)
    }

    fun zoomOut() {
        _zoomScale.value = maxOf(0.4f, _zoomScale.value - 0.15f)
    }

    fun resetZoom() {
        _zoomScale.value = 1.0f
        _panOffsetX.value = 30f
        _panOffsetY.value = 30f
    }

    fun handleCellClick(col: Int, row: Int) {
        val currentPlan = _plan.value
        when (_activeTool.value) {
            DesignTool.PEN -> {
                pushHistory(currentPlan)
                _plan.value = currentPlan.setCellSku(col, row, _activeSku.value)
            }
            DesignTool.ERASER -> {
                pushHistory(currentPlan)
                _plan.value = currentPlan.setCellSku(col, row, null)
            }
            DesignTool.ROW -> {
                pushHistory(currentPlan)
                val updates = mutableMapOf<Int, String?>()
                for (c in 0 until currentPlan.cols) {
                    updates[row * currentPlan.cols + c] = _activeSku.value
                }
                _plan.value = currentPlan.batchSetCells(updates)
            }
            DesignTool.COLUMN -> {
                pushHistory(currentPlan)
                val updates = mutableMapOf<Int, String?>()
                for (r in 0 until currentPlan.rows) {
                    updates[r * currentPlan.cols + col] = _activeSku.value
                }
                _plan.value = currentPlan.batchSetCells(updates)
            }
            DesignTool.RECTANGLE -> {
                // Handled in onRectPainted
            }
            else -> {
                pushHistory(currentPlan)
                _plan.value = currentPlan.setCellSku(col, row, _activeSku.value)
            }
        }
    }

    fun handleRectPaint(startCol: Int, startRow: Int, endCol: Int, endRow: Int) {
        val currentPlan = _plan.value
        pushHistory(currentPlan)
        val updates = mutableMapOf<Int, String?>()
        val fillSku = if (_activeTool.value == DesignTool.ERASER) null else _activeSku.value
        for (r in startRow..endRow) {
            for (c in startCol..endCol) {
                updates[r * currentPlan.cols + c] = fillSku
            }
        }
        _plan.value = currentPlan.batchSetCells(updates)
    }

    fun fillAll() {
        val currentPlan = _plan.value
        pushHistory(currentPlan)
        val updates = mutableMapOf<Int, String?>()
        for (idx in 0 until currentPlan.totalCells) {
            updates[idx] = _activeSku.value
        }
        _plan.value = currentPlan.batchSetCells(updates)
    }

    fun applyCheckerboard(primarySku: String, secondarySku: String) {
        val currentPlan = _plan.value
        pushHistory(currentPlan)
        val updates = mutableMapOf<Int, String?>()
        for (r in 0 until currentPlan.rows) {
            for (c in 0 until currentPlan.cols) {
                val idx = r * currentPlan.cols + c
                updates[idx] = if ((r + c) % 2 == 0) primarySku else secondarySku
            }
        }
        _plan.value = currentPlan.batchSetCells(updates)
    }

    fun applyBorderPattern(borderSku: String, innerSku: String) {
        val currentPlan = _plan.value
        pushHistory(currentPlan)
        val updates = mutableMapOf<Int, String?>()
        for (r in 0 until currentPlan.rows) {
            for (c in 0 until currentPlan.cols) {
                val idx = r * currentPlan.cols + c
                if (r == 0 || r == currentPlan.rows - 1 || c == 0 || c == currentPlan.cols - 1) {
                    updates[idx] = borderSku
                } else {
                    updates[idx] = innerSku
                }
            }
        }
        _plan.value = currentPlan.batchSetCells(updates)
    }

    fun applyParkingBay(accentSku: String, centerSku: String, outerSku: String) {
        val currentPlan = _plan.value
        pushHistory(currentPlan)
        val updates = mutableMapOf<Int, String?>()
        val rows = currentPlan.rows
        val cols = currentPlan.cols

        val bayStartCol = maxOf(1, (cols * 0.22).toInt())
        val bayEndCol = minOf(cols - 2, (cols * 0.78).toInt())
        val bayStartRow = maxOf(1, (rows * 0.20).toInt())
        val bayEndRow = minOf(rows - 2, (rows * 0.80).toInt())

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                if (r in bayStartRow..bayEndRow && c in bayStartCol..bayEndCol) {
                    if (r == bayStartRow || r == bayEndRow || c == bayStartCol || c == bayEndCol) {
                        updates[idx] = accentSku // Accent line
                    } else {
                        updates[idx] = centerSku // Center bay
                    }
                } else {
                    updates[idx] = outerSku // Outer garage floor
                }
            }
        }
        _plan.value = currentPlan.batchSetCells(updates)
    }

    fun applyStripes(sku1: String, sku2: String, isHorizontal: Boolean) {
        val currentPlan = _plan.value
        pushHistory(currentPlan)
        val updates = mutableMapOf<Int, String?>()
        for (r in 0 until currentPlan.rows) {
            for (c in 0 until currentPlan.cols) {
                val idx = r * currentPlan.cols + c
                val useFirst = if (isHorizontal) (r % 2 == 0) else (c % 2 == 0)
                updates[idx] = if (useFirst) sku1 else sku2
            }
        }
        _plan.value = currentPlan.batchSetCells(updates)
    }

    fun clearFloor() {
        pushHistory(_plan.value)
        _plan.value = _plan.value.clearGrid()
    }

    fun setDimensions(widthM: Double, lengthM: Double, tileSizeMm: Int) {
        pushHistory(_plan.value)
        _plan.value = _plan.value.copy(
            widthM = widthM,
            lengthM = lengthM,
            tileSizeMm = tileSizeMm,
            grid = emptyMap() // Reset grid for new dimensions to ensure geometric accuracy
        )
        resetZoom()
    }

    fun setWastePercentage(waste: Int) {
        _plan.value = _plan.value.copy(wastePercentage = waste)
    }

    fun setEdgeMode(mode: EdgeCoverageMode) {
        _plan.value = _plan.value.copy(edgeMode = mode)
    }

    fun setSelectedEdgeSku(sku: String) {
        _plan.value = _plan.value.copy(selectedEdgeSku = sku)
    }

    fun setSelectedCornerSku(sku: String) {
        _plan.value = _plan.value.copy(selectedCornerSku = sku)
    }

    fun setIncludeLed(include: Boolean) {
        _plan.value = _plan.value.copy(includeLedChannel = include)
    }

    fun setManualEdgeQty(qty: Int) {
        _plan.value = _plan.value.copy(manualEdgeQty = qty)
    }

    fun setManualCornerQty(qty: Int) {
        _plan.value = _plan.value.copy(manualCornerQty = qty)
    }

    fun setManualLedQty(qty: Int) {
        _plan.value = _plan.value.copy(manualLedQty = qty)
    }

    fun refreshPrices() {
        viewModelScope.launch {
            catalogRepository.refreshPricesFromSellzone()
        }
    }

    fun saveCurrentProject(name: String) {
        viewModelScope.launch {
            val saved = projectRepository.saveProject(name, _plan.value, _currentProjectId.value)
            _projectName.value = saved.name
            _currentProjectId.value = saved.id
        }
    }

    fun loadProject(project: SavedProject) {
        pushHistory(_plan.value)
        _plan.value = projectRepository.restoreGaragePlan(project)
        _projectName.value = project.name
        _currentProjectId.value = project.id
        resetZoom()
    }

    fun duplicateProject(project: SavedProject) {
        viewModelScope.launch {
            val duplicated = projectRepository.duplicateProject(project)
            loadProject(duplicated)
        }
    }

    fun renameProject(id: String, newName: String) {
        viewModelScope.launch {
            projectRepository.renameProject(id, newName)
            if (_currentProjectId.value == id) {
                _projectName.value = newName
            }
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(id)
            if (_currentProjectId.value == id) {
                _currentProjectId.value = null
            }
        }
    }

    private fun createInitialPlan(): GaragePlan {
        // Standard test dimensions requested in prompt: 10.0m x 9.2m with 400mm tiles
        // 10m / 0.4m = 25 cols, 9.2m / 0.4m = 23 rows
        val cols = 25
        val rows = 23
        val map = mutableMapOf<Int, String>()

        // Pre-paint a professional test floor pattern
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                if (r == 0 || r == rows - 1 || c == 0 || c == cols - 1) {
                    map[idx] = "DUa583" // Black perimeter
                } else if (r in 5..17 && c in 6..18) {
                    map[idx] = if ((r + c) % 2 == 0) "DUa584" else "DUa583" // Checkered bay
                } else if (r in 4..18 && (c == 5 || c == 19) || c in 5..19 && (r == 4 || r == 18)) {
                    map[idx] = "DUa586" // Red accent frame
                } else {
                    map[idx] = "DUa590" // Light grey floor
                }
            }
        }

        return GaragePlan(
            widthM = 10.0,
            lengthM = 9.2,
            tileSizeMm = 400,
            grid = map,
            wastePercentage = 5,
            edgeMode = EdgeCoverageMode.ENTRANCE_ONLY,
            selectedEdgeSku = "DUa587",
            selectedCornerSku = "DUa589",
            selectedLedSku = "DUa631",
            includeLedChannel = true
        )
    }
}
