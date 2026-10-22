package com.example.ui.screens.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.api.GeminiApiClient
import com.example.data.preferences.AssistantPreferences
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.WarningAmber
import com.example.voice.VoiceAssistantManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Real-Time Multimodal Camera Vision Hub powered by Gemini API.
 * Streams live CameraX frames, analyzes objects, scenes, text, and answers user questions in real-time.
 */
@Composable
fun CameraVisionScreen(
    geminiApiClient: GeminiApiClient,
    voiceManager: VoiceAssistantManager,
    preferences: AssistantPreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Camera Permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Camera state
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControlInstance by remember { mutableStateOf<Camera?>(null) }
    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }
    var frozenSnapshot by remember { mutableStateOf<Bitmap?>(null) }

    // Real-Time Analysis state
    var isLiveStreamingMode by remember { mutableStateOf(false) }
    var isAnalyzingFrame by remember { mutableStateOf(false) }
    var userQuestionText by remember { mutableStateOf("") }
    var visionReportText by remember { mutableStateOf<String?>(null) }
    var lastAnalyzedTimestamp by remember { mutableStateOf<String?>(null) }

    // Fallback Image Picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            frozenSnapshot = bmp
            visionReportText = null
        }
    }

    val multimodalRepo = remember { com.example.data.repository.GeminiMultimodalRepository() }

    // Function to capture and analyze current frame
    fun processCurrentFrame(query: String) {
        val bitmapToAnalyze = frozenSnapshot ?: previewViewInstance?.bitmap
        if (bitmapToAnalyze == null) {
            visionReportText = "ক্যামেরা ফ্রেম গ্রহণ করা সম্ভব হয়নি। অনুগ্রহ করে ক্যামেরা চালু থাকা নিশ্চিত করুন।"
            return
        }

        isAnalyzingFrame = true
        coroutineScope.launch {
            val result = multimodalRepo.analyzeFrameQuestion(
                bitmap = bitmapToAnalyze,
                question = query,
                ownerName = preferences.ownerTitle,
                customApiKey = preferences.customApiKey.ifBlank { null }
            )
            val report = result.getOrElse {
                geminiApiClient.analyzeCameraFrame(
                    bitmap = bitmapToAnalyze,
                    question = query,
                    customApiKey = preferences.customApiKey.ifBlank { null },
                    ownerName = preferences.ownerTitle
                )
            }
            visionReportText = report
            isAnalyzingFrame = false
            lastAnalyzedTimestamp = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())
            voiceManager.speak(report)
        }
    }

    // Continuous Real-Time Vision Loop (when isLiveStreamingMode is enabled)
    LaunchedEffect(isLiveStreamingMode, hasCameraPermission) {
        while (isLiveStreamingMode && hasCameraPermission) {
            if (!isAnalyzingFrame) {
                previewViewInstance?.bitmap?.let { currentBmp ->
                    isAnalyzingFrame = true
                    val autoResult = multimodalRepo.describeObjectsInFrame(
                        bitmap = currentBmp,
                        ownerName = preferences.ownerTitle,
                        customApiKey = preferences.customApiKey.ifBlank { null }
                    )
                    val autoReport = autoResult.getOrElse {
                        geminiApiClient.analyzeCameraFrame(
                            bitmap = currentBmp,
                            question = "ক্যামেরার ফ্রেমে বর্তমানে কী কী বস্তু বা পরিবর্তন দৃশ্যমান তা এক-দুই বাক্যে সংক্ষেপে বলো।",
                            customApiKey = preferences.customApiKey.ifBlank { null },
                            ownerName = preferences.ownerTitle
                        )
                    }
                    visionReportText = autoReport
                    isAnalyzingFrame = false
                    lastAnalyzedTimestamp = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date())
                    voiceManager.speak(autoReport)
                }
            }
            delay(3500) // Frame cadence to respect rate limits and conserve battery
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header with Live Status & Mode
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "রিয়েল-টাইম এআই ক্যামেরা ভিশন",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = "${preferences.ownerTitle}-এর জন্য Gemini Multimodal রিয়েল-টাইম ফ্রেম অ্যানালাইসিস",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    color = if (isLiveStreamingMode) Color(0xFFEF4444).copy(alpha = 0.2f) else EmeraldPrimary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isLiveStreamingMode) Color(0xFFEF4444) else EmeraldPrimary)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isLiveStreamingMode) Color(0xFFEF4444) else EmeraldPrimary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLiveStreamingMode) "লাইভ ভিশন" else "স্ট্যান্ডবাই",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLiveStreamingMode) Color(0xFFEF4444) else EmeraldPrimary
                        )
                    }
                }
            }
        }

        // 2. Camera Viewfinder or Permission Request Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF030712)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isLiveStreamingMode) Color(0xFFEF4444) else EmeraldPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .testTag("camera_viewfinder_card")
            ) {
                if (!hasCameraPermission) {
                    // Permission Request Prompt
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ক্যামেরা ব্যবহারের অনুমতি প্রয়োজন",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ক্যামেরার দৃশ্যমান বস্তু শনাক্তকরণ ও Gemini Multimodal অ্যানালাইসিসের জন্য ক্যামেরা পারমিশন দিন।",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.testTag("request_camera_permission_button")
                        ) {
                            Text("ক্যামেরা সক্রিয় করুন", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (frozenSnapshot != null) {
                    // Show Frozen Snapshot preview
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = frozenSnapshot!!.asImageBitmap(),
                            contentDescription = "ক্যাপচারকৃত ফ্রেম",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Top badge
                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "স্থির ফ্রেম প্রস্তুত",
                                color = EmeraldPrimary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Retake / Resume live button
                        OutlinedButton(
                            onClick = { frozenSnapshot = null },
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                        ) {
                            Text("লাইভ ক্যামেরায় ফিরুন", color = Color.White, fontSize = 11.sp)
                        }
                    }
                } else {
                    // Live CameraX Preview
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    previewViewInstance = this

                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = this.surfaceProvider
                                        }

                                        val cameraSelector = CameraSelector.Builder()
                                            .requireLensFacing(lensFacing)
                                            .build()

                                        try {
                                            cameraProvider.unbindAll()
                                            val cam = cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                cameraSelector,
                                                preview
                                            )
                                            cameraControlInstance = cam
                                        } catch (e: Exception) {
                                            Log.e("CameraVisionScreen", "Camera binding failed", e)
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                }
                            },
                            update = { view ->
                                previewViewInstance = view
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Viewfinder Overlay Reticle
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(160.dp)
                                .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        )

                        // Overlay Live Controls Bar (Flash, Flip, Snapshot)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Flashlight Toggle
                            IconButton(
                                onClick = {
                                    isTorchOn = !isTorchOn
                                    cameraControlInstance?.cameraControl?.enableTorch(isTorchOn)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "ফ্ল্যাশ",
                                    tint = if (isTorchOn) WarningAmber else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Camera Flip Toggle
                            IconButton(
                                onClick = {
                                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                        CameraSelector.LENS_FACING_FRONT
                                    } else {
                                        CameraSelector.LENS_FACING_BACK
                                    }
                                    // Trigger camera rebinding
                                    previewViewInstance?.let { pv ->
                                        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                                        cameraProvider.unbindAll()
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = pv.surfaceProvider
                                        }
                                        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                                        cameraControlInstance = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "ক্যামেরা পরিবর্তন",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Bottom Capture Snapshot button
                        IconButton(
                            onClick = {
                                previewViewInstance?.bitmap?.let { bmp ->
                                    frozenSnapshot = bmp
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .size(48.dp)
                                .background(EmeraldPrimary, CircleShape)
                                .testTag("capture_frame_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "ফ্রেম ক্যাপচার",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Real-Time Mode Switch & Question Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Continuous Live Stream Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (isLiveStreamingMode) Color(0xFFEF4444) else EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "স্বয়ংক্রিয় লাইভ বিশ্লেষণ মোড",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isLiveStreamingMode) "ক্যামেরা ফ্রেম প্রতি ৩ সেকেন্ডে অটো বিশ্লেষণ হচ্ছে" else "চালু করলে ক্যামেরা ফ্রেম নিজে থেকেই ব্যাখ্যা করবে",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Switch(
                            checked = isLiveStreamingMode,
                            onCheckedChange = { isLiveStreamingMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // User Question Input Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = userQuestionText,
                            onValueChange = { userQuestionText = it },
                            placeholder = {
                                Text("এই বস্তু/দৃশ্য সম্পর্কে প্রশ্ন লিখুন...", fontSize = 12.sp, color = Color.Gray)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = Color(0xFF334155),
                                cursorColor = EmeraldPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("camera_question_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Trigger Voice Question
                        IconButton(
                            onClick = {
                                voiceManager.startListening()
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "ভয়েস প্রশ্ন",
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Send question button
                        Button(
                            onClick = {
                                processCurrentFrame(userQuestionText)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("camera_analyze_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "বিশ্লেষণ",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Quick Preset Question Chips
        item {
            Text(
                text = "💡 দ্রুত জিজ্ঞাসা:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(4.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val presets = listOf(
                    "🔍 কী দেখতে পাচ্ছ?" to "ক্যামেরার ফ্রেমে দৃশ্যমান সমস্ত বস্তু বিশদভাবে বাংলায় বুঝিয়ে বলো।",
                    "🏷️ অবজেক্ট ও সংখ্যা" to "ফ্রেমে কতগুলো আলাদা বস্তু আছে তা শনাক্ত ও গণনা করে তালিকা দাও।",
                    "📝 লেখাগুলো পড়ো (OCR)" to "এই ফ্রেমে দৃশ্যমান সমস্ত লেখা ও টেক্সট নির্ভুলভাবে পড়ে বাংলায় অনুবাদ করো।",
                    "🎨 রং ও পরিবেশ" to "দৃশ্যটির প্রধান রং, আলো ও পরিবেশের বিবরণ দাও।",
                    "💡 এই জিনিসের কাজ কী?" to "ক্যামেরার সামনে থাকা প্রধান বস্তুটির ব্যবহার ও কার্যকারিতা বাংলায় বুঝিয়ে বলো।"
                )

                items(presets) { (label, prompt) ->
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.clickable {
                            userQuestionText = label
                            processCurrentFrame(prompt)
                        }
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = NeonGreen,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 5. Analysis Progress Indicator
        if (isAnalyzingFrame) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gemini Multimodal API ফ্রেম প্রসেস করছে...",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 6. Gemini Multimodal Vision Report Card
        if (visionReportText != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("gemini_vision_report_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gemini ভিশন বিশ্লেষণ",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                lastAnalyzedTimestamp?.let { ts ->
                                    Text(
                                        text = ts,
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        voiceManager.speak(visionReportText!!)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = "ভয়েসে শোনো",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = visionReportText!!,
                            color = Color(0xFFF1F5F9),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Feedback action row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = EmeraldPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Multimodal: gemini-3.5-flash",
                                    color = EmeraldPrimary,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                color = CyberCyan.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "রিয়েল-টাইম ক্যামেরা ভিশন",
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
