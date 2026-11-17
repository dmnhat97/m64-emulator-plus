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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

fun interface IntCallback { fun accept(value: Int) }
fun interface StringCallback { fun accept(value: String) }
fun interface Action { fun run() }
fun interface IntFormatter { fun format(value: Int): String }

/** Describes a dialog the host (Java) wants to show; rendered by [GxDialogHost]. */
sealed class GxDialogSpec {
    class Choice(
        val title: String, val entries: List<String>, val selected: Int,
        val neutralLabel: String?, val onNeutral: Action?, val onSelect: IntCallback
    ) : GxDialogSpec()

    class Slider(
        val title: String, val min: Int, val max: Int, val step: Int, val value: Int,
        val formatter: IntFormatter, val onConfirm: IntCallback
    ) : GxDialogSpec()

    class Text(val title: String, val value: String, val onConfirm: StringCallback) : GxDialogSpec()

    class Confirm(val title: String, val message: String, val onConfirm: Action, val onCancel: Action) : GxDialogSpec()
}

/** Shows [spec] in its own window; use inside a composition that already has [GamerTheme]. */
@Composable
fun GxDialogHost(spec: GxDialogSpec?, onDismiss: () -> Unit) {
    if (spec == null) return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        GxDialogScaffold(onDismiss) { GxDialogBody(spec, onDismiss) }
    }
}

/** Scrim + centered rounded card. Also used directly by DialogFragments that own their window. */
@Composable
fun GxDialogScaffold(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Gx.Scrim)
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .onPreviewKeyEvent { event ->
                if (event.key == Key.ButtonB) {
                    if (event.type == KeyEventType.KeyDown) onDismiss()
                    true
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.92f)
                .heightIn(max = maxHeight * 0.92f)
                .clip(shape)
                .background(Gx.Surface)
                .border(1.dp, Gx.Outline, shape)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun ColumnScope.GxDialogBody(spec: GxDialogSpec, onDismiss: () -> Unit) {
    when (spec) {
        is GxDialogSpec.Choice -> ChoiceBody(spec, onDismiss)
        is GxDialogSpec.Slider -> SliderBody(spec, onDismiss)
        is GxDialogSpec.Text -> TextBody(spec, onDismiss)
        is GxDialogSpec.Confirm -> ConfirmBody(spec.title, spec.message, onDismiss, { spec.onConfirm.run() }, { spec.onCancel.run() })
    }
}

@Composable
private fun DialogTitle(text: String) {
    Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Gx.Text, maxLines = 2)
}

@Composable
private fun ColumnScope.ChoiceBody(spec: GxDialogSpec.Choice, onDismiss: () -> Unit) {
    val listState = rememberLazyListState()
    val selectedFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (spec.selected > 0) listState.scrollToItem(spec.selected)
        repeat(10) {
            delay(120)
            if (runCatching { selectedFocus.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
        }
    }
    DialogTitle(spec.title)
    LazyColumn(Modifier.weight(1f, fill = false), state = listState, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        itemsIndexed(spec.entries) { index, entry ->
            val isSelected = index == spec.selected
            val first = if (spec.selected in spec.entries.indices) isSelected else index == 0
            ChoiceRow(entry, isSelected, if (first) Modifier.focusRequester(selectedFocus) else Modifier) {
                spec.onSelect.accept(index)
                onDismiss()
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (spec.neutralLabel != null) {
            GxButton(spec.neutralLabel) { spec.onNeutral?.run(); onDismiss() }
        }
        Spacer(Modifier.weight(1f))
        GxButton(stringResource(android.R.string.cancel)) { onDismiss() }
    }
}

@Composable
private fun ChoiceRow(entry: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    var rawFocus by remember { mutableStateOf(false) }
    val focused = rawFocus && LocalGamepadUi.current
    val shape = RoundedCornerShape(14.dp)
    val lines = entry.split('\n')
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (focused || selected) Gx.SurfaceHi else Color.Transparent)
            .border(if (focused) 3.dp else 1.dp, if (focused) Gx.Accent else if (selected) Gx.Accent else Gx.Outline, shape)
            .onFocusChanged { rawFocus = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).border(2.dp, if (selected) Gx.Accent else Gx.TextDim, CircleShape).padding(4.dp)
        ) {
            if (selected) Box(Modifier.fillMaxSize().clip(CircleShape).background(Gx.Accent))
        }
        Column(Modifier.weight(1f)) {
            Text(lines.first().trim(), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Gx.Text)
            lines.drop(1).joinToString(" ").trim().takeIf { it.isNotEmpty() }?.let {
                Text(it, fontSize = 12.sp, color = Gx.TextDim, maxLines = 3)
            }
        }
    }
}

