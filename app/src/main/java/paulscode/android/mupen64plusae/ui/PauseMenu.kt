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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import paulscode.android.mupen64plusae.R

/** One row of the legacy in-game menu (title is kept live by the host, for example "Speed: 100%"). */
class PauseEntry(val id: Int, val title: String, val children: List<PauseEntry>)

interface PauseActions {
    fun onEntry(id: Int)
    fun onClose()
}

class PauseState {
    var title by mutableStateOf("")
    var coverPath by mutableStateOf<String?>(null)
    var entries by mutableStateOf<List<PauseEntry>>(emptyList())
    var inputMode by mutableStateOf(InputMode.TOUCH)

    /** Incremented every time the menu is opened, so the controller focus can be reset. */
    var openCount by mutableIntStateOf(0)

    /** Called by the host when the menu opens. */
    fun onOpened(controllerConnected: Boolean) {
        inputMode = if (controllerConnected) InputMode.GAMEPAD else InputMode.TOUCH
        openCount++
    }

    fun find(id: Int): PauseEntry? {
        fun search(list: List<PauseEntry>): PauseEntry? {
            for (entry in list) {
                if (entry.id == id) return entry
                search(entry.children)?.let { return it }
            }
            return null
        }
        return search(entries)
    }
}

object PauseUi {
    @JvmStatic
    fun attach(view: ComposeView, state: PauseState, actions: PauseActions) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent { PauseMenuApp(state, actions) }
    }
}

private val playIds = listOf(
    R.id.menuItem_toggle_speed, R.id.menuItem_set_speed, R.id.menuItem_set_slot,
    R.id.menuItem_disable_frame_limiter, R.id.menuItem_reset
)
private val fileIds = listOf(R.id.menuItem_file_save, R.id.menuItem_file_load, R.id.menuItem_file_load_auto_save)
private val quickIds = setOf(R.id.menuItem_slot_save, R.id.menuItem_slot_load, R.id.menuItem_screenshot)
private val knownIds = playIds + fileIds + quickIds + listOf(
    R.id.menuItem_paks, R.id.menuItem_exit, R.id.menuItem_achievements, R.id.menuItem_setIme
)

@Composable
fun PauseMenuApp(state: PauseState, actions: PauseActions) {
    GamerTheme {
        val gamepadUi = state.inputMode == InputMode.GAMEPAD
        val resumeFocus = remember { FocusRequester() }
        LaunchedEffect(state.openCount, gamepadUi) {
            if (gamepadUi) {
                delay(150)
                runCatching { resumeFocus.requestFocus() }
            }
        }
        CompositionLocalProvider(LocalGamepadUi provides gamepadUi) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Gx.Surface)
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
                            key == Key.ButtonA || key == Key.ButtonB || key == Key.ButtonX || key == Key.ButtonY
                        if (event.type == KeyEventType.KeyDown && controller) state.inputMode = InputMode.GAMEPAD
                        if (key == Key.ButtonB) {
                            if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) actions.onClose()
                            true
                        } else false
                    }
            ) {
                PauseHeader(state)
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActions(state, actions, resumeFocus)
                    val play = playIds.mapNotNull { state.find(it) }
                    if (play.isNotEmpty()) {
                        SectionLabel(stringResource(R.string.gx_pause_game))
                        play.forEach { ActionRow(it.title, null) { actions.onEntry(it.id) } }
                    }
                    val paks = state.find(R.id.menuItem_paks)
                    if (paks != null && paks.children.isNotEmpty()) {
                        SectionLabel(paks.title)
                        paks.children.forEach { ActionRow(it.title, null) { actions.onEntry(it.id) } }
                    }
                    val files = fileIds.mapNotNull { state.find(it) }
                    if (files.isNotEmpty()) {
                        SectionLabel(stringResource(R.string.gx_pause_files))
                        files.forEach { ActionRow(it.title, null) { actions.onEntry(it.id) } }
                    }
                    val more = listOfNotNull(state.find(R.id.menuItem_achievements), state.find(R.id.menuItem_setIme)) +
                        state.entries.filter { it.id !in knownIds && it.children.isEmpty() }
                    if (more.isNotEmpty()) {
                        SectionLabel(stringResource(R.string.gx_pause_more))
                        more.forEach { ActionRow(it.title, null) { actions.onEntry(it.id) } }
                    }
                    state.find(R.id.menuItem_exit)?.let { exit ->
                        Spacer(Modifier.height(8.dp))
                        ActionRow(exit.title, null, danger = true) { actions.onEntry(exit.id) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                if (gamepadUi) {
                    Text(
                        stringResource(R.string.gx_pause_hint),
                        fontSize = 10.sp, color = Gx.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().background(Gx.Bg).padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PauseHeader(state: PauseState) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Gx.SurfaceHi, Gx.Surface)))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverImage(state.coverPath, Modifier.width(84.dp).height(62.dp).clip(RoundedCornerShape(10.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.gx_pause_title), fontSize = 11.sp, color = Gx.Accent, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(state.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Gx.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Gx.TextDim, letterSpacing = 1.sp, modifier = Modifier.padding(top = 10.dp, start = 2.dp))
}

@Composable
private fun QuickActions(state: PauseState, actions: PauseActions, resumeFocus: FocusRequester) {
    val save = state.find(R.id.menuItem_slot_save)
    val load = state.find(R.id.menuItem_slot_load)
    val shot = state.find(R.id.menuItem_screenshot)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickTile("▶", stringResource(R.string.gx_resume), Modifier.weight(1f).focusRequester(resumeFocus), primary = true) { actions.onClose() }
            if (shot != null) QuickTile("◎", shot.title, Modifier.weight(1f)) { actions.onEntry(shot.id) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (save != null) QuickTile("⇩", save.title, Modifier.weight(1f)) { actions.onEntry(save.id) }
            if (load != null) QuickTile("⇧", load.title, Modifier.weight(1f)) { actions.onEntry(load.id) }
        }
    }
}

@Composable
private fun QuickTile(glyph: String, label: String, modifier: Modifier = Modifier, primary: Boolean = false, onClick: () -> Unit) {
    var rawFocus by remember { mutableStateOf(false) }
    val focused = rawFocus && LocalGamepadUi.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .height(64.dp)
            .clip(shape)
            .background(if (primary) Brush.horizontalGradient(listOf(Gx.Accent, Gx.Accent2)) else Brush.horizontalGradient(listOf(Gx.SurfaceHi, Gx.SurfaceHi)))
            .border(if (focused) 3.dp else 1.dp, if (focused) androidx.compose.ui.graphics.Color.White else Gx.Outline, shape)
            .onFocusChanged { rawFocus = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(glyph, fontSize = 20.sp, color = androidx.compose.ui.graphics.Color.White, lineHeight = 22.sp)
        Text(label, fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
