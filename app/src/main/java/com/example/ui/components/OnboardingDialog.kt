package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldPrimary

@Composable
fun OnboardingWalkthroughDialog(
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(0) }

    val steps = listOf(
        OnboardingStep(
            title = "আসসালামু আলাইকুম, Alom Boss!",
            description = "আমি Maira Bot — তোমার ব্যক্তিগত VIP AI Assistant। প্রাকৃতিক বাংলা, ক্যামেরা স্ক্যানিং, অ্যাপ কন্ট্রোল ও ভয়েস সংলাপে আমি সবসময় প্রস্তুত।",
            icon = Icons.Default.Mic
        ),
        OnboardingStep(
            title = "ফ্লোটিং সহকারী বাবল",
            description = "যেকোনো অ্যাপের উপরে ছোট ড্র্যাগেবল বাবল রাখুন। এক ক্লিকেই ভয়েস ইনপুট বা স্ক্রিন সমাধান পাওয়া যাবে।",
            icon = Icons.Default.Layers
        ),
        OnboardingStep(
            title = "লাইভ স্ক্রিন বিশ্লেষণ",
            description = "ফোনের স্ক্রিনে আটকে গেলে স্ক্রিন সাহায্য নিন। জেমিনি ভিশন মডেল স্ক্রিনের ত্রুটি শনাক্ত করে বাংলায় করণীয় জানিয়ে দেবে।",
            icon = Icons.Default.ScreenSearchDesktop
        ),
        OnboardingStep(
            title = "নিরাপত্তা ও গোপনীয়তা",
            description = "আপনার অনুমতি ছাড়া কোনো তথ্য সংরক্ষণ বা গোপনে রেকর্ড করা হয় না। সমস্ত পারমিশন সম্পূর্ণ আপনার নিয়ন্ত্রণে।",
            icon = Icons.Default.Security
        )
    )

    val current = steps[step]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(current.icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(current.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = current.description,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Step dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    steps.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (index == step) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (index == step) EmeraldPrimary else Color.Gray)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step < steps.size - 1) {
                        step++
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(
                    text = if (step < steps.size - 1) "পরবর্তী" else "শুরু করুন",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (step > 0) {
                TextButton(onClick = { step-- }) {
                    Text("আগেরটি")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("এড়িয়ে যান")
                }
            }
        }
    )
}

data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
