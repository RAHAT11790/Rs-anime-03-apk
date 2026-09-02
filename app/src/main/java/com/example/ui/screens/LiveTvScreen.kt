package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiveChannel
import com.example.data.repository.AnimeRepository
import com.example.ui.components.AnimeImage
import com.example.ui.components.LiveChannelCard
import com.example.ui.theme.*

@Composable
fun LiveTvScreen(
    animeRepo: AnimeRepository,
    onWatchChannel: (LiveChannel) -> Unit,
    modifier: Modifier = Modifier
) {
    val liveChannels by animeRepo.liveChannels.collectAsState()
    var selectedChannel by remember(liveChannels) { mutableStateOf(liveChannels.firstOrNull()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("livetv_screen"),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Live TV & Streams",
                            color = AnimeTextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "24/7 non-stop anime broadcasts",
                            color = AnimeTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Surface(
                        color = AnimeDarkSurfaceCard,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AnimeBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cast,
                                contentDescription = "Cast",
                                tint = AnimeCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "HD LIVE",
                                color = AnimeCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Active Player / Channel Preview Banner
        if (selectedChannel != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AnimeDarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AnimeNeonRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                        ) {
                            AnimeImage(
                                model = selectedChannel!!.posterUrl,
                                contentDescription = selectedChannel!!.name,
                                modifier = Modifier.fillMaxSize()
                            )

                            Surface(
                                color = AnimeNeonRed,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "LIVE NOW",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Button(
                                onClick = { onWatchChannel(selectedChannel!!) },
                                colors = ButtonDefaults.buttonColors(containerColor = AnimeNeonRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .testTag("play_selected_channel_btn")
                            ) {
                                Text("Watch Stream", fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = selectedChannel!!.name,
                                color = AnimeTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Now Playing: ${selectedChannel!!.currentShow}",
                                color = AnimeCyan,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "All Available Live Channels",
                color = AnimeTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            )
        }

        items(liveChannels, key = { it.id }) { channel ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                LiveChannelCard(
                    channel = channel,
                    onClick = {
                        selectedChannel = channel
                        onWatchChannel(channel)
                    }
                )
            }
        }
    }
}
