package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavedProject
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsManagerDialog(
    projects: List<SavedProject>,
    currentProjectName: String,
    onSaveCurrent: (projectName: String) -> Unit,
    onLoadProject: (SavedProject) -> Unit,
    onDuplicateProject: (SavedProject) -> Unit,
    onRenameProject: (id: String, newName: String) -> Unit,
    onDeleteProject: (id: String) -> Unit,
    onDismiss: () -> Unit
) {
    var isSavingNew by remember { mutableStateOf(false) }
    var saveNameInput by remember { mutableStateOf(currentProjectName) }

    var renamingProjectId by remember { mutableStateOf<String?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Управление на проекти",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { isSavingNew = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreenAccent,
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    ),
                    modifier = Modifier.testTag("btn_save_new_project")
                ) {
                    Icon(Icons.Default.Save, "Запази", modifier = Modifier.size(16.dp))
                    Text(" Запази текущ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isSavingNew) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = GraphiteSurfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Име на новия проект:", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = saveNameInput,
                                onValueChange = { saveNameInput = it },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_save_project_name"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = EmeraldGreenAccent,
                                    unfocusedBorderColor = GraphiteBorder
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { isSavingNew = false }) {
                                    Text("Отказ", color = TextSecondary)
                                }
                                Button(
                                    onClick = {
                                        if (saveNameInput.isNotBlank()) {
                                            onSaveCurrent(saveNameInput.trim())
                                            isSavingNew = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGreenAccent,
                                        contentColor = androidx.compose.ui.graphics.Color.Black
                                    )
                                ) {
                                    Text("Потвърди запис")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (projects.isEmpty()) {
                    Text(
                        "Няма запазени проекти.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(projects, key = { it.id }) { project ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = GraphiteSurfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GraphiteBorder)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    if (renamingProjectId == project.id) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = renameInput,
                                                onValueChange = { renameInput = it },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = TextPrimary,
                                                    unfocusedTextColor = TextPrimary,
                                                    focusedBorderColor = EmeraldGreenAccent
                                                )
                                            )
                                            IconButton(onClick = {
                                                if (renameInput.isNotBlank()) {
                                                    onRenameProject(project.id, renameInput.trim())
                                                }
                                                renamingProjectId = null
                                            }) {
                                                Icon(Icons.Default.Save, "Запази име", tint = EmeraldGreenAccent)
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = project.name,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                modifier = Modifier.weight(1f)
                                            )

                                            IconButton(
                                                onClick = {
                                                    renamingProjectId = project.id
                                                    renameInput = project.name
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, "Преименувай", tint = MetallicTitanium, modifier = Modifier.size(16.dp))
                                            }

                                            IconButton(
                                                onClick = { onDuplicateProject(project) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, "Дублирай", tint = MetallicTitanium, modifier = Modifier.size(16.dp))
                                            }

                                            IconButton(
                                                onClick = { onDeleteProject(project.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, "Изтрий", tint = WarningRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${project.widthM} × ${project.lengthM} м | ${project.tileSizeMm} мм плочки",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                            Text(
                                                text = "Обновен: ${dateFormat.format(Date(project.updatedAt))}",
                                                color = TextMuted,
                                                fontSize = 10.sp
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                onLoadProject(project)
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EmeraldGreenContainer,
                                                contentColor = EmeraldGreenAccent
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.testTag("btn_load_project_${project.id}")
                                        ) {
                                            Icon(Icons.Default.FolderOpen, "Зареди", modifier = Modifier.size(14.dp))
                                            Text(" Зареди", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Затвори", color = TextSecondary)
            }
        },
        containerColor = GraphiteSurface
    )
}
