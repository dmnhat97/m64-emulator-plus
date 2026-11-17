/*
 * XM64 Emulator Plus, a modified version of Mupen64PlusAE
 * <p>
 * Copyright (C) 2026 XM64 Emulator Plus contributors
 * <p>
 * This file is part of XM64 Emulator Plus.
 * <p>
 * XM64 Emulator Plus is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * XM64 Emulator Plus is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General
 * Public License along with XM64 Emulator Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package paulscode.android.mupen64plusae.ui

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import paulscode.android.mupen64plusae.R
import java.io.File

/** Small in-memory cache of downscaled cover art. */
internal object CoverCache {
    private const val MAX_BYTES = 24 * 1024 * 1024
    private const val TARGET_PX = 640

    private val cache = object : LruCache<String, ImageBitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    fun peek(path: String): ImageBitmap? = cache.get(path)

    fun load(path: String): ImageBitmap? {
        cache.get(path)?.let { return it }
        val file = File(path)
        if (!file.isFile) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= TARGET_PX) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeFile(path, options) ?: return null
        return bitmap.asImageBitmap().also { cache.put(path, it) }
    }
}

@Composable
fun CoverImage(path: String?, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = path?.let { CoverCache.peek(it) }, path) {
        value = if (path.isNullOrEmpty()) null else withContext(Dispatchers.IO) { CoverCache.load(path) }
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(bitmap = loaded, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Image(
            painter = painterResource(R.drawable.default_coverart),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
