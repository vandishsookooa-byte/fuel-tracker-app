package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FuelDatabase
import com.example.data.FuelRecord
import com.example.data.FuelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class FuelViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FuelRepository

    init {
        val database = FuelDatabase.getDatabase(application)
        repository = FuelRepository(database.fuelRecordDao())
    }

    val records: StateFlow<List<FuelRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Form inputs
    var amountInput by mutableStateOf("1000")
    var priceInput by mutableStateOf("64.25")
    var odometerInput by mutableStateOf("325662")
    var efficiencyInput by mutableStateOf("15.0")
    var selectedDateMillis by mutableStateOf(System.currentTimeMillis())
    var notesInput by mutableStateOf("")

    // Status state
    var showDatePicker by mutableStateOf(false)
    var successNotification by mutableStateOf<String?>(null)

    // Calculated read-only variables from current input state
    val parsedAmount: Double
        get() = amountInput.toDoubleOrNull() ?: 0.0

    val parsedPrice: Double
        get() = priceInput.toDoubleOrNull() ?: 64.25

    val parsedOdometer: Double
        get() = odometerInput.toDoubleOrNull() ?: 325662.0

    val parsedEfficiency: Double
        get() = efficiencyInput.toDoubleOrNull() ?: 15.0

    val calculatedLitres: Double
        get() = if (parsedPrice > 0) parsedAmount / parsedPrice else 0.0

    val calculatedRange: Double
        get() = calculatedLitres * parsedEfficiency

    val calculatedTargetOdometer: Double
        get() = parsedOdometer + calculatedRange

    fun updateDate(year: Int, month: Int, day: Int) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        selectedDateMillis = calendar.timeInMillis
    }

    // Load setup with latest record values if present
    fun autofillFromLatest() {
        viewModelScope.launch {
            val latest = repository.getLatestRecord()
            if (latest != null) {
                // Pre-fill the odometer with the calculated target or actual of the last record
                // (usually next odometer is around the previous odometer + estimated or actual odometer)
                // Let's set the current odometer to latest.odometer + actual
                odometerInput = String.format("%.0f", latest.odometerReading)
                priceInput = String.format("%.2f", latest.pricePerLitre)
                efficiencyInput = String.format("%.1f", latest.averageMileage)
            }
        }
    }

    fun saveRecord() {
        val amount = parsedAmount
        val price = parsedPrice
        val odometer = parsedOdometer
        val efficiency = parsedEfficiency

        if (amount <= 0.0 || price <= 0.0 || odometer <= 0.0 || efficiency <= 0.0) {
            return
        }

        val record = FuelRecord(
            dateMillis = selectedDateMillis,
            amountSpent = amount,
            pricePerLitre = price,
            odometerReading = odometer,
            averageMileage = efficiency,
            notes = notesInput.trim()
        )

        viewModelScope.launch {
            repository.insert(record)
            
            // Clean up basic form, but carry forward latest odometer as baseline for next fueling
            // Note: Keep price and efficiency as baseline too!
            val nextOdometerBaseline = odometer + (amount / price * efficiency)
            odometerInput = String.format("%.0f", nextOdometerBaseline)
            notesInput = ""
            successNotification = "Saved refueling record and updated odometer guess!"
        }
    }

    fun deleteRecord(record: FuelRecord) {
        viewModelScope.launch {
            repository.delete(record)
        }
    }

    fun deleteRecordById(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun resetInputsToDefaults() {
        amountInput = "1000"
        priceInput = "64.25"
        odometerInput = "325662"
        efficiencyInput = "15.0"
        selectedDateMillis = System.currentTimeMillis()
        notesInput = ""
    }
}
