package com.example.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.CommandAction
import com.example.data.model.Sender
import com.example.data.model.UserMemoryItem
import com.example.ui.components.HolographicAiOrb
import com.example.ui.components.Interactive3DAudioSphereVisualizer
import com.example.ui.components.RealtimeAudioVisualizer
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.NeonGreen
import com.example.voice.VoiceState

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    voiceState: VoiceState,
    amplitude: Float,
    isMuted: Boolean,
    handsFreeActive: Boolean,
    memories: List<UserMemoryItem>,
    onSendMessage: (String) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onStopSpeaking: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleHandsFree: () -> Unit,
    onActionClicked: (CommandAction) -> Unit,
    onAddMemory: (String, String) -> Unit,
    onDeleteMemory: (String) -> Unit,
    currentTaskStep: String? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var use3dSphere by remember { mutableStateOf(true) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top Assistant Orb Header with 3D Audio Visualizer
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier.testTag("toggle_mute_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = if (isMuted) "আনমিউট করুন" else "মিউট করুন",
                                tint = if (isMuted) Color.Gray else EmeraldPrimary
                            )
                        }

                        // 3D Sphere toggle chip
                        Surface(
                            shape = CircleShape,
                            color = if (use3dSphere) CyberCyan.copy(alpha = 0.2f) else EmeraldPrimary.copy(alpha = 0.2f),
                            modifier = Modifier.clickable { use3dSphere = !use3dSphere }
                        ) {
                            Text(
                                text = if (use3dSphere) "🔮 3D Reactive Sphere" else "🪐 2D Hologram",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (use3dSphere) CyberCyan else EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { showMemoryDialog = true },
                            modifier = Modifier.testTag("user_memory_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = "স্মৃতি ও তথ্য",
                                tint = EmeraldTertiary
                            )
                        }
                    }

                    // Live Task Execution HUD (Overlay progress banner)
                    AnimatedVisibility(visible = !currentTaskStep.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xF0020617),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.8f)),
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("chat_task_execution_badge")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = NeonGreen,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentTaskStep ?: "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                            }
                        }
                    }

                    if (use3dSphere) {
                        Interactive3DAudioSphereVisualizer(
                            amplitude = amplitude,
                            voiceState = voiceState,
                            modifier = Modifier
                                .size(175.dp)
                                .padding(vertical = 4.dp),
                            onClick = {
                                when (voiceState) {
                                    VoiceState.LISTENING -> onStopVoice()
                                    VoiceState.SPEAKING -> onStopSpeaking()
                                    else -> onStartVoice()
                                }
                            }
                        )
                    } else {
                        HolographicAiOrb(
                            voiceState = voiceState,
                            amplitude = amplitude,
                            onClick = {
                                when (voiceState) {
                                    VoiceState.LISTENING -> onStopVoice()
                                    VoiceState.SPEAKING -> onStopSpeaking()
                                    else -> onStartVoice()
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    RealtimeAudioVisualizer(
                        amplitude = amplitude,
                        isActive = voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING
                    )

                    if (voiceState == VoiceState.SPEAKING) {
                        OutlinedButton(
                            onClick = onStopSpeaking,
                            modifier = Modifier.padding(top = 4.dp).testTag("stop_speaking_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "থামুন", tint = Color.Red)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("কথা থামান", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        onActionClicked = onActionClicked
                    )
                }
            }

            // Bottom Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (voiceState == VoiceState.LISTENING) onStopVoice() else onStartVoice()
                        },
                        containerColor = if (voiceState == VoiceState.LISTENING) Color.Red else EmeraldPrimary,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("voice_mic_fab")
                    ) {
                        Icon(
                            imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "ভয়েস ইনপুট",
                            tint = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("বাংলায় যেকোনো প্রশ্ন বা কমান্ড লিখুন...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = false,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText.trim())
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        modifier = Modifier.testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "পাঠান",
                            tint = if (inputText.isNotBlank()) EmeraldPrimary else Color.Gray
                        )
                    }
                }
            }
        }

        // Memory Dialog
        if (showMemoryDialog) {
            UserMemoryDialog(
                memories = memories,
                onDismiss = { showMemoryDialog = false },
                onAdd = onAddMemory,
                onDelete = onDeleteMemory
            )
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onActionClicked: (CommandAction) -> Unit
) {
    val isUser = message.sender == Sender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) EmeraldPrimary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isUser) "আপনি" else "Bangla AI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUser) EmeraldPrimary else CyberCyan
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 21.sp
                )

                // Optional Code Block
                if (message.isCode && !message.codeSnippet.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = message.codeLanguage ?: "Code",
                                color = EmeraldTertiary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = message.codeSnippet,
                                color = Color(0xFFF8FAFC),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Confidence Note / Citations
                if (!message.confidenceNote.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ℹ️ ${message.confidenceNote}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                // Suggested action chips
                if (message.suggestedActions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        message.suggestedActions.forEach { action ->
                            AssistChip(
                                onClick = { onActionClicked(action) },
                                label = { Text(action.label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserMemoryDialog(
    memories: List<UserMemoryItem>,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    var keyText by remember { mutableStateOf("") }
    var valueText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("সহকারীর সংরক্ষিত স্মৃতি ও তথ্য") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Bangla AI আপনার অনুমতি নিয়ে নিচের বিষয়গুলো মনে রাখে যাতে আপনাকে ব্যক্তিগতকৃত সেবা দিতে পারে।",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(memories) { mem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mem.key, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(mem.value, fontSize = 11.sp, color = EmeraldTertiary)
                            }
                            TextButton(onClick = { onDelete(mem.id) }) {
                                Text("মুছুন", color = Color.Red, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = { Text("শীর্ষক (যেমন: কাজের বিষয়)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text("তথ্য (যেমন: মোবাইল অ্যাপ ডেভেলপমেন্ট)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (keyText.isNotBlank() && valueText.isNotBlank()) {
                        onAdd(keyText.trim(), valueText.trim())
                        keyText = ""
                        valueText = ""
                    }
                }
            ) {
                Text("তথ্য যোগ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}
