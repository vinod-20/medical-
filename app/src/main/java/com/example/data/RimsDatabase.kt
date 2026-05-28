package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientScanDao {
    @Query("SELECT * FROM patient_scans ORDER BY CASE priority WHEN 'EMERGENCY' THEN 1 WHEN 'URGENT' THEN 2 ELSE 3 END, id DESC")
    fun getAllScans(): Flow<List<PatientScan>>

    @Query("SELECT * FROM patient_scans WHERE id = :id")
    suspend fun getScanById(id: Int): PatientScan?

    @Query("SELECT * FROM patient_scans WHERE patientId = :patientId ORDER BY id DESC")
    fun getScansByPatient(patientId: String): Flow<List<PatientScan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: PatientScan): Long

    @Update
    suspend fun updateScan(scan: PatientScan)

    @Query("UPDATE patient_scans SET radiologistImpression = :impression, status = :status WHERE id = :id")
    suspend fun updateDraft(id: Int, impression: String, status: String)

    @Query("UPDATE patient_scans SET doctorComments = :comments, isApproved = :approved, status = :status, approvedBy = :approvedBy WHERE id = :id")
    suspend fun approveScan(id: Int, comments: String, approved: Boolean, status: String, approvedBy: String)

    @Query("DELETE FROM patient_scans")
    suspend fun clearScans()
}

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments ORDER BY id DESC")
    fun getAllAppointments(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE patientId = :patientId ORDER BY id DESC")
    fun getAppointmentsByPatient(patientId: String): Flow<List<Appointment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: Appointment): Long

    @Update
    suspend fun updateAppointment(appointment: Appointment)

    @Query("UPDATE appointments SET paymentStatus = :status WHERE id = :id")
    suspend fun updatePaymentStatus(id: Int, status: String)

    @Query("DELETE FROM appointments")
    suspend fun clearAppointments()
}

@Dao
interface RimNotificationDao {
    @Query("SELECT * FROM rim_notifications WHERE recipientRole = :role ORDER BY timestamp DESC")
    fun getNotificationsByRole(role: String): Flow<List<RimNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: RimNotification): Long

    @Query("UPDATE rim_notifications SET isRead = 1 WHERE recipientRole = :role")
    suspend fun markAllAsRead(role: String)

    @Query("DELETE FROM rim_notifications")
    suspend fun clearNotifications()
}

@Database(entities = [PatientScan::class, Appointment::class, RimNotification::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patientScanDao(): PatientScanDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun rimNotificationDao(): RimNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rims_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
