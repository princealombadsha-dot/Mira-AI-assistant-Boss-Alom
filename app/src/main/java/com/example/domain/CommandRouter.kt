package com.example.domain

import android.provider.Settings
import com.example.data.model.ClassifiedCommand
import com.example.data.model.CommandCategory

object CommandRouter {

    fun classify(query: String): ClassifiedCommand {
        val q = query.trim().lowercase()

        // 1. Phone Inventory & Installed Apps
        if (q.contains("কী কী অ্যাপ আছে") || q.contains("আমার ফোনে কি কি অ্যাপ") ||
            q.contains("অ্যাপ তালিকা") || q.contains("সব অ্যাপ দেখাও") ||
            q.contains("installed apps") || q.contains("অ্যাপের তালিকা")
        ) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.APP_INVENTORY,
                titleBn = "ডিভাইস অ্যাপ ইনভেন্টরি স্ক্যান",
                explanationBn = "ফোনে ইনস্টল করা সমস্ত অ্যাপ্লিকেশনের তালিকা ও ক্যাটাগরি বিশ্লেষণ করা হবে।",
                actionTarget = "APP_INVENTORY",
                requiresConfirmation = false
            )
        }

        // 2. Device Health Scanner & Storage
        if (q.contains("স্টোরেজ কত খালি") || q.contains("স্টোরেজ কতটুকু") ||
            q.contains("মেমরি কত খালি") || q.contains("ডিভাইস স্বাস্থ্য") ||
            q.contains("হেলথ স্ক্যান") || q.contains("ফোন স্ক্যান করো") ||
            q.contains("device health") || q.contains("battery status") ||
            q.contains("ব্যাটারি কতটুকু") || q.contains("ফোনের অবস্থা")
        ) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.DEVICE_HEALTH,
                titleBn = "ডিভাইস স্বাস্থ্য ও স্টোরেজ ডায়াগনস্টিক",
                explanationBn = "স্টোরেজ, ব্যাটারি স্বাস্থ্য, নেটওয়ার্ক ও নিরাপত্তা নিরীক্ষা করা হবে।",
                actionTarget = "DEVICE_HEALTH",
                requiresConfirmation = false
            )
        }

        // 3. Accessibility & Voice-driven UI controls
        if (q.contains("নিচে স্ক্রল করো") || q.contains("scroll down") || q.contains("নিচে নামো")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.ACCESSIBILITY_ACTION,
                titleBn = "নিচে স্ক্রল সম্পাদন",
                explanationBn = "অ্যাক্সেসিবিলিটি সার্ভিসের মাধ্যমে স্ক্রিন নিচে স্ক্রল করা হবে।",
                actionTarget = "SCROLL_DOWN",
                requiresConfirmation = false
            )
        }

        if (q.contains("উপরে স্ক্রল করো") || q.contains("scroll up") || q.contains("উপরে ওঠো")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.ACCESSIBILITY_ACTION,
                titleBn = "উপরে স্ক্রল সম্পাদন",
                explanationBn = "অ্যাক্সেসিবিলিটি সার্ভিসের মাধ্যমে স্ক্রিন উপরে স্ক্রল করা হবে।",
                actionTarget = "SCROLL_UP",
                requiresConfirmation = false
            )
        }

        if (q.contains("আগের স্ক্রিনে ফিরে যাও") || q.contains("পেছনে যাও") || q.contains("go back") || q.contains("ব্যাক করো")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.ACCESSIBILITY_ACTION,
                titleBn = "পেছনে ফিরে যাওয়া (Back)",
                explanationBn = "অ্যাক্সেসিবিলিটি সার্ভিসের মাধ্যমে পূর্বের স্ক্রিনে ফিরে যাওয়া হবে।",
                actionTarget = "NAV_BACK",
                requiresConfirmation = false
            )
        }

        if (q.contains("স্ক্রিন পড়ে শোনাও") || q.contains("পড়ে শোনাও") || q.contains("read screen") || q.contains("লেখাগুলো পড়ো")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.ACCESSIBILITY_ACTION,
                titleBn = "স্ক্রিনের দৃশ্যমান টেক্সট পাঠ",
                explanationBn = "স্ক্রিনের লেখাগুলো স্বয়ংক্রিয়ভাবে পড়ে শোনানো হবে।",
                actionTarget = "READ_SCREEN",
                requiresConfirmation = false
            )
        }

        // 4. Settings navigation
        if (q.contains("wi-fi") || q.contains("ওয়াইফাই") || q.contains("ওয়াই-ফাই") || q.contains("wifi")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.OPEN_SETTINGS,
                titleBn = "ওয়াই-ফাই (Wi-Fi) সেটিংস",
                explanationBn = "আপনার ডিভাইসের ওয়াই-ফাই সেটিংস পেজ চালু করা হবে।",
                actionTarget = Settings.ACTION_WIFI_SETTINGS,
                requiresConfirmation = false
            )
        }

        if (q.contains("bluetooth") || q.contains("ব্লুটুথ")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.OPEN_SETTINGS,
                titleBn = "ব্লুটুথ (Bluetooth) সেটিংস",
                explanationBn = "ডিভাইসের ব্লুটুথ সংযোগ ও পেয়ারিং সেটিংস স্ক্রিন খোলা হবে।",
                actionTarget = Settings.ACTION_BLUETOOTH_SETTINGS,
                requiresConfirmation = false
            )
        }

        if (q.contains("display") || q.contains("ডিসপ্লে") || q.contains("উজ্জ্বলতা") || q.contains("ব্রাইটনেস")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.OPEN_SETTINGS,
                titleBn = "ডিসপ্লে ও উজ্জ্বলতা সেটিংস",
                explanationBn = "স্ক্রিন ব্রাইটনেস ও ডিসপ্লে কনফিগারেশন খোলা হবে।",
                actionTarget = Settings.ACTION_DISPLAY_SETTINGS,
                requiresConfirmation = false
            )
        }

        if (q.contains("ব্যাটারি সেটিংস") || q.contains("battery settings")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.OPEN_SETTINGS,
                titleBn = "ব্যাটারি ব্যবহার ও সেভার সেটিংস",
                explanationBn = "ফোনের ব্যাটারি ও চার্জ সংরক্ষণ সেটিংস খোলা হবে।",
                actionTarget = Settings.ACTION_BATTERY_SAVER_SETTINGS,
                requiresConfirmation = false
            )
        }

        // Camera & Vision Scanner
        if (q.contains("ক্যামেরা") || q.contains("স্ক্যানার") || q.contains("ছবি তোলো") ||
            q.contains("camera") || q.contains("scanner") || q.contains("টেক্সট স্ক্যান") ||
            q.contains("qr কোড") || q.contains("কিউআর")
        ) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.CAMERA_SCANNER,
                titleBn = "এআই ক্যামেরা ও ভিশন স্ক্যানার",
                explanationBn = "ক্যামেরা চালু করে ছবি ক্যাপচার, ওসিআর টেক্সট পাঠ ও দৃশ্য বিশ্লেষণ করা হবে।",
                actionTarget = "CAMERA_SCANNER",
                requiresConfirmation = false
            )
        }

        // App Launching
        if (q.contains("whatsapp") || q.contains("হোয়াটসঅ্যাপ")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.OPEN_APP,
                titleBn = "WhatsApp চালু",
                explanationBn = "হোয়াটসঅ্যাপ মেসেঞ্জার অ্যাপ্লিকেশন চালু করা হবে।",
                actionTarget = "com.whatsapp",
                requiresConfirmation = false
            )
        }

        if (q.contains("chrome") || q.contains("ক্রোম")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.OPEN_APP,
                titleBn = "Google Chrome চালু",
                explanationBn = "আপনার ডিভাইসের Google Chrome ব্রাউজার চালু করা হবে।",
                actionTarget = "com.android.chrome",
                requiresConfirmation = false
            )
        }

        if (q.contains("youtube") || q.contains("ইউটিউব")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.OPEN_APP,
                titleBn = "YouTube অ্যাপ চালু",
                explanationBn = "আপনার ডিভাইসের YouTube অ্যাপ্লিকেশন চালু করা হবে।",
                actionTarget = "com.google.android.youtube",
                requiresConfirmation = false
            )
        }

        if (q.contains("হোমে যাও") || q.contains("হোম স্ক্রিন") || q.contains("go home")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.OPEN_APP,
                titleBn = "হোম স্ক্রিনে প্রত্যাবর্তন",
                explanationBn = "সরাসরি সিস্টেমের হোম স্ক্রিনে ফিরে যাওয়া হবে।",
                actionTarget = "HOME",
                requiresConfirmation = false
            )
        }

        if (q.contains("গ্যালারি") || q.contains("ছবি গ্যালারি")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.OPEN_APP,
                titleBn = "ফটো গ্যালারি চালু",
                explanationBn = "ডিভাইসের ছবি ও ফটো গ্যালারি খোলা হবে।",
                actionTarget = "GALLERY",
                requiresConfirmation = false
            )
        }

        if (q.contains("ক্যালকুলেটর") || q.contains("calculator")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.OPEN_APP,
                titleBn = "ক্যালকুলেটর চালু",
                explanationBn = "ডিভাইসের ক্যালকুলেটর অ্যাপ্লিকেশন খোলা হবে।",
                actionTarget = "CALCULATOR",
                requiresConfirmation = false
            )
        }

        // Rest mode and session control
        if (q.contains("বিশ্রাম") || q.contains("রেস্ট মোড") || q.contains("sleep") || q.contains("কথা শোনা বন্ধ করো")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.REST_MODE,
                titleBn = "বিশ্রাম মোড সক্রিয়করণ",
                explanationBn = "সহকারী সব ধরনের সক্রিয় শ্রবণ সাময়িক স্থগিত রাখবে।",
                requiresConfirmation = false
            )
        }

        if (q.contains("বন্ধ হও") || q.contains("চলে যাও") || q.contains("প্রস্থান") || q.contains("exit")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.SESSION_CONTROL,
                titleBn = "সেশন সমাপ্ত ও প্রস্থান",
                explanationBn = "চলমান সেশন সমাপ্ত করে ফ্লোটিং বাবল বন্ধ করা হবে।",
                requiresConfirmation = true
            )
        }

        // Screen Help & Analysis
        if (q.contains("স্ক্রিন সাহায্য") || q.contains("স্ক্রিন দেখাও") || q.contains("স্ক্রিনে দেখানো সমস্যা") ||
            q.contains("স্ক্রিনের অর্থ") || q.contains("স্ক্রিন বিশ্লেষণ") || q.contains("এই ছবির মধ্যে কী আছে")
        ) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.SCREEN_HELP,
                titleBn = "লাইভ স্ক্রিন বিশ্লেষণ ও সাহায্য",
                explanationBn = "অনুমোদিত স্ক্রিনের ছবি নিয়ে কৃত্রিম বুদ্ধিমত্তার মাধ্যমে সমস্যা সমাধান ও ব্যাখ্যা দেওয়া হবে।",
                requiresConfirmation = false
            )
        }

        // Screen Recording
        if (q.contains("স্ক্রিন রেকর্ড") || q.contains("ভিডিও করো")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.SCREEN_RECORD,
                titleBn = "স্ক্রিন রেকর্ডিং শর্টকাট",
                explanationBn = "স্ক্রিনের ভিডিও ও অডিও ধারণ শুরু করতে অনুমতি প্রয়োজন।",
                requiresConfirmation = true
            )
        }

        // Code & Developer Workspace
        if (q.contains("ওয়েবসাইট তৈরি") || q.contains("কোড") || q.contains("প্রোগ্রাম") ||
            q.contains("অ্যাপ তৈরি") || q.contains("javascript") || q.contains("python") || q.contains("html")
        ) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.CODE_DEV,
                titleBn = "ডেভেলপার ওয়ার্কস্পেস ও কোড জেনারেশন",
                explanationBn = "নতুন প্রজেক্ট কোড তৈরি এবং ইন্টারেক্টিভ স্যান্ডবক্সে লাইভ রান করা হবে।",
                requiresConfirmation = false
            )
        }

        // Device controls (Flashlight, TV, CCTV)
        if (q.contains("ফ্ল্যাশলাইট") || q.contains("টর্চ") || q.contains("বাতি জ্বালাও")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.DEVICE_ACTION,
                titleBn = "ফ্ল্যাশলাইট (টর্চ) নিয়ন্ত্রণ",
                explanationBn = "ক্যামেরার ফ্ল্যাশলাইট টগল করা হবে।",
                actionTarget = "TORCH_TOGGLE",
                requiresConfirmation = false
            )
        }

        if (q.contains("টেলিভিশন") || q.contains("টিভি") || q.contains("স্মার্ট টিভি") || q.contains("কাস্ট")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.DEVICE_ACTION,
                titleBn = "স্মার্ট টিভি ও কাস্ট হাব",
                explanationBn = "স্মার্ট টিভি নিয়ন্ত্রণ ড্যাশবোর্ড খোলা হবে।",
                actionTarget = "SMART_TV_HUB",
                requiresConfirmation = false
            )
        }

        if (q.contains("সিসিটিভি") || q.contains("ক্যামেরা স্ট্রিম") || q.contains("cctv") || q.contains("আইপি ক্যামেরা")) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.DEVICE_ACTION,
                titleBn = "সিসিটিভি / আইপি ক্যামেরা মনিটর",
                explanationBn = "নিরাপত্তা ক্যামেরা ফিড ও মনিটরিং স্ক্রিন খোলা হবে।",
                actionTarget = "CCTV_MONITOR",
                requiresConfirmation = false
            )
        }

        // Web search & summarization
        if (q.contains("সংক্ষেপ করো") || q.contains("সারসংক্ষেপ") || q.contains("ওয়েবপেজ") ||
            q.contains("অনুবাদ করো") || q.contains("খুঁজে বের করো") || q.contains("সার্চ করো")
        ) {
            return ClassifiedCommand(
                rawQuery = query,
                category = CommandCategory.WEB_SEARCH,
                titleBn = "ওয়েব অনুসন্ধান ও বিশ্লেষণ ইঞ্জিন",
                explanationBn = "ইন-অ্যাপ ব্রাউজার বা নলেজ ইঞ্জিনে তথ্য অনুসন্ধান ও সংক্ষেপ করা হবে।",
                actionTarget = query,
                requiresConfirmation = false
            )
        }

        // General question answering
        return ClassifiedCommand(
            rawQuery = query,
            category = CommandCategory.ANSWER_QUESTION,
            titleBn = "সাধারণ বাংলা প্রশ্নোত্তর",
            explanationBn = "উন্নত জেমিনি মডেলের মাধ্যমে আপনার প্রশ্নের উত্তর প্রস্তুত করা হবে।",
            requiresConfirmation = false
        )
    }
}
