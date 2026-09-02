package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VipTier
import com.example.data.repository.AnimeRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.theme.*

@Composable
fun PremiumVipScreen(
    animeRepo: AnimeRepository,
    userPrefsRepo: UserPreferencesRepository,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isVip by userPrefsRepo.isVip.collectAsState()
    val vipTierName by userPrefsRepo.vipTierName.collectAsState()
    val coins by userPrefsRepo.coins.collectAsState()
    val vipTiers = animeRepo.vipTiers

    var selectedTierToBuy by remember { mutableStateOf<VipTier?>(null) }

    if (selectedTierToBuy != null) {
        val tier = selectedTierToBuy!!
        AlertDialog(
            onDismissRequest = { selectedTierToBuy = null },
            title = {
                Text(
                    text = "👑 Activate ${tier.name}",
                    color = AnimeTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Duration: ${tier.durationText}",
                        color = AnimeCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Price: ${tier.coinPrice} Reward Coins or $${tier.bdtPrice / 100}.99.",
                        color = AnimeTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Your Balance: $coins Coins",
                        color = AnimeGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (userPrefsRepo.spendCoins(tier.coinPrice)) {
                            userPrefsRepo.activateVip(tier.name)
                            Toast.makeText(context, "${tier.name} activated successfully!", Toast.LENGTH_LONG).show()
                            selectedTierToBuy = null
                        } else {
                            Toast.makeText(context, "Insufficient coins! Complete daily tasks to earn coins.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AnimeNeonRed)
                ) {
                    Text("Unlock with ${tier.coinPrice} Coins")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        userPrefsRepo.activateVip(tier.name)
                        Toast.makeText(context, "Payment successful! ${tier.name} activated.", Toast.LENGTH_LONG).show()
                        selectedTierToBuy = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AnimePurple)
                ) {
                    Text("Instant Checkout")
                }
            },
            containerColor = AnimeDarkSurfaceCard
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("premium_vip_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AnimeDarkSurfaceCard)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AnimeTextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "👑 RS ANIME VIP PASS",
                        color = AnimeGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (isVip) "Current Status: $vipTierName Active" else "Unlimited streaming & 4K Ultra HD quality",
                        color = if (isVip) AnimeGreen else AnimeTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // VIP Hero Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, AnimePurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    AnimePurple.copy(alpha = 0.4f),
                                    AnimeDarkSurfaceCard,
                                    AnimeCrimsonDark.copy(alpha = 0.3f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = AnimeGold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VIP Membership Perks",
                                color = AnimeTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        listOf(
                            "100% Ad-Free uninterrupted streaming experience",
                            "1080p FHD and 4K Ultra HD high-speed playback",
                            "Unlimited unlock for all premium episodes & movies",
                            "High-speed offline downloads support",
                            "Priority access to newly released simulcast anime"
                        ).forEach { perk ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AnimeCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = perk,
                                    color = AnimeTextPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subscription Plans
        item {
            Text(
                text = "Choose Your VIP Plan",
                color = AnimeTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(vipTiers, key = { it.id }) { tier ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (tier.isPopular) AnimeNeonRed else AnimeBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedTierToBuy = tier }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tier.name,
                                    color = AnimeTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (tier.isPopular) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = AnimeNeonRed,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "POPULAR",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = tier.durationText,
                                color = AnimeCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${tier.coinPrice} Coins",
                                color = AnimeGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "or $${tier.bdtPrice / 100}.99",
                                color = AnimeTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = AnimeBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    tier.perks.forEach { perk ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AnimeGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = perk, color = AnimeTextSecondary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { selectedTierToBuy = tier },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tier.isPopular) AnimeNeonRed else AnimeDarkSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Select Plan",
                            fontWeight = FontWeight.Bold,
                            color = if (tier.isPopular) Color.White else AnimeCyan
                        )
                    }
                }
            }
        }
    }
}
