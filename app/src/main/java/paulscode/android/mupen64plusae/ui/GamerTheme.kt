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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Color palette of the gamer UI (dark, high contrast, readable from arm's length on a handheld). */
object Gx {
    val Bg = Color(0xFF0A0C12)
    val Surface = Color(0xFF141824)
    val SurfaceHi = Color(0xFF1D2333)
    val Outline = Color(0xFF2B3350)
    val Text = Color(0xFFF3F4F8)
    val TextDim = Color(0xFFA9B3C9)
    val Accent = Color(0xFF5EA2FF)
    val Accent2 = Color(0xFF8B5CF6)
    val Gold = Color(0xFFFFC857)
    val Danger = Color(0xFFFF6B6B)
    val Scrim = Color(0xB3000000)
}

@Composable
fun GamerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gx.Accent,
            onPrimary = Color.White,
            secondary = Gx.Accent2,
            background = Gx.Bg,
            onBackground = Gx.Text,
            surface = Gx.Surface,
            onSurface = Gx.Text,
            surfaceVariant = Gx.SurfaceHi,
            onSurfaceVariant = Gx.TextDim,
            outline = Gx.Outline,
            error = Gx.Danger
        ),
        content = content
    )
}
