package com.example.ui.screens.game

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NeonGreen
import kotlin.math.sin
import kotlin.random.Random

data class Bullet(var x: Float, var y: Float)
data class Enemy(var x: Float, var y: Float, var speed: Float, var radius: Float, val color: Color)
data class Star(var x: Float, var y: Float, var speed: Float, val size: Float, val alpha: Float)
data class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val color: Color)

@Composable
fun ThreeJsGameScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
            .testTag("threejs_3d_game_screen")
    ) {
        // High-performance 60fps Native Jetpack Compose Canvas Game Engine (Zero WebView / Zero Mesa crash)
        NativeSpaceDefenderGame()
    }
}

@Composable
fun NativeSpaceDefenderGame(
    modifier: Modifier = Modifier
) {
    var score by remember { mutableIntStateOf(0) }
    var shield by remember { mutableIntStateOf(100) }
    var isPlaying by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }

    var playerX by remember { mutableFloatStateOf(0.5f) } // normalized 0..1
    val bullets = remember { mutableStateListOf<Bullet>() }
    val enemies = remember { mutableStateListOf<Enemy>() }
    val stars = remember { mutableStateListOf<Star>() }
    val particles = remember { mutableStateListOf<Particle>() }

    // Initialize Starfield
    LaunchedEffect(Unit) {
        if (stars.isEmpty()) {
            for (i in 0 until 80) {
                stars.add(
                    Star(
                        x = Random.nextFloat(),
                        y = Random.nextFloat(),
                        speed = Random.nextFloat() * 0.003f + 0.001f,
                        size = Random.nextFloat() * 3f + 1f,
                        alpha = Random.nextFloat() * 0.7f + 0.3f
                    )
                )
            }
        }
    }

    // Main 60fps Game Loop
    LaunchedEffect(isPlaying) {
        var spawnTimer = 0
        var shootCooldown = 0

        while (isPlaying) {
            withFrameNanos {
                // 1. Update stars
                stars.forEach { star ->
                    star.y += star.speed
                    if (star.y > 1f) {
                        star.y = 0f
                        star.x = Random.nextFloat()
                    }
                }

                // 2. Spawn enemies
                spawnTimer++
                if (spawnTimer % 45 == 0 && enemies.size < 8) {
                    val colors = listOf(Color(0xFFFF1744), Color(0xFFFF5252), Color(0xFFFF9100), CyberCyan)
                    enemies.add(
                        Enemy(
                            x = Random.nextFloat() * 0.8f + 0.1f,
                            y = -0.05f,
                            speed = Random.nextFloat() * 0.004f + 0.003f,
                            radius = 22f,
                            color = colors[Random.nextInt(colors.size)]
                        )
                    )
                }

                // 3. Update bullets
                val bulletIter = bullets.iterator()
                while (bulletIter.hasNext()) {
                    val b = bulletIter.next()
                    b.y -= 0.025f
                    if (b.y < -0.1f) {
                        bulletIter.remove()
                    }
                }

                // 4. Update enemies & collision detection
                val enemyIter = enemies.iterator()
                while (enemyIter.hasNext()) {
                    val e = enemyIter.next()
                    e.y += e.speed

                    // Hit by bullet
                    var hit = false
                    val bIter = bullets.iterator()
                    while (bIter.hasNext()) {
                        val b = bIter.next()
                        val dx = (b.x - e.x)
                        val dy = (b.y - e.y)
                        if (dx * dx + dy * dy < 0.0035f) {
                            hit = true
                            bIter.remove()
                            break
                        }
                    }

                    if (hit) {
                        score += 150
                        // Spawn explosion particles
                        for (p in 0 until 12) {
                            particles.add(
                                Particle(
                                    x = e.x,
                                    y = e.y,
                                    vx = (Random.nextFloat() - 0.5f) * 0.015f,
                                    vy = (Random.nextFloat() - 0.5f) * 0.015f,
                                    life = 1f,
                                    color = e.color
                                )
                            )
                        }
                        enemyIter.remove()
                        continue
                    }

                    // Collision with player
                    val pDistX = (e.x - playerX)
                    val pDistY = (e.y - 0.82f)
                    if (pDistX * pDistX + pDistY * pDistY < 0.0045f) {
                        shield -= 25
                        for (p in 0 until 16) {
                            particles.add(
                                Particle(
                                    x = playerX,
                                    y = 0.82f,
                                    vx = (Random.nextFloat() - 0.5f) * 0.02f,
                                    vy = (Random.nextFloat() - 0.5f) * 0.02f,
                                    life = 1f,
                                    color = Color(0xFFFF1744)
                                )
                            )
                        }
                        enemyIter.remove()
                        if (shield <= 0) {
                            shield = 0
                            isPlaying = false
                            isGameOver = true
                        }
                        continue
                    }

                    if (e.y > 1.05f) {
                        enemyIter.remove()
                    }
                }

                // 5. Update particles
                val pIter = particles.iterator()
                while (pIter.hasNext()) {
                    val p = pIter.next()
                    p.x += p.vx
                    p.y += p.vy
                    p.life -= 0.04f
                    if (p.life <= 0f) {
                        pIter.remove()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isPlaying) {
                if (isPlaying) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = playerX + (dragAmount.x / size.width)
                        playerX = newX.coerceIn(0.08f, 0.92f)
                    }
                }
            }
    ) {
        // Native Canvas Renderer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Draw Starfield
            stars.forEach { s ->
                drawCircle(
                    color = CyberCyan.copy(alpha = s.alpha),
                    radius = s.size,
                    center = Offset(s.x * w, s.y * h)
                )
            }

            // 2. Draw Bullets
            bullets.forEach { b ->
                drawRoundRect(
                    color = NeonGreen,
                    topLeft = Offset(b.x * w - 3f, b.y * h - 14f),
                    size = Size(6f, 24f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                )
            }

            // 3. Draw Enemies
            enemies.forEach { e ->
                val ex = e.x * w
                val ey = e.y * h
                // Aura glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(e.color.copy(alpha = 0.6f), Color.Transparent),
                        center = Offset(ex, ey),
                        radius = e.radius * 2f
                    ),
                    radius = e.radius * 2f,
                    center = Offset(ex, ey)
                )
                // Core Drone
                drawCircle(color = e.color, radius = e.radius, center = Offset(ex, ey))
                drawCircle(color = Color.White, radius = e.radius * 0.4f, center = Offset(ex, ey))
            }

            // 4. Draw Particles
            particles.forEach { p ->
                drawCircle(
                    color = p.color.copy(alpha = p.life.coerceIn(0f, 1f)),
                    radius = 4f * p.life,
                    center = Offset(p.x * w, p.y * h)
                )
            }

            // 5. Draw Player Spaceship
            if (!isGameOver) {
                val px = playerX * w
                val py = 0.82f * h

                // Engine flame
                val flameHeight = 18f + sin(System.currentTimeMillis() * 0.02f) * 6f
                val flamePath = Path().apply {
                    moveTo(px - 10f, py + 12f)
                    lineTo(px + 10f, py + 12f)
                    lineTo(px, py + 12f + flameHeight)
                    close()
                }
                drawPath(flamePath, color = CyberCyan)

                // Ship Hull
                val shipPath = Path().apply {
                    moveTo(px, py - 28f) // Nose
                    lineTo(px + 24f, py + 16f) // Right Wing
                    lineTo(px + 12f, py + 8f)
                    lineTo(px, py + 12f)
                    lineTo(px - 12f, py + 8f)
                    lineTo(px - 24f, py + 16f) // Left Wing
                    close()
                }
                drawPath(shipPath, color = NeonGreen)

                // Cockpit Orb
                drawCircle(color = Color.White, radius = 6f, center = Offset(px, py - 6f))
            }
        }

        // Top HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen)
            ) {
                Text(
                    text = "SCORE: $score",
                    color = NeonGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
            ) {
                Text(
                    text = "SHIELD: $shield%",
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // In-game Bottom Controls
        if (isPlaying) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(
                        onClick = { playerX = (playerX - 0.08f).coerceAtLeast(0.08f) },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF0F172A).copy(alpha = 0.8f), CircleShape)
                            .border(1.dp, CyberCyan, CircleShape)
                    ) {
                        Text("◀", color = CyberCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = { playerX = (playerX + 0.08f).coerceAtMost(0.92f) },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF0F172A).copy(alpha = 0.8f), CircleShape)
                            .border(1.dp, CyberCyan, CircleShape)
                    ) {
                        Text("▶", color = CyberCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Fire Laser Button
                IconButton(
                    onClick = {
                        if (bullets.size < 6) {
                            bullets.add(Bullet(playerX, 0.78f))
                        }
                    },
                    modifier = Modifier
                        .size(68.dp)
                        .background(NeonGreen.copy(alpha = 0.2f), CircleShape)
                        .border(2.dp, NeonGreen, CircleShape)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = "Fire", tint = NeonGreen, modifier = Modifier.size(32.dp))
                }
            }
        }

        // Start / Game Over Overlay Screens
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF020617).copy(alpha = 0.92f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = if (isGameOver) "MISSION FAILED" else "NEXUS 3D DEFENDER",
                        color = if (isGameOver) Color(0xFFFF1744) else NeonGreen,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isGameOver)
                            "চূড়ান্ত স্কোর: $score\nস্পেস ড্রোনগুলো আপনার শিল্ড নষ্ট করেছে।"
                        else
                            "BD-NEXUS AI স্পেস কমব্যাট ইঞ্জিন (100% Native 60FPS)\nসাইবার ড্রোন ধ্বংস করুন ও আকাশ সুরক্ষিত রাখুন।",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            score = 0
                            shield = 100
                            bullets.clear()
                            enemies.clear()
                            particles.clear()
                            playerX = 0.5f
                            isGameOver = false
                            isPlaying = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isGameOver) "RETRY MISSION" else "START MISSION",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "ড্র্যাগ করে বা বাটন চেপে ডানে-বামে সরান · ফায়ার বাটন দিয়ে লেজার শুট করুন",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
