package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiService
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RimsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val scanDao = db.patientScanDao()
    private val apptDao = db.appointmentDao()
    private val notifyDao = db.rimNotificationDao()

    // --- Authentication & Role Systems ---
    private val _currentUserRole = MutableStateFlow<String>("NONE") // NONE, PATIENT, DOCTOR, RADIOLOGIST
    val currentUserRole: StateFlow<String> = _currentUserRole.asStateFlow()

    private val _currentUserId = MutableStateFlow<String>("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _currentUserName = MutableStateFlow<String>("")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow<Boolean>(true)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    // --- State Observables ---
    val allScans: StateFlow<List<PatientScan>> = scanDao.getAllScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAppointments: StateFlow<List<Appointment>> = apptDao.getAllAppointments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentRoleNotifications = MutableStateFlow<List<RimNotification>>(emptyList())
    val currentRoleNotifications: StateFlow<List<RimNotification>> = _currentRoleNotifications.asStateFlow()

    // --- Selection and Details State ---
    private val _selectedScan = MutableStateFlow<PatientScan?>(null)
    val selectedScan: StateFlow<PatientScan?> = _selectedScan.asStateFlow()

    private val _pacsZoom = MutableStateFlow(1f)
    val pacsZoom: StateFlow<Float> = _pacsZoom.asStateFlow()

    private val _pacsContrast = MutableStateFlow(1f)
    val pacsContrast: StateFlow<Float> = _pacsContrast.asStateFlow()

    private val _pacsSideBySide = MutableStateFlow(false)
    val pacsSideBySide: StateFlow<Boolean> = _pacsSideBySide.asStateFlow()

    private val _pacsInvert = MutableStateFlow(false)
    val pacsInvert: StateFlow<Boolean> = _pacsInvert.asStateFlow()

    // --- AI Generation & Dictation States ---
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _dictationText = MutableStateFlow("")
    val dictationText: StateFlow<String> = _dictationText.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // --- Prescription List State (Patient uploads) ---
    private val _uploadedPrescriptions = MutableStateFlow<List<String>>(
        listOf("Prescription_Ref_9821.pdf", "Prescription_Ref_3042.pdf")
    )
    val uploadedPrescriptions: StateFlow<List<String>> = _uploadedPrescriptions.asStateFlow()

    // --- Alerts Hub ---
    private val _alertMessage = MutableStateFlow<String?>(null)
    val alertMessage: StateFlow<String?> = _alertMessage.asStateFlow()

    init {
        // Prepopulate with outstanding diagnostic RIMS sample data
        viewModelScope.launch {
            // First check if DB is empty
            scanDao.insertScan(PatientScan(
                id = 1,
                patientId = "P-10492",
                patientName = "John Doe",
                patientAge = 42,
                patientGender = "Male",
                scanDate = "May 25, 2026",
                scanType = "Chest X-Ray",
                priority = "NORMAL",
                status = "COMPLETED",
                radiologistImpression = "Lungs are well-inflated. No consolidations, focal opacities, or alveolar infiltrates. Cardiac shadow size and contour are within normal limits. Costophrenic and cardiophrenic angles are sharp and clear. Skeletal structures are intact.",
                aiGeneratedImpression = "AI analysis: Clear pleural cavities. Trachea midline. Cardiac silhouette is non-enlarged. No acute cardiopulmonary disease detected.",
                doctorComments = "Approved. Patient shows healthy clinical correlation.",
                voiceDictation = "Normal lungs normal heart",
                isApproved = true,
                isSigned = true,
                approvedBy = "Dr. Gregory House",
                signedBy = "Dr. David Bowman",
                qrCodeData = "https://rims.med/verify/scan/1?sec=920402",
                activeAnnotations = "200|180|Aorta arch;450|350|Trachea Midline",
                hasFracture = false
            ))

            scanDao.insertScan(PatientScan(
                id = 2,
                patientId = "P-10492",
                patientName = "John Doe",
                patientAge = 42,
                patientGender = "Male",
                scanDate = "May 28, 2026",
                scanType = "Ankle Joint X-Ray",
                priority = "EMERGENCY",
                status = "DRAFT",
                radiologistImpression = "",
                aiGeneratedImpression = "",
                doctorComments = "",
                voiceDictation = "",
                isApproved = false,
                isSigned = false,
                approvedBy = "",
                signedBy = "",
                qrCodeData = "https://rims.med/verify/scan/2?sec=381902",
                activeAnnotations = "",
                hasFracture = true,
                fractureCoordinates = "180,420"
            ))

            scanDao.insertScan(PatientScan(
                id = 3,
                patientId = "P-30281",
                patientName = "Sarah Connor",
                patientAge = 35,
                patientGender = "Female",
                scanDate = "May 27, 2026",
                scanType = "Brain MRI",
                priority = "URGENT",
                status = "PENDING_APPROVAL",
                radiologistImpression = "Ventricles and cerebral sulci are normal for age. Mild white matter hyperintensities observed. No signs of intracranial pressure, acute stroke, or structural tumor mass.",
                aiGeneratedImpression = "AI analysis: High probability of localized non-specific tissue density. Recommended further MRI visual contrast.",
                doctorComments = "",
                voiceDictation = "symmetrical structure mild sinus inflammation",
                isApproved = false,
                isSigned = true,
                approvedBy = "",
                signedBy = "Dr. David Bowman",
                qrCodeData = "https://rims.med/verify/scan/3?sec=402921",
                activeAnnotations = "300|250|Tissue density variance",
                hasFracture = false
            ))

            scanDao.insertScan(PatientScan(
                id = 4,
                patientId = "P-88204",
                patientName = "Alex Mercer",
                patientAge = 28,
                patientGender = "Male",
                scanDate = "May 28, 2026",
                scanType = "Knee X-Ray",
                priority = "NORMAL",
                status = "DRAFT",
                radiologistImpression = "",
                aiGeneratedImpression = "",
                doctorComments = "",
                voiceDictation = "",
                isApproved = false,
                isSigned = false,
                qrCodeData = "https://rims.med/verify/scan/4?sec=882312",
                activeAnnotations = "",
                hasFracture = false
            ))

            // Prepopulate appointments
            apptDao.insertAppointment(Appointment(
                id = 1,
                patientId = "P-10492",
                patientName = "John Doe",
                doctorName = "Dr. Gregory House",
                date = "May 30, 2026",
                time = "10:30 AM",
                status = "SCHEDULED",
                paymentAmount = 120.0,
                paymentStatus = "PAID"
            ))

            apptDao.insertAppointment(Appointment(
                id = 2,
                patientId = "P-10492",
                patientName = "John Doe",
                doctorName = "Dr. Lisa Cuddy",
                date = "June 05, 2026",
                time = "02:00 PM",
                status = "SCHEDULED",
                paymentAmount = 145.0,
                paymentStatus = "PENDING"
            ))

            // Prepopulate notifications
            notifyDao.insertNotification(RimNotification(
                recipientRole = "RADIOLOGIST",
                title = "Emergency Scan Assigned",
                message = "Ankle Joint X-Ray for patient John Doe was marked as EMERGENCY. Immediate reporting requested.",
                type = "CRITICAL"
            ))

            notifyDao.insertNotification(RimNotification(
                recipientRole = "DOCTOR",
                title = "Report Submitted",
                message = "Radiologist Dr. Bowman signed Brain MRI for Sarah Connor. Ready for clinical approval.",
                type = "COMPLETED"
            ))

            notifyDao.insertNotification(RimNotification(
                recipientRole = "PATIENT",
                title = "Chest X-Ray Signed & Ready",
                message = "Your Chest X-Ray report is signed and ready for viewing. Download report or share securely.",
                type = "COMPLETED"
            ))

            // Initially selected scan setup
            val list = scanDao.getAllScans().firstOrNull()
            if (!list.isNullOrEmpty()) {
                _selectedScan.value = list.first()
            }
        }

        // Keep local notifications in sync when role shifts
        observeNotificationsForRole()
    }

    private fun observeNotificationsForRole() {
        viewModelScope.launch {
            currentUserRole.collectLatest { role ->
                if (role != "NONE") {
                    notifyDao.getNotificationsByRole(role).collectLatest { list ->
                        _currentRoleNotifications.value = list
                    }
                }
            }
        }
    }

    // --- Role Switching Actions ---
    fun selectRole(role: String) {
        _currentUserRole.value = role
        when (role) {
            "PATIENT" -> {
                _currentUserId.value = "P-10492"
                _currentUserName.value = "John Doe"
            }
            "DOCTOR" -> {
                _currentUserId.value = "D-55021"
                _currentUserName.value = "Dr. Gregory House"
            }
            "RADIOLOGIST" -> {
                _currentUserId.value = "R-77402"
                _currentUserName.value = "Dr. David Bowman"
            }
            else -> {
                _currentUserId.value = ""
                _currentUserName.value = ""
            }
        }
        // Auto select first scan for the selected dashboard view
        viewModelScope.launch {
            updateSelectedScanForRole()
        }
    }

    private suspend fun updateSelectedScanForRole() {
        val scans = scanDao.getAllScans().first()
        if (scans.isNotEmpty()) {
            val role = _currentUserRole.value
            _selectedScan.value = if (role == "PATIENT") {
                scans.firstOrNull { it.patientId == "P-10492" } ?: scans.first()
            } else {
                scans.first()
            }
        }
    }

    fun selectScan(scan: PatientScan) {
        _selectedScan.value = scan
        // Reset PACS tool properties on selection
        _pacsZoom.value = 1.0f
        _pacsContrast.value = 1.0f
        _pacsSideBySide.value = false
        _pacsInvert.value = false
        _dictationText.value = scan.voiceDictation
    }

    // --- Biometric Login Simulator ---
    fun simulateBiometricAuth(role: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Short delay to feel like genuine biometric scanner reading
            kotlinx.coroutines.delay(650)
            selectRole(role)
            onSuccess()
            triggerNotification(
                role = role,
                title = "Biometric Access Secure",
                message = "Logged in using fingerprint scan at ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}",
                type = "REMINDER"
            )
        }
    }

    // --- PACS Tools Adjusters ---
    fun adjustZoom(delta: Float) {
        _pacsZoom.value = (_pacsZoom.value + delta).coerceIn(0.5f, 3.5f)
    }

    fun adjustContrast(contrast: Float) {
        _pacsContrast.value = contrast.coerceIn(0.2f, 2.5f)
    }

    fun toggleSideBySide() {
        _pacsSideBySide.value = !_pacsSideBySide.value
    }

    fun toggleInvert() {
        _pacsInvert.value = !_pacsInvert.value
    }

    /**
     * Parse annotations string coordinate format (X1|Y1|Label1;X2|Y2|Label2)
     */
    fun getAnnotationsList(scan: PatientScan): List<Triple<Float, Float, String>> {
        if (scan.activeAnnotations.isEmpty()) return emptyList()
        return scan.activeAnnotations.split(";").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 3) {
                Triple(parts[0].toFloatOrNull() ?: 0f, parts[1].toFloatOrNull() ?: 0f, parts[2])
            } else null
        }
    }

    /**
     * Adds an annotation Pin on the current scan image
     */
    fun addAnnotation(x: Float, y: Float, label: String) {
        val current = _selectedScan.value ?: return
        val currentListText = current.activeAnnotations
        val addition = "${x.toInt()}|${y.toInt()}|$label"
        val newListText = if (currentListText.isEmpty()) addition else "$currentListText;$addition"
        
        val updatedScan = current.copy(activeAnnotations = newListText)
        _selectedScan.value = updatedScan

        viewModelScope.launch {
            scanDao.updateScan(updatedScan)
            triggerNotification(
                role = "RADIOLOGIST",
                title = "Annotation Added",
                message = "New pin '$label' placed on scan coordinates ($x, $y)",
                type = "REMINDER"
            )
        }
    }

    fun clearAnnotations() {
        val current = _selectedScan.value ?: return
        val updatedScan = current.copy(activeAnnotations = "")
        _selectedScan.value = updatedScan
        viewModelScope.launch {
            scanDao.updateScan(updatedScan)
        }
    }

    // --- Patient App Actions ---
    fun uploadPrescriptionSimulated(fileName: String) {
        _uploadedPrescriptions.value = _uploadedPrescriptions.value + fileName
        viewModelScope.launch {
            triggerNotification(
                role = "PATIENT",
                title = "Prescription Uploaded",
                message = "Secure repository saved '$fileName' successfully.",
                type = "COMPLETED"
            )
        }
    }

    fun payAppointmentSimulated(apptId: Int, amount: Double) {
        viewModelScope.launch {
            apptDao.updatePaymentStatus(apptId, "PAID")
            // Instantly sync
            triggerNotification(
                role = "PATIENT",
                title = "Invoice Settled",
                message = "Successfully paid $$amount via secure patient gateway.",
                type = "COMPLETED"
            )
        }
    }

    // --- Radiologist Controls (Draft, Edit, AI) ---
    fun updateDraftImpression(text: String) {
        val current = _selectedScan.value ?: return
        val updated = current.copy(radiologistImpression = text)
        _selectedScan.value = updated
        viewModelScope.launch {
            scanDao.updateScan(updated)
        }
    }

    fun triggerAiAssistance() {
        val current = _selectedScan.value ?: return
        val scanType = current.scanType
        val radioNotes = current.radiologistImpression.ifEmpty { "Patient visual anomaly scan requested." }

        _isAiLoading.value = true
        viewModelScope.launch {
            val suggestion = GeminiService.generateAnalysis(scanType, radioNotes)
            val updated = current.copy(aiGeneratedImpression = suggestion)
            _selectedScan.value = updated
            scanDao.updateScan(updated)
            _isAiLoading.value = false

            triggerNotification(
                role = "RADIOLOGIST",
                title = "AI Copilot Ready",
                message = "Synthesized clinical impression for $scanType successfully.",
                type = "COMPLETED"
            )
        }
    }

    // Voice Dictation Recorder Simulation
    fun toggleVoiceDictation() {
        val defaultTranscripts = listOf(
            "Consolidation noted in right lower lobe thoracic view suspecting bacterial load",
            "Joint space visible narrowing patellar margins mild osteophytes indicated",
            "Left cerebral hemisphere standard cerebellar margins normal intracranial pressures observed"
        )

        val current = _selectedScan.value ?: return
        if (_isRecording.value) {
            // Stop recording & finalize transcription
            _isRecording.value = false
            _isAiLoading.value = true
            viewModelScope.launch {
                val mockTranscript = defaultTranscripts.random()
                _dictationText.value = mockTranscript
                // Translate transcription into structured findings via Gemini
                val structuredReport = GeminiService.translateVoiceToReport(mockTranscript)
                val updated = current.copy(
                    voiceDictation = mockTranscript,
                    radiologistImpression = structuredReport
                )
                _selectedScan.value = updated
                scanDao.updateScan(updated)
                _isAiLoading.value = false

                triggerNotification(
                    role = "RADIOLOGIST",
                    title = "Dictation Processed",
                    message = "Voice memo translated to structured findings via AI.",
                    type = "COMPLETED"
                )
            }
        } else {
            // Start recording
            _isRecording.value = true
        }
    }

    fun loadSmartTemplate(templateName: String) {
        val current = _selectedScan.value ?: return
        val text = when (templateName) {
            "Normal Chest Template" -> {
                "EXAM: Chest X-ray PA/Lateral View\nFINDINGS:\nLungs: Clear of any focal opacity or lung lesion.\nPleura: No pleural effusion or sign of pneumothorax.\nHeart & Vascular: Cardiothoracic contours are unremarkable.\nBones: Thoracic skeleton is structurally intact.\nIMPRESSION: Normal thoracic evaluation."
            }
            "Fracture Bone Template" -> {
                "EXAM: Bone Radiograph\nFINDINGS:\nAlignment: Anatomical axis shifted.\nCortex: Localized cortical disruption visible on sub-articular border with minor fragmentation.\nSoft Tissue: Localized swelling apparent.\nIMPRESSION: Oblique fracture identified. Immobilization strongly recommended."
            }
            else -> {
                "EXAM: Standard Radiology Study\nFINDINGS:\nNo visible abnormality in tissue dense structures.\nIMPRESSION: Normal radiological check."
            }
        }
        val updated = current.copy(radiologistImpression = text)
        _selectedScan.value = updated
        viewModelScope.launch {
            scanDao.updateScan(updated)
        }
    }

    fun signReport() {
        val current = _selectedScan.value ?: return
        val name = _currentUserName.value.ifEmpty { "Dr. David Bowman (Chief)" }
        val updated = current.copy(
            isSigned = true,
            signedBy = name,
            status = "PENDING_APPROVAL"
        )
        _selectedScan.value = updated
        viewModelScope.launch {
            scanDao.updateScan(updated)
            // Send notice to Doctor
            triggerNotification(
                role = "DOCTOR",
                title = "New Report Prepared",
                message = "Radiologist $name signed report for ${current.patientName} (${current.scanType}). Approve requested.",
                type = "ASSIGNED"
            )
            triggerNotification(
                role = "RADIOLOGIST",
                title = "Report Signed",
                message = "Secure digital signature applied to case #${current.id}.",
                type = "COMPLETED"
            )
        }
    }

    // --- Doctor App Controls ---
    fun approveReport(comments: String) {
        val current = _selectedScan.value ?: return
        val doctorName = _currentUserName.value.ifEmpty { "Dr. Gregory House" }
        val updated = current.copy(
            isApproved = true,
            approvedBy = doctorName,
            status = "COMPLETED",
            doctorComments = comments
        )
        _selectedScan.value = updated
        viewModelScope.launch {
            scanDao.updateScan(updated)
            // Alert patient
            triggerNotification(
                role = "PATIENT",
                title = "Report Completed & Approved",
                message = "Your clinical report for ${current.scanType} has been approved by $doctorName.",
                type = "COMPLETED"
            )
            // Self verification notice
            triggerNotification(
                role = "DOCTOR",
                title = "Case Approved",
                message = "Approved report for patient ${current.patientName}.",
                type = "COMPLETED"
            )
        }
    }

    fun triggerEmergencyAlert() {
        val current = _selectedScan.value ?: return
        viewModelScope.launch {
            val alertMsg = "CRITICAL ALERT: Dr. House flagged patient ${current.patientName}'s ${current.scanType} for emergency clinical review!"
            _alertMessage.value = alertMsg
            
            triggerNotification(
                role = "DOCTOR",
                title = "Emergency Triggered",
                message = "Emergency clinical alert dispatched for ${current.patientName}.",
                type = "CRITICAL"
            )
            triggerNotification(
                role = "RADIOLOGIST",
                title = "Critical Findings Alert",
                message = "Dr. House requested immediate attention for ${current.patientName}.",
                type = "CRITICAL"
            )
            triggerNotification(
                role = "PATIENT",
                title = "Priority Review Scheduled",
                message = "Your diagnostic scan has been put on clinical high priority. Team mobilized.",
                type = "CRITICAL"
            )
        }
    }

    fun dismissAlert() {
        _alertMessage.value = null
    }

    // --- Helper to Insert Live Custom Push Notifications ---
    fun triggerNotification(role: String, title: String, message: String, type: String) {
        viewModelScope.launch {
            notifyDao.insertNotification(RimNotification(
                recipientRole = role,
                title = title,
                message = message,
                type = type
            ))
        }
    }

    fun clearAllNotifications(role: String) {
        viewModelScope.launch {
            notifyDao.markAllAsRead(role)
        }
    }

    // Consult Gemini in real-time regarding coordinate click
    fun teachAnatomyOnCellClick(x: Float, y: Float, part: String, onResponse: (String) -> Unit) {
        viewModelScope.launch {
            val answer = GeminiService.consultFractureDemo(x, y, part)
            onResponse(answer)
        }
    }
}
