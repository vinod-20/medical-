package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patient_scans")
data class PatientScan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: String,
    val patientName: String,
    val patientAge: Int,
    val patientGender: String,
    val scanDate: String,
    val scanType: String, // e.g., "Chest X-Ray", "Brain MRI", "Knee X-Ray"
    val priority: String, // "NORMAL", "URGENT", "EMERGENCY"
    val status: String, // "DRAFT", "PENDING_APPROVAL", "COMPLETED"
    val radiologistImpression: String = "",
    val aiGeneratedImpression: String = "",
    val doctorComments: String = "",
    val voiceDictation: String = "",
    val isApproved: Boolean = false,
    val isSigned: Boolean = false,
    val approvedBy: String = "",
    val signedBy: String = "",
    val qrCodeData: String = "",
    val activeAnnotations: String = "", // list of coordinates: "X|Y|label"
    val hasFracture: Boolean = false,
    val fractureCoordinates: String = "" // e.g., "x,y"
)

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val patientId: String,
    val patientName: String,
    val doctorName: String,
    val date: String,
    val time: String,
    val status: String, // "SCHEDULED", "COMPLETED", "CANCELLED"
    val paymentAmount: Double = 150.00,
    val paymentStatus: String = "PENDING" // "PENDING", "PAID"
)

@Entity(tableName = "rim_notifications")
data class RimNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recipientRole: String, // "PATIENT", "DOCTOR", "RADIOLOGIST"
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String // "CRITICAL", "COMPLETED", "ASSIGNED", "REMINDER"
)
