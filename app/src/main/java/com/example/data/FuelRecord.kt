package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_records")
data class FuelRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateMillis: Long,          // Date of refuel
    val amountSpent: Double,       // Rupees spent (e.g., 1000.0)
    val pricePerLitre: Double,     // Price per litre of fuel (e.g., 64.25)
    val odometerReading: Double,   // Odometer reading at refueling (e.g., 325662)
    val averageMileage: Double,    // km/L efficiency used for the prediction (e.g., 15.0)
    val notes: String = ""
) {
    val fuelLitres: Double
        get() = if (pricePerLitre > 0) amountSpent / pricePerLitre else 0.0

    val estimatedRange: Double
        get() = fuelLitres * averageMileage

    val targetOdometer: Double
        get() = odometerReading + estimatedRange
}
