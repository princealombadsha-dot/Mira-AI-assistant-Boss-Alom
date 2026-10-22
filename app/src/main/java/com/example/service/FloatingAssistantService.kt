package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.BanglaAIApplication
import com.example.MainActivity
import com.example.R
import com.example.data.api.GeminiApiClient
import com.example.data.preferences.AssistantPreferences
import androidx.compose.ui.graphics.PathEffect
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MyraMintGlow
import com.example.ui.theme.MyraCosmicDark
import com.example.ui.theme.MyraNeonGreen
import com.example.ui.theme.MyraCyan
import com.example.ui.theme.MyraDarkBg
import com.example.ui.theme.MyraPanel
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.MyraMintGlow
import com.example.ui.theme.MyraCosmicDark
import com.example.ui.theme.MyApplicationTheme
import com.example.nexus.ExecutionStep
import com.example.nexus.NexusEngineProvider
import com.example.nexus.NexusExecutionListener
import com.example.nexus.ReActPlanResult
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class FloatingAssistantService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val ACTION_START = "com.example.service.START_FLOATING"
        const val ACTION_STOP = "com.example.service.STOP_FLOATING"
        var isRunning = false
            private set
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private lateinit var preferences: AssistantPreferences
    private val geminiApiClient = GeminiApiClient()

    private lateinit var windowParams: WindowManager.LayoutParams
    private var isExpandedState by mutableStateOf(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        preferences = AssistantPreferences(this)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1001, buildNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1001, buildNotification())
        }
        setupFloatingComposeOverlay()

        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val voiceIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("START_VOICE", true)
        }
        val voicePendingIntent = PendingIntent.getActivity(
            this, 2, voiceIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingAssistantService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, BanglaAIApplication.FLOATING_CHANNEL_ID)
            .setContentTitle("Maira Bot ফ্লোটিং বাবল সক্রিয়")
            .setContentText("${preferences.ownerTitle}-এর সেবায় স্ক্রিনের উপর প্রস্তুত")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_btn_speak_now, "ভয়েস ইনপুট", voicePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "বন্ধ করুন", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun setupFloatingComposeOverlay() {
        if (composeView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 300
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingAssistantService)
            setViewTreeSavedStateRegistryOwner(this@FloatingAssistantService)
            setViewTreeViewModelStoreOwner(this@FloatingAssistantService)

            setContent {
                MyApplicationTheme(themeMode = preferences.themeMode) {
                    FloatingComposeBubbleRoot(
                        isExpanded = isExpandedState,
                        onToggleExpand = {
                            isExpandedState = !isExpandedState
                            updateWindowFocusability(isExpandedState)
                        },
                        onTriggerVoice = {
                            val intent = Intent(this@FloatingAssistantService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("START_VOICE", true)
                            }
                            startActivity(intent)
                            isExpandedState = false
                            updateWindowFocusability(false)
                        },
                        onTriggerScreenHelp = {
                            val intent = Intent(this@FloatingAssistantService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("TRIGGER_SCREEN_HELP", true)
                            }
                            startActivity(intent)
                            isExpandedState = false
                            updateWindowFocusability(false)
                        },
                        onTriggerDeviceIntel = {
                            val intent = Intent(this@FloatingAssistantService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("NAV_INTEL", true)
                            }
                            startActivity(intent)
                            isExpandedState = false
                            updateWindowFocusability(false)
                        },
                        onTriggerScrollDown = {
                            val success = MairaAccessibilityService.scroll(true)
                            Toast.makeText(this@FloatingAssistantService, if (success) "নিচে স্ক্রল করা হয়েছে" else "অ্যাক্সেসিবিলিটি সক্রিয় প্রয়োজন", Toast.LENGTH_SHORT).show()
                        },
                        onTriggerNavigateBack = {
                            val success = MairaAccessibilityService.navigateBack()
                            Toast.makeText(this@FloatingAssistantService, if (success) "পূর্বের স্ক্রিনে ব্যাক করা হয়েছে" else "অ্যাক্সেসিবিলিটি সক্রিয় প্রয়োজন", Toast.LENGTH_SHORT).show()
                        },
                        onTriggerReadScreen = {
                            val text = MairaAccessibilityService.readScreen()
                            Toast.makeText(this@FloatingAssistantService, text.take(120), Toast.LENGTH_LONG).show()
                        },
                        onOpenMainApp = {
                            val intent = Intent(this@FloatingAssistantService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(intent)
                            isExpandedState = false
                            updateWindowFocusability(false)
                        },
                        onDismissBubble = {
                            preferences.floatingOverlayEnabled = false
                            stopSelf()
                        },
                        geminiApiClient = geminiApiClient,
                        preferences = preferences
                    )
                }
            }
        }

        // Handle Touch dragging and Edge-Snapping
        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event == null) return false

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = windowParams.x
                        initialY = windowParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return false // Allow compose to process clicks
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                            isDragging = true
                            windowParams.x = initialX + dx
                            windowParams.y = initialY + dy
                            windowManager?.updateViewLayout(composeView, windowParams)
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            val metrics = DisplayMetrics()
                            @Suppress("DEPRECATION")
                            windowManager?.defaultDisplay?.getMetrics(metrics)
                            val screenWidth = metrics.widthPixels
                            // Snap smoothly to left or right edge
                            windowParams.x = if (windowParams.x < screenWidth / 2) 20 else screenWidth - 180
                            windowManager?.updateViewLayout(composeView, windowParams)
                            isDragging = false
                            return true
                        }
                    }
                }
                return false
            }
        })

        composeView = view
        try {
            windowManager?.addView(view, windowParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateWindowFocusability(expanded: Boolean) {
        val flags = if (expanded) {
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        windowParams.flags = flags
        composeView?.let {
            try {
                windowManager?.updateViewLayout(it, windowParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()

        composeView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        composeView = null
    }
}

@Composable
fun FloatingComposeBubbleRoot(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTriggerVoice: () -> Unit,
    onTriggerScreenHelp: () -> Unit,
    onTriggerDeviceIntel: () -> Unit,
    onTriggerScrollDown: () -> Unit,
    onTriggerNavigateBack: () -> Unit,
    onTriggerReadScreen: () -> Unit,
    onOpenMainApp: () -> Unit,
    onDismissBubble: () -> Unit,
    geminiApiClient: GeminiApiClient,
    preferences: AssistantPreferences
) {
    val coroutineScope = rememberCoroutineScope()
    val localCtx = LocalContext.current
    val cognitiveEngine = remember { NexusEngineProvider.getCognitiveEngine(localCtx, geminiApiClient) }

    var quickQueryText by remember { mutableStateOf("") }
    var quickAnswerText by remember { mutableStateOf<String?>(null) }
    var isThinking by remember { mutableStateOf(false) }
    var activeStepTag by remember { mutableStateOf<String?>(null) }
    var activeStepDesc by remember { mutableStateOf<String?>(null) }

    DisposableEffect(cognitiveEngine) {
        val listener = object : NexusExecutionListener {
            override fun onStepStarted(step: ExecutionStep) {
                activeStepTag = step.taskTag
                activeStepDesc = step.descriptionBn
            }

            override fun onStepCompleted(step: ExecutionStep) {
                activeStepTag = step.taskTag
                activeStepDesc = step.descriptionBn
            }

            override fun onPlanFinished(result: ReActPlanResult) {
                activeStepTag = null
                activeStepDesc = null
            }
        }
        cognitiveEngine.addListener(listener)
        onDispose {
            cognitiveEngine.removeListener(listener)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "myraPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val shadowSpread by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shadow_spread"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600),
            repeatMode = RepeatMode.Restart
        ),
        label = "rot"
    )

    Column(
        modifier = Modifier.wrapContentSize(),
        horizontalAlignment = Alignment.Start
    ) {
        // Real-Time Task Execution Overlay HUD
        AnimatedVisibility(
            visible = activeStepTag != null || isThinking,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF2020617)),
                modifier = Modifier
                    .width(280.dp)
                    .padding(bottom = 6.dp)
                    .border(1.5.dp, CyberCyan.copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                    .testTag("floating_task_execution_hud")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = NeonGreen,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeStepTag ?: "[Task: Executing]",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                    }
                    if (!activeStepDesc.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = activeStepDesc!!,
                            fontSize = 10.sp,
                            color = Color(0xFFCBD5E1),
                            maxLines = 2
                        )
                    }
                }
            }
        }
        // Floating Bubble Head - MYRA VIP 3D Floating Orb (radial-gradient #60efff at 30% 30% to #00ff87 40% to #060810 85%)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(75.dp)
                .clip(CircleShape)
                .clickable { onToggleExpand() }
                .testTag("myra-floating-orb")
                .testTag("myra-floating-bubble")
                .testTag("myra-overlay-bubble")
                .testTag("floating_compose_bubble")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (size.minDimension / 2f) * 0.75f * pulseScale

                // 1. Box-Shadow Glow: 0 0 50px rgba(0, 255, 135, 0.6)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MyraNeonGreen.copy(alpha = glowAlpha * 0.7f),
                            MyraCyan.copy(alpha = glowAlpha * 0.35f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * shadowSpread
                    ),
                    radius = baseRadius * shadowSpread
                )

                // 2. 3D Sphere Surface: radial-gradient(circle at 30% 30%, #60efff 0%, #00ff87 40%, #060810 85%)
                val lightOffset = Offset(center.x * 0.6f, center.y * 0.6f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to MyraCyan,
                            0.40f to MyraNeonGreen,
                            0.85f to MyraDarkBg,
                            1.0f to MyraDarkBg
                        ),
                        center = lightOffset,
                        radius = baseRadius
                    ),
                    radius = baseRadius,
                    center = center
                )

                // 3. Sensor Ring dashed rim: 2px dashed rgba(0, 255, 135, 0.4)
                drawCircle(
                    color = MyraNeonGreen.copy(alpha = 0.5f),
                    radius = baseRadius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                )

                // 4. Orbiting energy particles
                val orbitRadius = baseRadius * 0.68f
                val particleCount = 6
                for (i in 0 until particleCount) {
                    val angle = Math.toRadians((i * (360.0 / particleCount) + rotationAngle.toDouble()))
                    val px = center.x + (orbitRadius * cos(angle)).toFloat()
                    val py = center.y + (orbitRadius * sin(angle)).toFloat()
                    drawCircle(
                        color = (if (i % 2 == 0) MyraNeonGreen else MyraCyan).copy(alpha = 0.85f),
                        radius = 2.dp.toPx(),
                        center = Offset(px, py)
                    )
                }

                // 5. Core-Pulse Halo Shadow: 0 0 20px #60efff
                val coreDotRadius = 15.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MyraCyan.copy(alpha = 0.8f),
                            MyraCyan.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = coreDotRadius * 1.5f
                    ),
                    radius = coreDotRadius * 1.5f,
                    center = center
                )

                // Core Solid White
                drawCircle(
                    color = Color.White,
                    radius = coreDotRadius,
                    center = center
                )
            }

            // Core-Pulse Center Glyph
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .testTag("core-pulse")
                    .testTag("core-dot"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ম",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MyraCosmicDark
                )
            }

            // Online status dot
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(MyraMintGlow)
                    .border(1.5.dp, MyraCosmicDark, CircleShape)
            )
        }

        // Expanded Floating Quick-Action & Q/A Overlay Panel
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF20F172A)), // Sleek Dark Cyber Navy
                modifier = Modifier
                    .width(310.dp)
                    .padding(top = 8.dp)
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .testTag("floating_quick_panel")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {

                    // Panel Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Maira Bot • ${preferences.ownerTitle} VIP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }

                        IconButton(
                            onClick = onToggleExpand,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "সংকুচিত করুন", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Quick Buttons (Voice, Screen Help, Device Intel)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onTriggerVoice,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("floating_voice_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("ভয়েস", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onTriggerScreenHelp,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).testTag("floating_screen_help_button")
                        ) {
                            Icon(Icons.Default.ScreenSearchDesktop, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("স্ক্রিন", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onTriggerDeviceIntel,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1.1f).testTag("floating_device_intel_button")
                        ) {
                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("ডিভাইস", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Accessibility UI Controls Row (Scroll down, Navigate Back, Read Screen)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTriggerScrollDown,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("স্ক্রল", fontSize = 9.sp, color = CyberCyan)
                        }

                        OutlinedButton(
                            onClick = onTriggerNavigateBack,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("ব্যাক", fontSize = 9.sp, color = CyberCyan)
                        }

                        OutlinedButton(
                            onClick = onTriggerReadScreen,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("পড়ুন", fontSize = 9.sp, color = EmeraldPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Bengali Question Input
                    OutlinedTextField(
                        value = quickQueryText,
                        onValueChange = { quickQueryText = it },
                        placeholder = { Text("এখানে যেকোনো প্রশ্ন লিখুন...", fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        trailingIcon = {
                            if (quickQueryText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val q = quickQueryText.trim()
                                        quickQueryText = ""
                                        isThinking = true
                                        coroutineScope.launch {
                                            val result = cognitiveEngine.solveAutonomousProblem(
                                                query = q,
                                                personality = preferences.personality,
                                                ownerName = preferences.ownerTitle,
                                                customApiKey = preferences.customApiKey.ifBlank { null }
                                            )
                                            quickAnswerText = result.finalResponseText
                                            isThinking = false
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "পাঠান", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Inline Thinking & Answer Area
                    if (isThinking) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("উত্তর তৈরি হচ্ছে...", fontSize = 11.sp, color = CyberCyan)
                        }
                    } else if (quickAnswerText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = quickAnswerText!!,
                                color = Color(0xFFF1F5F9),
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Footer actions: Open App & Dismiss
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenMainApp() }
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = EmeraldTertiary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পূর্ণ অ্যাপ খুলুন", fontSize = 11.sp, color = EmeraldTertiary)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onDismissBubble() }
                                .padding(4.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("বাবল বন্ধ", fontSize = 11.sp, color = Color.Red)
                        }
                    }
                }
            }
        }
    }
}
