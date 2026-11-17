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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.InputDevice
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import androidx.preference.TwoStatePreference
import kotlinx.coroutines.delay

/** Supplies the (live) preference tree of the host fragment. */
fun interface PrefsScreenProvider {
    fun screen(): PreferenceScreen?
}

object ControllerUtil {
    @JvmStatic
    fun isControllerConnected(): Boolean {
        for (id in InputDevice.getDeviceIds()) {
            val device = InputDevice.getDevice(id) ?: continue
            if (device.isVirtual) continue
            val sources = device.sources
            if ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) return true
        }
        return false
    }
}

class PrefsState {
    /** Bumped whenever a value may have changed so the list is read again. */
    var version by mutableIntStateOf(0)
    var inputMode by mutableStateOf(if (ControllerUtil.isControllerConnected()) InputMode.GAMEPAD else InputMode.TOUCH)

    fun bump() {
        version++
    }
}

object PrefsUi {
    @JvmStatic
    fun attach(view: ComposeView, state: PrefsState, provider: PrefsScreenProvider, onBack: Runnable) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent { PrefsApp(state, provider) { onBack.run() } }
    }
}

private enum class RowKind { HEADER, TOGGLE, ITEM }

private class PrefRow(
    val pref: Preference,
    val kind: RowKind,
    val title: String,
    val summary: String,
    val enabled: Boolean,
    val checked: Boolean,
    val icon: Drawable?
)

private fun collect(group: PreferenceGroup, out: MutableList<PrefRow>) {
    for (i in 0 until group.preferenceCount) {
        val pref = group.getPreference(i)
        if (!pref.isVisible) continue
        val title = pref.title?.toString().orEmpty()
        when {
            pref is PreferenceCategory -> {
                out += PrefRow(pref, RowKind.HEADER, title, "", true, false, null)
                collect(pref, out)
            }
            pref is TwoStatePreference -> {
                val summary = (if (pref.isChecked) pref.summaryOn else pref.summaryOff) ?: pref.summary
                out += PrefRow(pref, RowKind.TOGGLE, title, summary?.toString().orEmpty(), pref.isEnabled, pref.isChecked, pref.icon)
            }
            else -> out += PrefRow(pref, RowKind.ITEM, title, pref.summary?.toString().orEmpty(), pref.isEnabled && pref.isSelectable, false, pref.icon)
        }
    }
}

private fun Drawable.toImageBitmapOrNull() = runCatching {
    val w = intrinsicWidth.coerceIn(1, 128)
    val h = intrinsicHeight.coerceIn(1, 128)
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, w, h)
    draw(canvas)
    bitmap.asImageBitmap()
}.getOrNull()

@Composable
fun PrefsApp(state: PrefsState, provider: PrefsScreenProvider, onBack: () -> Unit) {
    GamerTheme {
        val gamepadUi = state.inputMode == InputMode.GAMEPAD
        // Activities change summaries / visibility directly on the preference objects, so re-read often
        LaunchedEffect(Unit) {
            while (true) {
                delay(500)
                state.bump()
            }
        }
        val rows = remember(state.version) {
            val list = mutableListOf<PrefRow>()
            provider.screen()?.let { collect(it, list) }
            list
        }
        val firstFocusable = rows.indexOfFirst { it.kind != RowKind.HEADER && it.enabled }
        val firstFocus = remember { FocusRequester() }
        LaunchedEffect(gamepadUi, rows.isNotEmpty()) {
            if (gamepadUi && rows.isNotEmpty()) {
                delay(150)
                runCatching { firstFocus.requestFocus() }
            }
        }
        CompositionLocalProvider(LocalGamepadUi provides gamepadUi) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Gx.Bg)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Press) state.inputMode = InputMode.TOUCH
                            }
                        }
                    }
                    .onPreviewKeyEvent { event ->
                        val key = event.key
                        val controller = key == Key.DirectionUp || key == Key.DirectionDown ||
                            key == Key.DirectionLeft || key == Key.DirectionRight || key == Key.DirectionCenter ||
                            key == Key.ButtonA || key == Key.ButtonB
                        if (event.type == KeyEventType.KeyDown && controller) state.inputMode = InputMode.GAMEPAD
                        if (key == Key.ButtonB) {
                            if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) onBack()
                            true
                        } else false
                    }
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(rows, key = { index, row -> "${row.pref.key ?: row.title}#$index" }) { index, row ->
                        when (row.kind) {
                            RowKind.HEADER -> Text(
                                row.title,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Gx.Accent, letterSpacing = 1.sp,
                                modifier = Modifier.padding(top = if (index == 0) 0.dp else 14.dp, start = 4.dp)
                            )
                            else -> PrefItem(
                                row,
                                if (index == firstFocusable) Modifier.focusRequester(firstFocus) else Modifier
                            ) {
                                row.pref.performClick()
                                state.bump()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrefItem(row: PrefRow, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var rawFocus by remember { mutableStateOf(false) }
    val focused = rawFocus && LocalGamepadUi.current
    val shape = RoundedCornerShape(14.dp)
    val icon = remember(row.icon) { row.icon?.toImageBitmapOrNull() }
    Row(
        modifier
            .fillMaxWidth()
            .alpha(if (row.enabled) 1f else 0.4f)
            .clip(shape)
            .background(if (focused) Gx.SurfaceHi else Gx.Surface)
            .border(if (focused) 3.dp else 1.dp, if (focused) Gx.Accent else Gx.Outline, shape)
            .onFocusChanged { rawFocus = it.isFocused }
            .then(if (row.enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Gx.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (row.summary.isNotEmpty()) {
                Text(row.summary, fontSize = 13.sp, color = Gx.TextDim, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
        }
        if (row.kind == RowKind.TOGGLE) {
            Switch(checked = row.checked, onCheckedChange = null)
        } else if (row.enabled) {
            Text("›", fontSize = 24.sp, color = Gx.TextDim)
        }
    }
}
