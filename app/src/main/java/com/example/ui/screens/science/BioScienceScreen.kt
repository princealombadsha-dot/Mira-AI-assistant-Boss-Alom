package com.example.ui.screens.science

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.api.GeminiApiClient
import com.example.data.model.Personality
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NeonGreen
import kotlinx.coroutines.launch

@Composable
fun BioScienceScreen(
    geminiApiClient: GeminiApiClient,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf("GENOMICS") }
    var dnaSequenceInput by remember { mutableStateOf("ATGCGATCGATCGATCGTAA") }
    var transcribedRna by remember { mutableStateOf("") }
    var translatedPeptide by remember { mutableStateOf("") }
    var gcContent by remember { mutableStateOf(0.0) }

    var bioQueryText by remember { mutableStateOf("") }
    var bioAnswerText by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    fun analyzeDna(seq: String) {
        val clean = seq.uppercase().replace(Regex("[^ATGC]"), "")
        transcribedRna = clean.replace("T", "U")
        val gc = clean.count { it == 'G' || it == 'C' }
        gcContent = if (clean.isNotEmpty()) (gc.toDouble() / clean.length) * 100 else 0.0

        // Simple codon translation
        val codonMap = mapOf(
            "AUG" to "Met (Start)", "UUU" to "Phe", "UUC" to "Phe", "UUA" to "Leu",
            "UCU" to "Ser", "UAU" to "Tyr", "UGU" to "Cys", "UGG" to "Trp",
            "CUU" to "Leu", "CCU" to "Pro", "CAU" to "His", "CGU" to "Arg",
            "AUU" to "Ile", "ACU" to "Thr", "AAU" to "Asn", "AGU" to "Ser",
            "GUU" to "Val", "GCU" to "Ala", "GAU" to "Asp", "GGU" to "Gly",
            "UAA" to "Stop", "UAG" to "Stop", "UGA" to "Stop"
        )
        val peptide = StringBuilder()
        for (i in 0 until transcribedRna.length - 2 step 3) {
            val codon = transcribedRna.substring(i, i + 3)
            peptide.append(codonMap[codon] ?: "Xaa").append("-")
        }
        translatedPeptide = peptide.toString().removeSuffix("-").ifEmpty { "কোনো কমপ্লিট কোডন পাওয়া যায়নি" }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.15f))
                        .border(1.dp, CyberCyan, CircleShape)
                ) {
                    Icon(Icons.Default.Biotech, contentDescription = null, tint = CyberCyan)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("বায়োসায়েন্স ও জেনেটিক্স ইন্টেলিজেন্স", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    Text("DNA/RNA সিকোয়েন্সিং, প্রোটিন অ্যানালাইসিস ও বায়োমেডিকেল এআই", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        // Category Filter
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedCategory == "GENOMICS",
                    onClick = { selectedCategory = "GENOMICS" },
                    label = { Text("🧬 জিনোমিক্স ও DNA", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                FilterChip(
                    selected = selectedCategory == "MOLECULAR",
                    onClick = { selectedCategory = "MOLECULAR" },
                    label = { Text("🔬 মলিকুলার বায়োলজি", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Coronavirus, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }
        }

        // DNA Sequence Analyzer Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth().testTag("dna_analyzer_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("DNA / RNA সিকোয়েন্স ট্রানস্ক্রিপশন ও অনুবাদক", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = NeonGreen)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dnaSequenceInput,
                        onValueChange = { dnaSequenceInput = it },
                        label = { Text("DNA সিকোয়েন্স প্রবেশ করুন (A, T, G, C)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { analyzeDna(dnaSequenceInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("সিকোয়েন্স বিশ্লেষণ করুন", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (transcribedRna.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("mRNA সিকোয়েন্স: $transcribedRna", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyberCyan)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("GC কন্টেন্ট: ${String.format("%.1f", gcContent)}%", fontSize = 11.sp, color = EmeraldPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("পেপটাইড চেইন: $translatedPeptide", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Bio-AI Inquiry Engine
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth().testTag("bio_ai_inquiry_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("বায়োমেডিকেল ও জেনেটিক্স বিশেষজ্ঞ এআই", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CyberCyan)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = bioQueryText,
                        onValueChange = { bioQueryText = it },
                        placeholder = { Text("CRISPR, প্রোটিন ফোল্ডিং বা জিন থেরাপি সম্পর্কে প্রশ্ন করুন...", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (bioQueryText.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val q = "বায়োসায়েন্স ও জেনেটিক্স বিশ্লেষণ: $bioQueryText"
                                        bioQueryText = ""
                                        isAnalyzing = true
                                        coroutineScope.launch {
                                            val ans = geminiApiClient.generateBengaliResponse(
                                                prompt = q,
                                                personality = Personality.PROFESSIONAL,
                                                conversationHistory = emptyList(),
                                                memories = emptyList()
                                            )
                                            bioAnswerText = ans
                                            isAnalyzing = false
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "পাঠান", tint = CyberCyan)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    if (isAnalyzing) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("বায়োমেডিকেল নলেজবেস অ্যানালাইসিস চলছে...", fontSize = 11.sp, color = CyberCyan)
                        }
                    } else if (bioAnswerText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = bioAnswerText!!,
                                color = Color(0xFFF1F5F9),
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
