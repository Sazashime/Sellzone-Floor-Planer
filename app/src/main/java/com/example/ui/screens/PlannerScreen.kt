package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.SyncStatus
import com.example.ui.components.AccessoriesConfigSection
import com.example.ui.components.CatalogSelector
import com.example.ui.components.DesignToolbar
import com.example.ui.components.DimensionsDialog
import com.example.ui.components.GarageGridCanvas
import com.example.ui.components.ProjectsManagerDialog
import com.example.ui.components.ShoppingListView
import com.example.ui.theme.EmeraldGreenAccent
import com.example.ui.theme.EmeraldGreenContainer
import com.example.ui.theme.GraphiteBackground
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceElevated
import com.example.ui.theme.GraphiteSurfaceVariant
import com.example.ui.theme.MetallicTitanium
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningRed
import com.example.ui.theme.WorkshopAmber
import com.example.ui.viewmodel.PlannerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PlannerTab(val title: String, val icon: ImageVector) {
    EDITOR("Под", Icons.Default.GridOn),
    CATALOG("Каталог", Icons.Default.Layers),
    TOOLS("Модели", Icons.Default.Build),
    ACCESSORIES("Кантове", Icons.Default.ViewInAr),
    CALCULATION("Сметка", Icons.Default.ShoppingCart),
    PROJECTS("Проекти", Icons.Default.Folder)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel = viewModel()
) {
    val context = LocalContext.current
    val plan by viewModel.plan.collectAsState()
    val calculation by viewModel.calculation.collectAsState()
    val activeTool by viewModel.activeTool.collectAsState()
    val activeSku by viewModel.activeSku.collectAsState()
    val activeProduct by viewModel.activeProduct.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val panOffsetX by viewModel.panOffsetX.collectAsState()
    val panOffsetY by viewModel.panOffsetY.collectAsState()
    val showPricesInBgn by viewModel.showPricesInBgn.collectAsState()
    val projectName by viewModel.projectName.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val catalogProducts by viewModel.catalogRepository.products.collectAsState()
    val syncStatus by viewModel.catalogRepository.syncStatus.collectAsState()
    val lastSyncTime by viewModel.catalogRepository.lastSyncTimestamp.collectAsState()
    val savedProjects by viewModel.projectRepository.savedProjects.collectAsState()

    var selectedTab by remember { mutableStateOf(PlannerTab.EDITOR) }
    var showDimensionsDialog by remember { mutableStateOf(false) }
    var showProjectsDialog by remember { mutableStateOf(false) }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Toast notifications on sync status changes
    LaunchedEffect(syncStatus) {
        when (val status = syncStatus) {
            is SyncStatus.Success -> {
                Toast.makeText(context, status.message, Toast.LENGTH_SHORT).show()
            }
            is SyncStatus.Error -> {
                Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(GraphiteBackground)
            .statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = EmeraldGreenContainer,
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreenAccent)
                            ) {
                                Text(
                                    text = "SELLZONE PRO",
                                    color = EmeraldGreenAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = projectName,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "${plan.widthM}×${plan.lengthM} м (${plan.garageAreaM2} м²) • ${plan.cols}×${plan.rows} плочки",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    // Dimensions Dialog Button
                    IconButton(
                        onClick = { showDimensionsDialog = true },
                        modifier = Modifier.testTag("topbar_btn_dimensions")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Промени размери",
                            tint = EmeraldGreenAccent
                        )
                    }

                    // Refresh Live Prices Button
                    IconButton(
                        onClick = { viewModel.refreshPrices() },
                        modifier = Modifier.testTag("topbar_btn_refresh_prices")
                    ) {
                        if (syncStatus is SyncStatus.Syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = EmeraldGreenAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Обнови цени от Sellzone.bg",
                                tint = if (syncStatus is SyncStatus.Success) EmeraldGreenAccent else MetallicTitanium
                            )
                        }
                    }

                    // Currency toggle
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { viewModel.toggleCurrency() }
                            .testTag("topbar_toggle_currency"),
                        color = GraphiteSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
                    ) {
                        Text(
                            text = if (showPricesInBgn) "BGN" else "EUR",
                            color = EmeraldGreenAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Saved projects button
                    IconButton(
                        onClick = { showProjectsDialog = true },
                        modifier = Modifier.testTag("topbar_btn_projects")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Проекти",
                            tint = MetallicTitanium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GraphiteSurface)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = GraphiteSurface,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_navigation_bar")
            ) {
                PlannerTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldGreenAccent,
                            selectedTextColor = EmeraldGreenAccent,
                            unselectedIconColor = MetallicTitanium,
                            unselectedTextColor = TextMuted,
                            indicatorColor = EmeraldGreenContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GraphiteBackground)
        ) {
            val isWideScreen = maxWidth >= 760.dp

            if (isWideScreen) {
                // Wide tablet / landscape layout: Split editor on left, inspector/tools on right
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1.3f)) {
                        EditorWorkspace(
                            plan = plan,
                            viewModel = viewModel,
                            activeTool = activeTool,
                            activeSku = activeSku,
                            zoomScale = zoomScale,
                            panOffsetX = panOffsetX,
                            panOffsetY = panOffsetY,
                            activeProduct = activeProduct,
                            allTileProducts = viewModel.catalogRepository.getFlooringTiles(),
                            canUndo = canUndo,
                            canRedo = canRedo
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(GraphiteSurface)
                            .border(androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder))
                    ) {
                        when (selectedTab) {
                            PlannerTab.EDITOR, PlannerTab.TOOLS -> {
                                DesignToolbar(
                                    activeTool = activeTool,
                                    onToolSelected = { viewModel.setActiveTool(it) },
                                    activeProduct = activeProduct,
                                    allTileProducts = viewModel.catalogRepository.getFlooringTiles(),
                                    canUndo = canUndo,
                                    canRedo = canRedo,
                                    onUndo = { viewModel.undo() },
                                    onRedo = { viewModel.redo() },
                                    onFillAll = { viewModel.fillAll() },
                                    onApplyCheckerboard = { p, s -> viewModel.applyCheckerboard(p, s) },
                                    onApplyBorderPattern = { b, i -> viewModel.applyBorderPattern(b, i) },
                                    onApplyParkingBay = { a, c, o -> viewModel.applyParkingBay(a, c, o) },
                                    onApplyStripes = { s1, s2, h -> viewModel.applyStripes(s1, s2, h) },
                                    onClearFloor = { viewModel.clearFloor() }
                                )
                            }
                            PlannerTab.CATALOG -> {
                                CatalogSelector(
                                    products = catalogProducts,
                                    selectedSku = activeSku,
                                    onProductSelected = { viewModel.setActiveProduct(it) },
                                    showPricesInBgn = showPricesInBgn
                                )
                            }
                            PlannerTab.ACCESSORIES -> {
                                AccessoriesConfigSection(
                                    plan = plan,
                                    calculation = calculation,
                                    repository = viewModel.catalogRepository,
                                    onEdgeModeChanged = { viewModel.setEdgeMode(it) },
                                    onEdgeSkuSelected = { viewModel.setSelectedEdgeSku(it) },
                                    onCornerSkuSelected = { viewModel.setSelectedCornerSku(it) },
                                    onLedToggled = { viewModel.setIncludeLed(it) },
                                    onManualEdgeQtyChanged = { viewModel.setManualEdgeQty(it) },
                                    onManualCornerQtyChanged = { viewModel.setManualCornerQty(it) },
                                    onManualLedQtyChanged = { viewModel.setManualLedQty(it) },
                                    showPricesInBgn = showPricesInBgn
                                )
                            }
                            PlannerTab.CALCULATION -> {
                                ShoppingListView(
                                    calculation = calculation,
                                    wastePercentage = plan.wastePercentage,
                                    onWastePercentageChanged = { viewModel.setWastePercentage(it) },
                                    showPricesInBgn = showPricesInBgn,
                                    onToggleCurrency = { viewModel.toggleCurrency() }
                                )
                            }
                            PlannerTab.PROJECTS -> {
                                ProjectsContent(
                                    projects = savedProjects,
                                    currentProjectName = projectName,
                                    viewModel = viewModel,
                                    onDismiss = {}
                                )
                            }
                        }
                    }
                }
            } else {
                // Mobile layout: Tab content switching
                when (selectedTab) {
                    PlannerTab.EDITOR -> {
                        EditorWorkspace(
                            plan = plan,
                            viewModel = viewModel,
                            activeTool = activeTool,
                            activeSku = activeSku,
                            zoomScale = zoomScale,
                            panOffsetX = panOffsetX,
                            panOffsetY = panOffsetY,
                            activeProduct = activeProduct,
                            allTileProducts = viewModel.catalogRepository.getFlooringTiles(),
                            canUndo = canUndo,
                            canRedo = canRedo
                        )
                    }
                    PlannerTab.CATALOG -> {
                        CatalogSelector(
                            products = catalogProducts,
                            selectedSku = activeSku,
                            onProductSelected = {
                                viewModel.setActiveProduct(it)
                                selectedTab = PlannerTab.EDITOR
                            },
                            showPricesInBgn = showPricesInBgn
                        )
                    }
                    PlannerTab.TOOLS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "Инструменти и шаблони за дизайн",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                            DesignToolbar(
                                activeTool = activeTool,
                                onToolSelected = {
                                    viewModel.setActiveTool(it)
                                    selectedTab = PlannerTab.EDITOR
                                },
                                activeProduct = activeProduct,
                                allTileProducts = viewModel.catalogRepository.getFlooringTiles(),
                                canUndo = canUndo,
                                canRedo = canRedo,
                                onUndo = { viewModel.undo() },
                                onRedo = { viewModel.redo() },
                                onFillAll = {
                                    viewModel.fillAll()
                                    selectedTab = PlannerTab.EDITOR
                                },
                                onApplyCheckerboard = { p, s ->
                                    viewModel.applyCheckerboard(p, s)
                                    selectedTab = PlannerTab.EDITOR
                                },
                                onApplyBorderPattern = { b, i ->
                                    viewModel.applyBorderPattern(b, i)
                                    selectedTab = PlannerTab.EDITOR
                                },
                                onApplyParkingBay = { a, c, o ->
                                    viewModel.applyParkingBay(a, c, o)
                                    selectedTab = PlannerTab.EDITOR
                                },
                                onApplyStripes = { s1, s2, h ->
                                    viewModel.applyStripes(s1, s2, h)
                                    selectedTab = PlannerTab.EDITOR
                                },
                                onClearFloor = {
                                    viewModel.clearFloor()
                                    selectedTab = PlannerTab.EDITOR
                                }
                            )
                        }
                    }
                    PlannerTab.ACCESSORIES -> {
                        AccessoriesConfigSection(
                            plan = plan,
                            calculation = calculation,
                            repository = viewModel.catalogRepository,
                            onEdgeModeChanged = { viewModel.setEdgeMode(it) },
                            onEdgeSkuSelected = { viewModel.setSelectedEdgeSku(it) },
                            onCornerSkuSelected = { viewModel.setSelectedCornerSku(it) },
                            onLedToggled = { viewModel.setIncludeLed(it) },
                            onManualEdgeQtyChanged = { viewModel.setManualEdgeQty(it) },
                            onManualCornerQtyChanged = { viewModel.setManualCornerQty(it) },
                            onManualLedQtyChanged = { viewModel.setManualLedQty(it) },
                            showPricesInBgn = showPricesInBgn
                        )
                    }
                    PlannerTab.CALCULATION -> {
                        ShoppingListView(
                            calculation = calculation,
                            wastePercentage = plan.wastePercentage,
                            onWastePercentageChanged = { viewModel.setWastePercentage(it) },
                            showPricesInBgn = showPricesInBgn,
                            onToggleCurrency = { viewModel.toggleCurrency() }
                        )
                    }
                    PlannerTab.PROJECTS -> {
                        ProjectsContent(
                            projects = savedProjects,
                            currentProjectName = projectName,
                            viewModel = viewModel,
                            onDismiss = { selectedTab = PlannerTab.EDITOR }
                        )
                    }
                }
            }
        }
    }

    // --- Dialogs ---
    if (showDimensionsDialog) {
        DimensionsDialog(
            initialWidthM = plan.widthM,
            initialLengthM = plan.lengthM,
            initialTileSizeMm = plan.tileSizeMm,
            onConfirm = { w, l, s ->
                viewModel.setDimensions(w, l, s)
                showDimensionsDialog = false
            },
            onDismiss = { showDimensionsDialog = false }
        )
    }

    if (showProjectsDialog) {
        ProjectsManagerDialog(
            projects = savedProjects,
            currentProjectName = projectName,
            onSaveCurrent = { viewModel.saveCurrentProject(it) },
            onLoadProject = { viewModel.loadProject(it) },
            onDuplicateProject = { viewModel.duplicateProject(it) },
            onRenameProject = { id, newName -> viewModel.renameProject(id, newName) },
            onDeleteProject = { viewModel.deleteProject(it) },
            onDismiss = { showProjectsDialog = false }
        )
    }
}

