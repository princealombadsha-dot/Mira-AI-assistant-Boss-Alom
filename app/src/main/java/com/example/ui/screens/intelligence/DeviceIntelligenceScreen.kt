package com.example.ui.screens.intelligence

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppCategory
import com.example.data.model.AuditLogEntry
import com.example.data.model.DeviceHealthReport
import com.example.data.model.HealthFinding
import com.example.data.model.InstalledAppInfo
import com.example.data.model.NearbyDevice
import com.example.data.model.SeverityLevel
import com.example.data.model.SocialAccountConnection
import com.example.device.DeviceIntelligenceManager
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.voice.VoiceAssistantManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DeviceIntelligenceScreen(
    deviceIntelManager: DeviceIntelligenceManager,
    voiceManager: VoiceAssistantManager,
    ownerTitle: String,
    onLaunchApp: (String) -> Unit,
    onOpenSetting: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Health, 1: Apps, 2: Devices & Social, 3: Audit Log
    val tabTitles = listOf("স্বাস্থ্য স্ক্যান", "অ্যাপ ইনভেন্টরি", "ডিভাইস ও অ্যাকাউন্ট", "অডিট হিস্ট্রি")

    var isScanning by remember { mutableStateOf(false) }
    var healthReport by remember { mutableStateOf<DeviceHealthReport?>(null) }
    val installedApps = remember { mutableStateListOf<InstalledAppInfo>() }
    val nearbyDevices = remember { mutableStateListOf<NearbyDevice>() }
    val socialConnections = remember { mutableStateListOf<SocialAccountConnection>() }
    val auditLogs = remember { mutableStateListOf<AuditLogEntry>() }

    var appSearchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<AppCategory?>(null) }

    fun refreshAll() {
        isScanning = true
        coroutineScope.launch {
            val report = withContext(Dispatchers.IO) { deviceIntelManager.runDeviceHealthDiagnostics() }
            val apps = withContext(Dispatchers.IO) { deviceIntelManager.getInstalledApplications() }
            val devices = withContext(Dispatchers.IO) { deviceIntelManager.getNearbyBondedDevices() }
            val socials = withContext(Dispatchers.IO) { deviceIntelManager.getSocialConnections() }
            val logs = withContext(Dispatchers.IO) { deviceIntelManager.getAuditLogs() }

            healthReport = report
            installedApps.clear()
            installedApps.addAll(apps)
            nearbyDevices.clear()
            nearbyDevices.addAll(devices)
            socialConnections.clear()
            socialConnections.addAll(socials)
            auditLogs.clear()
            auditLogs.addAll(logs)

            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        refreshAll()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ডিভাইস ইন্টেলিজেন্স ও স্বাস্থ্য হাব",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "$ownerTitle-এর ফোনের রিয়েল-টাইম অডিট ও অ্যাপ ইনভেন্টরি",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    IconButton(
                        onClick = { refreshAll() },
                        modifier = Modifier.testTag("refresh_intel_button")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "রিফ্রেশ স্ক্যান", tint = CyberCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TabRow(
                    selectedTabIndex = selectedSubTab,
                    containerColor = Color.Transparent,
                    contentColor = EmeraldPrimary
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedSubTab == index,
                            onClick = { selectedSubTab = index },
                            text = { Text(title, fontSize = 11.sp, fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        }

        // SubTab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedSubTab) {
                0 -> HealthTabContent(
                    report = healthReport,
                    isScanning = isScanning,
                    ownerTitle = ownerTitle,
                    onVoiceSummary = {
                        healthReport?.let { rep ->
                            val speech = deviceIntelManager.getBengaliVoiceHealthSummary(rep, ownerTitle)
                            voiceManager.speak(speech)
                        }
                    },
                    onOpenSetting = onOpenSetting,
                    onExportReport = {
                        healthReport?.let { rep ->
                            val reportText = "=== Maira Bot ডিভাইস হেলথ রিপোর্ট ===\nমালিক: $ownerTitle\nমডেল: ${rep.deviceModel}\nওএস: ${rep.androidVersion}\nমোট মেমরি: ${rep.totalStorageGb} GB\nখালি মেমরি: ${rep.freeStorageGb} GB\nব্যাটারি: ${rep.batteryPercent}%\nনেটওয়ার্ক: ${rep.networkType}\nঅনুসন্ধান:\n" +
                                    rep.findings.joinToString("\n") { "• [${it.severity.labelBn}] ${it.titleBn}: ${it.descriptionBn}" }

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Maira Bot ডিভাইস হেলথ ডায়াগনস্টিক রিপোর্ট")
                                putExtra(Intent.EXTRA_TEXT, reportText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "রিপোর্ট এক্সপোর্ট করুন"))
                        }
                    }
                )

                1 -> AppsTabContent(
                    apps = installedApps,
                    searchQuery = appSearchQuery,
                    onQueryChange = { appSearchQuery = it },
                    selectedCategory = selectedCategoryFilter,
                    onCategorySelect = { selectedCategoryFilter = it },
                    onVoiceSummary = {
                        val speech = deviceIntelManager.getBengaliVoiceAppsSummary(installedApps, ownerTitle)
                        voiceManager.speak(speech)
                    },
                    onLaunchApp = onLaunchApp
                )

                2 -> DevicesAndSocialContent(
                    nearbyDevices = nearbyDevices,
                    socialConnections = socialConnections,
                    onOpenSocial = { conn ->
                        val pm = context.packageManager
                        val launchIntent = pm.getLaunchIntentForPackage(conn.intentPackage)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        } else {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(conn.officialWebUrl)))
                        }
                    },
                    onOpenSecurity = { url ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )

                3 -> AuditLogContent(
                    logs = auditLogs,
                    onClearLogs = {
                        deviceIntelManager.clearAuditLogs()
                        auditLogs.clear()
                        Toast.makeText(context, "অডিট হিস্ট্রি মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun HealthTabContent(
    report: DeviceHealthReport?,
    isScanning: Boolean,
    ownerTitle: String,
    onVoiceSummary: () -> Unit,
    onOpenSetting: (String) -> Unit,
    onExportReport: () -> Unit
) {
    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldPrimary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick summary card with voice trigger
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(report.deviceModel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        IconButton(onClick = onVoiceSummary) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "ভয়েস সামারি", tint = CyberCyan)
                        }
                    }

                    Text(report.androidVersion, fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Storage bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("স্টোরেজ ব্যবহার", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${report.usedStoragePercent}% (${report.freeStorageGb} GB খালি)", fontSize = 12.sp, color = if (report.isLowStorage) Color.Red else EmeraldPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (report.usedStoragePercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (report.isLowStorage) Color.Red else EmeraldPrimary,
                        trackColor = Color(0xFF334155)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status Chips Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusChip(
                            title = "ব্যাটারি",
                            value = "${report.batteryPercent}% ${if (report.isCharging) "⚡" else ""}",
                            icon = Icons.Default.BatteryChargingFull,
                            color = if (report.batteryPercent < 20) Color(0xFFFFB74D) else EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        StatusChip(
                            title = "নেটওয়ার্ক",
                            value = report.networkType,
                            icon = Icons.Default.Wifi,
                            color = if (report.isNetworkConnected) CyberCyan else Color.Gray,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Service permission states
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PermissionBadge(label = "ফ্লোটিং বাবল", isGranted = report.isOverlayEnabled, modifier = Modifier.weight(1f))
                        PermissionBadge(label = "মাইক্রোফোন", isGranted = report.isMicGranted, modifier = Modifier.weight(1f))
                        PermissionBadge(label = "অ্যাক্সেসিবিলিটি", isGranted = report.isAccessibilityEnabled, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Export report button
        item {
            Button(
                onClick = onExportReport,
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ডায়াগনস্টিক রিপোর্ট শেয়ার / সেভ করুন", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Findings title
        item {
            Text("ডায়াগনস্টিক ফলাফল ও সুপারিশ (${report.findings.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmeraldPrimary)
        }

        // Findings items
        items(report.findings) { finding ->
            FindingCard(finding = finding, onOpenSetting = onOpenSetting)
        }
    }
}

@Composable
private fun StatusChip(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(title, fontSize = 10.sp, color = Color.Gray)
                Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PermissionBadge(label: String, isGranted: Boolean, modifier: Modifier = Modifier) {
    Surface(
        color = if (isGranted) Color(0x2200E676) else Color(0x22EF4444),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) Color(0xFF00E676) else Color(0xFFEF4444))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isGranted) Color(0xFF00E676) else Color(0xFFEF4444)
            )
        }
    }
}

@Composable
private fun FindingCard(finding: HealthFinding, onOpenSetting: (String) -> Unit) {
    val borderColor = when (finding.severity) {
        SeverityLevel.SAFE -> Color(0xFF00E676)
        SeverityLevel.INFO -> CyberCyan
        SeverityLevel.WARNING -> Color(0xFFFFB74D)
        SeverityLevel.CRITICAL -> Color(0xFFEF4444)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (finding.severity == SeverityLevel.SAFE) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = borderColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(finding.titleBn, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = borderColor)
                }

                Surface(
                    color = borderColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = finding.severity.labelBn,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = borderColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(finding.descriptionBn, fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "পরামর্শ: ${finding.recommendationBn}",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )

                if (finding.actionIntent != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = { onOpenSetting(finding.actionIntent) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("সেটিংস ঠিক করুন", fontSize = 10.sp, color = CyberCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppsTabContent(
    apps: List<InstalledAppInfo>,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    selectedCategory: AppCategory?,
    onCategorySelect: (AppCategory?) -> Unit,
    onVoiceSummary: () -> Unit,
    onLaunchApp: (String) -> Unit
) {
    val filteredApps = apps.filter { app ->
        val matchesQuery = searchQuery.isBlank() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
        val matchesCat = selectedCategory == null || app.category == selectedCategory
        matchesQuery && matchesCat
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // App search & voice summary button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text("অ্যাপ খুঁজুন (নাম বা প্যাকেজ)...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onVoiceSummary,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "ভয়েস সামারি", tint = CyberCyan)
                }
            }
        }

        // Categories chip row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onCategorySelect(null) },
                        label = { Text("সব (${apps.size})", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldPrimary)
                    )
                }
                items(AppCategory.values()) { cat ->
                    val count = apps.count { it.category == cat }
                    if (count > 0) {
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { onCategorySelect(cat) },
                            label = { Text("${cat.displayNameBn.substringBefore(" ")} ($count)", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldPrimary)
                        )
                    }
                }
            }
        }

        // Count info
        item {
            Text("মোট পাওয়া গেছে: ${filteredApps.size}টি অ্যাপ্লিকেশন", fontSize = 12.sp, color = Color.Gray)
        }

        // App list
        items(filteredApps) { app ->
            AppItemRow(app = app, onLaunchApp = onLaunchApp)
        }
    }
}

@Composable
private fun AppItemRow(app: InstalledAppInfo, onLaunchApp: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(app.appName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (app.isSystemApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFF334155),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("সিস্টেম", fontSize = 8.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
                Text(app.packageName, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                Text(app.category.displayNameBn, fontSize = 9.sp, color = CyberCyan)
            }

            if (app.isLaunchable) {
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onLaunchApp(app.packageName) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Launch, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("খুলুন", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DevicesAndSocialContent(
    nearbyDevices: List<NearbyDevice>,
    socialConnections: List<SocialAccountConnection>,
    onOpenSocial: (SocialAccountConnection) -> Unit,
    onOpenSecurity: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("নিকটস্থ ও পেয়ার্ড ডিভাইস (${nearbyDevices.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmeraldPrimary)
            Text("ব্লুটুথ ও লোকাল নেটওয়ার্কের অনুমোদিত ডিভাইস সংযোগ", fontSize = 10.sp, color = Color.Gray)
        }

        items(nearbyDevices) { dev ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(dev.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("${dev.deviceType} • ${dev.address}", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Surface(
                        color = Color(0x2200E676),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = dev.signalStrength,
                            color = EmeraldPrimary,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("সামাজিক যোগাযোগ ও অ্যাকাউন্ট লিংক", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = EmeraldPrimary)
            Text("অফিসিয়াল অ্যাপ ও নিরাপত্তা সেন্টারের শর্টকাট", fontSize = 10.sp, color = Color.Gray)
        }

        items(socialConnections) { conn ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(conn.providerName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(conn.accountLabel, fontSize = 10.sp, color = Color.Gray)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { onOpenSecurity(conn.securityCenterUrl) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(12.dp), tint = CyberCyan)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("সিকিউরিটি", fontSize = 9.sp, color = CyberCyan)
                            }

                            Button(
                                onClick = { onOpenSocial(conn) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("খুলুন", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogContent(logs: List<AuditLogEntry>, onClearLogs: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("অ্যাকশন সেফটি ও অডিট লগ", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EmeraldPrimary)
                    Text("Maira Bot কর্তৃক সম্পাদিত সমস্ত কাজের স্বচ্ছ রেকর্ড", fontSize = 11.sp, color = Color.Gray)
                }

                if (logs.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearLogs,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("লগ মুছুন", color = Color.Red, fontSize = 10.sp)
                    }
                }
            }
        }

        if (logs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("এখনও কোনো অডিট লগ রেকর্ড করা হয়নি।", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        } else {
            items(logs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.actionTitleBn, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("টার্গেট: ${log.actionTarget}", fontSize = 10.sp, color = Color.Gray)
                            Text(log.statusTextBn, fontSize = 10.sp, color = EmeraldPrimary)
                        }

                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp)),
                                fontSize = 9.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
