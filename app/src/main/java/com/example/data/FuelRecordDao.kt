package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelRecordDao {
    @Query("SELECT * FROM fuel_records ORDER BY dateMillis DESC")
    fun getAllRecords(): Flow<List<FuelRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: FuelRecord)

    @Delete
    suspend fun deleteRecord(record: FuelRecord)

    @Query("DELETE FROM fuel_records WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    @Query("SELECT * FROM fuel_records ORDER BY odometerReading DESC LIMIT 1")
    suspend fun getLatestRecord(): FuelRecord?
}
