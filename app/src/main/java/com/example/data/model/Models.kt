package com.example.data.model

import java.util.UUID

enum class Personality(val displayName: String, val promptModifier: String) {
    FRIENDLY("বন্ধুত্বপূর্ণ (Friendly)", "তুমি একজন অত্যন্ত অমায়িক, আন্তরিক ও সহমর্মী সহকারী। সহজ ও সাবলীল বাংলায় কথা বলো।"),
    FORMAL("আনুষ্ঠানিক (Formal)", "তুমি একজন শ্রদ্ধাশীল, মার্জিত ও প্রমিত বাংলাভাষী সহকারী। সাধু বা শিষ্ট চলিত ভাষায় নির্ভুলভাবে উত্তর দাও।"),
    PROFESSIONAL("পেশাদার (Professional)", "তুমি একজন কর্মদক্ষ, তথ্যসমৃদ্ধ ও পেশাদার AI সহকারী। সংক্ষিপ্ত ও কাজের কথায় মনোনিবেশ করো।"),
    CREATIVE("সৃজনশীল (Creative)", "তুমি একজন সৃষ্টিশীল, উদ্ভাবনী এবং অনুপ্রেরণাদায়ী AI সহকারী। নতুন নতুন চিন্তা ও আর্ট তৈরিতে সাহায্য করো।"),
    ADAPTIVE("অভিযোজিত (Adaptive)", "তুমি BD-NEXUS AI এর অত্যন্ত বুদ্ধিমান, অভিযোজিত ও সক্রিয় সহকারী।")
}

enum class LanguageMode(val displayName: String, val localeCode: String) {
    BANGLA("বাংলা (Bengali)", "bn-BD"),
    BILINGUAL("বাংলা + English", "bn-BD"),
    ENGLISH("English", "en-US")
}

enum class ThemeMode(val displayName: String) {
    EMERALD_CYBER("Emerald Cyber (চিরায়ত সাইবার সবুজ)"),
    SUNSET_GOLD("Sunset Gold (সোনালী আভা)"),
    MIDNIGHT_AMOLED("Midnight AMOLED (গাঢ় অন্ধকার)")
}

enum class Sender {
    USER,
    ASSISTANT,
    SYSTEM
}

data class CommandAction(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val iconName: String = "arrow_forward",
    val actionType: String,
    val payload: String = ""
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val sender: Sender,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedActions: List<CommandAction> = emptyList(),
    val isCode: Boolean = false,
    val codeLanguage: String? = null,
    val codeSnippet: String? = null,
    val sourceCitations: List<String> = emptyList(),
    val confidenceNote: String? = null
)

data class UserMemoryItem(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class CommandCategory {
    OPEN_SETTINGS,
    OPEN_APP,
    APP_INVENTORY,
    DEVICE_HEALTH,
    CAMERA_SCANNER,
    ACCESSIBILITY_ACTION,
    WEB_SEARCH,
    ANALYZE_CONTENT,
    CODE_DEV,
    DEVICE_ACTION,
    SCREEN_HELP,
    SCREEN_RECORD,
    REST_MODE,
    SESSION_CONTROL,
    ANSWER_QUESTION,
    UNSUPPORTED
}

data class ClassifiedCommand(
    val rawQuery: String,
    val category: CommandCategory,
    val titleBn: String,
    val explanationBn: String,
    val actionTarget: String = "",
    val requiresConfirmation: Boolean = false,
    val isExecutable: Boolean = true
)

enum class AppCategory(val displayNameBn: String) {
    COMMUNICATION("যোগাযোগ (Communication)"),
    SOCIAL("সামাজিক যোগাযোগ (Social)"),
    BROWSER("ইন্টারনেট ব্রাউজার (Browser)"),
    CAMERA("ক্যামেরা ও ছবি (Camera/Photo)"),
    GAMES("গেমস ও বিনোদন (Games)"),
    BANKING("ব্যাংকিং ও ফাইন্যান্স (Banking)"),
    PRODUCTIVITY("প্রোডাক্টিভিটি ও টুলস (Productivity)"),
    MEDIA("মিডিয়া প্লেয়ার (Media)"),
    SYSTEM("সিস্টেম ইউটিলিটি (System)"),
    OTHER("অন্যান্য (Other)")
}

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val category: AppCategory,
    val isLaunchable: Boolean,
    val isSystemApp: Boolean,
    val versionName: String = "1.0",
    val targetSdk: Int = 34
)

enum class SeverityLevel(val labelBn: String) {
    SAFE("নিরাপদ"),
    INFO("তথ্যমূলক"),
    WARNING("সতর্কতা"),
    CRITICAL("জরুরি মনোযোগ")
}

data class HealthFinding(
    val id: String = UUID.randomUUID().toString(),
    val titleBn: String,
    val descriptionBn: String,
    val severity: SeverityLevel,
    val recommendationBn: String,
    val actionIntent: String? = null
)

data class DeviceHealthReport(
    val deviceModel: String,
    val androidVersion: String,
    val totalStorageGb: Double,
    val freeStorageGb: Double,
    val usedStoragePercent: Int,
    val isLowStorage: Boolean,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val networkType: String,
    val isNetworkConnected: Boolean,
    val isOverlayEnabled: Boolean,
    val isMicGranted: Boolean,
    val isCameraGranted: Boolean,
    val isAccessibilityEnabled: Boolean,
    val findings: List<HealthFinding>,
    val timestamp: Long = System.currentTimeMillis()
)

data class AuditLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val actionTitleBn: String,
    val actionTarget: String,
    val category: CommandCategory,
    val statusTextBn: String,
    val userConfirmed: Boolean = true
)

data class NearbyDevice(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String,
    val deviceType: String,
    val isPaired: Boolean,
    val signalStrength: String = "মডারেট",
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

data class SocialAccountConnection(
    val id: String,
    val providerName: String,
    val accountLabel: String,
    val isConnected: Boolean,
    val intentPackage: String,
    val officialWebUrl: String,
    val securityCenterUrl: String
)

data class AccessibilityNodeSummary(
    val text: String,
    val contentDescription: String,
    val viewIdResourceName: String?,
    val className: String,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val isEditable: Boolean
)

enum class DeviceType(val displayNameBn: String) {
    SMART_TV("স্মার্ট টেলিভিশন (Smart TV)"),
    CCTV_CAMERA("নিরাপত্তা ক্যামেরা (CCTV/IP Cam)"),
    SMART_LIGHT("স্মার্ট বাতি (Smart Light)"),
    THERMOSTAT("তাপমাত্রা নিয়ন্ত্রক (Thermostat)")
}

data class DeviceItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: DeviceType,
    val statusText: String,
    val isOnline: Boolean = true,
    val isPoweredOn: Boolean = false,
    val ipOrUrl: String = "",
    val authUsername: String = "",
    val hasAuth: Boolean = false
)

data class ScreenAnalysisResult(
    val title: String,
    val fullExplanationBn: String,
    val extractedText: String,
    val actionSteps: List<String>,
    val recommendedSettingIntent: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class PowerIntensiveTaskInfo(
    val id: String,
    val nameBn: String,
    val descriptionBn: String,
    val powerImpactBn: String,
    val isRunning: Boolean,
    val isDisableable: Boolean = true
)

data class BatteryStatusInfo(
    val levelPercent: Int,
    val isCharging: Boolean,
    val isLowBattery: Boolean,
    val temperatureCelsius: Float,
    val voltageMv: Int,
    val healthLabelBn: String,
    val activePowerIntensiveTasks: List<PowerIntensiveTaskInfo>
)
