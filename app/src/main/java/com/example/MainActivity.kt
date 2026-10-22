package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DynamicForm
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.api.GeminiApiClient
import com.example.data.model.AuditLogEntry
import com.example.data.model.ChatMessage
import com.example.data.model.ClassifiedCommand
import com.example.data.model.CommandAction
import com.example.data.model.CommandCategory
import com.example.data.model.ScreenAnalysisResult
import com.example.data.model.Sender
import com.example.data.model.UserMemoryItem
import com.example.data.preferences.AssistantPreferences
import com.example.device.DeviceIntelligenceManager
import com.example.domain.CommandRouter
import com.example.service.FloatingAssistantService
import com.example.service.MairaAccessibilityService
import com.example.service.ScreenHelpCapture
import com.example.service.ScreenRecordingService
import com.example.ui.components.FloatingOrb
import com.example.ui.components.OnboardingWalkthroughDialog
import com.example.ui.screens.browser.WebKnowledgeScreen
import com.example.ui.screens.camera.CameraVisionScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.command.CommandCenterScreen
import com.example.ui.screens.dev.DeveloperWorkspaceScreen
import com.example.ui.screens.intelligence.DeviceIntelligenceScreen
import com.example.ui.screens.recorder.ScreenRecorderScreen
import com.example.ui.screens.screenhelp.ScreenHelpScreen
import com.example.ui.screens.settings.SettingsScreen
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.SportsEsports
import com.example.data.firebase.FirebaseManager
import com.example.ui.screens.creative.CreativeStudioScreen
import com.example.ui.screens.game.ThreeJsGameScreen
import com.example.ui.screens.science.BioScienceScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.nexus.ExecutionStep
import com.example.nexus.NexusCognitiveEngine
import com.example.nexus.NexusEngineProvider
import com.example.nexus.NexusExecutionListener
import com.example.nexus.ReActPlanResult
import com.example.voice.VoiceAssistantManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var preferences: AssistantPreferences
    private lateinit var geminiApiClient: GeminiApiClient
    private lateinit var voiceManager: VoiceAssistantManager
    private lateinit var screenHelpCapture: ScreenHelpCapture
    private lateinit var deviceIntelManager: DeviceIntelligenceManager
    private lateinit var cognitiveEngine: NexusCognitiveEngine
    private lateinit var firebaseManager: FirebaseManager

    private var currentTaskStep by mutableStateOf<String?>(null)
    private val messages = mutableStateListOf<ChatMessage>()
    private val memories = mutableStateListOf<UserMemoryItem>()

    private var pendingMediaProjectionAction: String? = null // "HELP" or "RECORD"
    private var pendingScreenHelpQuery: String? = null

    // Screen Help State
    private var isAnalyzingScreen by mutableStateOf(false)
    private var screenHelpResult by mutableStateOf<ScreenAnalysisResult?>(null)

    // Current Navigation tab index
    // 1: MYRA AI Hub, 0: Chat, 2: Camera, 3: Screen Help, 4: Dev, 5: Browser, 6: Recorder, 7: Device Intel, 8: Settings
    private var selectedTabIndex by mutableIntStateOf(1)
    private var externalSearchQuery by mutableStateOf<String?>(null)
    private var pendingConfirmationCommand by mutableStateOf<ClassifiedCommand?>(null)

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        // Permissions updated
    }

    // MediaProjection Launcher
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            when (pendingMediaProjectionAction) {
                "HELP" -> {
                    handleScreenHelpCapture(result.resultCode, result.data!!)
                }
                "RECORD" -> {
                    startRecordingService(result.resultCode, result.data!!)
                }
            }
        } else {
            Toast.makeText(this, "স্ক্রিন ব্যবহারের অনুমতি পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            isAnalyzingScreen = false
        }
        pendingMediaProjectionAction = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferences = AssistantPreferences(this)
        geminiApiClient = GeminiApiClient()
        voiceManager = VoiceAssistantManager(this, preferences)
        screenHelpCapture = ScreenHelpCapture(this, geminiApiClient)
        deviceIntelManager = DeviceIntelligenceManager(this)
        cognitiveEngine = NexusEngineProvider.getCognitiveEngine(this, geminiApiClient)

        cognitiveEngine.addListener(object : NexusExecutionListener {
            override fun onStepStarted(step: ExecutionStep) {
                currentTaskStep = step.taskTag
            }

            override fun onStepCompleted(step: ExecutionStep) {
                currentTaskStep = step.taskTag
            }

            override fun onPlanFinished(result: ReActPlanResult) {
                currentTaskStep = null
            }
        })

        firebaseManager = FirebaseManager(this)
        memories.addAll(preferences.getMemories())

        CoroutineScope(Dispatchers.IO).launch {
            val cloudMemories = firebaseManager.fetchCloudMemories()
            if (cloudMemories.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    cloudMemories.forEach { cm ->
                        if (memories.none { it.key.equals(cm.key, ignoreCase = true) }) {
                            memories.add(cm)
                        }
                    }
                    preferences.saveMemories(memories)
                }
            }
        }

        // Initial Greeting for Alom Boss
        val greeting = "হ্যালো Alom Boss! আমি আপনার Maira Bot। সমস্ত সিস্টেম এবং ভয়েস ফিচার সচল রয়েছে।"
        if (preferences.isFirstLaunch) {
            messages.add(
                ChatMessage(
                    text = greeting,
                    sender = Sender.ASSISTANT,
                    suggestedActions = listOf(
                        CommandAction(label = "ডিভাইস হেলথ", actionType = "DEVICE_INTEL"),
                        CommandAction(label = "ক্যামেরা ভিশন", actionType = "CAMERA"),
                        CommandAction(label = "কোড এডিটর", actionType = "DEV"),
                        CommandAction(label = "Wi-Fi সেটিংস", actionType = "SETTINGS", payload = Settings.ACTION_WIFI_SETTINGS)
                    )
                )
            )
            if (!preferences.firstGreetingGiven) {
                voiceManager.speak(greeting)
                preferences.firstGreetingGiven = true
            }
        } else {
            messages.add(
                ChatMessage(
                    text = greeting,
                    sender = Sender.ASSISTANT,
                    suggestedActions = listOf(
                        CommandAction(label = "ডিভাইস হেলথ", actionType = "DEVICE_INTEL"),
                        CommandAction(label = "ক্যামেরা ভিশন", actionType = "CAMERA"),
                        CommandAction(label = "কোড এডিটর", actionType = "DEV"),
                        CommandAction(label = "স্ক্রিন রেকর্ডার", actionType = "RECORDER")
                    )
                )
            )
        }

        // Setup voice callbacks
        voiceManager.onCommandRecognized = { text ->
            handleUserQuery(text)
        }

        voiceManager.onSessionDismissRequested = {
            moveTaskToBack(true)
        }

        voiceManager.onRestModeRequested = {
            Toast.makeText(this, "সহকারী বিশ্রাম মোডে আছে", Toast.LENGTH_SHORT).show()
        }

        // Request initial permissions
        requestAppPermissions()

        // Handle incoming intent (e.g. from floating bubble or notification)
        handleIntentTriggers(intent)

        setContent {
            var currentTheme by remember { mutableStateOf(preferences.themeMode) }
            var showOnboarding by remember { mutableStateOf(preferences.isFirstLaunch) }
            var alwaysOnMicActive by remember { mutableStateOf(preferences.alwaysOnMicEnabled) }

            val voiceState by voiceManager.voiceState.collectAsState()
            val audioAmplitude by voiceManager.audioAmplitude.collectAsState()
            val isRecording by ScreenRecordingService.isRecording.collectAsState()
            val isPaused by ScreenRecordingService.isPaused.collectAsState()
            val elapsedSeconds by ScreenRecordingService.elapsedSeconds.collectAsState()
            val lastSavedVideoUri by ScreenRecordingService.lastSavedVideoUri.collectAsState()

            MyApplicationTheme(themeMode = currentTheme) {
                Scaffold(
                    topBar = {
                        Surface(
                            color = Color(0xFF112240),
                            border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth().statusBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Maira Bot VIP",
                                    color = Color(0xFF00E676),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = Color(0xFF00E676).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFF00E676))
                                ) {
                                    Text(
                                        text = "অনলাইন ও সচল",
                                        color = Color(0xFF00E676),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    },
                    bottomBar = {
                        BanglaAiBottomNavigation(
                            selectedIndex = selectedTabIndex,
                            onTabSelected = { selectedTabIndex = it }
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (selectedTabIndex) {
                            0 -> ChatScreen(
                                messages = messages,
                                voiceState = voiceState,
                                amplitude = audioAmplitude,
                                isMuted = preferences.isMuted,
                                handsFreeActive = preferences.handsFreeEnabled,
                                memories = memories,
                                onSendMessage = { handleUserQuery(it) },
                                onStartVoice = { voiceManager.startListening() },
                                onStopVoice = { voiceManager.stopListening() },
                                onStopSpeaking = { voiceManager.stopSpeaking() },
                                onToggleMute = {
                                    preferences.isMuted = !preferences.isMuted
                                    if (preferences.isMuted) voiceManager.stopSpeaking()
                                },
                                onToggleHandsFree = {
                                    preferences.handsFreeEnabled = !preferences.handsFreeEnabled
                                },
                                onActionClicked = { action -> handleCommandAction(action) },
                                onAddMemory = { k, v ->
                                    val item = UserMemoryItem(key = k, value = v)
                                    memories.add(item)
                                    preferences.saveMemories(memories)
                                },
                                onDeleteMemory = { id ->
                                    memories.removeAll { it.id == id }
                                    preferences.saveMemories(memories)
                                },
                                currentTaskStep = currentTaskStep
                            )

                            1 -> CommandCenterScreen(
                                onExecuteAction = { cmd -> executeClassifiedCommand(cmd) },
                                onNavigateToDev = {
                                    selectedTabIndex = 4
                                },
                                onNavigateToBrowser = { query ->
                                    externalSearchQuery = query
                                    selectedTabIndex = 5
                                },
                                onNavigateToScreenHelp = {
                                    selectedTabIndex = 3
                                },
                                preferences = preferences,
                                onRequestAllPermissions = {
                                    requestAppPermissions()
                                    requestOverlayPermission()
                                    preferences.allPermissionsGranted = true
                                    Toast.makeText(this@MainActivity, "সকল পারমিশন সফলভাবে সক্রিয় করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                onToggleAlwaysOnMic = {
                                    val newState = !preferences.alwaysOnMicEnabled
                                    preferences.alwaysOnMicEnabled = newState
                                    alwaysOnMicActive = newState
                                    if (newState) {
                                        startFloatingService()
                                        voiceManager.startListening()
                                        Toast.makeText(this@MainActivity, "মাইক্রোফোন অলওয়েজ-অন মোডে চালু হয়েছে।", Toast.LENGTH_SHORT).show()
                                    } else {
                                        voiceManager.stopListening()
                                        Toast.makeText(this@MainActivity, "অলওয়েজ-অন মাইক্রোফোন বন্ধ করা হয়েছে।", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                isAlwaysOnMicActive = alwaysOnMicActive
                            )

                            2 -> CameraVisionScreen(
                                geminiApiClient = geminiApiClient,
                                voiceManager = voiceManager,
                                preferences = preferences
                            )

                            3 -> ScreenHelpScreen(
                                isAnalyzing = isAnalyzingScreen,
                                analysisResult = screenHelpResult,
                                onRequestScreenCapture = { query ->
                                    pendingMediaProjectionAction = "HELP"
                                    pendingScreenHelpQuery = query
                                    launchMediaProjection()
                                },
                                onStopSharing = {
                                    screenHelpResult = null
                                },
                                onOpenSettingIntent = { intentAction ->
                                    try {
                                        startActivity(Intent(intentAction))
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "সেটিংস খুলতে ব্যর্থ", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            4 -> DeveloperWorkspaceScreen(
                                geminiApiClient = geminiApiClient
                            )

                            5 -> WebKnowledgeScreen(
                                initialUrlOrQuery = externalSearchQuery ?: "https://www.google.com",
                                geminiApiClient = geminiApiClient
                            )

                            6 -> ScreenRecorderScreen(
                                isRecording = isRecording,
                                isPaused = isPaused,
                                elapsedSeconds = elapsedSeconds,
                                lastSavedUri = lastSavedVideoUri,
                                onRequestStartRecording = {
                                    pendingMediaProjectionAction = "RECORD"
                                    launchMediaProjection()
                                },
                                onPauseRecording = {
                                    startService(Intent(this@MainActivity, ScreenRecordingService::class.java).apply {
                                        action = ScreenRecordingService.ACTION_PAUSE
                                    })
                                },
                                onResumeRecording = {
                                    startService(Intent(this@MainActivity, ScreenRecordingService::class.java).apply {
                                        action = ScreenRecordingService.ACTION_RESUME
                                    })
                                },
                                onStopRecording = {
                                    startService(Intent(this@MainActivity, ScreenRecordingService::class.java).apply {
                                        action = ScreenRecordingService.ACTION_STOP
                                    })
                                }
                            )

                            7 -> DeviceIntelligenceScreen(
                                deviceIntelManager = deviceIntelManager,
                                voiceManager = voiceManager,
                                ownerTitle = preferences.ownerTitle,
                                onLaunchApp = { pkg -> launchApplicationSafely(pkg) },
                                onOpenSetting = { action ->
                                    try {
                                        startActivity(Intent(action))
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "সেটিংস খুলতে ব্যর্থ", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            8 -> SettingsScreen(
                                preferences = preferences,
                                onFloatingToggled = { enabled ->
                                    if (enabled) {
                                        if (Settings.canDrawOverlays(this@MainActivity)) {
                                            startFloatingService()
                                        } else {
                                            requestOverlayPermission()
                                        }
                                    } else {
                                        stopFloatingService()
                                    }
                                },
                                onThemeChanged = { newTheme ->
                                    currentTheme = newTheme
                                },
                                onRelaunchOnboarding = {
                                    showOnboarding = true
                                }
                            )

                            9 -> ThreeJsGameScreen()

                            10 -> BioScienceScreen(
                                geminiApiClient = geminiApiClient
                            )

                            11 -> CreativeStudioScreen(
                                geminiApiClient = geminiApiClient
                            )
                        }

                        // Sensitive Action Confirmation Dialog
                        pendingConfirmationCommand?.let { cmd ->
                            AlertDialog(
                                onDismissRequest = { pendingConfirmationCommand = null },
                                title = {
                                    Text("নিশ্চিতকরণ প্রয়োজন", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                },
                                text = {
                                    Column {
                                        Text(cmd.titleBn, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(cmd.explanationBn, fontSize = 12.sp, color = Color.Gray)
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val target = cmd
                                            pendingConfirmationCommand = null
                                            executeConfirmedCommand(target)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                    ) {
                                        Text("হ্যাঁ, সম্পাদন করো", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { pendingConfirmationCommand = null }) {
                                        Text("বাতিল")
                                    }
                                }
                            )
                        }

                        // First launch onboarding
                        if (showOnboarding) {
                            OnboardingWalkthroughDialog(
                                onDismiss = {
                                    showOnboarding = false
                                    preferences.isFirstLaunch = false
                                }
                            )
                        }

                        // Persistent on-screen Floating Orb expanding into Chat Interface
                        FloatingOrb(
                            externalMessages = messages,
                            onSendMessage = { query -> handleUserQuery(query) },
                            onVoiceClick = {
                                if (voiceState == com.example.voice.VoiceState.LISTENING) {
                                    voiceManager.stopListening()
                                } else {
                                    voiceManager.triggerManualListening()
                                }
                            },
                            isThinking = (voiceState == com.example.voice.VoiceState.THINKING),
                            activeTaskStep = currentTaskStep,
                            onActionClicked = { action -> handleCommandAction(action) }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentTriggers(intent)
    }

    private fun handleIntentTriggers(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra("START_VOICE", false)) {
            selectedTabIndex = 0
            voiceManager.startListening()
        } else if (intent.getBooleanExtra("TRIGGER_SCREEN_HELP", false)) {
            selectedTabIndex = 3
            pendingMediaProjectionAction = "HELP"
            launchMediaProjection()
        } else if (intent.getBooleanExtra("NAV_INTEL", false)) {
            selectedTabIndex = 7
        }
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingAssistantService::class.java).apply {
            action = FloatingAssistantService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopFloatingService() {
        val intent = Intent(this, FloatingAssistantService::class.java).apply {
            action = FloatingAssistantService.ACTION_STOP
        }
        startService(intent)
    }

    private fun launchMediaProjection() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun handleScreenHelpCapture(resultCode: Int, data: Intent) {
        isAnalyzingScreen = true
        CoroutineScope(Dispatchers.Main).launch {
            val result = screenHelpCapture.captureAndAnalyze(
                resultCode = resultCode,
                resultData = data,
                customQuery = pendingScreenHelpQuery,
                apiKey = preferences.customApiKey.ifBlank { null }
            )
            screenHelpResult = result
            isAnalyzingScreen = false
            voiceManager.speak(result.fullExplanationBn)

            deviceIntelManager.recordAuditLog(
                AuditLogEntry(
                    actionTitleBn = "লাইভ স্ক্রিন বিশ্লেষণ",
                    actionTarget = "MediaProjection Screen Frame",
                    category = CommandCategory.SCREEN_HELP,
                    statusTextBn = "সফলভাবে বিশ্লেষণ সম্পন্ন"
                )
            )
        }
    }

    private fun startRecordingService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, ScreenRecordingService::class.java).apply {
            action = ScreenRecordingService.ACTION_START
            putExtra(ScreenRecordingService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordingService.EXTRA_RESULT_DATA, data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        selectedTabIndex = 6

        deviceIntelManager.recordAuditLog(
            AuditLogEntry(
                actionTitleBn = "স্ক্রিন রেকর্ডিং শুরু",
                actionTarget = "Screen Recording Service",
                category = CommandCategory.SCREEN_RECORD,
                statusTextBn = "রেকর্ডিং সার্ভিস চালু"
            )
        )
    }

    private fun handleUserQuery(query: String) {
        messages.add(ChatMessage(text = query, sender = Sender.USER))

        // Check if classified as an actionable command
        val cmd = CommandRouter.classify(query)
        if (cmd.category != CommandCategory.ANSWER_QUESTION && cmd.category != CommandCategory.UNSUPPORTED) {
            executeClassifiedCommand(cmd)
            return
        }

        // Execute through BD-NEXUS Autonomous ReAct Cognitive Engine
        CoroutineScope(Dispatchers.Main).launch {
            val result = cognitiveEngine.solveAutonomousProblem(
                query = query,
                personality = preferences.personality,
                ownerName = preferences.ownerTitle,
                customApiKey = preferences.customApiKey.ifBlank { null }
            )

            val response = result.finalResponseText
            val isCode = response.contains("```")
            val codeSnippet = if (isCode) {
                response.substringAfter("```").substringBefore("```").trim()
            } else null

            messages.add(
                ChatMessage(
                    text = response,
                    sender = Sender.ASSISTANT,
                    isCode = isCode,
                    codeSnippet = codeSnippet,
                    codeLanguage = if (isCode) "Kotlin/Python/JS" else null,
                    suggestedActions = listOf(
                        CommandAction(label = "ডিভাইস অডিট", actionType = "DEVICE_INTEL"),
                        CommandAction(label = "স্ক্রিন সাহায্য", actionType = "SCREEN_HELP"),
                        CommandAction(label = "কমান্ড হাব", actionType = "COMMANDS")
                    )
                )
            )

            voiceManager.speak(response)
        }
    }

    private fun executeClassifiedCommand(cmd: ClassifiedCommand) {
        if (cmd.requiresConfirmation) {
            pendingConfirmationCommand = cmd
        } else {
            executeConfirmedCommand(cmd)
        }
    }

    private fun executeConfirmedCommand(cmd: ClassifiedCommand) {
        val owner = preferences.ownerTitle
        when (cmd.category) {
            CommandCategory.APP_INVENTORY -> {
                selectedTabIndex = 7
                val apps = deviceIntelManager.getInstalledApplications()
                val speech = deviceIntelManager.getBengaliVoiceAppsSummary(apps, owner)
                messages.add(ChatMessage(text = speech, sender = Sender.ASSISTANT))
                voiceManager.speak(speech)
                deviceIntelManager.recordAuditLog(
                    AuditLogEntry(
                        actionTitleBn = cmd.titleBn,
                        actionTarget = "Installed Applications Inventory",
                        category = cmd.category,
                        statusTextBn = "মোট ${apps.size}টি অ্যাপ তালিকাভুক্ত"
                    )
                )
            }

            CommandCategory.DEVICE_HEALTH -> {
                selectedTabIndex = 7
                val report = deviceIntelManager.runDeviceHealthDiagnostics()
                val speech = deviceIntelManager.getBengaliVoiceHealthSummary(report, owner)
                messages.add(ChatMessage(text = speech, sender = Sender.ASSISTANT))
                voiceManager.speak(speech)
                deviceIntelManager.recordAuditLog(
                    AuditLogEntry(
                        actionTitleBn = cmd.titleBn,
                        actionTarget = "System Diagnostics Engine",
                        category = cmd.category,
                        statusTextBn = "স্বাস্থ্য অডিট সম্পন্ন: ${report.findings.size}টি অনুসন্ধান"
                    )
                )
            }

            CommandCategory.ACCESSIBILITY_ACTION -> {
                when (cmd.actionTarget) {
                    "SCROLL_DOWN" -> {
                        val success = MairaAccessibilityService.scroll(true)
                        val msg = if (success) "স্ক্রিন নিচে স্ক্রল করা হয়েছে।" else "অ্যাক্সেসিবিলিটি সার্ভিস সক্রিয় করুন।"
                        messages.add(ChatMessage(text = msg, sender = Sender.ASSISTANT))
                        voiceManager.speak(msg)
                    }
                    "SCROLL_UP" -> {
                        val success = MairaAccessibilityService.scroll(false)
                        val msg = if (success) "স্ক্রিন উপরে স্ক্রল করা হয়েছে।" else "অ্যাক্সেসিবিলিটি সার্ভিস সক্রিয় করুন।"
                        messages.add(ChatMessage(text = msg, sender = Sender.ASSISTANT))
                        voiceManager.speak(msg)
                    }
                    "NAV_BACK" -> {
                        val success = MairaAccessibilityService.navigateBack()
                        val msg = if (success) "পূর্বের স্ক্রিনে ফিরে যাওয়া হয়েছে।" else "অ্যাক্সেসিবিলিটি সার্ভিস সক্রিয় করুন।"
                        messages.add(ChatMessage(text = msg, sender = Sender.ASSISTANT))
                        voiceManager.speak(msg)
                    }
                    "READ_SCREEN" -> {
                        val text = MairaAccessibilityService.readScreen()
                        messages.add(ChatMessage(text = text, sender = Sender.ASSISTANT))
                        voiceManager.speak(text)
                    }
                }
                deviceIntelManager.recordAuditLog(
                    AuditLogEntry(
                        actionTitleBn = cmd.titleBn,
                        actionTarget = cmd.actionTarget,
                        category = cmd.category,
                        statusTextBn = "অ্যাক্সেসিবিলিটি কমান্ড এক্সিকিউট সম্পন্ন"
                    )
                )
            }

            CommandCategory.OPEN_SETTINGS -> {
                try {
                    startActivity(Intent(cmd.actionTarget))
                    val reply = "আপনার অনুরোধ অনুযায়ী '${cmd.titleBn}' খোলা হয়েছে।"
                    messages.add(ChatMessage(text = reply, sender = Sender.ASSISTANT))
                    voiceManager.speak(reply)
                    deviceIntelManager.recordAuditLog(
                        AuditLogEntry(
                            actionTitleBn = cmd.titleBn,
                            actionTarget = cmd.actionTarget,
                            category = cmd.category,
                            statusTextBn = "সফলভাবে সেটিংস পেজ চালু হয়েছে"
                        )
                    )
                } catch (e: Exception) {
                    Toast.makeText(this, "সেটিংস স্ক্রিন খুলতে ব্যর্থ", Toast.LENGTH_SHORT).show()
                }
            }

            CommandCategory.CAMERA_SCANNER -> {
                selectedTabIndex = 2
                voiceManager.speak("$owner, আপনার জন্য এআই ক্যামেরা ও ভিশন স্ক্যানার প্রস্তুত!")
                deviceIntelManager.recordAuditLog(
                    AuditLogEntry(
                        actionTitleBn = cmd.titleBn,
                        actionTarget = "Camera Vision Scanner",
                        category = cmd.category,
                        statusTextBn = "ক্যামেরা স্ক্যানার প্রস্তুত"
                    )
                )
            }

            CommandCategory.OPEN_APP -> {
                launchApplicationSafely(cmd.actionTarget)
                deviceIntelManager.recordAuditLog(
                    AuditLogEntry(
                        actionTitleBn = cmd.titleBn,
                        actionTarget = cmd.actionTarget,
                        category = cmd.category,
                        statusTextBn = "অ্যাপ্লিকেশন চালু সম্পন্ন"
                    )
                )
            }

            CommandCategory.SCREEN_HELP -> {
                selectedTabIndex = 3
                pendingMediaProjectionAction = "HELP"
                launchMediaProjection()
            }

            CommandCategory.SCREEN_RECORD -> {
                selectedTabIndex = 6
                pendingMediaProjectionAction = "RECORD"
                launchMediaProjection()
            }

            CommandCategory.CODE_DEV -> {
                selectedTabIndex = 4
            }

            CommandCategory.WEB_SEARCH -> {
                externalSearchQuery = cmd.actionTarget
                selectedTabIndex = 5
            }

            CommandCategory.REST_MODE -> {
                voiceManager.enterRestMode()
                val reply = "সহকারী বিশ্রাম মোডে গেছে। আমাকে পুনরায় সক্রিয় করতে যেকোনো সময় ডাকুন।"
                messages.add(ChatMessage(text = reply, sender = Sender.ASSISTANT))
                deviceIntelManager.recordAuditLog(
                    AuditLogEntry(
                        actionTitleBn = "বিশ্রাম মোড সক্রিয়করণ",
                        actionTarget = "Voice Listener",
                        category = cmd.category,
                        statusTextBn = "শ্রবণ সাময়িক স্থগিত"
                    )
                )
            }

            CommandCategory.SESSION_CONTROL -> {
                stopFloatingService()
                moveTaskToBack(true)
            }

            else -> {
                handleUserQuery(cmd.rawQuery)
            }
        }
    }

    private fun launchApplicationSafely(target: String) {
        val owner = preferences.ownerTitle
        try {
            when (target) {
                "CAMERA_SCANNER" -> {
                    selectedTabIndex = 2
                    voiceManager.speak("$owner, এআই ক্যামেরা প্রস্তুত!")
                }
                "HOME" -> {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(homeIntent)
                    voiceManager.speak("$owner, হোম স্ক্রিনে ফিরে যাওয়া হচ্ছে।")
                }
                "GALLERY" -> {
                    val galleryIntent = Intent(Intent.ACTION_VIEW).apply {
                        type = "image/*"
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(Intent.createChooser(galleryIntent, "গ্যালারি খুলুন"))
                    voiceManager.speak("$owner, ফটো গ্যালারি খোলা হচ্ছে।")
                }
                "CALCULATOR" -> {
                    val calcIntent = Intent().apply {
                        action = Intent.ACTION_MAIN
                        addCategory(Intent.CATEGORY_APP_CALCULATOR)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    if (calcIntent.resolveActivity(packageManager) != null) {
                        startActivity(calcIntent)
                    } else {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=calculator")))
                    }
                    voiceManager.speak("$owner, ক্যালকুলেটর খোলা হচ্ছে।")
                }
                else -> {
                    val launchIntent = packageManager.getLaunchIntentForPackage(target)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(launchIntent)
                        voiceManager.speak("$owner, অ্যাপটি চালু করা হয়েছে।")
                    } else {
                        if (target.contains("youtube")) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://m.youtube.com")))
                        } else if (target.contains("chrome")) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")))
                        } else {
                            Toast.makeText(this, "অ্যাপটি সরাসরি পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "অ্যাপটি চালু করতে সমস্যা হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCommandAction(action: CommandAction) {
        when (action.actionType) {
            "SETTINGS" -> {
                try {
                    startActivity(Intent(action.payload))
                } catch (e: Exception) {
                    Toast.makeText(this, "সেটিংস স্ক্রিন খুলতে ব্যর্থ", Toast.LENGTH_SHORT).show()
                }
            }
            "DEVICE_INTEL" -> {
                selectedTabIndex = 7
            }
            "CAMERA" -> {
                selectedTabIndex = 2
            }
            "SCREEN_HELP" -> {
                selectedTabIndex = 3
            }
            "COMMANDS" -> {
                selectedTabIndex = 1
            }
            "DEV" -> {
                selectedTabIndex = 4
            }
            "RECORDER" -> {
                selectedTabIndex = 6
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
    }
}

@Composable
fun BanglaAiBottomNavigation(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        NavigationItem("MYRA AI হাব", Icons.Default.DynamicForm, 1),
        NavigationItem("এআই চ্যাট", Icons.Default.Chat, 0),
        NavigationItem("ক্যামেরা ভিশন", Icons.Default.CameraAlt, 2),
        NavigationItem("কোড এডিটর", Icons.Default.Code, 4),
        NavigationItem("ডিভাইস হেলথ", Icons.Default.HealthAndSafety, 7),
        NavigationItem("স্মার্ট রিডার", Icons.Default.Language, 5),
        NavigationItem("স্ক্রিন সাহায্য", Icons.Default.ScreenSearchDesktop, 3),
        NavigationItem("স্ক্রিন রেকর্ড", Icons.Default.Videocam, 6),
        NavigationItem("VIP সেটিংস", Icons.Default.Settings, 8),
        NavigationItem("3D ডিফেন্ডার", Icons.Default.SportsEsports, 9),
        NavigationItem("বায়োসায়েন্স", Icons.Default.Biotech, 10),
        NavigationItem("স্টুডিও", Icons.Default.AutoAwesome, 11)
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    selected = selectedIndex == item.index,
                    onClick = { onTabSelected(item.index) },
                    icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.testTag("nav_item_${item.index}")) },
                    label = { Text(item.label, fontSize = 9.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        indicatorColor = EmeraldPrimary
                    )
                )
            }
        }
    }
}

data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val index: Int
)
