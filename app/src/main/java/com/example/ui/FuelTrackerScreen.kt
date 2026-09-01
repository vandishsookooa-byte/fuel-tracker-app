package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.FuelRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelTrackerScreen(
    viewModel: FuelViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val records by viewModel.records.collectAsStateWithLifecycle()

    // Setup Date Picker Dialog
    val calendar = Calendar.getInstance().apply {
        timeInMillis = viewModel.selectedDateMillis
    }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            viewModel.updateDate(year, month, dayOfMonth)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Formatters
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val formattedSelectedDate = dateFormat.format(Date(viewModel.selectedDateMillis))

    // Automatically fill inputs from previous log if empty/first initialization
    LaunchedEffect(records) {
        if (records.isNotEmpty() && viewModel.odometerInput == "325662") {
            viewModel.autofillFromLatest()
        }
    }

    // Dismiss success banner automatically
    LaunchedEffect(viewModel.successNotification) {
        if (viewModel.successNotification != null) {
            kotlinx.coroutines.delay(4000)
            viewModel.successNotification = null
        }
    }

    // Statistics calculations
    val stats = remember(records) {
        val totalSpent = records.sumOf { it.amountSpent }
        val totalLitres = records.sumOf { it.fuelLitres }
        
        // Calculate history-based real vehicle efficiency
        val realEfficiency = if (records.size >= 2) {
            val sorted = records.sortedBy { it.odometerReading }
            val distance = sorted.last().odometerReading - sorted.first().odometerReading
            val fuelExceptLast = sorted.subList(0, sorted.size - 1).sumOf { it.fuelLitres }
            if (fuelExceptLast > 0 && distance > 0) distance / fuelExceptLast else 0.0
        } else {
            0.0
        }

        Triple(totalSpent, totalLitres, realEfficiency)
    }

    val (totalSpent, _, realEfficiency) = stats

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
            ) {
                // Natural Tones Header - Custom inline design matching index.html
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                val currentDayStr = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date())
                                Text(
                                    text = currentDayStr.uppercase(Locale.getDefault()),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.5.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Fuel ",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Light,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Tracker",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            // Avatar bubble matching header
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Fuel nozzle organic icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Odometer Reader Banner Card- styled precisely like HTML Odometer Box
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("odometer_banner_card"),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp)
                        ) {
                            Text(
                                text = "Current Odometer (Baseline)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = String.format("%,.0f", viewModel.parsedOdometer),
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-1).sp,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "km",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Color.White.copy(alpha = 0.4f))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.tertiary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Healthy Engine Status Tracker Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Stats Dashboard Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        StatCard(
                            title = stringResource(id = R.string.stat_total_spent),
                            value = "Rs ${String.format("%,.0f", totalSpent)}",
                            icon = Icons.Default.Info,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = "Vehicle Average",
                            value = if (realEfficiency > 0) "${String.format("%.1f", realEfficiency)} km/L" else "---",
                            subtitle = if (realEfficiency > 0) "Based on history" else "Need 2+ records",
                            icon = Icons.Default.Refresh,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Main Refuel Calculator Form Card - Premium 32dp Rounded Container
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calculator_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(32.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Refueling Parameters",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    ),
                                    shape = RoundedCornerShape(50.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = "Rs ${viewModel.priceInput} / L",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Refuel Date Input Trigger Row (styled elegantly like fields)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable { datePickerDialog.show() }
                                    .padding(vertical = 12.dp, horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Pick Date",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(id = R.string.field_refuel_date),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = formattedSelectedDate,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "CHOOSE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Refueling Amount spent (Rs)
                            OutlinedTextField(
                                value = viewModel.amountInput,
                                onValueChange = { viewModel.amountInput = it },
                                label = { Text(stringResource(id = R.string.field_amount_spent)) },
                                placeholder = { Text("e.g. 1000") },
                                prefix = { Text("Rs ", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("amount_input"),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp),
                                isError = viewModel.parsedAmount <= 0.0 && viewModel.amountInput.isNotEmpty(),
                                trailingIcon = {
                                    if (viewModel.amountInput.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.amountInput = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear Amount")
                                        }
                                    }
                                }
                            )

                            // Quick Preset chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("500", "1000", "1500", "2000").forEach { preset ->
                                    FilterChip(
                                        selected = viewModel.amountInput == preset,
                                        onClick = { viewModel.amountInput = preset },
                                        label = { Text("Rs $preset") },
                                        modifier = Modifier.testTag("preset_$preset")
                                    )
                                }
                            }

                            // Fuel Price (Rs/L)
                            OutlinedTextField(
                                value = viewModel.priceInput,
                                onValueChange = { viewModel.priceInput = it },
                                label = { Text(stringResource(id = R.string.field_fuel_price)) },
                                suffix = { Text("Rs/L", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("price_input"),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp),
                                isError = viewModel.parsedPrice <= 0.0 && viewModel.priceInput.isNotEmpty(),
                                trailingIcon = {
                                    if (viewModel.priceInput != "64.25") {
                                        IconButton(onClick = { viewModel.priceInput = "64.25" }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Reset Price")
                                        }
                                    }
                                }
                            )

                            // Current Odometer Reading (km)
                            OutlinedTextField(
                                value = viewModel.odometerInput,
                                onValueChange = { viewModel.odometerInput = it },
                                label = { Text(stringResource(id = R.string.field_current_odometer)) },
                                suffix = { Text("km", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("odometer_input"),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                shape = RoundedCornerShape(12.dp),
                                isError = viewModel.parsedOdometer <= 0.0 && viewModel.odometerInput.isNotEmpty(),
                                trailingIcon = {
                                    if (viewModel.odometerInput != "325662") {
                                        IconButton(onClick = { viewModel.odometerInput = "325662" }) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Reset Odometer")
                                        }
                                    }
                                }
                            )

                            // Vehicle Fuel Efficiency / Average mileage Slider (km/L)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Assumed vehicle mileage",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${String.format("%.1f", viewModel.parsedEfficiency)} km/L",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = viewModel.parsedEfficiency.toFloat(),
                                    onValueChange = { viewModel.efficiencyInput = String.format("%.1f", it) },
                                    valueRange = 5f..45f,
                                    steps = 79,
                                    modifier = Modifier.testTag("efficiency_slider")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("12.0", "15.0", "18.0", "22.0").forEach { preset ->
                                        AssistChip(
                                            onClick = { viewModel.efficiencyInput = preset },
                                            label = { Text("$preset km/L") }
                                        )
                                    }
                                }
                            }

                            // Optional Notes Input
                            OutlinedTextField(
                                value = viewModel.notesInput,
                                onValueChange = { viewModel.notesInput = it },
                                label = { Text(stringResource(id = R.string.field_notes)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("notes_input"),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Live Calculations Section (Matched with visual prototype layout)
                            AnimatedVisibility(
                                visible = viewModel.parsedAmount > 0 && viewModel.parsedPrice > 0,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("forecast_results"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "FUEL VOLUME",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    letterSpacing = 1.sp
                                                )
                                                Text(
                                                    text = String.format("%.2f L", viewModel.calculatedLitres),
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                            }
                                            
                                            Box(
                                                modifier = Modifier
                                                    .width(1.dp)
                                                    .height(36.dp)
                                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                            )
                                            
                                            Column(
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Text(
                                                    text = "ADDED RANGE",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    letterSpacing = 1.sp
                                                )
                                                Text(
                                                    text = "+${String.format("%.1f", viewModel.calculatedRange)} km",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.tertiary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "TARGET MILEAGE TO REACH",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = String.format("%,.1f km", viewModel.calculatedTargetOdometer),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 25.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "You will reach this target with your current Rs ${viewModel.amountInput} refuel.",
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Save confirmation button
                            Button(
                                onClick = {
                                    viewModel.saveRecord()
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("save_log_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                enabled = viewModel.parsedAmount > 0.0 && viewModel.parsedPrice > 0.0 && viewModel.parsedOdometer > 0.0 && viewModel.parsedEfficiency > 0.0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Confirm log button icon"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Confirm Refuel Log",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                // History Title Container
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Refueling Records Run History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "${records.size} logs",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Empty Check
                if (records.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("empty_history_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Empty fuel log",
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Your log is empty",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enter fuel bills to populate calculations and track mileage metrics over time.",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                } else {
                    items(items = records, key = { it.id }) { record ->
                        LogRecordRow(
                            record = record,
                            dateFormat = dateFormat,
                            onDelete = { viewModel.deleteRecord(record) }
                        )
                    }
                }
            }

            // Top notification bar overlay
            AnimatedVisibility(
                visible = viewModel.successNotification != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Check",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = viewModel.successNotification ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LogRecordRow(
    record: FuelRecord,
    dateFormat: SimpleDateFormat,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("log_record_${record.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1.1f)
            ) {
                Text(
                    text = dateFormat.format(Date(record.dateMillis)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Odometer",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "${String.format("%,.0f", record.odometerReading)} km",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.weight(1.4f)
            ) {
                Text(
                    text = "Spent Rs ${String.format("%,.0f", record.amountSpent)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${String.format("%.2f", record.fuelLitres)} L @ Rs ${String.format("%.2f", record.pricePerLitre)}/L",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (record.notes.isNotEmpty()) {
                    Text(
                        text = "• ${record.notes}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1.1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Target to Reach",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "${String.format("%,.0f", record.targetOdometer)} km",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Range: +${String.format("%.0f", record.estimatedRange)}k",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_record_${record.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete record button",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        modifier = modifier.testTag("stat_card_${title.replace(" ", "_").lowercase()}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                )
            }
        }
    }
}
