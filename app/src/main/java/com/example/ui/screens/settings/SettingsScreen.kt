package com.example.ui.screens.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Personality
import com.example.data.model.ThemeMode
import com.example.data.preferences.AssistantPreferences
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldPrimary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferences: AssistantPreferences,
    onFloatingToggled: (Boolean) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onRelaunchOnboarding: () -> Unit,
    firebaseManager: FirebaseManager? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPersonality by remember { mutableStateOf(preferences.personality) }
    var selectedTheme by remember { mutableStateOf(preferences.themeMode) }
    var speakingRate by remember { mutableFloatStateOf(preferences.speakingRate) }
    var speakingPitch by remember { mutableFloatStateOf(preferences.speakingPitch) }
    var isFloatingEnabled by remember { mutableStateOf(preferences.floatingOverlayEnabled) }
    var handsFreeEnabled by remember { mutableStateOf(preferences.handsFreeEnabled) }
    var idleTimeout by remember { mutableIntStateOf(preferences.idleTimeoutSeconds) }
    var batterySaver by remember { mutableStateOf(preferences.batterySaverEnabled) }
    var customApiKey by remember { mutableStateOf(preferences.customApiKey) }
    var ownerName by remember { mutableStateOf(preferences.ownerName) }
    var ownerTitle by remember { mutableStateOf(preferences.ownerTitle) }
    var assistantName by remember { mutableStateOf(preferences.assistantName) }
    var wakeWordEnabled by remember { mutableStateOf(preferences.wakeWordEnabled) }
    var wakeWord by remember { mutableStateOf(preferences.wakeWord) }
    var quietHours by remember { mutableStateOf(preferences.quietHoursEnabled) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Alom Boss VIP কন্ট্রোল প্যানেল", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
            Text("Maira Bot-এর ব্যক্তিগতকরণ, ওয়েক-ওয়ার্ড ('হাই মাইরা'), কন্ঠস্বর ও অগ্রাধিকার", fontSize = 11.sp, color = Color.Gray)
        }

        // 0. Owner Identity & Address Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("👑 মালিকের পরিচয় ও সম্বোধন (Owner Profile)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmeraldPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = {
                            ownerName = it
                            preferences.ownerName = it
                        },
                        label = { Text("মালিকের নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = ownerTitle,
                        onValueChange = {
                            ownerTitle = it
                            preferences.ownerTitle = it
                        },
                        label = { Text("সহকারী যেভাবে সম্বোধন করবে (যেমন: Alom Boss, বস)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = assistantName,
                        onValueChange = {
                            assistantName = it
                            preferences.assistantName = it
                        },
                        label = { Text("সহকারীর নাম (Maira Bot)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Firebase Cloud Authentication & Firestore Sync
        item {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val currentUser = firebaseManager?.currentUser?.collectAsState()?.value
            val authError = firebaseManager?.authError?.collectAsState()?.value

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("firebase_auth_sync_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥 ফায়ারবেস ক্লাউড ও গুগল সাইন-ইন", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmeraldPrimary)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currentUser != null) EmeraldPrimary.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = if (currentUser != null) "সংযুক্ত (Connected)" else "ডিসকানেক্টেড",
                                color = if (currentUser != null) EmeraldPrimary else Color.Red,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (currentUser != null) {
                        Text("সাইন-ইন ইমেইল: ${currentUser.email ?: "Google Account"}", fontSize = 12.sp, color = Color.White)
                        Text("ইউজার আইডি: ${currentUser.uid}", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { firebaseManager.signOut() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("firebase_sign_out_button")
                        ) {
                            Text("লগআউট (Sign Out)", color = Color.Red, fontSize = 12.sp)
                        }
                    } else {
                        Text("আপনার স্মৃতি ও কনফিগারেশন ক্লাউডে নিরাপদ রাখতে গুগল দিয়ে সাইন-ইন করুন।", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val ok = firebaseManager?.signInWithGoogle() ?: false
                                    if (ok) {
                                        Toast.makeText(context, "গুগল সাইন-ইন সফল হয়েছে!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "সাইন-ইন সম্পন্ন হয়নি বা বাতিল করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("google_sign_in_button")
                        ) {
                            Text("গুগল দিয়ে সাইন-ইন করুন (Sign in with Google)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (authError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(authError, color = Color.Red, fontSize = 10.sp)
                    }
                }
            }
        }

        // 0.1 Wake Word Detection & Quiet Hours
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ওয়েক-ওয়ার্ড অ্যাক্টিভেশন (Wake-Word)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("'$wakeWord' বললে সহকারী স্বয়ংক্রিয়ভাবে উত্তর দেবে", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = wakeWordEnabled,
                            onCheckedChange = {
                                wakeWordEnabled = it
                                preferences.wakeWordEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("শান্ত সময় (Quiet Hours / DND)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("অপ্রয়োজনীয় অডিও ঘোষণা বা কথা বন্ধ রাখা", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = quietHours,
                            onCheckedChange = {
                                quietHours = it
                                preferences.quietHoursEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                        )
                    }
                }
            }
        }

        // 1. Personality Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("সহকারীর ব্যক্তিত্ব (Personality)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Personality.values().forEach { personality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPersonality = personality
                                    preferences.personality = personality
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPersonality == personality,
                                onClick = {
                                    selectedPersonality = personality
                                    preferences.personality = personality
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(personality.displayName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(personality.promptModifier, fontSize = 10.sp, color = Color.Gray, lineHeight = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. Voice & Speech Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("কন্ঠস্বর ও স্পিচ নিয়ন্ত্রণ (Voice)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("কথা বলার গতি (Speed: ${String.format("%.1f", speakingRate)}x):", fontSize = 12.sp)
                    Slider(
                        value = speakingRate,
                        onValueChange = {
                            speakingRate = it
                            preferences.speakingRate = it
                        },
                        valueRange = 0.7f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = EmeraldPrimary, activeTrackColor = EmeraldPrimary)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("কন্ঠের সুর (Pitch: ${String.format("%.1f", speakingPitch)}):", fontSize = 12.sp)
                    Slider(
                        value = speakingPitch,
                        onValueChange = {
                            speakingPitch = it
                            preferences.speakingPitch = it
                        },
                        valueRange = 0.7f..1.4f,
                        colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("হ্যান্ডস-ফ্রি মোড (Hands-free)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("ক্রমাগত কথা শোনা এবং উত্তর দেওয়া", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = handsFreeEnabled,
                            onCheckedChange = {
                                handsFreeEnabled = it
                                preferences.handsFreeEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                        )
                    }
                }
            }
        }

        // 3. Floating Overlay Bubble Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("অন্য অ্যাপের উপর ফ্লোটিং বাবল", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("স্ক্রিনে ছোট ড্র্যাগেবল বাটন প্রদর্শন", fontSize = 10.sp, color = Color.Gray)
                            }
                        }

                        Switch(
                            checked = isFloatingEnabled,
                            onCheckedChange = {
                                isFloatingEnabled = it
                                preferences.floatingOverlayEnabled = it
                                onFloatingToggled(it)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary),
                            modifier = Modifier.testTag("floating_bubble_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val intent = Intent(context, com.nexus.aiagent.ui.MainActivity::class.java)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("open_nexus_agent_button")
                    ) {
                        Text(
                            text = "BD-NEXUS AI এজেন্ট ও Room ভয়েস হিস্টোরি খুলুন",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 4. Themes
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("থিম নির্বাচন (Themes)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ThemeMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTheme = mode
                                    preferences.themeMode = mode
                                    onThemeChanged(mode)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTheme == mode,
                                onClick = {
                                    selectedTheme = mode
                                    preferences.themeMode = mode
                                    onThemeChanged(mode)
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(mode.displayName, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 5. Battery & Diagnostics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ব্যাটারি ও ডেটা সেভার মোড", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("অ্যানিমেশন ও ব্যাকগ্রাউন্ড অডিও কমিয়ে চার্জ বাঁচান", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = batterySaver,
                            onCheckedChange = {
                                batterySaver = it
                                preferences.batterySaverEnabled = it
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                        )
                    }
                }
            }
        }

        // 6. Gemini API Key Configuration
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini AI API কি কনফিগারেশন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        "AI Studio Secrets প্যানেলের কি স্বয়ংক্রিয়ভাবে ব্যবহৃত হয়। প্রয়োজনে কাস্টম কি প্রদান করতে পারেন:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customApiKey,
                        onValueChange = {
                            customApiKey = it
                            preferences.customApiKey = it
                        },
                        placeholder = { Text("AI Studio Gemini API Key...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // 7. Onboarding Tour Relaunch
        item {
            OutlinedButton(
                onClick = onRelaunchOnboarding,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("বাংলা অনবোর্ডিং টিউটোরিয়াল পুনরায় দেখুন")
            }
        }
    }
}
