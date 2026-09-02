package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DailyTasksScreen(
    userPrefsRepo: UserPreferencesRepository,
    onOpenVipScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val coins by userPrefsRepo.coins.collectAsState()
    val streakDays by userPrefsRepo.streakDays.collectAsState()
    val dailyTasks by userPrefsRepo.dailyTasks.collectAsState()
    val watchTimeMins by userPrefsRepo.watchTimeMinutes.collectAsState()

    var promoInput by remember { mutableStateOf("") }
    var friendRefInput by remember { mutableStateOf("") }
    var showSpinDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("daily_tasks_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Coin Wallet & Streak Banner Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, AnimeGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    AnimeDarkSurfaceCard,
                                    AnimeDarkSurfaceVariant,
                                    AnimeDarkSurfaceCard
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Reward Coins Balance",
                                    color = AnimeTextSecondary,
                                    fontSize = 12.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = AnimeGold,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$coins",
                                        color = AnimeGold,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Coins",
                                        color = AnimeTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Button(
                                onClick = onOpenVipScreen,
                                colors = ButtonDefaults.buttonColors(containerColor = AnimePurple),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = AnimeCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("VIP Store", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = AnimeBorder)
                        Spacer(modifier = Modifier.height(14.dp))

                        // 7-Day Streak Row
                        Text(
                            text = "🔥 Daily Login Streak ($streakDays Days Active)",
                            color = AnimeTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val daysOfWeek = listOf("Day 1", "Day 2", "Day 3", "Day 4", "Day 5", "Day 6", "Day 7")
                            val rewards = listOf(10, 12, 14, 16, 18, 20, 30)

                            daysOfWeek.forEachIndexed { idx, label ->
                                val dayNum = idx + 1
                                val isDone = dayNum <= streakDays
                                val isToday = dayNum == streakDays

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isDone) AnimeGold else AnimeDarkSurfaceVariant
                                            )
                                            .border(
                                                1.dp,
                                                if (isToday) AnimeNeonRed else AnimeBorder,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "+${rewards[idx]}",
                                                color = AnimeGold,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        color = if (isDone) AnimeTextPrimary else AnimeTextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Lucky Spin Wheel Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, AnimeCyan.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSpinDialog = true }
                    .testTag("lucky_spin_banner")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(AnimeCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            tint = AnimeCyan,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🎰 Lucky Wheel Spin",
                            color = AnimeTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Spin daily for free to win up to 50 reward coins!",
                            color = AnimeTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { showSpinDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AnimeCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Spin", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Daily Tasks Section
        item {
            Text(
                text = "Daily Tasks",
                color = AnimeTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(dailyTasks, key = { it.id }) { task ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AnimeDarkSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (task.iconName) {
                                        "check" -> Icons.Default.CheckCircle
                                        "play" -> Icons.Default.PlayCircle
                                        "casino" -> Icons.Default.Casino
                                        else -> Icons.Default.Share
                                    },
                                    contentDescription = null,
                                    tint = if (task.isCompleted) AnimeGreen else AnimeNeonRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = task.title,
                                    color = AnimeTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = task.description,
                                    color = AnimeTextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Reward / Claim button
                        if (task.isCompleted) {
                            Surface(
                                color = AnimeGreen.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Done ✓",
                                    color = AnimeGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    when (task.id) {
                                        "task_daily_checkin" -> {
                                            if (userPrefsRepo.claimDailyCheckIn()) {
                                                Toast.makeText(context, "Daily Check-in claimed! Coins added.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        "task_lucky_spin" -> {
                                            showSpinDialog = true
                                        }
                                        "task_watch_30min" -> {
                                            Toast.makeText(context, "Currently watched: $watchTimeMins/30 minutes", Toast.LENGTH_SHORT).show()
                                        }
                                        "task_invite_friend" -> {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Referral Code", userPrefsRepo.userReferralCode)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Referral code copied: ${userPrefsRepo.userReferralCode}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AnimeNeonRed),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("+${task.rewardCoins} Coins", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Progress bar if multi-step (e.g. watch 30 min)
                    if (task.maxProgress > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LinearProgressIndicator(
                                progress = { (task.progress.toFloat() / task.maxProgress.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = AnimeNeonRed,
                                trackColor = AnimeDarkSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${task.progress}/${task.maxProgress} mins",
                                color = AnimeTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. Invite & Referral Program
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = AnimeCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Invite & Earn Bonus",
                            color = AnimeTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Share your referral code with friends. Earn 25 coins for every friend who joins!",
                        color = AnimeTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // User's own referral code card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AnimeDarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AnimeCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Your Referral Code:", color = AnimeTextSecondary, fontSize = 10.sp)
                                Text(userPrefsRepo.userReferralCode, color = AnimeCyan, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            }

                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Referral Code", userPrefsRepo.userReferralCode)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AnimeCyan),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Copy Code", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Enter friend's referral code input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = friendRefInput,
                            onValueChange = { friendRefInput = it },
                            placeholder = { Text("Enter friend's referral code", fontSize = 12.sp, color = AnimeTextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = AnimeDarkSurfaceVariant,
                                unfocusedContainerColor = AnimeDarkSurfaceVariant,
                                focusedTextColor = AnimeTextPrimary,
                                unfocusedTextColor = AnimeTextPrimary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        )

                        Button(
                            onClick = {
                                val result = userPrefsRepo.claimInviteReward(friendRefInput)
                                Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                                if (result.first) friendRefInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AnimeNeonRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text("Claim", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 5. Redeem Promo Code Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎁 Redeem Promo Code",
                        color = AnimeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter official promo codes (e.g. RSANIME50, VIPFREE, RAHAT100)",
                        color = AnimeTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = promoInput,
                            onValueChange = { promoInput = it },
                            placeholder = { Text("Enter Promo Code", fontSize = 12.sp, color = AnimeTextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = AnimeDarkSurfaceVariant,
                                unfocusedContainerColor = AnimeDarkSurfaceVariant,
                                focusedTextColor = AnimeTextPrimary,
                                unfocusedTextColor = AnimeTextPrimary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        )

                        var isRedeeming by remember { mutableStateOf(false) }
                        val coroutineScope = rememberCoroutineScope()

                        Button(
                            onClick = {
                                if (promoInput.isBlank()) {
                                    Toast.makeText(context, "Please enter a promo code", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isRedeeming = true
                                coroutineScope.launch {
                                    val result = userPrefsRepo.redeemPromoCodeAsync(promoInput)
                                    isRedeeming = false
                                    Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                                    if (result.first) promoInput = ""
                                }
                            },
                            enabled = !isRedeeming,
                            colors = ButtonDefaults.buttonColors(containerColor = AnimeGold),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            if (isRedeeming) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Text("Redeem", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Lucky Spin Wheel Modal Dialog
    if (showSpinDialog) {
        LuckySpinWheelDialog(
            onDismiss = { showSpinDialog = false },
            onRewardWon = { reward ->
                userPrefsRepo.claimSpinReward(reward)
                Toast.makeText(context, "Congratulations! You won $reward coins!", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
fun LuckySpinWheelDialog(
    onDismiss: () -> Unit,
    onRewardWon: (Int) -> Unit
) {
    var isSpinning by remember { mutableStateOf(false) }
    var wonCoins by remember { mutableStateOf<Int?>(null) }
    val rotation = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    val wheelSlices = listOf(10, 20, 5, 30, 15, 50, 8, 25)
    val sliceAngle = 360f / wheelSlices.size

    AlertDialog(
        onDismissRequest = { if (!isSpinning) onDismiss() },
        title = {
            Text(
                text = "🎰 Lucky Spin Wheel",
                fontWeight = FontWeight.Bold,
                color = AnimeTextPrimary,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Spin the wheel to win free coins!",
                    color = AnimeTextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Wheel Canvas
                    Canvas(modifier = Modifier.size(220.dp)) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val radius = canvasWidth / 2f
                        val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

                        val colors = listOf(
                            Color(0xFFFF2A5F),
                            Color(0xFF8A2BE2),
                            Color(0xFF00F0FF),
                            Color(0xFFFFB800),
                            Color(0xFF00E676),
                            Color(0xFFFF6D00),
                            Color(0xFF2979FF),
                            Color(0xFFE040FB)
                        )

                        wheelSlices.forEachIndexed { i, _ ->
                            val startAngle = (i * sliceAngle) + rotation.value
                            drawArc(
                                color = colors[i % colors.size],
                                startAngle = startAngle,
                                sweepAngle = sliceAngle,
                                useCenter = true,
                                topLeft = Offset(0f, 0f),
                                size = Size(canvasWidth, canvasHeight),
                                style = Fill
                            )
                        }

                        // Outer border
                        drawCircle(
                            color = Color.White,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 4f)
                        )

                        // Center Pin
                        drawCircle(
                            color = Color.White,
                            radius = 16f,
                            center = center
                        )
                        drawCircle(
                            color = Color(0xFF0A0914),
                            radius = 8f,
                            center = center
                        )
                    }

                    // Top Pointer Arrow
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(20.dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, AnimeNeonRed, CircleShape)
                    )
                }

                if (wonCoins != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "🎉 You won +$wonCoins Coins!",
                        color = AnimeGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isSpinning && wonCoins == null) {
                        isSpinning = true
                        coroutineScope.launch {
                            val targetSlice = (0 until wheelSlices.size).random()
                            val spins = (5..8).random()
                            val targetRotation = (spins * 360f) + (targetSlice * sliceAngle) + (sliceAngle / 2f)

                            rotation.animateTo(
                                targetValue = targetRotation,
                                animationSpec = tween(
                                    durationMillis = 3500,
                                    easing = FastOutSlowInEasing
                                )
                            )

                            val winningCoin = wheelSlices[targetSlice]
                            wonCoins = winningCoin
                            isSpinning = false
                            onRewardWon(winningCoin)
                        }
                    } else if (wonCoins != null) {
                        onDismiss()
                    }
                },
                enabled = !isSpinning,
                colors = ButtonDefaults.buttonColors(containerColor = AnimeNeonRed)
            ) {
                Text(
                    text = if (wonCoins != null) "Claim Reward" else if (isSpinning) "Spinning..." else "Spin Now!",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (!isSpinning) {
                TextButton(onClick = onDismiss) {
                    Text("Close", color = AnimeTextSecondary)
                }
            }
        },
        containerColor = AnimeDarkSurfaceCard
    )
}
