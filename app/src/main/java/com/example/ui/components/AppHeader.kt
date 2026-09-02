package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun AppHeader(
    coins: Int,
    isVip: Boolean,
    onSearchClick: () -> Unit,
    onCoinsClick: () -> Unit,
    onVipClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AnimeDarkBackground.copy(alpha = 0.95f),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Brand Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { }
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, AnimeNeonRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "RS Anime Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "RS",
                            color = AnimeNeonRed,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ANIME",
                            color = AnimeTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "CLOUD STREAMING",
                        color = AnimeCyan,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // Right Action Badges: Coins, VIP, Search
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Coins Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AnimeDarkSurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AnimeGold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clickable { onCoinsClick() }
                        .testTag("header_coins_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Coins",
                            tint = AnimeGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$coins",
                            color = AnimeGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // VIP Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isVip) AnimePurple.copy(alpha = 0.3f) else AnimeDarkSurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isVip) AnimePurple else AnimeBorder
                    ),
                    modifier = Modifier
                        .clickable { onVipClick() }
                        .testTag("header_vip_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "VIP",
                            tint = if (isVip) AnimeCyan else AnimeGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isVip) "VIP" else "PASS",
                            color = if (isVip) AnimeCyan else AnimeTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Search Icon Button
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, AnimeBorder, RoundedCornerShape(8.dp))
                        .testTag("header_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Anime",
                        tint = AnimeTextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
