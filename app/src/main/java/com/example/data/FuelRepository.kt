package com.example.data

import kotlinx.coroutines.flow.Flow

class FuelRepository(private val fuelRecordDao: FuelRecordDao) {
    val allRecords: Flow<List<FuelRecord>> = fuelRecordDao.getAllRecords()

    suspend fun insert(record: FuelRecord) {
        fuelRecordDao.insertRecord(record)
    }

    suspend fun delete(record: FuelRecord) {
        fuelRecordDao.deleteRecord(record)
    }

    suspend fun deleteById(id: Int) {
        fuelRecordDao.deleteRecordById(id)
    }

    suspend fun getLatestRecord(): FuelRecord? {
        return fuelRecordDao.getLatestRecord()
    }
}
