package com.drivetheory.cbt.presentation.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drivetheory.cbt.R

@Composable
fun BrandLogo(modifier: Modifier = Modifier, size: Dp = 120.dp) {
    val context = LocalContext.current
    val imageState = remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        // Try canonical name first, otherwise load first image under assets/brand (webp/png/jpg)
        fun tryOpen(path: String): Boolean {
            return try {
                context.assets.open(path).use { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    if (bmp != null) {
                        imageState.value = bmp.asImageBitmap()
                        true
                    } else false
                }
            } catch (_: Exception) {
                false
            }
        }
        if (!tryOpen("brand/logo.webp")) {
            try {
                val files = context.assets.list("brand")?.toList().orEmpty()
                val candidate = files.firstOrNull { it.endsWith(".webp", true) }
                    ?: files.firstOrNull { it.endsWith(".png", true) }
                    ?: files.firstOrNull { it.endsWith(".jpg", true) || it.endsWith(".jpeg", true) }
                if (candidate != null) {
                    tryOpen("brand/$candidate")
                }
            } catch (_: Exception) { /* ignore */ }
        }
    }

    val img = imageState.value
    if (img != null) {
        Image(bitmap = img, contentDescription = "Brand Logo", modifier = modifier.size(size))
    } else {
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "App Icon",
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(size)
        )
    }
}
