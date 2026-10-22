package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.CommandAction
import com.example.data.model.Sender
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldBackground
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldSecondary
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.MyraMintGlow
import com.example.ui.theme.MyraCosmicDark
import com.example.ui.theme.MyraNeonGreen
import com.example.ui.theme.MyraCyan
import com.example.ui.theme.MyraDarkBg
import com.example.ui.theme.MyraPanel
import com.example.ui.theme.MyraGold
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A persistent on-screen Floating Orb Compose component that floats over the UI,
 * supports smooth drag gestures, pulses with audio/breathing aura animations,
 * and smoothly expands into an interactive floating chat interface upon interaction.
 */
@Composable
fun FloatingOrb(
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false,
    externalMessages: List<ChatMessage>? = null,
    onSendMessage: ((String) -> Unit)? = null,
    onVoiceClick: (() -> Unit)? = null,
    isThinking: Boolean = false,
    activeTaskStep: String? = null,
    onActionClicked: ((CommandAction) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(initialExpanded) }

    // Internal fallback messages if externalMessages not provided
    val internalMessages = remember {
        mutableStateListOf(
            ChatMessage(
                text = "হ্যালো Alom Boss! আমি আপনার Maira Bot। সমস্ত সিস্টেম এবং ভয়েস ফিচার সচল রয়েছে।",
                sender = Sender.ASSISTANT,
                suggestedActions = listOf(
                    CommandAction(label = "ডিভাইস অডিট", actionType = "DEVICE_INTEL"),
                    CommandAction(label = "ক্যামেরা ভিশন", actionType = "CAMERA"),
                    CommandAction(label = "কোড এডিটর", actionType = "DEV")
                )
            )
        )
    }

    val messages = externalMessages ?: internalMessages
    var inputQuery by remember { mutableStateOf("") }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Drag offset coordinates
    var offsetX by remember { mutableFloatStateOf(100f) }
    var offsetY by remember { mutableFloatStateOf(280f) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        // Keep orb within screen bounds
        val boundedX = offsetX.coerceIn(0f, (maxWidthPx - 260f).coerceAtLeast(0f))
        val boundedY = offsetY.coerceIn(60f, (maxHeightPx - 320f).coerceAtLeast(60f))

        Box(
            modifier = Modifier
                .offset {
                    if (isExpanded) {
                        // When expanded, position comfortably centered/top-anchored
                        IntOffset(
                            x = ((maxWidthPx - with(density) { 360.dp.toPx() }) / 2).toInt().coerceAtLeast(16),
                            y = (maxHeightPx * 0.12f).toInt()
                        )
                    } else {
                        IntOffset(boundedX.roundToInt(), boundedY.roundToInt())
                    }
                }
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    (scaleIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) + fadeIn()) togetherWith
                            (scaleOut(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut())
                },
                label = "floating_orb_expand_transition"
            ) { expanded ->
                if (!expanded) {
                    // COLLAPSED: Draggable Futuristic Glowing Orb
                    CollapsedOrbView(
                        isThinking = isThinking,
                        onClick = { isExpanded = true },
                        onDrag = { dx, dy ->
                            offsetX += dx
                            offsetY += dy
                        }
                    )
                } else {
                    // EXPANDED: Full Floating Chat Interface
                    ExpandedChatCard(
                        messages = messages,
                        inputQuery = inputQuery,
                        onInputChange = { inputQuery = it },
                        isThinking = isThinking,
                        activeTaskStep = activeTaskStep,
                        onSend = {
                            if (inputQuery.isNotBlank()) {
                                val text = inputQuery.trim()
                                inputQuery = ""
                                if (onSendMessage != null) {
                                    onSendMessage(text)
                                } else {
                                    internalMessages.add(ChatMessage(text = text, sender = Sender.USER))
                                }
                                coroutineScope.launch {
                                    if (messages.isNotEmpty()) {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                }
                            }
                        },
                        onVoice = onVoiceClick,
                        onActionClicked = { action ->
                            onActionClicked?.invoke(action)
                        },
                        onCollapse = { isExpanded = false }
                    )
                }
            }
        }
    }
}

