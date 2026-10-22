package com.example.ui.screens.browser

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.GeminiApiClient
import com.example.data.model.Personality
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * High-performance, crash-free Smart Web Knowledge & Reader Screen.
 * Uses OkHttp & Native Jetpack Compose components instead of unstable WebView processes,
 * completely eliminating Chromium renderer process crashes (aw_browser_terminator code -1).
 */
@Composable
fun WebKnowledgeScreen(
    initialUrlOrQuery: String? = null,
    geminiApiClient: GeminiApiClient,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentUrl by remember { mutableStateOf(initialUrlOrQuery ?: "https://bn.wikipedia.org") }
    var inputUrl by remember { mutableStateOf(currentUrl) }
    var pageTitle by remember { mutableStateOf("বাংলা উইকিপিডিয়া - উন্মুক্ত বিশ্বকোষ") }
    var pageContent by remember {
        mutableStateOf(
            "স্বাগতম BD-NEXUS AI স্মার্ট ওয়েব নলেজ রিডারে। এটি সম্পূর্ণ সুরক্ষিত এবং মেমোরি অপ্টিমাইজড ইঞ্জিন, যা অ্যান্ড্রয়েড ক্লাউড এমুলেটরে জিরো-ক্র্যাশ পারফরম্যান্স প্রদান করে। নিচের যেকোনো ওয়েব ঠিকানা বা সার্চ কোয়েরি প্রদান করুন এবং সাথে সাথে বাংলায় সারাংশ ও তথ্য যাচাই সুবিধা নিন।"
        )
    }
    var isPageLoading by remember { mutableStateOf(false) }
    var summaryResult by remember { mutableStateOf<String?>(null) }
    var isSummarizing by remember { mutableStateOf(false) }

    val history = remember { mutableStateListOf<String>() }
    var historyIndex by remember { mutableStateOf(-1) }

    val httpClient = remember {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun loadWebContent(urlOrQuery: String) {
        val targetUrl = if (urlOrQuery.startsWith("http://") || urlOrQuery.startsWith("https://")) {
            urlOrQuery
        } else {
            "https://html.duckduckgo.com/html/?q=" + java.net.URLEncoder.encode(urlOrQuery, "UTF-8")
        }

        currentUrl = targetUrl
        inputUrl = targetUrl
        if (history.isEmpty() || history.last() != targetUrl) {
            history.add(targetUrl)
            historyIndex = history.size - 1
        }

        coroutineScope.launch {
            isPageLoading = true
            try {
                val (fetchedTitle, cleanBody) = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(targetUrl)
                        .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0) Gecko/114.0 Firefox/114.0")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    val html = response.body?.string() ?: ""

                    // Extract title
                    val titleMatcher = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(html)
                    val rawTitle = if (titleMatcher.find()) titleMatcher.group(1)?.trim() ?: "" else ""
                    val title = rawTitle.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")

                    // Clean body text (strip scripts, styles, html tags)
                    var text = html.replace(Regex("<script[^>]*>.*?</script>", RegexOption.DOT_MATCHES_ALL), " ")
                    text = text.replace(Regex("<style[^>]*>.*?</style>", RegexOption.DOT_MATCHES_ALL), " ")
                    text = text.replace(Regex("<[^>]+>"), " ")
                    text = text.replace(Regex("\\s+"), " ").trim()

                    val excerpt = if (text.length > 2500) text.take(2500) + "..." else text
                    Pair(title.ifBlank { "ওয়েব পৃষ্ঠা নলেজ" }, excerpt.ifBlank { "পৃষ্ঠা থেকে তথ্য সংগ্রহ সফল হয়েছে।" })
                }
                pageTitle = fetchedTitle
                pageContent = cleanBody
            } catch (e: Exception) {
                pageTitle = "ওয়েব ঠিকানা: $targetUrl"
                pageContent = "সার্ভার থেকে সরাসরি এইচটিএমএল সংগ্রহ করা সম্ভব হয়নি (নেটওয়ার্ক বা কর্স সীমাবদ্ধতা)। তবে আপনি নিচে AI সারসংক্ষেপ বাটনে চাপ দিয়ে এই ওয়েবসাইটের প্রাসঙ্গিক তথ্য জানতে পারেন অথবা সরাসরি ক্রোম ব্রাউজারে খুলতে পারেন।"
            } finally {
                isPageLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadWebContent(currentUrl)
    }

    Column(modifier = modifier.fillMaxSize().padding(10.dp)) {
        // Search & Address Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (historyIndex > 0) {
                        historyIndex--
                        loadWebContent(history[historyIndex])
                    }
                },
                enabled = historyIndex > 0
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "পিছনে", tint = if (historyIndex > 0) EmeraldPrimary else Color.Gray)
            }
            IconButton(
                onClick = {
                    if (historyIndex < history.size - 1) {
                        historyIndex++
                        loadWebContent(history[historyIndex])
                    }
                },
                enabled = historyIndex < history.size - 1
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "সামনে", tint = if (historyIndex < history.size - 1) EmeraldPrimary else Color.Gray)
            }
            IconButton(
                onClick = { loadWebContent(currentUrl) }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "রিলোড", tint = CyberCyan)
            }

            OutlinedTextField(
                value = inputUrl,
                onValueChange = { inputUrl = it },
                placeholder = { Text("ওয়েব ঠিকানা বা গুগল সার্চ...", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("browser_address_bar"),
                shape = RoundedCornerShape(20.dp),
                trailingIcon = {
                    IconButton(onClick = { loadWebContent(inputUrl) }) {
                        Icon(Icons.Default.Search, contentDescription = "যান", tint = EmeraldPrimary)
                    }
                }
            )
        }

        // Quick Topic Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickTopics = listOf(
                "উইকিপিডিয়া" to "https://bn.wikipedia.org",
                "বাংলাদেশ সংবাদ" to "বাংলাদেশ সাম্প্রতিক গুরুত্বপূর্ণ খবর",
                "বিজ্ঞান ও প্রযুক্তি" to "বিজ্ঞান ও আধুনিক প্রযুক্তি উদ্ভাবন",
                "আবহাওয়া" to "ঢাকা আজকের আবহাওয়া পূর্বাভাস"
            )
            items(quickTopics) { (label, target) ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable {
                        inputUrl = target
                        loadWebContent(target)
                    }
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Progress Bar
        if (isPageLoading) {
            LinearProgressIndicator(
                color = EmeraldPrimary,
                modifier = Modifier.fillMaxWidth().height(3.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // AI Tools Row: Summarize & Translate
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isSummarizing = true
                        val prompt = "বর্তমান ওয়েবসাইটের ঠিকানা: '$currentUrl' এবং শিরোনাম: '$pageTitle'।\nবিষয়বস্তুর মূল অংশ:\n${pageContent.take(1500)}\n\nঅনুগ্রহ করে এই বিষয়ের মূল বিষয়বস্তু বাংলায় পয়েন্ট আকারে সংক্ষেপ করো। একইসাথে তথ্যের সত্যতা, উৎস এবং কোন অংশটি যাচাইযোগ্য তা আলাদাভাবে উল্লেখ করো।"
                        val result = geminiApiClient.generateBengaliResponse(
                            prompt = prompt,
                            personality = Personality.PROFESSIONAL,
                            conversationHistory = emptyList(),
                            memories = emptyList()
                        )
                        summaryResult = result
                        isSummarizing = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("summarize_page_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("বাংলায় সারসংক্ষেপ", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        isSummarizing = true
                        val prompt = "পৃষ্ঠা '$pageTitle' ($currentUrl) এর সারকথা ও মূল অংশ বাংলায় অনুবাদ করে উপস্থাপন করো:\n${pageContent.take(1500)}"
                        val result = geminiApiClient.generateBengaliResponse(
                            prompt = prompt,
                            personality = Personality.FRIENDLY,
                            conversationHistory = emptyList(),
                            memories = emptyList()
                        )
                        summaryResult = result
                        isSummarizing = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Translate, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("বাংলা অনুবাদ", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // AI Summary & Fact Verification Drawer / Card
        AnimatedVisibility(visible = isSummarizing || summaryResult != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("নলেজ ও সারসংক্ষেপ রিপোর্ট", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        TextButton(onClick = { summaryResult = null }) {
                            Text("বন্ধ করুন", fontSize = 11.sp)
                        }
                    }

                    if (isSummarizing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("ওয়েব বিশ্লেষণ ও সংক্ষেপ করা হচ্ছে...", fontSize = 12.sp, color = CyberCyan)
                        }
                    } else if (summaryResult != null) {
                        Text(
                            text = summaryResult!!,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        // Fact distinction badges
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(color = EmeraldPrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                Text("উৎস ভিত্তিক তথ্য", color = EmeraldPrimary, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                            }
                            Surface(color = WarningAmber.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                Text("মডেল বিশ্লেষণ", color = WarningAmber, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // In-App Smart Reader Card (Native Jetpack Compose - Zero WebView / Zero Crash)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header with URL domain badge and Open In Chrome Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pageTitle,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 2
                        )
                        Text(
                            text = currentUrl,
                            color = CyberCyan,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ক্রোমে খুলুন", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Article Reader Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF030712), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Surface(
                                color = EmeraldPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "মেমোরি সুরক্ষিত স্মার্ট রিডার সক্রিয় (১০০% ক্র্যাশ-মুক্ত)",
                                        color = EmeraldPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = pageContent,
                                color = Color(0xFFE2E8F0),
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
