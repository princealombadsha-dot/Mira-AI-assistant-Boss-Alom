package com.example.ui.screens.devices

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceItem
import com.example.data.model.DeviceType
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.WarningAmber

@Composable
fun SecurityDevicesScreen(
    hasMicPermission: Boolean,
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Devices & Smart Home, 1: CCTV Monitor, 2: Permissions Dashboard
    var isTorchOn by remember { mutableStateOf(false) }

    // Torch control helper
    fun toggleTorch() {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            isTorchOn = !isTorchOn
            cameraManager.setTorchMode(cameraId, isTorchOn)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val devices = remember {
        mutableStateListOf(
            DeviceItem(name = "Samsung Smart TV (ড্রয়িং রুম)", type = DeviceType.SMART_TV, statusText = "সংযুক্ত • Wi-Fi 5GHz", isOnline = true, isPoweredOn = true, ipOrUrl = "192.168.1.105"),
            DeviceItem(name = "প্রধান ফটক CCTV ক্যামেরা", type = DeviceType.CCTV_CAMERA, statusText = "অনলাইন • RTSP এনক্রিপ্টেড", isOnline = true, ipOrUrl = "rtsp://192.168.1.120:554/live", hasAuth = true),
            DeviceItem(name = "লিভিং রুম স্মার্ট লাইট", type = DeviceType.SMART_LIGHT, statusText = "চালু • উজ্জ্বলতা ৮০%", isOnline = true, isPoweredOn = true),
            DeviceItem(name = "স্মার্ট থার্মোস্ট্যাট / এসি", type = DeviceType.THERMOSTAT, statusText = "তাপমাত্রা ২৪°C", isOnline = true, isPoweredOn = true)
        )
    }

    Column(modifier = modifier.fillMaxSize().padding(14.dp)) {
        // Title
        Text(
            text = "ডিভাইস ও নিরাপত্তা ইন্টিগ্রেশন",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = EmeraldPrimary
        )
        Text(
            text = "স্মার্ট টিভি, সিসিটিভি ক্যামেরা, স্মার্ট হোম এবং অ্যান্ড্রয়েড সিস্টেম পারমিশন হাব",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("🏠 ডিভাইস ও টিভি", fontSize = 11.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("📹 সিসিটিভি", fontSize = 11.sp) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("🛡️ পারমিশন", fontSize = 11.sp) })
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab) {
            0 -> {
                // Smart TV & Smart Home Hub
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Phone Torch Tile
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isTorchOn) EmeraldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FlashlightOn, contentDescription = null, tint = if (isTorchOn) EmeraldPrimary else Color.Gray)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("ফোনের ফ্ল্যাশলাইট (টর্চ)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(if (isTorchOn) "বর্তমানে চালু আছে" else "বর্তমানে বন্ধ", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                                Button(
                                    onClick = { toggleTorch() },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isTorchOn) Color.Red else EmeraldPrimary)
                                ) {
                                    Text(if (isTorchOn) "বন্ধ করুন" else "জ্বালান", color = Color.Black, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    items(devices) { device ->
                        DeviceCard(
                            device = device,
                            onTogglePower = {
                                val idx = devices.indexOf(device)
                                if (idx >= 0) {
                                    devices[idx] = device.copy(isPoweredOn = !device.isPoweredOn)
                                }
                            }
                        )
                    }
                }
            }
            1 -> {
                // CCTV Monitor
                CctvMonitorSection(devices.filter { it.type == DeviceType.CCTV_CAMERA })
            }
            2 -> {
                // Permissions Dashboard
                PermissionsDashboardSection(
                    hasMicPermission = hasMicPermission,
                    hasOverlayPermission = hasOverlayPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestMic = onRequestMicPermission,
                    onRequestOverlay = onRequestOverlayPermission,
                    onRequestNotification = onRequestNotificationPermission
                )
            }
        }
    }
}