/**
 * Collapsed state: Premium VIP 3D Animated Floating Orb (#myra-floating-orb)
 * 140px diameter, radial gradient (#60efff at 30% 30% to #00ff87 40% to #060810 85%),
 * 180px rotating dashed sensor ring, 45px glowing white core pulse, 3s floatOrb and 2s pulseGlow.
 */
@Composable
private fun CollapsedOrbView(
    isThinking: Boolean,
    onClick: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "myraVipOrb")

    // floatOrb 3s infinite: translateY(0px) to translateY(-15px)
    val floatOrbY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_orb_y"
    )

    // pulseGlow 2s infinite alternate: 35px glow (alpha 0.5) to 70px glow (alpha 0.9)
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow_alpha"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // rotateRing 10s linear infinite: 0 to 360 deg
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate_ring"
    )

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .size(180.dp)
            .offset { IntOffset(0, (floatOrbY * density.density).roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // 1. Particle & Sensor Ring: 180px x 180px, 2px dashed rgba(0, 255, 135, 0.4), rotating 360deg
        Canvas(
            modifier = Modifier
                .size(180.dp)
                .graphicsLayer { rotationZ = ringRotation }
                .testTag("sensor-ring")
        ) {
            val r = (size.minDimension / 2f) - 3.dp.toPx()
            drawCircle(
                color = MyraNeonGreen.copy(alpha = 0.45f),
                radius = r,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )
            )
        }

        // 2. Premium VIP 3D Animated Floating Orb: 140px x 140px
        Box(
            modifier = Modifier
                .size(140.dp)
                .testTag("myra-floating-orb")
                .testTag("myra-floating-bubble")
                .testTag("myra-overlay-bubble")
                .testTag("floating_orb_bubble"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val orbRadius = (size.minDimension / 2f) * 0.90f * pulseScale

                // Box-shadow: 0 0 50px rgba(0, 255, 135, 0.6)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MyraNeonGreen.copy(alpha = pulseGlowAlpha * 0.65f),
                            MyraCyan.copy(alpha = pulseGlowAlpha * 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = orbRadius * 1.55f
                    ),
                    radius = orbRadius * 1.55f
                )

                // 3D Sphere Surface: radial-gradient(circle at 30% 30%, #60efff 0%, #00ff87 40%, #060810 85%)
                val lightCenterX = cx * 0.6f // 30% from left
                val lightCenterY = cy * 0.6f // 30% from top
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to (if (isThinking) CyberCyan else MyraCyan),
                            0.40f to (if (isThinking) EmeraldSecondary else MyraNeonGreen),
                            0.85f to MyraDarkBg,
                            1.0f to MyraDarkBg
                        ),
                        center = Offset(lightCenterX, lightCenterY),
                        radius = orbRadius
                    ),
                    radius = orbRadius,
                    center = Offset(cx, cy)
                )

                // Inset highlight / inner rim border: inset 0 0 30px rgba(96, 239, 255, 0.8)
                drawCircle(
                    color = MyraCyan.copy(alpha = 0.65f),
                    radius = orbRadius,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Dynamic orbiting energy particles
                val orbitRadius = orbRadius * 0.72f
                val particleCount = 8
                for (i in 0 until particleCount) {
                    val angle = Math.toRadians((i * (360.0 / particleCount) + ringRotation.toDouble()))
                    val px = cx + (orbitRadius * cos(angle)).toFloat()
                    val py = cy + (orbitRadius * sin(angle)).toFloat()
                    drawCircle(
                        color = (if (i % 2 == 0) MyraNeonGreen else MyraCyan).copy(alpha = 0.85f),
                        radius = 2.5.dp.toPx(),
                        center = Offset(px, py)
                    )
                }

                // Core halo glow: 0 0 20px #60efff
                val coreHaloRadius = 22.5.dp.toPx() * 1.6f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MyraCyan.copy(alpha = 0.8f),
                            MyraCyan.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = coreHaloRadius
                    ),
                    radius = coreHaloRadius,
                    center = Offset(cx, cy)
                )
            }

            // 3. Core Pulse: width 45px, height 45px, background #ffffff, border-radius 50%, box-shadow 0 0 20px #60efff
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, MyraCyan, CircleShape)
                    .testTag("core-pulse")
                    .testTag("core-dot"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ম",
                    color = MyraDarkBg,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * Expanded state: Full Floating Chat Interface Card
 */
@Composable
private fun ExpandedChatCard(
    messages: List<ChatMessage>,
    inputQuery: String,
    onInputChange: (String) -> Unit,
    isThinking: Boolean,
    activeTaskStep: String?,
    onSend: () -> Unit,
    onVoice: (() -> Unit)?,
    onActionClicked: (CommandAction) -> Unit,
    onCollapse: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Card(
        modifier = Modifier
            .widthIn(min = 300.dp, max = 380.dp)
            .heightIn(min = 400.dp, max = 520.dp)
            .padding(8.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp))
            .border(1.5.dp, EmeraldPrimary.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .testTag("floating_expanded_chat_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1320).copy(alpha = 0.96f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Header: Assistant Title, Online Status & Collapse Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                Brush.radialGradient(listOf(NeonGreen, EmeraldPrimary)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Maira Bot VIP",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(NeonGreen, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isThinking) "চিন্তাভাবনা করছে..." else "অনলাইন ও সচল",
                                color = if (isThinking) CyberCyan else NeonGreen,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Collapse back to Orb button
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        .testTag("collapse_floating_orb_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Minimize Orb",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Task Step HUD Banner (if ReAct task running)
            AnimatedVisibility(visible = !activeTaskStep.isNullOrBlank()) {
                activeTaskStep?.let { step ->
                    Surface(
                        color = Color(0xFF022C22).copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            CircularProgressIndicator(
                                color = NeonGreen,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = step,
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Messages LazyColumn
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubbleItem(message = msg, onActionClicked = onActionClicked)
                }

                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = EmeraldPrimary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Maira Bot বিশ্লেষণ করছে...",
                                color = CyberCyan,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Quick suggestion chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val chips = listOf(
                    "📱 ফোন হেলথ",
                    "📷 ক্যামেরা ভিশন",
                    "⚡ ইউটিউব চালু করো",
                    "🛡️ স্ক্রিন রেকর্ড"
                )
                items(chips) { chip ->
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            onInputChange(chip)
                        }
                    ) {
                        Text(
                            text = chip,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Input Row: TextField + Mic + Send
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputQuery,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text("এখানে লিখুন...", fontSize = 12.sp, color = Color.Gray)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = EmeraldPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp, max = 50.dp)
                        .testTag("floating_chat_input"),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Voice Trigger button
                if (onVoice != null) {
                    IconButton(
                        onClick = onVoice,
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .testTag("floating_chat_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // Send Button
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(42.dp)
                        .background(EmeraldPrimary, CircleShape)
                        .testTag("floating_chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual message bubble inside the floating chat
 */
@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    onActionClicked: (CommandAction) -> Unit
) {
    val isUser = message.sender == Sender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isUser) EmeraldPrimary else Color(0xFF1E293B),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isUser) 14.dp else 2.dp,
                bottomEnd = if (isUser) 2.dp else 14.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.text,
                    color = if (isUser) Color.Black else Color.White,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal
                )

                // Optional action chips inside bubble
                if (!isUser && message.suggestedActions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        message.suggestedActions.take(3).forEach { action ->
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onActionClicked(action) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚡ ${action.label}",
                                        color = NeonGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
