package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldGreenAccent
import com.example.ui.theme.EmeraldGreenContainer
import com.example.ui.theme.GraphiteBorder
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.GraphiteSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WorkshopAmber
import kotlin.math.ceil

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DimensionsDialog(
    initialWidthM: Double,
    initialLengthM: Double,
    initialTileSizeMm: Int,
    onConfirm: (widthM: Double, lengthM: Double, tileSizeMm: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var widthStr by remember { mutableStateOf(initialWidthM.toString()) }
    var lengthStr by remember { mutableStateOf(initialLengthM.toString()) }
    var selectedTileSizeMm by remember { mutableIntStateOf(initialTileSizeMm) }

    val widthVal = widthStr.replace(",", ".").toDoubleOrNull() ?: initialWidthM
    val lengthVal = lengthStr.replace(",", ".").toDoubleOrNull() ?: initialLengthM

    val calculatedCols = maxOf(1, ceil((widthVal * 1000.0) / selectedTileSizeMm).toInt())
    val calculatedRows = maxOf(1, ceil((lengthVal * 1000.0) / selectedTileSizeMm).toInt())
    val calculatedTotalCells = calculatedCols * calculatedRows
    val calculatedAreaM2 = (widthVal * lengthVal * 100).toLong() / 100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Размери на гаража и решетка",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Бързи шаблони:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Presets
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val presets = listOf(
                        Triple("10.0 × 9.2 м (Тестов)", 10.0, 9.2),
                        Triple("6.0 × 4.0 м (Двоен)", 6.0, 4.0),
                        Triple("5.0 × 3.0 м (Единичен)", 5.0, 3.0),
                        Triple("8.0 × 6.0 м (Сервиз)", 8.0, 6.0)
                    )
                    presets.forEach { (label, w, l) ->
                        Surface(
                            modifier = Modifier.clickable {
                                widthStr = w.toString()
                                lengthStr = l.toString()
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (widthVal == w && lengthVal == l) EmeraldGreenContainer else GraphiteSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (widthVal == w && lengthVal == l) EmeraldGreenAccent else GraphiteBorder
                            )
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (widthVal == w && lengthVal == l) EmeraldGreenAccent else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Width & Length Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = widthStr,
                        onValueChange = { widthStr = it },
                        label = { Text("Ширина (м)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_width"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldGreenAccent,
                            unfocusedBorderColor = GraphiteBorder,
                            focusedLabelColor = EmeraldGreenAccent,
                            unfocusedLabelColor = TextMuted
                        ),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = lengthStr,
                        onValueChange = { lengthStr = it },
                        label = { Text("Дължина (м)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_length"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = EmeraldGreenAccent,
                            unfocusedBorderColor = GraphiteBorder,
                            focusedLabelColor = EmeraldGreenAccent,
                            unfocusedLabelColor = TextMuted
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tile size selection
                Text(
                    "Размер на модулните плочки:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTileSizeMm == 400,
                        onClick = { selectedTileSizeMm = 400 },
                        label = {
                            Text("400 × 400 мм (Стандартни)", fontSize = 11.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreenContainer,
                            selectedLabelColor = EmeraldGreenAccent,
                            containerColor = GraphiteSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("tile_size_400")
                    )

                    FilterChip(
                        selected = selectedTileSizeMm == 500,
                        onClick = { selectedTileSizeMm = 500 },
                        label = {
                            Text("500 × 500 мм (Про / DUa653)", fontSize = 11.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreenContainer,
                            selectedLabelColor = EmeraldGreenAccent,
                            containerColor = GraphiteSurfaceVariant,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("tile_size_500")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Calculated summary box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = GraphiteSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Обща площ:", color = TextSecondary, fontSize = 12.sp)
                            Text("$calculatedAreaM2 м²", color = EmeraldGreenAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Решетка на пода:", color = TextSecondary, fontSize = 12.sp)
                            Text("$calculatedCols колони × $calculatedRows реда", color = TextPrimary, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Общ брой плочки:", color = TextSecondary, fontSize = 12.sp)
                            Text("$calculatedTotalCells броя", color = WorkshopAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (widthVal > 0.0 && lengthVal > 0.0) {
                        onConfirm(widthVal, lengthVal, selectedTileSizeMm)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldGreenAccent,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("btn_confirm_dimensions")
            ) {
                Text("Генерирай под", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отказ", color = TextSecondary)
            }
        },
        containerColor = GraphiteSurface
    )
}
