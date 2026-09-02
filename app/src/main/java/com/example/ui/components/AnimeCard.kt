package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnimeItem
import com.example.data.model.AnimeType
import com.example.data.model.WatchHistoryItem
import com.example.ui.theme.*

@Composable
fun AnimeCard(
    anime: AnimeItem,
    isBookmarked: Boolean,
    onCardClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder.copy(alpha = 0.6f)),
        modifier = modifier
            .width(140.dp)
            .clickable { onCardClick() }
            .testTag("anime_card_${anime.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                AnimeImage(
                    model = anime.poster,
                    contentDescription = anime.title,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Badges: Sub/Dub or Movie, Rating
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (anime.type == AnimeType.MOVIE) AnimeCyan else AnimeNeonRed,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (anime.type == AnimeType.MOVIE) "MOVIE" else "SERIES",
                            color = if (anime.type == AnimeType.MOVIE) Color.Black else Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Rating chip
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AnimeGold,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = anime.rating,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Bookmark icon button on bottom right of image
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("bookmark_btn_${anime.id}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (isBookmarked) AnimeNeonRed else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Text Info
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = anime.title,
                    color = AnimeTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = anime.category,
                        color = AnimeCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = anime.year,
                        color = AnimeTextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    historyItem: WatchHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder),
        modifier = modifier
            .width(220.dp)
            .clickable { onClick() }
            .testTag("history_card_${historyItem.animeId}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                AnimeImage(
                    model = historyItem.poster,
                    contentDescription = historyItem.title,
                    modifier = Modifier.fillMaxSize()
                )

                // Play icon overlay in center
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(AnimeNeonRed.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Progress Bar at bottom of thumbnail
                val progress = if (historyItem.totalDurationMs > 0) {
                    (historyItem.lastPositionMs.toFloat() / historyItem.totalDurationMs.toFloat()).coerceIn(0f, 1f)
                } else 0.5f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = AnimeNeonRed,
                    trackColor = Color.Black.copy(alpha = 0.5f)
                )
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = historyItem.title,
                    color = AnimeTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${historyItem.seasonName} • Ep ${historyItem.episodeNumber}",
                    color = AnimeCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
