package com.example.data.repository

import android.content.Context
import com.example.data.model.EdgeCoverageMode
import com.example.data.model.GaragePlan
import com.example.data.model.SavedProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProjectRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("sellzone_projects_store", Context.MODE_PRIVATE)
    private val _savedProjects = MutableStateFlow<List<SavedProject>>(emptyList())
    val savedProjects: StateFlow<List<SavedProject>> = _savedProjects.asStateFlow()

    init {
        loadProjectsFromStorage()
    }

    private fun loadProjectsFromStorage() {
        val jsonStr = prefs.getString("projects_json", null)
        if (jsonStr.isNullOrEmpty()) {
            val initialDemo = createDefaultDemoProject()
            _savedProjects.value = listOf(initialDemo)
            persistProjects(listOf(initialDemo))
        } else {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<SavedProject>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        SavedProject(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            updatedAt = obj.getLong("updatedAt"),
                            widthM = obj.getDouble("widthM"),
                            lengthM = obj.getDouble("lengthM"),
                            tileSizeMm = obj.getInt("tileSizeMm"),
                            wastePercentage = obj.getInt("wastePercentage"),
                            edgeMode = obj.getString("edgeMode"),
                            selectedEdgeSku = obj.getString("selectedEdgeSku"),
                            selectedCornerSku = obj.getString("selectedCornerSku"),
                            selectedLedSku = obj.getString("selectedLedSku"),
                            includeLedChannel = obj.getBoolean("includeLedChannel"),
                            manualEdgeQty = if (obj.has("manualEdgeQty") && !obj.isNull("manualEdgeQty")) obj.getInt("manualEdgeQty") else null,
                            manualCornerQty = if (obj.has("manualCornerQty") && !obj.isNull("manualCornerQty")) obj.getInt("manualCornerQty") else null,
                            manualLedQty = if (obj.has("manualLedQty") && !obj.isNull("manualLedQty")) obj.getInt("manualLedQty") else null,
                            serializedGrid = obj.getString("serializedGrid")
                        )
                    )
                }
                _savedProjects.value = list
            } catch (e: Exception) {
                val demo = createDefaultDemoProject()
                _savedProjects.value = listOf(demo)
            }
        }
    }

    private fun persistProjects(projects: List<SavedProject>) {
        try {
            val array = JSONArray()
            for (p in projects) {
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("name", p.name)
                obj.put("updatedAt", p.updatedAt)
                obj.put("widthM", p.widthM)
                obj.put("lengthM", p.lengthM)
                obj.put("tileSizeMm", p.tileSizeMm)
                obj.put("wastePercentage", p.wastePercentage)
                obj.put("edgeMode", p.edgeMode)
                obj.put("selectedEdgeSku", p.selectedEdgeSku)
                obj.put("selectedCornerSku", p.selectedCornerSku)
                obj.put("selectedLedSku", p.selectedLedSku)
                obj.put("includeLedChannel", p.includeLedChannel)
                p.manualEdgeQty?.let { obj.put("manualEdgeQty", it) }
                p.manualCornerQty?.let { obj.put("manualCornerQty", it) }
                p.manualLedQty?.let { obj.put("manualLedQty", it) }
                obj.put("serializedGrid", p.serializedGrid)
                array.put(obj)
            }
            prefs.edit().putString("projects_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveProject(name: String, plan: GaragePlan, existingId: String? = null): SavedProject = withContext(Dispatchers.IO) {
        val id = existingId ?: UUID.randomUUID().toString()
        val serializedGrid = serializeGrid(plan.grid)
        val project = SavedProject(
            id = id,
            name = name,
            updatedAt = System.currentTimeMillis(),
            widthM = plan.widthM,
            lengthM = plan.lengthM,
            tileSizeMm = plan.tileSizeMm,
            wastePercentage = plan.wastePercentage,
            edgeMode = plan.edgeMode.name,
            selectedEdgeSku = plan.selectedEdgeSku,
            selectedCornerSku = plan.selectedCornerSku,
            selectedLedSku = plan.selectedLedSku,
            includeLedChannel = plan.includeLedChannel,
            manualEdgeQty = plan.manualEdgeQty,
            manualCornerQty = plan.manualCornerQty,
            manualLedQty = plan.manualLedQty,
            serializedGrid = serializedGrid
        )

        val currentList = _savedProjects.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index >= 0) {
            currentList[index] = project
        } else {
            currentList.add(0, project)
        }
        _savedProjects.value = currentList
        persistProjects(currentList)
        project
    }

    suspend fun duplicateProject(project: SavedProject): SavedProject = withContext(Dispatchers.IO) {
        val newProject = project.copy(
            id = UUID.randomUUID().toString(),
            name = "${project.name} (Копие)",
            updatedAt = System.currentTimeMillis()
        )
        val currentList = _savedProjects.value.toMutableList()
        currentList.add(0, newProject)
        _savedProjects.value = currentList
        persistProjects(currentList)
        newProject
    }

    suspend fun renameProject(id: String, newName: String) = withContext(Dispatchers.IO) {
        val currentList = _savedProjects.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(name = newName, updatedAt = System.currentTimeMillis())
            _savedProjects.value = currentList
            persistProjects(currentList)
        }
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        val currentList = _savedProjects.value.filterNot { it.id == id }
        _savedProjects.value = currentList
        persistProjects(currentList)
    }

    fun restoreGaragePlan(project: SavedProject): GaragePlan {
        val mode = try {
            EdgeCoverageMode.valueOf(project.edgeMode)
        } catch (e: Exception) {
            EdgeCoverageMode.ENTRANCE_ONLY
        }
        return GaragePlan(
            widthM = project.widthM,
            lengthM = project.lengthM,
            tileSizeMm = project.tileSizeMm,
            grid = deserializeGrid(project.serializedGrid),
            wastePercentage = project.wastePercentage,
            edgeMode = mode,
            selectedEdgeSku = project.selectedEdgeSku,
            selectedCornerSku = project.selectedCornerSku,
            selectedLedSku = project.selectedLedSku,
            includeLedChannel = project.includeLedChannel,
            manualEdgeQty = project.manualEdgeQty,
            manualCornerQty = project.manualCornerQty,
            manualLedQty = project.manualLedQty
        )
    }

    private fun serializeGrid(grid: Map<Int, String>): String {
        val sb = StringBuilder()
        var first = true
        for ((idx, sku) in grid) {
            if (!first) sb.append(";")
            sb.append(idx).append(":").append(sku)
            first = false
        }
        return sb.toString()
    }

    private fun deserializeGrid(str: String): Map<Int, String> {
        if (str.isEmpty()) return emptyMap()
        val map = mutableMapOf<Int, String>()
        val pairs = str.split(";")
        for (p in pairs) {
            val parts = p.split(":")
            if (parts.size == 2) {
                val idx = parts[0].toIntOrNull()
                if (idx != null) {
                    map[idx] = parts[1]
                }
            }
        }
        return map
    }

    private fun createDefaultDemoProject(): SavedProject {
        // 10m x 9.2m with 400mm tiles -> 25 cols x 23 rows = 575 tiles
        val cols = 25
        val rows = 23
        val map = mutableMapOf<Int, String>()

        // Generate a striking sport workshop pattern: Black border, checkered center, red parking accent
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                // Border: Black DUa583
                if (r == 0 || r == rows - 1 || c == 0 || c == cols - 1) {
                    map[idx] = "DUa583" // Black
                } else if (r in 5..17 && c in 6..18) {
                    // Central car bay: Checkerboard of Grey DUa584 and Black DUa583
                    if ((r + c) % 2 == 0) {
                        map[idx] = "DUa584" // Grey
                    } else {
                        map[idx] = "DUa583" // Black
                    }
                } else if (r in 4..18 && (c == 5 || c == 19) || c in 5..19 && (r == 4 || r == 18)) {
                    // Accent border line: Red DUa586
                    map[idx] = "DUa586" // Red
                } else {
                    // Outer floor area: Light Grey DUa590
                    map[idx] = "DUa590"
                }
            }
        }

        return SavedProject(
            id = "demo_sport_garage_10x9.2",
            name = "Спортен гараж 10×9.2m (Тестов проект)",
            updatedAt = System.currentTimeMillis(),
            widthM = 10.0,
            lengthM = 9.2,
            tileSizeMm = 400,
            wastePercentage = 5,
            edgeMode = EdgeCoverageMode.ENTRANCE_ONLY.name,
            selectedEdgeSku = "DUa587",
            selectedCornerSku = "DUa589",
            selectedLedSku = "DUa631",
            includeLedChannel = true,
            serializedGrid = serializeGrid(map)
        )
    }
}
