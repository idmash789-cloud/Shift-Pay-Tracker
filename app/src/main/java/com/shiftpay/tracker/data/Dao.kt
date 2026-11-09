package com.shiftpay.tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs WHERE archived = 0 ORDER BY name")
    fun observeJobs(): Flow<List<Job>>

    @Query("SELECT * FROM jobs ORDER BY name")
    suspend fun getAllJobs(): List<Job>

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun getJob(id: Long): Job?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: Job): Long

    @Update
    suspend fun update(job: Job)

    @Delete
    suspend fun delete(job: Job)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY startMillis DESC")
    fun observeShifts(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE jobId = :jobId ORDER BY startMillis DESC")
    fun observeShiftsForJob(jobId: Long): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE endMillis IS NULL ORDER BY startMillis")
    suspend fun getRunningShifts(): List<Shift>

    @Query("SELECT * FROM shifts WHERE jobId = :jobId AND endMillis IS NULL ORDER BY startMillis LIMIT 1")
    suspend fun getRunningShiftForJob(jobId: Long): Shift?

    @Query("SELECT * FROM shifts WHERE id = :id")
    suspend fun getShift(id: Long): Shift?

    @Query("SELECT * FROM shifts WHERE startMillis >= :from AND startMillis < :to ORDER BY startMillis")
    suspend fun getShiftsInRange(from: Long, to: Long): List<Shift>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(shift: Shift): Long

    @Update
    suspend fun update(shift: Shift)

    @Delete
    suspend fun delete(shift: Shift)
}

@Dao
interface ShiftTemplateDao {
    @Query("SELECT * FROM shift_templates ORDER BY jobId, startHour, startMinute")
    fun observeTemplates(): Flow<List<ShiftTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: ShiftTemplate): Long

    @Update
    suspend fun update(template: ShiftTemplate)

    @Delete
    suspend fun delete(template: ShiftTemplate)
}

@Dao
interface DeductionDao {
    @Query("SELECT * FROM deductions ORDER BY name")
    fun observeDeductions(): Flow<List<Deduction>>

    @Query("SELECT * FROM deductions WHERE jobId = :jobId")
    suspend fun getForJob(jobId: Long): List<Deduction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(deduction: Deduction): Long

    @Delete
    suspend fun delete(deduction: Deduction)
}

/* No SettingsDao: settings live in DataStore — see SettingsStore.kt. */
