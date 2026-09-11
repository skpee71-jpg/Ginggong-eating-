package com.example.ginggong.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ginggong.data.model.FoodLog
import com.example.ginggong.data.model.MealType
import com.example.ginggong.data.model.ScanNutritionResult
import com.example.ginggong.ui.viewmodel.ScanState

@Composable
fun FoodDiaryScreen(
    foodLogs: List<FoodLog>,
    scanState: ScanState,
    onAddFoodLog: (
        mealType: MealType,
        title: String,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float,
        satFat: Float,
        fiber: Float,
        sugar: Float,
        sodium: Float,
        servingSize: String
    ) -> Unit,
    onDeleteFoodLog: (Long) -> Unit,
    onAnalyzeFoodPhoto: (Bitmap) -> Unit,
    onAnalyzeNutritionLabelPhoto: (Bitmap) -> Unit,
    onLookupBarcode: (String) -> Unit,
    onResetScanState: () -> Unit
) {
    val context = LocalContext.current

    var selectedMealType by remember { mutableStateOf(MealType.BREAKFAST) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var showBarcodeManualDialog by remember { mutableStateOf(false) }
    var barcodeInput by remember { mutableStateOf("8850123456789") }

    // Custom food add fields
    var foodName by remember { mutableStateOf("") }
    var caloriesStr by remember { mutableStateOf("") }
    var proteinStr by remember { mutableStateOf("") }
    var carbsStr by remember { mutableStateOf("") }
    var fatStr by remember { mutableStateOf("") }
    var satFatStr by remember { mutableStateOf("") }
    var fiberStr by remember { mutableStateOf("") }
    var sugarStr by remember { mutableStateOf("") }
    var sodiumStr by remember { mutableStateOf("") }
    var servingSize by remember { mutableStateOf("1 portion") }

    var pendingScanMode by remember { mutableStateOf("FOOD_AI") } // "FOOD_AI" or "NUTRITION_LABEL"

    // Photo pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }

            if (pendingScanMode == "FOOD_AI") {
                onAnalyzeFoodPhoto(bitmap)
            } else {
                onAnalyzeNutritionLabelPhoto(bitmap)
            }
        }
    }

    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            if (pendingScanMode == "FOOD_AI") {
                onAnalyzeFoodPhoto(it)
            } else {
                onAnalyzeNutritionLabelPhoto(it)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            try {
                cameraCaptureLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission denied. Use Gallery to upload photos.", Toast.LENGTH_LONG).show()
        }
    }

    fun launchCameraOrRequestPermission() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            try {
                cameraCaptureLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "Unable to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Handle Scan Success Dialog
    if (scanState is ScanState.Success) {
        val res = scanState.result
        var titleEdit by remember { mutableStateOf(res.title) }
        var calEdit by remember { mutableStateOf(res.calories.toString()) }
        var proEdit by remember { mutableStateOf(res.proteinG.toString()) }
        var carbEdit by remember { mutableStateOf(res.carbsG.toString()) }
        var fatEdit by remember { mutableStateOf(res.fatG.toString()) }
        var satFatEdit by remember { mutableStateOf(res.saturatedFatG.toString()) }
        var fiberEdit by remember { mutableStateOf(res.fiberG.toString()) }
        var sugarEdit by remember { mutableStateOf(res.sugarG.toString()) }
        var sodEdit by remember { mutableStateOf(res.sodiumMg.toString()) }
        var servingEdit by remember { mutableStateOf(res.servingSize) }

        AlertDialog(
            onDismissRequest = onResetScanState,
            title = {
                Text(
                    text = when (scanState.mode) {
                        "FOOD_AI" -> "🤖 AI Food Recognized (${res.confidence}% Conf)"
                        "NUTRITION_LABEL" -> "📋 Nutrition Label Scanned"
                        else -> "📦 Barcode Item Found"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = titleEdit,
                        onValueChange = { titleEdit = it },
                        label = { Text("Item Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = calEdit,
                            onValueChange = { calEdit = it },
                            label = { Text("Calories (kcal)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = servingEdit,
                            onValueChange = { servingEdit = it },
                            label = { Text("Serving Size") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = proEdit,
                            onValueChange = { proEdit = it },
                            label = { Text("Protein (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = carbEdit,
                            onValueChange = { carbEdit = it },
                            label = { Text("Carbs (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fatEdit,
                            onValueChange = { fatEdit = it },
                            label = { Text("Fat (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = satFatEdit,
                            onValueChange = { satFatEdit = it },
                            label = { Text("Sat Fat (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fiberEdit,
                            onValueChange = { fiberEdit = it },
                            label = { Text("Fiber (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sugarEdit,
                            onValueChange = { sugarEdit = it },
                            label = { Text("Sugar (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddFoodLog(
                            selectedMealType,
                            titleEdit,
                            calEdit.toIntOrNull() ?: 200,
                            proEdit.toFloatOrNull() ?: 10f,
                            carbEdit.toFloatOrNull() ?: 20f,
                            fatEdit.toFloatOrNull() ?: 5f,
                            satFatEdit.toFloatOrNull() ?: 1f,
                            fiberEdit.toFloatOrNull() ?: 2f,
                            sugarEdit.toFloatOrNull() ?: 4f,
                            sodEdit.toFloatOrNull() ?: 150f,
                            servingEdit
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                ) {
                    Text("Save to $selectedMealType")
                }
            },
            dismissButton = {
                TextButton(onClick = onResetScanState) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBarcodeManualDialog) {
        AlertDialog(
            onDismissRequest = { showBarcodeManualDialog = false },
            title = { Text("Barcode Scanner") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Scan or enter barcode number to fetch nutrition facts automatically:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = barcodeInput,
                        onValueChange = { barcodeInput = it },
                        label = { Text("Barcode String / EAN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBarcodeManualDialog = false
                        onLookupBarcode(barcodeInput)
                    }
                ) {
                    Text("Lookup Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBarcodeManualDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showManualAddDialog) {
        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text("Add Food Item manually") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text("Food Name") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = caloriesStr,
                            onValueChange = { caloriesStr = it },
                            label = { Text("Calories") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = servingSize,
                            onValueChange = { servingSize = it },
                            label = { Text("Serving") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = proteinStr,
                            onValueChange = { proteinStr = it },
                            label = { Text("Protein (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = carbsStr,
                            onValueChange = { carbsStr = it },
                            label = { Text("Carbs (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fatStr,
                            onValueChange = { fatStr = it },
                            label = { Text("Fat (g)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddFoodLog(
                            selectedMealType,
                            foodName.ifEmpty { "Custom Dish" },
                            caloriesStr.toIntOrNull() ?: 250,
                            proteinStr.toFloatOrNull() ?: 12f,
                            carbsStr.toFloatOrNull() ?: 30f,
                            fatStr.toFloatOrNull() ?: 8f,
                            0f, 2f, 4f, 200f,
                            servingSize
                        )
                        showManualAddDialog = false
                    }
                ) {
                    Text("Add to Diary")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Scanner Action Cards
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "📸 Smart AI & Label Scanners",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0066FF)
                    )
                    Text(
                        text = "Snap a food photo, scan product barcodes, or read nutrition facts labels instantly.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (scanState is ScanState.Loading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF0066FF)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Analyzing image with AI...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Button 1: AI Food Recognition
                            Button(
                                onClick = {
                                    pendingScanMode = "FOOD_AI"
                                    launchCameraOrRequestPermission()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0066FF))
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Food Recognition (Photo)")
                            }

                            // Button 2: Nutrition Label Scanner (NEW)
                            Button(
                                onClick = {
                                    pendingScanMode = "NUTRITION_LABEL"
                                    launchCameraOrRequestPermission()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0052D4))
                            ) {
                                Icon(imageVector = Icons.Default.MedicalInformation, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Nutrition Facts Label Scanner (OCR)")
                            }

                            // Button 3: Barcode Scanner
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showBarcodeManualDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4364F7))
                                ) {
                                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Barcode")
                                }

                                Button(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gallery", color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            // Meal Selector Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedMealType.ordinal,
                edgePadding = 0.dp
            ) {
                MealType.values().forEach { meal ->
                    Tab(
                        selected = selectedMealType == meal,
                        onClick = { selectedMealType = meal },
                        text = {
                            Text(
                                text = meal.displayName,
                                fontWeight = if (selectedMealType == meal) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        val filteredLogs = foodLogs.filter { it.mealType == selectedMealType }
        val mealCaloriesSum = filteredLogs.sumOf { it.calories }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${selectedMealType.displayName} Logged",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total: $mealCaloriesSum kcal",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = { showManualAddDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item")
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items logged for ${selectedMealType.displayName} today. Use AI Photo scan or Add Item above!",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(filteredLogs) { log ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.foodName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${log.servingSize} • ${log.calories} kcal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0066FF)
                            )
                            Text(
                                text = "P: ${log.proteinG.toInt()}g | C: ${log.carbsG.toInt()}g | F: ${log.fatG.toInt()}g | Fiber: ${log.fiberG.toInt()}g | Sugar: ${log.sugarG.toInt()}g",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        IconButton(onClick = { onDeleteFoodLog(log.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}