@Composable
fun DeviceCard(device: DeviceItem, onTogglePower: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (device.type) {
                        DeviceType.SMART_TV -> Icons.Default.Tv
                        DeviceType.CCTV_CAMERA -> Icons.Default.Videocam
                        DeviceType.SMART_LIGHT -> Icons.Default.Lightbulb
                        DeviceType.THERMOSTAT -> Icons.Default.Thermostat
                    }
                    Icon(icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(device.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(device.statusText, fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Switch(
                    checked = device.isPoweredOn,
                    onCheckedChange = { onTogglePower() },
                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                )
            }

            if (device.type == DeviceType.SMART_TV && device.isPoweredOn) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.VolumeDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ভলিউম -", fontSize = 10.sp)
                    }
                    OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ভলিউম +", fontSize = 10.sp)
                    }
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("কাস্ট করুন", fontSize = 10.sp, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun CctvMonitorSection(cctvDevices: List<DeviceItem>) {
    var streamUrl by remember { mutableStateOf("rtsp://admin:secure_pass@192.168.1.120:554/ch1") }
    var isStreaming by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(220.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (isStreaming) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("লাইভ সিসিটিভি এনক্রিপ্টেড ফিড সক্রিয়", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("লগ: কোনো অস্বাভাবিক নড়াচড়া শনাক্ত হয়নি", color = EmeraldPrimary, fontSize = 10.sp)
                    }
                } else {
                    Text("ফিড সাময়িকভাবে বন্ধ", color = Color.Gray, fontSize = 12.sp)
                }

                Surface(
                    color = Color.Red.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                ) {
                    Text("LIVE 🔴", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("RTSP / আইপি ক্যামেরা ইউআরএল:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = streamUrl,
            onValueChange = { streamUrl = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { isStreaming = !isStreaming },
                colors = ButtonDefaults.buttonColors(containerColor = if (isStreaming) Color.Red else EmeraldPrimary),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isStreaming) "ফিড থামান" else "ফিড দেখুন", color = Color.Black)
            }
        }
    }
}

@Composable
fun PermissionsDashboardSection(
    hasMicPermission: Boolean,
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestMic: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotification: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            PermissionItemTile(
                name = "মাইক্রোফোন (ভয়েস ইনপুট)",
                desc = "বাংলা ভাষা শনাক্তকরণ ও হ্যান্ডস-ফ্রি ভয়েস কমান্ডের জন্য আবশ্যক।",
                isGranted = hasMicPermission,
                onRequest = onRequestMic
            )
        }
        item {
            PermissionItemTile(
                name = "স্ক্রিনের উপর ফ্লোটিং বাবল (Overlay)",
                desc = "অন্যান্য অ্যাপ ব্যবহার করার সময় ফ্লোটিং বাবল প্রদর্শনের অনুমতি।",
                isGranted = hasOverlayPermission,
                onRequest = onRequestOverlay
            )
        }
        item {
            PermissionItemTile(
                name = "বিজ্ঞপ্তি ও নোটিফিকেশন",
                desc = "ফ্লোটিং সহকারী ও স্ক্রিন রেকর্ডিং স্ট্যাটাস দেখানোর জন্য।",
                isGranted = hasNotificationPermission,
                onRequest = onRequestNotification
            )
        }
        item {
            PermissionItemTile(
                name = "অ্যাপ সেটিংস ও তথ্য",
                desc = "অন্যান্য সিস্টেম অনুমতি পর্যালোচনা বা পরিবর্তন করতে সেটিংস খুলুন।",
                isGranted = true,
                onRequest = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun PermissionItemTile(
    name: String,
    desc: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (isGranted) EmeraldPrimary else Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Text(desc, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp), lineHeight = 16.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (!isGranted) {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("অনুমতি দিন", color = Color.Black, fontSize = 11.sp)
                }
            } else {
                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("অনুমোদিত", color = EmeraldPrimary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}
