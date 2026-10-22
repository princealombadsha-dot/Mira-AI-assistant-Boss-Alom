package com.example.ui.screens.command

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import com.example.data.model.CommandCategory
import com.example.domain.CommandRouter
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import com.example.data.preferences.AssistantPreferences
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.MyraMintGlow
import com.example.ui.theme.MyraGold
import com.example.ui.theme.MyraCardBg
import com.example.ui.theme.MyraCardBorder
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.theme.MyraNeonGreen
import com.example.ui.theme.MyraCyan
import com.example.ui.theme.MyraDarkBg
import com.example.ui.theme.MyraPanel
import com.example.ui.theme.MyraBorder
import com.example.ui.theme.MyraGoldStart
import com.example.ui.theme.MyraGoldEnd
import com.example.ui.theme.MyraNeonGreenDark
import com.example.ui.theme.MyraCosmicDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandCenterScreen(
    onExecuteAction: (com.example.data.model.ClassifiedCommand) -> Unit,
    onNavigateToDev: (String) -> Unit,
    onNavigateToBrowser: (String) -> Unit,
    onNavigateToScreenHelp: () -> Unit,
    preferences: AssistantPreferences? = null,
    onRequestAllPermissions: (() -> Unit)? = null,
    onToggleAlwaysOnMic: (() -> Unit)? = null,
    isAlwaysOnMicActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeClassification by remember { mutableStateOf<com.example.data.model.ClassifiedCommand?>(null) }
    var executionResultText by remember { mutableStateOf<String?>(null) }

    val presetExamples = listOf(
        "আমার ফোনের Wi-Fi সেটিংস খুলে দাও।",
        "এই লেখাটার অর্থ বুঝিয়ে বলো।",
        "এই ওয়েবপেজটা বাংলায় সংক্ষেপ করো।",
        "আমার জন্য একটি ওয়েবসাইট তৈরি করো।",
        "এই ছবির মধ্যে কী আছে বুঝিয়ে দাও।",
        "স্ক্রিনে দেখানো সমস্যাটা সমাধানের ধাপ বলো।",
        "এখন বিশ্রাম মোড চালু করো।"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // MYRA AI — FULL CONTROL ENGINE (VIP Dashboard)
        Card(
            colors = CardDefaults.cardColors(containerColor = MyraPanel),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MyraBorder, RoundedCornerShape(20.dp))
                .testTag("myra_system_control_hub")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // VIP Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(MyraGoldStart, MyraGoldEnd)
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "WORLD CLASS AI SENSOR MODEL",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "MYRA AI — FULL CONTROL ENGINE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MyraNeonGreen
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "গুগল জেমিনি ও আধুনিক নিউরাল মডেল দ্বারা পরিচালিত অটোমেটেড ফোন কন্ট্রোলার",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA8C5),
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status Box: ⚙️ সিস্টেম পারমিশন ও সেন্সর স্ট্যাটাস
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF090E1A)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MyraBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MyraGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚙️ সিস্টেম পারমিশন ও সেন্সর স্ট্যাটাস",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (preferences?.allPermissionsGranted == true) {
                                "✅ সকল নেটিভ পারমিশন, ফুল ডিভাইস এক্সেস এবং অ্যাক্সেসিবিলিটি সেন্সর সচল করা হয়েছে!"
                            } else {
                                "অ্যালাউ বাটনে ক্লিক করে সব পারমিশন একটিভ করুন। এরপর এটি সার্বক্ষণিক ব্যাকগ্রাউন্ডে চলবে।"
                            },
                            fontSize = 13.sp,
                            color = if (preferences?.allPermissionsGranted == true) MyraNeonGreen else Color(0xFF9CA8C5),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Gradient Action Button: সব ক্ষমতা ও পারমিশন অন করুন
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(MyraNeonGreen, MyraNeonGreenDark)
                                    )
                                )
                                .clickable {
                                    onRequestAllPermissions?.invoke()
                                }
                                .padding(vertical = 12.dp)
                                .testTag("grant_permissions_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "সব ক্ষমতা ও পারমিশন অন করুন",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card 2: 🎙️ অলওয়েজ-অন মাইক্রোফোন
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF090E1A)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MyraBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isAlwaysOnMicActive) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = null,
                                    tint = if (isAlwaysOnMicActive) MyraNeonGreen else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🎙️ অলওয়েজ-অন মাইক্রোফোন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAlwaysOnMicActive) MyraNeonGreen.copy(alpha = 0.2f) else Color.DarkGray)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isAlwaysOnMicActive) "সক্রিয়" else "নিষ্ক্রিয়",
                                    color = if (isAlwaysOnMicActive) MyraNeonGreen else Color.LightGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isAlwaysOnMicActive) {
                                "🟢 মাইক্রোফোন ব্যাকগ্রাউন্ডে সার্বক্ষণিক অন রয়েছে এবং আপনার যেকোনো কথা শুনছে।"
                            } else {
                                "মাইক্রোফোন ব্যাকগ্রাউন্ডে সার্বক্ষণিক সক্রিয় রাখতে নিচের বাটনে ক্লিক করুন।"
                            },
                            fontSize = 12.sp,
                            color = if (isAlwaysOnMicActive) MyraNeonGreen else Color(0xFF9CA8C5),
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onToggleAlwaysOnMic?.invoke() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAlwaysOnMicActive) MyraGold else MyraCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("toggle_always_on_mic_button")
                        ) {
                            Text(
                                text = if (isAlwaysOnMicActive) "মাইক বন্ধ করুন" else "মাইক সার্বক্ষণিক চালু করুন",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Header
        Text(
            text = "ইউনিভার্সাল কমান্ড সেন্টার",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldPrimary
        )
        Text(
            text = "প্রাকৃতিক বাংলায় যেকোনো নির্দেশ দিন — সহকারী বুঝে যথাযথ পদক্ষেপ নেবে",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // Autonomous Actions Bar (Direct triggers from MYRA AI Autonomous Controller)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    searchQuery = "ইউটিউব থেকে আমার পছন্দের গানটি প্লে করো"
                    activeClassification = CommandRouter.classify(searchQuery)
                    onExecuteAction(activeClassification!!)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131827)),
                border = BorderStroke(1.dp, MyraNeonGreen.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("🎵 গান", fontSize = 11.sp, color = MyraNeonGreen, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    searchQuery = "স্ক্রিনে সোশ্যাল মিডিয়া লাইক ও কমেন্ট করো"
                    activeClassification = CommandRouter.classify(searchQuery)
                    onExecuteAction(activeClassification!!)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131827)),
                border = BorderStroke(1.dp, MyraCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("❤️ লাইক", fontSize = 11.sp, color = MyraCyan, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    searchQuery = "প্লে-স্টোর ওপেন করে অ্যাপ ইনস্টল করো"
                    activeClassification = CommandRouter.classify(searchQuery)
                    onExecuteAction(activeClassification!!)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131827)),
                border = BorderStroke(1.dp, MyraGold.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("📲 অ্যাপ", fontSize = 11.sp, color = MyraGold, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    searchQuery = "একটি চমৎকার শায়রি শোনাও"
                    activeClassification = CommandRouter.classify(searchQuery)
                    onExecuteAction(activeClassification!!)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131827)),
                border = BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("✨ শায়রি", fontSize = 11.sp, color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold)
            }
        }

        // Search Bar with Submit
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                if (it.isNotBlank()) {
                    activeClassification = CommandRouter.classify(it)
                    executionResultText = null
                } else {
                    activeClassification = null
                }
            },
            placeholder = { Text("এখানে বাংলা কমান্ড লিখুন...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    Button(
                        onClick = {
                            activeClassification = CommandRouter.classify(searchQuery)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text("বিশ্লেষণ", color = Color.Black, fontSize = 11.sp)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("universal_command_input"),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Preset command suggestions
        Text("দ্রুত কমান্ডের উদাহরণ:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(presetExamples) { example ->
                FilterChip(
                    selected = searchQuery == example,
                    onClick = {
                        searchQuery = example
                        activeClassification = CommandRouter.classify(example)
                        executionResultText = null
                    },
                    label = { Text(example, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Classification Card
        AnimatedVisibility(visible = activeClassification != null) {
            val cmd = activeClassification ?: return@AnimatedVisibility

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = getCategoryIcon(cmd.category),
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(cmd.titleBn, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldPrimary)
                            Text("শ্রেণীবিভাগ: ${cmd.category.name}", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(cmd.explanationBn, fontSize = 13.sp, lineHeight = 19.sp)

                    if (cmd.requiresConfirmation) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = WarningAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "⚠️ এই কমান্ডটি কার্যকর করতে আপনার নিশ্চিতকরণ আবশ্যক।",
                                color = WarningAmber,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                executionResultText = "কমান্ড কার্যকর করা হচ্ছে..."
                                onExecuteAction(cmd)
                                executionResultText = "সফলভাবে সম্পন্ন হয়েছে: ${cmd.titleBn}"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.testTag("confirm_execute_command_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (cmd.requiresConfirmation) "নিশ্চিত করে সম্পন্ন করুন" else "পদক্ষেপ নিন", color = Color.Black)
                        }

                        if (cmd.category == CommandCategory.CODE_DEV) {
                            OutlinedButton(onClick = { onNavigateToDev(searchQuery) }) {
                                Icon(Icons.Default.Code, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ওয়ার্কস্পেস খুলুন")
                            }
                        }

                        if (cmd.category == CommandCategory.WEB_SEARCH) {
                            OutlinedButton(onClick = { onNavigateToBrowser(searchQuery) }) {
                                Icon(Icons.Default.Language, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ব্রাউজারে দেখুন")
                            }
                        }

                        if (cmd.category == CommandCategory.SCREEN_HELP) {
                            OutlinedButton(onClick = { onNavigateToScreenHelp() }) {
                                Icon(Icons.Default.ScreenSearchDesktop, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("স্ক্রিন সাহায্য খুলুন")
                            }
                        }
                    }

                    // Execution feedback
                    if (executionResultText != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.padding(end = 6.dp))
                            Text(executionResultText!!, fontSize = 12.sp, color = EmeraldPrimary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Supported Action Categories Summary
        Text("সহায়তাপ্রাপ্ত প্রধান সেবাসমূহ:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            item {
                CategoryInfoTile(
                    title = "ডিভাইস ও অ্যান্ড্রয়েড সেটিংস",
                    desc = "ওয়াই-ফাই, ব্লুটুথ, ডিসপ্লে উজ্জ্বলতা ও ব্যাটারি সেটিংস অবিলম্বে চালু করা।",
                    icon = Icons.Default.Settings
                )
            }
            item {
                CategoryInfoTile(
                    title = "লাইভ স্ক্রিন বিশ্লেষণ ও সহায়তা",
                    desc = "যেকোনো অ্যাপের সমস্যা, ত্রুটি মেসেজ বিশ্লেষণ ও ধাপে ধাপে সমাধান।",
                    icon = Icons.Default.ScreenSearchDesktop
                )
            }
            item {
                CategoryInfoTile(
                    title = "স্ক্রিন রেকর্ডিং শর্টকাট",
                    desc = "উচ্চমানের স্ক্রিন ভিডিও ধারণ, বিরতি ও মেমোরিতে সংরক্ষণ।",
                    icon = Icons.Default.Videocam
                )
            }
            item {
                CategoryInfoTile(
                    title = "ওয়েবসাইট ও কোড নির্মাণ",
                    desc = "এইচটিএমএল, সিএসএস, জাভাস্ক্রিপ্ট এবং পাইথন কোড লেখা ও লাইভ স্যান্ডবক্স টেস্ট।",
                    icon = Icons.Default.Code
                )
            }
            item {
                CategoryInfoTile(
                    title = "বিশ্রাম মোড ও সেশন নিয়ন্ত্রণ",
                    desc = "'বিশ্রাম মোডে যাও' বলে ভয়েস লিসেনিং সাময়িক স্থগিত রাখা।",
                    icon = Icons.Default.NightsStay
                )
            }
        }
    }
}

@Composable
private fun CategoryInfoTile(title: String, desc: String, icon: ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.padding(end = 12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(desc, fontSize = 11.sp, color = Color.Gray, lineHeight = 16.sp)
            }
        }
    }
}

private fun getCategoryIcon(cat: CommandCategory): ImageVector {
    return when (cat) {
        CommandCategory.OPEN_SETTINGS -> Icons.Default.Settings
        CommandCategory.SCREEN_HELP -> Icons.Default.ScreenSearchDesktop
        CommandCategory.SCREEN_RECORD -> Icons.Default.Videocam
        CommandCategory.CODE_DEV -> Icons.Default.Code
        CommandCategory.WEB_SEARCH -> Icons.Default.Language
        CommandCategory.REST_MODE -> Icons.Default.NightsStay
        else -> Icons.Default.HelpOutline
    }
}
