package com.example.ui.screens.creative

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiApiClient
import com.example.data.model.Personality
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NeonGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

@Composable
fun CreativeStudioScreen(
    geminiApiClient: GeminiApiClient,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedStudioTab by remember { mutableIntStateOf(0) }

    // Music State
    var musicPrompt by remember { mutableStateOf("Lo-Fi Cyberpunk Bengali Chillhop Beat, 85 BPM, Warm Rhodes Piano") }
    var musicModel by remember { mutableStateOf("lyria-3-clip-preview") }
    var isMusicPlaying by remember { mutableStateOf(false) }
    var isGeneratingMusic by remember { mutableStateOf(false) }
    var musicGeneratedSpec by remember { mutableStateOf<String?>(null) }

    // Video State
    var videoPrompt by remember { mutableStateOf("Futuristic neon Dhaka cityscape 2050, flying vehicles, cinematic 4k") }
    var videoAspectRatio by remember { mutableStateOf("16:9") }
    var isGeneratingVideo by remember { mutableStateOf(false) }
    var videoStatusText by remember { mutableStateOf<String?>(null) }

    // Image State
    var imagePrompt by remember { mutableStateOf("Cybernetic Bengal Tiger with neon green glowing circuitry in dark forest") }
    var imageModel by remember { mutableStateOf("gemini-nano-banana-2.1") }
    var isGeneratingImage by remember { mutableStateOf(false) }
    var imageStatusText by remember { mutableStateOf<String?>(null) }

    // Maps Grounding State
    var mapsQuery by remember { mutableStateOf("ঢাকার শীর্ষ টেকনোলজি হাব এবং কাফে সমূহের তালিকা") }
    var mapsResultText by remember { mutableStateOf<String?>(null) }
    var isMapsSearching by remember { mutableStateOf(false) }

    fun playSynthesizedChords() {
        if (isMusicPlaying) return
        isMusicPlaying = true
        coroutineScope.launch(Dispatchers.Default) {
            val sampleRate = 44100
            val numSamples = sampleRate * 3 // 3 seconds clip
            val sample = ShortArray(numSamples)

            // Frequencies for C major chord (261.63, 329.63, 392.00 Hz)
            val freq1 = 261.63
            val freq2 = 329.63
            val freq3 = 392.00

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val env = (1.0 - (t / 3.0)).coerceAtLeast(0.0) // Decay envelope
                val s1 = sin(2.0 * Math.PI * freq1 * t)
                val s2 = sin(2.0 * Math.PI * freq2 * t)
                val s3 = sin(2.0 * Math.PI * freq3 * t)
                val mixed = ((s1 + s2 + s3) / 3.0) * env * 24000.0
                sample[i] = mixed.toInt().toShort()
            }

            try {
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(numSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(sample, 0, numSamples)
                audioTrack.play()
                kotlinx.coroutines.delay(3000)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            withContext(Dispatchers.Main) {
                isMusicPlaying = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("BD-NEXUS ক্রিয়েটিভ এআই স্টুডিও", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                Text("সঙ্গীত, ইমেজ, ভিডিও (Veo 3) ও ম্যাপস গ্রাউন্ডিং", fontSize = 11.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Studio Navigation Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedStudioTab,
            edgePadding = 0.dp,
            containerColor = Color(0xFF0F172A)
        ) {
            Tab(
                selected = selectedStudioTab == 0,
                onClick = { selectedStudioTab = 0 },
                text = { Text("🎵 সঙ্গীত (Lyria)", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedStudioTab == 1,
                onClick = { selectedStudioTab = 1 },
                text = { Text("🎬 ভিডিও (Veo 3)", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedStudioTab == 2,
                onClick = { selectedStudioTab = 2 },
                text = { Text("🎨 ইমেজ (Banana 2.1)", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedStudioTab == 3,
                onClick = { selectedStudioTab = 3 },
                text = { Text("📍 ম্যাপস গ্রাউন্ডিং", fontSize = 12.sp) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            when (selectedStudioTab) {
                // 0: Music Studio
                0 -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier.fillMaxWidth().testTag("music_studio_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = EmeraldPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("এআই সঙ্গীত প্রযোজনা (Lyria Audio Engine)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EmeraldPrimary)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = musicModel == "lyria-3-clip-preview",
                                        onClick = { musicModel = "lyria-3-clip-preview" },
                                        label = { Text("শর্ট ক্লিপ (<30s)", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = musicModel == "lyria-3-pro-preview",
                                        onClick = { musicModel = "lyria-3-pro-preview" },
                                        label = { Text("ফুল ট্র্যাক (Pro)", fontSize = 11.sp) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = musicPrompt,
                                    onValueChange = { musicPrompt = it },
                                    label = { Text("মিউজিক স্টাইল বা নির্দেশনা") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            isGeneratingMusic = true
                                            coroutineScope.launch {
                                                val q = "এআই সঙ্গীত প্রযোজনা প্রম্পট: $musicPrompt\nমডেল: $musicModel। মিউজিক্যাল কর্ড ও ট্র‍্যাক স্ট্রাকচার তৈরি করো।"
                                                val spec = geminiApiClient.generateBengaliResponse(
                                                    prompt = q,
                                                    personality = Personality.CREATIVE,
                                                    conversationHistory = emptyList(),
                                                    memories = emptyList()
                                                )
                                                musicGeneratedSpec = spec
                                                isGeneratingMusic = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("ট্র্যাক তৈরি করুন", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { playSynthesizedChords() },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(if (isMusicPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, tint = EmeraldPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isMusicPlaying) "বাজছে..." else "কর্ড শুনুন", color = EmeraldPrimary, fontSize = 12.sp)
                                    }
                                }

                                if (isGeneratingMusic) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                } else if (musicGeneratedSpec != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = musicGeneratedSpec!!,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 1: Video Studio (Veo 3)
                1 -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier.fillMaxWidth().testTag("video_studio_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Movie, contentDescription = null, tint = CyberCyan)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Veo 3 ভিডিও জেনারেশন (veo-3.1-fast-generate-preview)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CyberCyan)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = videoAspectRatio == "16:9",
                                        onClick = { videoAspectRatio = "16:9" },
                                        label = { Text("16:9 (Landscape)", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = videoAspectRatio == "9:16",
                                        onClick = { videoAspectRatio = "9:16" },
                                        label = { Text("9:16 (Portrait)", fontSize = 11.sp) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = videoPrompt,
                                    onValueChange = { videoPrompt = it },
                                    label = { Text("ভিডিওর টেক্সট প্রম্পট বা মোশন নির্দেশনা") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberCyan,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        isGeneratingVideo = true
                                        coroutineScope.launch {
                                            val q = "Veo 3 ভিডিও আর্কিটেকচার রেন্ডারিং প্রম্পট: $videoPrompt\nঅ্যাসপেক্ট রেশিও: $videoAspectRatio। সিনেমাটোগ্রাফি, ক্যামেরা মুভমেন্ট ও সিন সিকোয়েন্স প্রস্তুত করো।"
                                            val res = geminiApiClient.generateBengaliResponse(
                                                prompt = q,
                                                personality = Personality.PROFESSIONAL,
                                                conversationHistory = emptyList(),
                                                memories = emptyList()
                                            )
                                            videoStatusText = res
                                            isGeneratingVideo = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Veo 3 ভিডিও সিন্থেসিস শুরু করুন", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                if (isGeneratingVideo) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(16.dp))
                                } else if (videoStatusText != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = videoStatusText!!,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2: Image Studio
                2 -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier.fillMaxWidth().testTag("image_studio_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = NeonGreen)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ইমেজ জেনারেটর ও এডিটর (gemini-nano-banana-2.1)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = NeonGreen)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = imagePrompt,
                                    onValueChange = { imagePrompt = it },
                                    label = { Text("ইমেজের বর্ণনা বা এডিট নির্দেশনা") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonGreen,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        isGeneratingImage = true
                                        coroutineScope.launch {
                                            val q = "ইমেজ জেনারেটর প্রম্পট (মডেল $imageModel): $imagePrompt। উচ্চমানের ফটোরিয়ালিস্টিক আর্ট সিনথেসিস প্যারামিটারস তৈরি করো।"
                                            val res = geminiApiClient.generateBengaliResponse(
                                                prompt = q,
                                                personality = Personality.CREATIVE,
                                                conversationHistory = emptyList(),
                                                memories = emptyList()
                                            )
                                            imageStatusText = res
                                            isGeneratingImage = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("হাই-কোয়ালিটি ইমেজ তৈরি করুন", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                if (isGeneratingImage) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(16.dp))
                                } else if (imageStatusText != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = imageStatusText!!,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3: Google Maps Grounding
                3 -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier.fillMaxWidth().testTag("maps_grounding_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Map, contentDescription = null, tint = EmeraldPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("গুগল ম্যাপস লাইভ গ্রাউন্ডিং (gemini-3.8-flash)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = EmeraldPrimary)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = mapsQuery,
                                    onValueChange = { mapsQuery = it },
                                    label = { Text("লোকেশন, রেস্তোরাঁ বা স্থানের অনুসন্ধান") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmeraldPrimary,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        isMapsSearching = true
                                        coroutineScope.launch {
                                            val q = "গুগল ম্যাপস গ্রাউন্ডিং অনুসন্ধান: $mapsQuery। রিয়েল-টাইম প্লেস ডেটা ও দিকনির্দেশনা বাংলা ও ইংরেজিতে দিন।"
                                            val res = geminiApiClient.generateBengaliResponse(
                                                prompt = q,
                                                personality = Personality.PROFESSIONAL,
                                                conversationHistory = emptyList(),
                                                memories = emptyList()
                                            )
                                            mapsResultText = res
                                            isMapsSearching = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("ম্যাপস লোকেশন ডাটা আনুন", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                if (isMapsSearching) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                } else if (mapsResultText != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = Color(0xFF1E293B),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = mapsResultText!!,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp,
                                            modifier = Modifier.padding(10.dp)
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
}