@Composable
private fun ColumnScope.SliderBody(spec: GxDialogSpec.Slider, onDismiss: () -> Unit) {
    val gamepadUi = LocalGamepadUi.current
    var value by remember { mutableFloatStateOf(spec.value.toFloat()) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (gamepadUi) {
            delay(150)
            runCatching { focus.requestFocus() }
        }
    }
    val step = spec.step.coerceAtLeast(1)
    DialogTitle(spec.title)
    Text(
        spec.formatter.format(value.roundToInt()),
        fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Gx.Accent, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Slider(
        value = value,
        onValueChange = { value = it },
        valueRange = spec.min.toFloat()..spec.max.toFloat(),
        steps = ((spec.max - spec.min) / step - 1).coerceAtLeast(0),
        colors = SliderDefaults.colors(thumbColor = Gx.Accent, activeTrackColor = Gx.Accent, inactiveTrackColor = Gx.Outline),
        modifier = Modifier.fillMaxWidth().focusRequester(focus)
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
        GxButton(stringResource(android.R.string.cancel)) { onDismiss() }
        GxButton(stringResource(android.R.string.ok), primary = true) {
            spec.onConfirm.accept(value.roundToInt())
            onDismiss()
        }
    }
}

@Composable
private fun ColumnScope.TextBody(spec: GxDialogSpec.Text, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(spec.value) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { focus.requestFocus() }
    }
    val confirm = {
        spec.onConfirm.accept(text)
        onDismiss()
    }
    val shape = RoundedCornerShape(16.dp)
    DialogTitle(spec.title)
    BasicTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        textStyle = TextStyle(color = Gx.Text, fontSize = 16.sp),
        cursorBrush = SolidColor(Gx.Accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { confirm() }),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Gx.Bg)
            .border(1.dp, Gx.Outline, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .focusRequester(focus)
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
        GxButton(stringResource(android.R.string.cancel)) { onDismiss() }
        GxButton(stringResource(android.R.string.ok), primary = true) { confirm() }
    }
}

@Composable
internal fun ColumnScope.ConfirmBody(title: String, message: String, onDismiss: () -> Unit, onConfirm: () -> Unit, onCancel: () -> Unit) {
    val gamepadUi = LocalGamepadUi.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (gamepadUi) {
            delay(150)
            runCatching { focus.requestFocus() }
        }
    }
    DialogTitle(title)
    if (message.isNotEmpty()) Text(message, fontSize = 15.sp, color = Gx.TextDim)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
        GxButton(stringResource(android.R.string.cancel)) { onCancel(); onDismiss() }
        GxButton(stringResource(android.R.string.ok), primary = true, modifier = Modifier.focusRequester(focus)) { onConfirm(); onDismiss() }
    }
}

/** For DialogFragments that own their window: draws the scrim and card around a confirmation. */
object ConfirmUi {
    @JvmStatic
    fun attach(view: ComposeView, title: String, message: String, onOk: Runnable, onCancel: Runnable) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            GamerTheme {
                androidx.compose.runtime.CompositionLocalProvider(LocalGamepadUi provides ControllerUtil.isControllerConnected()) {
                    GxDialogScaffold(onDismiss = { onCancel.run() }) {
                        ConfirmBody(title, message, onDismiss = {}, onConfirm = { onOk.run() }, onCancel = { onCancel.run() })
                    }
                }
            }
        }
    }
}
