package com.example.ui.components

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.AnimeDarkSurfaceVariant

@Composable
fun AnimeImage(
    model: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val localResId = getDrawableResId(model)

    if (localResId != null) {
        Image(
            painter = painterResource(id = localResId),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else if (model.startsWith("http://") || model.startsWith("https://")) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(model)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        // Fallback default poster
        Image(
            painter = painterResource(id = R.drawable.poster_1),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

@SuppressLint("DiscouragedApi")
@DrawableRes
fun getDrawableResId(name: String): Int? {
    return when (name.lowercase().replace("-", "_")) {
        "hero_1" -> R.drawable.hero_1
        "hero_2" -> R.drawable.hero_2
        "hero_3" -> R.drawable.hero_3
        "poster_1" -> R.drawable.poster_1
        "poster_2" -> R.drawable.poster_2
        "poster_3" -> R.drawable.poster_3
        "poster_4" -> R.drawable.poster_4
        "poster_5" -> R.drawable.poster_5
        "poster_6" -> R.drawable.poster_6
        "splash_action_bg" -> R.drawable.splash_action_bg
        "rs_logo" -> R.drawable.rs_logo
        "app_logo" -> R.drawable.app_logo
        else -> null
    }
}