@Composable
fun EditorWorkspace(
    plan: com.example.data.model.GaragePlan,
    viewModel: PlannerViewModel,
    activeTool: com.example.ui.components.DesignTool,
    activeSku: String,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    activeProduct: com.example.data.model.SellzoneProduct,
    allTileProducts: List<com.example.data.model.SellzoneProduct>,
    canUndo: Boolean,
    canRedo: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main Scaled Canvas
        GarageGridCanvas(
            plan = plan,
            catalogRepository = viewModel.catalogRepository,
            activeTool = activeTool,
            activeSku = activeSku,
            zoomScale = zoomScale,
            panOffsetX = panOffsetX,
            panOffsetY = panOffsetY,
            onPanChanged = { x, y -> viewModel.setPanOffset(x, y) },
            onCellClicked = { c, r -> viewModel.handleCellClick(c, r) },
            onRectPainted = { sc, sr, ec, er -> viewModel.handleRectPaint(sc, sr, ec, er) },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Quick Tools Bar at top of canvas
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            DesignToolbar(
                activeTool = activeTool,
                onToolSelected = { viewModel.setActiveTool(it) },
                activeProduct = activeProduct,
                allTileProducts = allTileProducts,
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onFillAll = { viewModel.fillAll() },
                onApplyCheckerboard = { p, s -> viewModel.applyCheckerboard(p, s) },
                onApplyBorderPattern = { b, i -> viewModel.applyBorderPattern(b, i) },
                onApplyParkingBay = { a, c, o -> viewModel.applyParkingBay(a, c, o) },
                onApplyStripes = { s1, s2, h -> viewModel.applyStripes(s1, s2, h) },
                onClearFloor = { viewModel.clearFloor() }
            )
        }

        // Active Product Indicator badge (bottom left of canvas)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            shape = RoundedCornerShape(8.dp),
            color = GraphiteSurfaceElevated.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(activeProduct.colorHex))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                )
                Column {
                    Text(
                        text = "Активен: ${activeProduct.sku} (${activeProduct.colorName})",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Инструмент: ${activeTool.labelBg}",
                        color = EmeraldGreenAccent,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Zoom controls (bottom right of canvas)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
            shape = RoundedCornerShape(8.dp),
            color = GraphiteSurfaceElevated.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.zoomOut() },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("zoom_out_btn")
                ) {
                    Icon(Icons.Default.Remove, "Намали", tint = TextPrimary, modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "${(zoomScale * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { viewModel.resetZoom() }
                )

                IconButton(
                    onClick = { viewModel.zoomIn() },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("zoom_in_btn")
                ) {
                    Icon(Icons.Default.Add, "Увеличи", tint = TextPrimary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun ProjectsContent(
    projects: List<com.example.data.model.SavedProject>,
    currentProjectName: String,
    viewModel: PlannerViewModel,
    onDismiss: () -> Unit
) {
    ProjectsManagerDialog(
        projects = projects,
        currentProjectName = currentProjectName,
        onSaveCurrent = { viewModel.saveCurrentProject(it) },
        onLoadProject = {
            viewModel.loadProject(it)
            onDismiss()
        },
        onDuplicateProject = { viewModel.duplicateProject(it) },
        onRenameProject = { id, newName -> viewModel.renameProject(id, newName) },
        onDeleteProject = { viewModel.deleteProject(it) },
        onDismiss = onDismiss
    )
}
