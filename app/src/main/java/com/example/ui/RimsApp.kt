package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.GeminiService
import com.example.data.Appointment
import com.example.data.PatientScan
import com.example.data.RimNotification
import com.example.viewmodel.RimsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RimsApp(viewModel: RimsViewModel) {
    val currentRole by viewModel.currentUserRole.collectAsState()
    val activeAlarm by viewModel.alertMessage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Content
        Crossfade(targetState = currentRole, label = "RoleTransition") { role ->
            when (role) {
                "NONE" -> AuthScreen(viewModel)
                "PATIENT" -> PatientDashboard(viewModel)
                "DOCTOR" -> DoctorDashboard(viewModel)
                "RADIOLOGIST" -> RadiologistDashboard(viewModel)
            }
        }

        // Global Urgent Emergency Banner overlay
        activeAlarm?.let { alert ->
            EmergencyOverlayBanner(
                message = alert,
                onDismiss = { viewModel.dismissAlert() }
            )
        }
    }
}

@Composable
fun EmergencyOverlayBanner(message: String, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .pointerInput(Unit) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = alpha)
            ),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Critical Clinical Alert",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "CRITICAL CLINICAL ALERT",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .testTag("dismiss_critical_alert")
                ) {
                    Text("Acknowledge & Mobilize Team", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: RimsViewModel) {
    var selectedRole by remember { mutableStateOf("PATIENT") }
    var pinText by remember { mutableStateOf("") }
    val isBiometric by viewModel.isBiometricEnabled.collectAsState()
    var isAuthenticating by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00526B), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 1.2f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Branding Brand Vector Logo using Compose elements
            MedicalBrandingHeader()

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SECURE PORTAL LOGIN",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Role Tabs Selector Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.background,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("PATIENT", "DOCTOR", "RADIOLOGIST").forEach { role ->
                            val active = selectedRole == role
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { selectedRole = role }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = role,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Simulated hospital PIN
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { if (it.length <= 6) pinText = it },
                        label = { Text("Clinical Access PIN") },
                        placeholder = { Text("6-Digit secure code") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Security lock") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (pinText.isNotEmpty() && !isAuthenticating) {
                                isAuthenticating = true
                                scope.launch {
                                    viewModel.selectRole(selectedRole)
                                    isAuthenticating = false
                                }
                            }
                        }),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isAuthenticating) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else {
                        Button(
                            onClick = {
                                isAuthenticating = true
                                scope.launch {
                                    viewModel.selectRole(selectedRole)
                                    isAuthenticating = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("login_button")
                        ) {
                            Text("ENTER SYSTEM", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }

                        if (isBiometric) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        isAuthenticating = true
                                        viewModel.simulateBiometricAuth(selectedRole) {
                                            isAuthenticating = false
                                        }
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Scan Fingerprint",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Biometric Hospital Sign-In",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap symbol for dynamic touch bypass",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Regulatory match",
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Complies with HIPAA Data Standards",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun MedicalBrandingHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                    ),
                    RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(48.dp)) {
                // Outer ring representing MRI/CT scanner
                drawCircle(
                    color = Color.White,
                    radius = size.width / 2.3f,
                    style = Stroke(width = 4.dp.toPx())
                )
                // Pulse waves
                drawPath(
                    path = Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.5f)
                        lineTo(size.width * 0.4f, size.height * 0.5f)
                        lineTo(size.width * 0.5f, size.height * 0.2f)
                        lineTo(size.width * 0.6f, size.height * 0.8f)
                        lineTo(size.width * 0.7f, size.height * 0.5f)
                        lineTo(size.width * 0.85f, size.height * 0.5f)
                    },
                    color = Color.White,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "RIMS ENGINE",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 3.sp
        )
        Text(
            text = "X-Reporting Department System",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
    }
}

// --- Dynamic Interactive PACS Radiograph Drawing --
@Composable
fun PacsViewerCanvas(
    scan: PatientScan,
    zoom: Float,
    contrast: Float,
    invert: Boolean,
    sideBySide: Boolean,
    modifier: Modifier = Modifier,
    viewModel: RimsViewModel,
    allowAnnotations: Boolean = false
) {
    var anatomyTeachedText by remember { mutableStateOf<String?>(null) }
    var interactiveX by remember { mutableStateOf(-1f) }
    var interactiveY by remember { mutableStateOf(-1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .background(Color.Black)
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        if (sideBySide) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Panel: Normal Reference Scan
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .drawBehind {
                            drawPACSImage(
                                scanType = scan.scanType,
                                isReference = true,
                                zoom = zoom,
                                contrast = contrast,
                                invert = invert,
                                hasFracture = false,
                                textLabel = "D-REF: NORMAL REFERENCE"
                            )
                        }
                )
                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                )
                // Right Panel: Active Case Scan
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(scan) {
                            detectTapGestures(
                                onTap = { offset ->
                                    if (allowAnnotations) {
                                        interactiveX = offset.x
                                        interactiveY = offset.y
                                        // Request tutorial from Gemini regarding coordinates
                                        viewModel.teachAnatomyOnCellClick(
                                            offset.x,
                                            offset.y,
                                            scan.scanType
                                        ) { msg ->
                                            anatomyTeachedText = msg
                                        }
                                    }
                                }
                            )
                        }
                        .drawBehind {
                            drawPACSImage(
                                scanType = scan.scanType,
                                isReference = false,
                                zoom = zoom,
                                contrast = contrast,
                                invert = invert,
                                hasFracture = scan.hasFracture,
                                textLabel = "ACTIVE CASE: #${scan.id}"
                            )
                        }
                ) {
                    RenderOnsetImageElements(scan = scan, zoom = zoom, viewModel = viewModel)
                }
            }
        } else {
            // Full Single Canvas viewport
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(scan) {
                        detectTapGestures(
                            onTap = { offset ->
                                if (allowAnnotations) {
                                    interactiveX = offset.x
                                    interactiveY = offset.y
                                    viewModel.teachAnatomyOnCellClick(
                                        offset.x,
                                        offset.y,
                                        scan.scanType
                                    ) { msg ->
                                        anatomyTeachedText = msg
                                    }
                                }
                            }
                        )
                    }
                    .drawBehind {
                        drawPACSImage(
                            scanType = scan.scanType,
                            isReference = false,
                            zoom = zoom,
                            contrast = contrast,
                            invert = invert,
                            hasFracture = scan.hasFracture,
                            textLabel = "PACS SECURE WORKSPACE | ZOOM ${"%.1f".format(zoom)}x"
                        )
                    }
            ) {
                RenderOnsetImageElements(scan = scan, zoom = zoom, viewModel = viewModel)
            }
        }

        // Mini diagnostic HUD box
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "DICOM v3.0",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = scan.scanType.uppercase(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Tap anatomy dialog assistant
        anatomyTeachedText?.let { teacher ->
            AlertDialog(
                onDismissRequest = { anatomyTeachedText = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = "Anatomy Guide",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Radiological Anatomy Assist", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Tapped Point: X=${interactiveX.toInt()}px, Y=${interactiveY.toInt()}px\n",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(text = teacher, fontSize = 14.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { anatomyTeachedText = null }) {
                        Text("Dismiss")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        val label = "Anomaly Area [X:${interactiveX.toInt()}]"
                        viewModel.addAnnotation(interactiveX, interactiveY, label)
                        anatomyTeachedText = null
                    }) {
                        Text("Pin Anomaly Tag", color = MaterialTheme.colorScheme.primary)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
fun BoxScope.RenderOnsetImageElements(
    scan: PatientScan,
    zoom: Float,
    viewModel: RimsViewModel
) {
    // Dynamic coordinate offsets according to zoom
    val annotations = viewModel.getAnnotationsList(scan)
    
    // Draw existing visual annotation overlays on top
    annotations.forEach { item ->
        val px = item.first * zoom
        val py = item.second * zoom

        Box(
            modifier = Modifier
                .offset(x = px.dp / 3f, y = py.dp / 3f)
                .background(MaterialTheme.colorScheme.secondary, CircleShape)
                .size(8.dp)
        )
        Box(
            modifier = Modifier
                .offset(x = (px.dp / 3f) + 12.dp, y = (py.dp / 3f) - 6.dp)
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(text = item.third, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }

    // Blink fracture demo indicator
    if (scan.hasFracture && scan.fractureCoordinates.isNotEmpty()) {
        val coords = scan.fractureCoordinates.split(",")
        val fx = (coords.getOrNull(0)?.toFloatOrNull() ?: 0f) * zoom
        val fy = (coords.getOrNull(1)?.toFloatOrNull() ?: 0f) * zoom

        val infiniteTransition = rememberInfiniteTransition(label = "fracture_blink")
        val radius by infiniteTransition.animateFloat(
            initialValue = 8f,
            targetValue = 24f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "radius"
        )
        
        Box(
            modifier = Modifier
                .offset(x = (fx.dp / 3f) - 12.dp, y = (fy.dp / 3f) - 12.dp)
                .size(24.dp)
                .drawBehind {
                    drawCircle(
                        color = Color.Red,
                        radius = radius.dp.toPx() / 2,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
        )
    }
}

fun DrawScope.drawPACSImage(
    scanType: String,
    isReference: Boolean,
    zoom: Float,
    contrast: Float,
    invert: Boolean,
    hasFracture: Boolean,
    textLabel: String
) {
    val canvasBg = if (invert) Color(0xFFE2E8F0) else Color(0xFF07090C)
    val boneColor = if (invert) Color.DarkGray.copy(alpha = 0.9f * contrast) else Color.White.copy(alpha = 0.85f * contrast)
    val lungCavityColor = if (invert) Color.White.copy(alpha = 0.5f * contrast) else Color.DarkGray.copy(alpha = 0.35f * contrast)
    val accentLines = if (invert) Color.Gray else Color.DarkGray

    // Back fill
    drawRect(color = canvasBg)

    // Center Reference grids
    drawLine(
        color = accentLines.copy(alpha = 0.25f),
        start = Offset(0f, size.height / 2),
        end = Offset(size.width, size.height / 2),
        strokeWidth = 1f
    )
    drawLine(
        color = accentLines.copy(alpha = 0.25f),
        start = Offset(size.width / 2, 0f),
        end = Offset(size.width / 2, size.height),
        strokeWidth = 1f
    )

    // Scale context with respect to zoom center
    val cx = size.width / 2
    val cy = size.height / 2

    // Simple custom Vector Radiograph drawer
    when {
        scanType.contains("Chest", ignoreCase = true) -> {
            // Draw dual lung lung lobes
            val lungWidth = 72f * zoom
            val lungHeight = 130f * zoom

            // Left Lung
            drawOval(
                color = lungCavityColor,
                topLeft = Offset(cx - lungWidth - 10f, cy - lungHeight / 2),
                size = androidx.compose.ui.geometry.Size(lungWidth, lungHeight)
            )

            // Right Lung
            drawOval(
                color = lungCavityColor,
                topLeft = Offset(cx + 10f, cy - lungHeight / 2),
                size = androidx.compose.ui.geometry.Size(lungWidth, lungHeight)
            )

            // Spinal column vertebrae
            val vertCount = 14
            for (i in 0 until vertCount) {
                val vy = (cy - lungHeight / 2) + (i * (lungHeight / vertCount))
                drawRect(
                    color = boneColor.copy(alpha = 0.5f),
                    topLeft = Offset(cx - 8f * zoom, vy),
                    size = androidx.compose.ui.geometry.Size(16f * zoom, 6f * zoom)
                )
            }

            // Bone Rib Cage structures
            for (i in 0 until 7) {
                val ry = (cy - lungHeight / 2) + (i * 18f * zoom) + 12f * zoom
                // Left rib curves
                drawArc(
                    color = boneColor.copy(alpha = 0.4f),
                    startAngle = 100f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(cx - lungWidth - 25f, ry - 10f),
                    size = androidx.compose.ui.geometry.Size(lungWidth + 20f, 40f * zoom),
                    style = Stroke(width = 3f * zoom)
                )
                // Right rib curves
                drawArc(
                    color = boneColor.copy(alpha = 0.4f),
                    startAngle = 320f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(cx + 5f, ry - 10f),
                    size = androidx.compose.ui.geometry.Size(lungWidth + 20f, 40f * zoom),
                    style = Stroke(width = 3f * zoom)
                )
            }

            // Heart contour
            drawPath(
                path = Path().apply {
                    moveTo(cx - 15f * zoom, cy)
                    quadraticTo(cx - 30f * zoom, cy + 25f * zoom, cx + 10f * zoom, cy + 28f * zoom)
                    quadraticTo(cx + 25f * zoom, cy, cx - 15f * zoom, cy)
                },
                color = boneColor.copy(alpha = 0.55f)
            )
        }

        scanType.contains("Brain", ignoreCase = true) -> {
            val r = 90f * zoom
            // Outer Skull Dome
            drawCircle(
                color = boneColor,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 4f * zoom)
            )

            // Inner Cerebral convolutions
            val brainMatterColor = if (invert) Color.White.copy(alpha = 0.3f * contrast) else Color.Gray.copy(alpha = 0.35f * contrast)
            drawCircle(
                color = brainMatterColor,
                radius = r - 10f * zoom,
                center = Offset(cx, cy)
            )

            // Drawing representative ventricles
            val ventPath = Path().apply {
                moveTo(cx - 20f * zoom, cy - 30f * zoom)
                quadraticTo(cx, cy - 45f * zoom, cx, cy - 5f * zoom)
                quadraticTo(cx - 10f * zoom, cy + 20f * zoom, cx - 15f * zoom, cy - 30f * zoom)
            }
            drawPath(
                path = ventPath,
                color = if (invert) Color.DarkGray else Color.White,
                style = Stroke(width = 2.dp.toPx())
            )

            // Mirror Ventricle
            val mirrorVentPath = Path().apply {
                moveTo(cx + 20f * zoom, cy - 30f * zoom)
                quadraticTo(cx, cy - 45f * zoom, cx, cy - 5f * zoom)
                quadraticTo(cx + 10f * zoom, cy + 20f * zoom, cx + 15f * zoom, cy - 30f * zoom)
            }
            drawPath(
                path = mirrorVentPath,
                color = if (invert) Color.DarkGray else Color.White,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        else -> {
            // Default ankle/joint bone structure containing fracture demo
            val boneWidth = 24f * zoom
            // Main Tibia bone path shading
            drawRoundRect(
                color = boneColor,
                topLeft = Offset(cx - boneWidth - 8f, cy - 100f * zoom),
                size = androidx.compose.ui.geometry.Size(boneWidth, 140f * zoom),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
            )

            // Fibula bone side
            drawRoundRect(
                color = boneColor.copy(alpha = 0.7f),
                topLeft = Offset(cx + 8f, cy - 100f * zoom),
                size = androidx.compose.ui.geometry.Size(12f * zoom, 120f * zoom),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
            )

            // Joint base
            drawCircle(
                color = boneColor,
                radius = boneWidth * 1.3f,
                center = Offset(cx, cy + 50f * zoom)
            )

            if (hasFracture) {
                // Draw bold Crimson fracture line across side cartilage cortex
                val offsetFx = cx - boneWidth
                val offsetFy = cy

                drawPath(
                    path = Path().apply {
                        moveTo(offsetFx - 10f, offsetFy)
                        lineTo(offsetFx + 20f, offsetFy + 8f * zoom)
                        lineTo(offsetFx - 5f, offsetFy + 16f * zoom)
                        lineTo(offsetFx + 25f, offsetFy + 22f * zoom)
                    },
                    color = Color.Red,
                    style = Stroke(width = 3f * zoom)
                )
            }
        }
    }

    // Top overlay header label
    val labelColor = if (invert) Color.Black else Color.Gray
    // Small text indicating magnification
    // We cannot use compose drawtext simply easily without a native canvas callback, so we draw visual boundary ticks instead!
    drawLine(
        color = labelColor.copy(alpha = 0.5f),
        start = Offset(20f, 20f),
        end = Offset(40f, 20f),
        strokeWidth = 2f
    )
    drawLine(
        color = labelColor.copy(alpha = 0.5f),
        start = Offset(20f, 20f),
        end = Offset(20f, 40f),
        strokeWidth = 2f
    )
}

// --- Dynamic Notification Hub Drawer ---
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DrawerNotificationsHub(
    role: String,
    viewModel: RimsViewModel,
    onClose: () -> Unit
) {
    val unreadNotifications by viewModel.currentRoleNotifications.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onClose)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .clickable { /* prevent clicks passing */ }
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DIAGNOSTIC ALERTS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Row {
                    IconButton(onClick = { viewModel.clearAllNotifications(role) }) {
                        Icon(Icons.Default.ClearAll, contentDescription = "Clear notifications", tint = Color.Gray)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close panel", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (unreadNotifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "Empty",
                            tint = Color.Gray,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Inbox clear. No active alerts.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(unreadNotifications) { item ->
                        val cardBg = when (item.type) {
                            "CRITICAL" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            "ASSIGNED" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                            "COMPLETED" -> Color(0xFF2ECC71).copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.background
                        }
                        val cardBorderColor = when (item.type) {
                            "CRITICAL" -> MaterialTheme.colorScheme.error
                            "ASSIGNED" -> MaterialTheme.colorScheme.secondary
                            "COMPLETED" -> Color(0xFF2ECC71)
                            else -> Color.DarkGray
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, cardBorderColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = when (item.type) {
                                        "CRITICAL" -> Icons.Default.NewReleases
                                        "ASSIGNED" -> Icons.Default.Assignment
                                        "COMPLETED" -> Icons.Default.CheckCircle
                                        else -> Icons.Default.NotificationImportant
                                    }
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = "Type",
                                        tint = cardBorderColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.title.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = cardBorderColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = item.message,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.timestamp)),
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- dashboard view shared items ---
@Composable
fun MainTitleBar(
    roleTitle: String,
    roleIcon: ImageVector,
    viewModel: RimsViewModel,
    onNotificationsClick: () -> Unit
) {
    val currentRole by viewModel.currentUserRole.collectAsState()
    val listNotif by viewModel.currentRoleNotifications.collectAsState()
    val unreadCount = listNotif.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .drawBehind {
                val strokeWidth = 1f * density
                val y = size.height - strokeWidth / 2
                drawLine(
                    color = Color(0xFFE1E3E1),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Portal Logout click leads back to Auth Screen
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    .clickable { viewModel.selectRole("NONE") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Login,
                    contentDescription = "Sign out",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = roleIcon,
                        contentDescription = "Role info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = roleTitle, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Text(text = "RIMS Mobile Hub", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Live notifications bell
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    .clickable(onClick = onNotificationsClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (unreadCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                    contentDescription = "View alerts",
                    tint = if (unreadCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(20.dp)
                )
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 2.dp, end = 2.dp)
                            .background(Color.Red, CircleShape)
                            .size(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------
// B. PATIENT APP HUB
// --------------------------------------------------
@Composable
fun PatientDashboard(viewModel: RimsViewModel) {
    val scans by viewModel.allScans.collectAsState()
    val appointments by viewModel.allAppointments.collectAsState()
    val prescriptions by viewModel.uploadedPrescriptions.collectAsState()
    val selectedScan by viewModel.selectedScan.collectAsState()
    val patientScans = scans.filter { it.patientId == "P-10492" }

    var currentSubTab by remember { mutableStateOf("REPORTS") } // REPORTS, APPOINTMENTS, VERIFY
    var showNotifPanel by remember { mutableStateOf(false) }
    var selectedViewReport by remember { mutableStateOf<PatientScan?>(null) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = currentSubTab == "REPORTS",
                    onClick = { currentSubTab = "REPORTS" },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "My Reports") },
                    label = { Text("My Cases", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = currentSubTab == "APPOINTMENTS",
                    onClick = { currentSubTab = "APPOINTMENTS" },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Booking") },
                    label = { Text("Appointments", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = currentSubTab == "VERIFY",
                    onClick = { currentSubTab = "VERIFY" },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "QR Verify") },
                    label = { Text("QR Verify", fontSize = 11.sp) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MainTitleBar(
                    roleTitle = "PATIENT ACCESS: JOHN DOE",
                    roleIcon = Icons.Default.Person,
                    viewModel = viewModel,
                    onNotificationsClick = { showNotifPanel = true }
                )

                AnimatedContent(targetState = currentSubTab, label = "PatientTab") { tab ->
                    when (tab) {
                        "REPORTS" -> PatientReportsTab(
                            scans = patientScans,
                            prescriptions = prescriptions,
                            onViewReport = { selectedViewReport = it },
                            onUploadPres = { viewModel.uploadPrescriptionSimulated(it) }
                        )
                        "APPOINTMENTS" -> PatientAppointmentsTab(
                            appointments = appointments,
                            onPay = { id, amount -> viewModel.payAppointmentSimulated(id, amount) }
                        )
                        "VERIFY" -> PatientQrVerifyTab(patientScans)
                    }
                }
            }

            // Reports Dialog Overlay
            selectedViewReport?.let { scan ->
                PatientDetailedReportDialog(
                    scan = scan,
                    viewModel = viewModel,
                    onClose = { selectedViewReport = null }
                )
            }

            if (showNotifPanel) {
                DrawerNotificationsHub("PATIENT", viewModel, onClose = { showNotifPanel = false })
            }
        }
    }
}

@Composable
fun PatientReportsTab(
    scans: List<PatientScan>,
    prescriptions: List<String>,
    onViewReport: (PatientScan) -> Unit,
    onUploadPres: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = "Access",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Interactive Diagnostics Engine", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Review historical imaging, track doctor sign-offs, and generate secure clinical credentials.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }
        }

        item {
            Text("SECURE IMAGING RECORDS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), letterSpacing = 1.sp)
        }

        if (scans.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No local imaging scans found. Contact administrator.", color = Color.Gray)
                }
            }
        } else {
            items(scans) { scan ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFE1E3E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(scan.scanType, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (scan.status == "COMPLETED") Color(0xFF2ECC71).copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = scan.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (scan.status == "COMPLETED") Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("REGISTRATION DATE", fontSize = 9.sp, color = Color.Gray)
                                Text(scan.scanDate, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column {
                                Text("ASSIGNED SPECIALIST", fontSize = 9.sp, color = Color.Gray)
                                Text(scan.signedBy.ifEmpty { "Pending Signature" }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onViewReport(scan) },
                                modifier = Modifier.weight(1f).testTag("view_report_button")
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = "View", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("INTERACTIVE REPORT", fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Prescription section in College Project guidelines
        item {
            Text("SECURE PRESCRIPTION ARCHIVES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), letterSpacing = 1.sp)
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE1E3E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Prescription Authorization", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    Text("Upload clinical orders or referrals required for subsequent diagnostic scans.", fontSize = 11.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Button(
                        onClick = {
                            val fileNum = (1000..9999).random()
                            onUploadPres("Prescription_Referral_$fileNum.pdf")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth().testTag("upload_prescription")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Upload", tint = MaterialTheme.colorScheme.onSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate PDF Upload", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    prescriptions.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF icon", tint = Color.Red, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                            }
                            Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.Green, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatientAppointmentsTab(
    appointments: List<Appointment>,
    onPay: (Int, Double) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("SCHEDULER & SETTLEMENTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
        }

        items(appointments) { appt ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE1E3E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(appt.doctorName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                            Text("Specialist Consultation", fontSize = 11.sp, color = Color.Gray)
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (appt.paymentStatus == "PAID") Color(0xFF2ECC71).copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                    CircleShape
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = appt.paymentStatus,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (appt.paymentStatus == "PAID") Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Event, contentDescription = "Date", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(appt.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = "Time", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(appt.time, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFE1E3E1))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("SCAN SERVICE FEE", fontSize = 10.sp, color = Color.Gray)
                            Text("$${appt.paymentAmount}0", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }

                        if (appt.paymentStatus == "PENDING") {
                            Button(
                                onClick = { onPay(appt.id, appt.paymentAmount) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("pay_fee_button")
                            ) {
                                Text("PAY NOW", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = "Settled", tint = Color.Green, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Payment Cleared", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PatientQrVerifyTab(scans: List<PatientScan>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                "QR-SECURED REPORT VERIFICATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFE1E3E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Clinical Trust Engine",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Radiological records are authenticated via Cryptographic Hash labels. Government or insurance offices scanning these QR cards draw direct database confirmations.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Draw symbolic QR Code using Compose Path & Canvas
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                            .size(160.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Draw realistic geometric QR pattern blocks
                            drawRect(color = Color.Black, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(35f, 35f))
                            drawRect(color = Color.Black, topLeft = Offset(size.width - 35f, 0f), size = androidx.compose.ui.geometry.Size(35f, 35f))
                            drawRect(color = Color.Black, topLeft = Offset(0f, size.height - 35f), size = androidx.compose.ui.geometry.Size(35f, 35f))

                            // Custom randomized diagnostic lines
                            drawLine(color = Color.Black, start = Offset(50f, 10f), end = Offset(100f, 10f), strokeWidth = 8f)
                            drawLine(color = Color.Black, start = Offset(10f, 50f), end = Offset(10f, 100f), strokeWidth = 8f)
                            drawRect(color = Color.Black, topLeft = Offset(60f, 60f), size = androidx.compose.ui.geometry.Size(40f, 40f))
                            drawRect(color = Color.Black, topLeft = Offset(size.width - 60f, size.height - 60f), size = androidx.compose.ui.geometry.Size(20f, 40f))
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Global Cryptographic verification ID:",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "SHA256-RIMS-0xFFFF-829104",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun PatientDetailedReportDialog(
    scan: PatientScan,
    viewModel: RimsViewModel,
    onClose: () -> Unit
) {
    var shareNotice by remember { mutableStateOf<String?>(null) }
    var contrastValue by remember { mutableStateOf(1.0f) }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${scan.scanType} Clinical Report",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Embedded PACS Reader
                Text(
                    text = "RADIOGRAPHIC VIEWER",
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Interactive radiograph
                PacsViewerCanvas(
                    scan = scan,
                    zoom = 1f,
                    contrast = contrastValue,
                    invert = false,
                    sideBySide = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    viewModel = viewModel,
                    allowAnnotations = false
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Adjustment slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Contrast adjuster", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Interactive Contrast: ", fontSize = 11.sp, color = Color.Gray)
                    Slider(
                        value = contrastValue,
                        onValueChange = { contrastValue = it },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Official Letterhead and findings
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    border = BorderStroke(1.dp, Color(0xFFE1E3E1))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "X-REPORTING DEPT",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                            Text(
                                "#CASE-${scan.id}",
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Patient: ${scan.patientName} (${scan.patientGender}, Age ${scan.patientAge})", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
                        Text("Study Date: ${scan.scanDate}", fontSize = 12.sp, color = Color.Gray)
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFE1E3E1))
                        Spacer(modifier = Modifier.height(14.dp))

                        Text("CLINICAL SCAN FINDINGS:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = scan.radiologistImpression.ifEmpty { "Clinical draft report in preparation. Check alerts later." },
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        if (scan.aiGeneratedImpression.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Android, contentDescription = "AI Assist", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("AI-GENERATED IMPRESSION:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(scan.aiGeneratedImpression, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }

                        if (scan.doctorComments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("ATTENDING CLINICAL COMMENTS:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary)
                            Text(scan.doctorComments, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = Color(0xFFE1E3E1))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Radiologist & Doctor Electronic Credentials
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text("RADIOLOGIST SIGN SIGN-OFF", fontSize = 9.sp, color = Color.Gray)
                                Text(scan.signedBy.ifEmpty { "UNSIGNED" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (scan.isSigned) Color(0xFF4CAF50) else Color(0xFFE53935))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("CLINICAL DEPT APPROVAL", fontSize = 9.sp, color = Color.Gray)
                                Text(scan.approvedBy.ifEmpty { "PENDING" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (scan.isApproved) Color(0xFF4CAF50) else Color(0xFFE53935))
                            }
                        }
                    }
                }

                shareNotice?.let { notice ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(notice, color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { shareNotice = "PDF securely shared to clinical care team via encrypted link." },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share text", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Secure Share", color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(
                    onClick = { shareNotice = "PDF loaded into local Android Downloads storage." },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download text", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download Report", color = MaterialTheme.colorScheme.onSecondary)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

// --------------------------------------------------
// C. DOCTOR APP DASHBOARD
// --------------------------------------------------
@Composable
fun DoctorDashboard(viewModel: RimsViewModel) {
    val scans by viewModel.allScans.collectAsState()
    val selectedScan by viewModel.selectedScan.collectAsState()
    
    var diagnosticComments by remember { mutableStateOf("") }
    var contrast by remember { mutableStateOf(1f) }
    var sideBySide by remember { mutableStateOf(false) }
    var showNotif by remember { mutableStateOf(false) }
    var isApproving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MainTitleBar(
                    roleTitle = "DOCTOR PORTAL: DR. GREGORY HOUSE",
                    roleIcon = Icons.Default.MedicalServices,
                    viewModel = viewModel,
                    onNotificationsClick = { showNotif = true }
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Column: Patient Case Queues
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "CLINICAL APPROVAL QUEUE",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(scans) { item ->
                                val actsColor = if (selectedScan?.id == item.id) MaterialTheme.colorScheme.primary else Color.Transparent
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(if (selectedScan?.id == item.id) 2.dp else 1.dp, if (selectedScan?.id == item.id) MaterialTheme.colorScheme.primary else Color(0xFFE1E3E1)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectScan(item) }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(item.patientName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (item.priority == "EMERGENCY") MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                        else if (item.priority == "URGENT") MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                                        else Color.Gray.copy(alpha = 0.15f),
                                                        CircleShape
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    item.priority,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (item.priority == "EMERGENCY") MaterialTheme.colorScheme.error
                                                    else if (item.priority == "URGENT") MaterialTheme.colorScheme.primary
                                                    else Color(0xFF44474E)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text(item.scanType, fontSize = 11.sp, color = Color.Gray)
                                            Text(
                                                if (item.isApproved) "APPROVED" else "PENDING",
                                                fontSize = 9.sp,
                                                color = if (item.isApproved) Color(0xFF4CAF50) else Color(0xFFE53935),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Column: PACS Radiograph & Approval Board
                    selectedScan?.let { scan ->
                        Column(
                            modifier = Modifier
                                .weight(2f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "PACS IMAGING INTERACTIVE WORKSPACE",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Interactive Canvas PACS Drawer
                            PacsViewerCanvas(
                                scan = scan,
                                zoom = 1.2f,
                                contrast = contrast,
                                invert = false,
                                sideBySide = sideBySide,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                viewModel = viewModel,
                                allowAnnotations = false
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // PACS Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { sideBySide = !sideBySide },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Compare, contentDescription = "Compare side by side", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Side-by-Side", fontSize = 10.sp, color = Color.Black)
                                }
                                Box(modifier = Modifier.weight(1.5f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Tune, contentDescription = "Contrast icon", modifier = Modifier.size(14.dp), tint = Color.Gray)
                                        Slider(
                                            value = contrast,
                                            onValueChange = { contrast = it },
                                            valueRange = 0.5f..2.5f,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Findings Board
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("RADIOLOGIST IN-PROGRESS WORK", fontSize = 10.sp, color = Color.Gray)
                                    Text(
                                        scan.signedBy.ifEmpty { "Draft status" }.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = scan.radiologistImpression.ifEmpty { "Radiologist has not saved impressions yet." },
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Doctor Action form
                            Text("ATTENDING PHYSICIAN DECISION BOARD", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = diagnosticComments,
                                onValueChange = { diagnosticComments = it },
                                label = { Text("Attending Doctor Comments") },
                                placeholder = { Text("Add instructions or lab approvals here...") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.approveReport(diagnosticComments)
                                        diagnosticComments = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("approve_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Approve", tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Approve Case", color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.triggerEmergencyAlert() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("emergency_button")
                                ) {
                                    Icon(Icons.Default.Emergency, contentDescription = "Emergency", tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("FLAG EMERGENCY", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (showNotif) {
                DrawerNotificationsHub("DOCTOR", viewModel, onClose = { showNotif = false })
            }
        }
    }
}

// --------------------------------------------------
// D. RADIOLOGIST APP WORKSPACE
// --------------------------------------------------
@Composable
fun RadiologistDashboard(viewModel: RimsViewModel) {
    val scans by viewModel.allScans.collectAsState()
    val selectedScan by viewModel.selectedScan.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()

    val pacsZoom by viewModel.pacsZoom.collectAsState()
    val pacsContrast by viewModel.pacsContrast.collectAsState()
    val pacsInvert by viewModel.pacsInvert.collectAsState()
    val pacsSideBySide by viewModel.pacsSideBySide.collectAsState()

    var annotationText by remember { mutableStateOf("") }
    var showNotif by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                MainTitleBar(
                    roleTitle = "RADIOLOGIST ENGINE: DR. BOWMAN",
                    roleIcon = Icons.Default.MedicalServices,
                    viewModel = viewModel,
                    onNotificationsClick = { showNotif = true }
                )

                Row(modifier = Modifier.fillMaxSize()) {
                    // Queue Priority Column
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "PRIORITIZED CASES QUEUE",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(scans) { item ->
                                val actsColor = if (selectedScan?.id == item.id) MaterialTheme.colorScheme.primary else Color.Transparent
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(if (selectedScan?.id == item.id) 2.dp else 1.dp, if (selectedScan?.id == item.id) MaterialTheme.colorScheme.primary else Color(0xFFE1E3E1)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectScan(item) }
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(item.patientName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (item.priority == "EMERGENCY") MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                        else if (item.priority == "URGENT") MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                                        else Color.Gray.copy(alpha = 0.15f),
                                                        CircleShape
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    item.priority,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (item.priority == "EMERGENCY") MaterialTheme.colorScheme.error
                                                    else if (item.priority == "URGENT") MaterialTheme.colorScheme.primary
                                                    else Color(0xFF44474E)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text(item.scanType, fontSize = 11.sp, color = Color.Gray)
                                            Text(
                                                text = if (item.isSigned) "SIGNED" else "DRAFT",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 9.sp,
                                                color = if (item.isSigned) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // RADIOLOGIST REPORTING CANVAS & CONTROLS
                    selectedScan?.let { scan ->
                        Column(
                            modifier = Modifier
                                .weight(2.2f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "PACS INTERACTIVE DIAGNOSTIC SCREEN",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            // Multi-touch Zoom/contrast PACS Canvas block
                            PacsViewerCanvas(
                                scan = scan,
                                zoom = pacsZoom,
                                contrast = pacsContrast,
                                invert = pacsInvert,
                                sideBySide = pacsSideBySide,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp),
                                viewModel = viewModel,
                                allowAnnotations = true
                            )

                            Text("Tap radiograph above to learn structure, or flag coordinates", fontSize = 10.sp, color = Color.LightGray, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))

                            // PACS Controls toolbox Row
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { viewModel.adjustZoom(0.3f) },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                    ) { Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White) }
                                    
                                    IconButton(
                                        onClick = { viewModel.adjustZoom(-0.3f) },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                    ) { Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White) }

                                    IconButton(
                                        onClick = { viewModel.toggleInvert() },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                    ) { Icon(Icons.Default.InvertColors, contentDescription = "Invert window", tint = Color.White) }

                                    IconButton(
                                        onClick = { viewModel.toggleSideBySide() },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                    ) { Icon(Icons.Default.Compare, contentDescription = "Compare reference", tint = Color.White) }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune, contentDescription = "Contrast tuning", modifier = Modifier.size(16.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Slider(
                                        value = pacsContrast,
                                        onValueChange = { viewModel.adjustContrast(it) },
                                        valueRange = 0.5f..2.5f,
                                        modifier = Modifier.width(100.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Smart reporting templates Card
                            Text("DIAGNOSTIC REPORT BUILDER", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.loadSmartTemplate("Normal Chest Template") },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Normal PA Chest", fontSize = 9.sp, color = Color.White) }

                                Button(
                                    onClick = { viewModel.loadSmartTemplate("Fracture Bone Template") },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Bone Frac Temp", fontSize = 9.sp, color = Color.White) }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Findings Text field and electronic signatures
                            OutlinedTextField(
                                value = scan.radiologistImpression,
                                onValueChange = { viewModel.updateDraftImpression(it) },
                                label = { Text("Radiographic Findings & Impressions") },
                                placeholder = { Text("Draft your radiological observations here...") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 10
                            )

                            if (isAiLoading) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI Copilot synthesizing radiology narrative...", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive AI Copilot Toolbar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.triggerAiAssistance() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.weight(1f).testTag("trigger_ai")
                                ) {
                                    Icon(Icons.Default.Android, contentDescription = "AI assistant", tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("AI Impressions", fontSize = 10.sp, color = Color.Black)
                                }

                                Button(
                                    onClick = { viewModel.toggleVoiceDictation() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                                    ),
                                    modifier = Modifier.weight(1f).testTag("dictate_button")
                                ) {
                                    Icon(
                                        imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "Voice Dictate",
                                        tint = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isRecording) "Stop Rec" else "Dictation Voice", fontSize = 10.sp, color = Color.Black)
                                }
                            }

                            if (scan.aiGeneratedImpression.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .border(0.5.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("AI COPILOT STATEMENT:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        Text(scan.aiGeneratedImpression, fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Signature Submission block
                            Button(
                                onClick = { viewModel.signReport() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("sign_report_button")
                            ) {
                                Icon(Icons.Default.BorderColor, contentDescription = "Sign report with pen", tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SIGN REPORT & TRANSMIT TO CLINIC", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }

            if (showNotif) {
                DrawerNotificationsHub("RADIOLOGIST", viewModel, onClose = { showNotif = false })
            }
        }
    }
}

// Inline custom coloring ternary replacement checker
fun colorBorderForSelection(isSelected: Boolean, primary: Color, other: Color): Color {
    return if (isSelected) primary else other
}
