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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import paulscode.android.mupen64plusae.GalleryItem
import paulscode.android.mupen64plusae.R
import paulscode.android.mupen64plusae.util.PlayStats

private val CardShape = RoundedCornerShape(14.dp)

/** True while the player drives the UI with a controller / D-pad. */
internal val LocalGamepadUi = compositionLocalOf { false }

// ---------------------------------------------------------------------------------------------
// Root
// ---------------------------------------------------------------------------------------------

@Composable
fun GamerApp(state: HomeState, actions: HomeActions) {
    GamerTheme {
        val gamepadUi = state.inputMode == InputMode.GAMEPAD
        BackHandler(enabled = state.details == null && state.tab != HomeTab.HOME) {
            state.tab = HomeTab.HOME
        }
        CompositionLocalProvider(LocalGamepadUi provides gamepadUi) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Gx.Bg)
                    .pointerInput(Unit) {
                        // A finger on the screen switches back to the clean touch UI
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Press) state.inputMode = InputMode.TOUCH
                            }
                        }
                    }
                    .onPreviewKeyEvent { handleGlobalKey(it, state, actions) }
            ) {
                // The host activity already applies the top / right system bar margins
                BoxWithConstraints(
                    Modifier.fillMaxSize().windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Left + WindowInsetsSides.Bottom)
                            .union(WindowInsets.displayCutout.only(WindowInsetsSides.Right))
                    )
                ) {
                    val landscape = maxWidth >= maxHeight
                    val content: @Composable (Modifier) -> Unit = { modifier ->
                        Box(modifier) {
                            when (state.tab) {
                                HomeTab.HOME -> HomeContent(state, actions, landscape)
                                HomeTab.LIBRARY -> LibraryContent(state, actions, landscape)
                                HomeTab.SETTINGS -> SettingsContent(actions, landscape)
                            }
                        }
                    }
                    if (landscape) {
                        Column(Modifier.fillMaxSize()) {
                            Row(Modifier.weight(1f).fillMaxWidth()) {
                                NavRail(state, actions)
                                content(Modifier.weight(1f).fillMaxHeight())
                            }
                            if (gamepadUi) HintBar()
                        }
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            content(Modifier.weight(1f).fillMaxWidth())
                            if (gamepadUi) HintBar()
                            BottomNav(state, actions)
                        }
                    }
                }
            }
            state.details?.let { DetailsDialog(it, state, actions) }
        }
    }
}

private fun isControllerKey(key: Key): Boolean = key == Key.DirectionUp || key == Key.DirectionDown ||
    key == Key.DirectionLeft || key == Key.DirectionRight || key == Key.DirectionCenter ||
    key == Key.ButtonA || key == Key.ButtonB || key == Key.ButtonX || key == Key.ButtonY ||
    key == Key.ButtonL1 || key == Key.ButtonR1 || key == Key.ButtonStart || key == Key.ButtonSelect

/** Controller shortcuts that work everywhere: L1/R1 tabs, B back, Y favorite, X details. */
private fun handleGlobalKey(event: KeyEvent, state: HomeState, actions: HomeActions): Boolean {
    val key = event.key
    if (event.type == KeyEventType.KeyDown && isControllerKey(key)) state.inputMode = InputMode.GAMEPAD
    if (key != Key.ButtonL1 && key != Key.ButtonR1 && key != Key.ButtonB &&
        key != Key.ButtonY && key != Key.ButtonX) return false
    if (key == Key.ButtonB && state.tab == HomeTab.HOME) return false
    if (event.type != KeyEventType.KeyDown) return true
    if (event.nativeKeyEvent.repeatCount > 0) return true
    val tabs = HomeTab.entries
    when (key) {
        Key.ButtonL1 -> state.tab = tabs[(state.tab.ordinal + tabs.size - 1) % tabs.size]
        Key.ButtonR1 -> state.tab = tabs[(state.tab.ordinal + 1) % tabs.size]
        Key.ButtonB -> state.tab = HomeTab.HOME
        Key.ButtonY -> state.focused?.let { actions.onToggleFavorite(it) }
        Key.ButtonX -> state.focused?.let { state.details = it }
    }
    return true
}

// ---------------------------------------------------------------------------------------------
// Navigation rail + hint bar
// ---------------------------------------------------------------------------------------------

@Composable
private fun NavRail(state: HomeState, actions: HomeActions) {
    Column(
        Modifier.fillMaxHeight().width(80.dp).background(Gx.Surface).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("XM64", color = Gx.Accent, fontWeight = FontWeight.Black, fontSize = 15.sp, lineHeight = 18.sp)
        RailButton("⌂", stringResource(R.string.gx_nav_home), state.tab == HomeTab.HOME) { state.tab = HomeTab.HOME }
        RailButton("▦", stringResource(R.string.gx_nav_library), state.tab == HomeTab.LIBRARY) { state.tab = HomeTab.LIBRARY }
        RailButton("＋", stringResource(R.string.gx_nav_add), false) { actions.onAddGames() }
        Spacer(Modifier.weight(1f))
        RailButton("⚙", stringResource(R.string.gx_nav_settings), state.tab == HomeTab.SETTINGS) { state.tab = HomeTab.SETTINGS }
    }
}

@Composable
private fun BottomNav(state: HomeState, actions: HomeActions) {
    Row(
        Modifier.fillMaxWidth().background(Gx.Surface).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        RailButton("\u2302", stringResource(R.string.gx_nav_home), state.tab == HomeTab.HOME) { state.tab = HomeTab.HOME }
        RailButton("\u25A6", stringResource(R.string.gx_nav_library), state.tab == HomeTab.LIBRARY) { state.tab = HomeTab.LIBRARY }
        RailButton("\uFF0B", stringResource(R.string.gx_nav_add), false) { actions.onAddGames() }
        RailButton("\u2699", stringResource(R.string.gx_nav_settings), state.tab == HomeTab.SETTINGS) { state.tab = HomeTab.SETTINGS }
    }
}

@Composable
private fun RailButton(glyph: String, label: String, selected: Boolean, onClick: () -> Unit) {
    var rawFocus by remember { mutableStateOf(false) }
    val focused = rawFocus && LocalGamepadUi.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .width(68.dp)
            .clip(shape)
            .background(if (selected) Gx.SurfaceHi else Color.Transparent)
            .then(if (focused) Modifier.border(3.dp, Gx.Accent, shape) else Modifier)
            .onFocusChanged { rawFocus = it.isFocused }
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(glyph, fontSize = 20.sp, lineHeight = 24.sp, color = if (selected) Gx.Accent else if (focused) Gx.Text else Gx.TextDim)
        Text(label, fontSize = 10.sp, color = if (selected) Gx.Accent else if (focused) Gx.Text else Gx.TextDim, maxLines = 1, lineHeight = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun HintBar() {
    Row(
        Modifier.fillMaxWidth().background(Gx.Surface).padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Text(stringResource(R.string.gx_hint_bar), fontSize = 10.sp, color = Gx.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------------------------------------
// Home
// ---------------------------------------------------------------------------------------------

@Composable
private fun HomeContent(state: HomeState, actions: HomeActions, landscape: Boolean) {
    val heroFocus = remember { FocusRequester() }
    val hero = state.recents.firstOrNull() ?: state.items.firstOrNull()
    val gamepadUi = LocalGamepadUi.current
    LaunchedEffect(hero?.md5, state.tab, gamepadUi) {
        if (gamepadUi) {
            delay(120)
            runCatching { heroFocus.requestFocus() }
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp, 12.dp, 24.dp, 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { HomeHeader(state.items.size) }
        if (hero == null) {
            item { EmptyState(actions, heroFocus) }
        } else {
            item { Hero(hero, state, actions, heroFocus, landscape) }
            val recent = state.recents.filter { it.md5 != hero.md5 }
            if (recent.isNotEmpty()) item { GameRow(stringResource(R.string.gx_section_recent), recent, state, actions) }
            val favorites = state.items.filter { it.favorite }
            if (favorites.isNotEmpty()) item { GameRow(stringResource(R.string.gx_section_favorites), favorites, state, actions) }
            item { GameRow(stringResource(R.string.gx_section_all), state.items, state, actions) }
        }
    }
}

@Composable
private fun HomeHeader(count: Int) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            stringResource(R.string.app_name), fontSize = 26.sp, fontWeight = FontWeight.Black, color = Gx.Text,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
        )
        Text(
            if (count == 1) stringResource(R.string.gx_games_count_one) else stringResource(R.string.gx_games_count, count),
            fontSize = 14.sp, color = Gx.TextDim, maxLines = 1, softWrap = false,
            modifier = Modifier.padding(bottom = 3.dp)
        )
    }
}

@Composable
private fun statsText(item: GalleryItem): String {
    val context = LocalContext.current
    val parts = mutableListOf<String>()
    if (item.favorite) parts += context.getString(R.string.stat_favorite)
    parts += if (item.lastPlayed > 0) {
        context.getString(R.string.stat_lastPlayed, PlayStats.formatLastPlayed(context, item.lastPlayed.toLong()))
    } else {
        context.getString(R.string.stat_neverPlayed)
    }
    if (item.playSeconds > 0) {
        parts += context.getString(R.string.stat_timePlayed, PlayStats.formatDuration(context, item.playSeconds))
    }
    return parts.joinToString("   •   ")
}

@Composable
private fun Hero(item: GalleryItem, state: HomeState, actions: HomeActions, focus: FocusRequester, landscape: Boolean) {
    val body: @Composable () -> Unit = { HeroBody(item, state, actions, focus) }
    if (landscape) {
        HeroRow(item, body)
    } else {
        HeroColumn(item, body)
    }
}

@Composable
private fun HeroRow(item: GalleryItem, body: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(Gx.SurfaceHi, Gx.Surface)))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        CoverImage(
            item.artPath,
            Modifier.height(140.dp).aspectRatio(1.35f).clip(RoundedCornerShape(16.dp))
        )
        Box(Modifier.weight(1f).align(Alignment.CenterVertically)) { body() }
    }
}

@Composable
private fun HeroBody(item: GalleryItem, state: HomeState, actions: HomeActions, focus: FocusRequester) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.gx_continue), fontSize = 12.sp, color = Gx.Accent, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(item.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gx.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(statsText(item), fontSize = 12.sp, color = Gx.TextDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GxButton(stringResource(R.string.gx_resume), primary = true, modifier = Modifier.focusRequester(focus)) {
                    actions.onPlay(item, PlayMode.RESUME)
                }
                GxButton(stringResource(R.string.gx_details)) { state.details = item }
            }
        }
}

@Composable
private fun HeroColumn(item: GalleryItem, body: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(Gx.SurfaceHi, Gx.Surface)))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CoverImage(item.artPath, Modifier.fillMaxWidth().aspectRatio(1.9f).clip(RoundedCornerShape(16.dp)))
        body()
    }
}

@Composable
private fun EmptyState(actions: HomeActions, focus: FocusRequester) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Gx.Surface).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(R.string.gx_empty_title), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Gx.Text)
        Text(stringResource(R.string.gx_empty_body), fontSize = 14.sp, color = Gx.TextDim)
        Spacer(Modifier.height(6.dp))
        GxButton(stringResource(R.string.gx_empty_cta), primary = true, modifier = Modifier.focusRequester(focus)) { actions.onAddGames() }
    }
}

@Composable
private fun GameRow(title: String, list: List<GalleryItem>, state: HomeState, actions: HomeActions) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gx.Text)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)) {
            items(list, key = { it.md5 }) { item ->
                GameCard(item, state, onLongClick = { actions.onToggleFavorite(item) }) { state.details = item }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Game card, buttons, chips
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameCard(item: GalleryItem, state: HomeState, modifier: Modifier = Modifier, onLongClick: () -> Unit = {}, onClick: () -> Unit) {
    var rawFocus by remember { mutableStateOf(false) }
    val focused = rawFocus && LocalGamepadUi.current
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, label = "cardScale")
    Column(
        modifier
            .width(172.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged {
                rawFocus = it.isFocused
                if (it.isFocused) state.focused = item
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.35f)
                .clip(CardShape)
                .border(if (focused) 3.dp else 1.dp, if (focused) Gx.Accent else Gx.Outline, CardShape)
        ) {
            CoverImage(item.artPath, Modifier.fillMaxSize())
            if (item.favorite) {
                Text(
                    "★",
                    color = Gx.Gold,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                        .background(Color(0x99000000), RoundedCornerShape(10.dp)).padding(horizontal = 6.dp)
                )
            }
        }
        Text(
            item.toString(),
            fontSize = 13.sp,
            color = if (focused) Gx.Text else Gx.TextDim,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp)
        )
    }
}

@Composable
internal fun GxButton(text: String, modifier: Modifier = Modifier, primary: Boolean = false, onClick: () -> Unit) {
    var rawFocus by remember { mutableStateOf(false) }
    val focused = rawFocus && LocalGamepadUi.current
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(if (primary) Brush.horizontalGradient(listOf(Gx.Accent, Gx.Accent2)) else SolidColor(Gx.SurfaceHi))
            .border(if (focused) 3.dp else 1.dp, if (focused) Color.White else Gx.Outline, shape)
            .onFocusChanged { rawFocus = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
private fun GxChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var rawFocus by remember { mutableStateOf(false) }
    val focused = rawFocus && LocalGamepadUi.current
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .height(40.dp)
            .clip(shape)
            .background(if (selected) Gx.SurfaceHi else Color.Transparent)
            .border(if (focused) 3.dp else 1.dp, if (focused) Gx.Accent else if (selected) Gx.Accent else Gx.Outline, shape)
            .onFocusChanged { rawFocus = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Gx.Text else Gx.TextDim, fontSize = 14.sp)
    }
}

// ---------------------------------------------------------------------------------------------
// Library
// ---------------------------------------------------------------------------------------------

@Composable
private fun LibraryContent(state: HomeState, actions: HomeActions, landscape: Boolean) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(state.tab) {
        delay(120)
        runCatching { firstFocus.requestFocus() }
    }
    val list = if (state.favoritesOnly) state.items.filter { it.favorite } else state.items
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val searchField: @Composable (Modifier) -> Unit = { modifier ->
            SearchField(state.query, modifier) {
                state.query = it
                actions.onSearch(it)
            }
        }
        val allChip: @Composable () -> Unit = {
            GxChip(stringResource(R.string.gallery_filter_all), !state.favoritesOnly, Modifier.focusRequester(firstFocus)) { state.favoritesOnly = false }
        }
        val favChip: @Composable () -> Unit = {
            GxChip(stringResource(R.string.gallery_filter_favorites), state.favoritesOnly) { state.favoritesOnly = true }
        }
        if (landscape) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                searchField(Modifier.weight(1f))
                allChip()
                favChip()
            }
        } else {
            searchField(Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                allChip()
                favChip()
            }
        }
        if (list.isEmpty()) {
            Text(
                stringResource(if (state.favoritesOnly) R.string.gallery_favorites_empty else R.string.gx_no_results),
                color = Gx.TextDim, fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(172.dp),
                contentPadding = PaddingValues(6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(list, key = { it.md5 }) { item ->
                    GameCard(item, state, onLongClick = { actions.onToggleFavorite(item) }) { state.details = item }
                }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    var rawFocus by remember { mutableStateOf(false) }
    val focused = rawFocus
    val shape = RoundedCornerShape(20.dp)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = Gx.Text, fontSize = 15.sp),
        cursorBrush = SolidColor(Gx.Accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(Gx.Surface)
            .border(if (focused) 3.dp else 1.dp, if (focused) Gx.Accent else Gx.Outline, shape)
            .onFocusChanged { rawFocus = it.isFocused },
        decorationBox = { inner ->
            Box(Modifier.padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(stringResource(R.string.gx_search_hint), color = Gx.TextDim, fontSize = 15.sp, maxLines = 1, softWrap = false)
                }
                inner()
            }
        }
    )
}

// ---------------------------------------------------------------------------------------------
// Game details (side sheet)
// ---------------------------------------------------------------------------------------------

@Composable
private fun DetailsDialog(item: GalleryItem, state: HomeState, actions: HomeActions) {
    val close = { state.details = null }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(item.md5) {
        delay(120)
        runCatching { firstFocus.requestFocus() }
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Gx.Scrim)
                .pointerInput(Unit) { detectTapGestures { close() } }
                .onPreviewKeyEvent {
                    if (it.key == Key.ButtonB) {
                        if (it.type == KeyEventType.KeyDown) close()
                        true
                    } else false
                }
        ) {
            Column(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(390.dp)
                    .background(Gx.Surface)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoverImage(item.artPath, Modifier.fillMaxWidth().aspectRatio(2.2f).clip(RoundedCornerShape(16.dp)))
                Text(item.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Gx.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(statsText(item), fontSize = 12.sp, color = Gx.TextDim)
                Spacer(Modifier.height(4.dp))
                ActionRow(stringResource(R.string.gx_resume), stringResource(R.string.gx_hint_resume), Modifier.focusRequester(firstFocus)) {
                    close(); actions.onPlay(item, PlayMode.RESUME)
                }
                ActionRow(stringResource(R.string.gx_start_over), stringResource(R.string.gx_hint_start)) {
                    close(); actions.onPlay(item, PlayMode.START)
                }
                ActionRow(stringResource(R.string.gx_action_join), null) { close(); actions.onPlay(item, PlayMode.NETPLAY_JOIN) }
                ActionRow(stringResource(R.string.gx_action_host), null) { close(); actions.onPlay(item, PlayMode.NETPLAY_HOST) }
                ActionRow(
                    stringResource(if (item.favorite) R.string.menuItem_unfavorite else R.string.menuItem_favorite), null
                ) { actions.onToggleFavorite(item) }
                ActionRow(stringResource(R.string.gx_action_settings), null) { close(); actions.onGameSettings(item) }
                ActionRow(stringResource(R.string.gx_action_shortcut), null) { close(); actions.onShortcut(item) }
                ActionRow(stringResource(R.string.gx_action_remove), null, danger = true) { close(); actions.onRemove(item) }
            }
        }
    }
}

@Composable
internal fun ActionRow(label: String, hint: String?, modifier: Modifier = Modifier, danger: Boolean = false, onClick: () -> Unit) {
    var rawFocus by remember { mutableStateOf(false) }
    val focused = rawFocus && LocalGamepadUi.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (focused) Gx.SurfaceHi else Color.Transparent)
            .border(if (focused) 3.dp else 1.dp, if (focused) Gx.Accent else Gx.Outline, shape)
            .onFocusChanged { rawFocus = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (danger) Gx.Danger else Gx.Text)
        if (hint != null) Text(hint, fontSize = 12.sp, color = Gx.TextDim)
    }
}

// ---------------------------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------------------------

private class SettingRow(val glyph: String, val label: Int, val rowId: Int)
private class SettingGroup(val title: Int, val rows: List<SettingRow>)

private val settingGroups = listOf(
    SettingGroup(R.string.menuItem_settings, listOf(
        SettingRow("▤", R.string.categoryLibrary_title, R.id.rowLibrary),
        SettingRow("▭", R.string.categoryDisplay_title, R.id.rowDisplay),
        SettingRow("✦", R.string.categoryShaders_title, R.id.rowShaders),
        SettingRow("♪", R.string.categoryAudio_title, R.id.rowAudio),
        SettingRow("◈", R.string.categoryInput_title, R.id.rowInput),
        SettingRow("☝", R.string.categoryTouchscreen_title, R.id.rowTouchscreen),
        SettingRow("▣", R.string.categoryData_title, R.id.rowData),
        SettingRow("★", R.string.categoryRetroAchievements_title, R.id.rowRetroAchievements)
    )),
    SettingGroup(R.string.menuItem_profiles, listOf(
        SettingRow("⚙", R.string.menuItem_emulationProfiles, R.id.rowEmuProfiles),
        SettingRow("☝", R.string.menuItem_touchscreenProfiles, R.id.rowTouchProfiles),
        SettingRow("◈", R.string.menuItem_controllerProfiles, R.id.rowControllerProfiles)
    )),
    SettingGroup(R.string.menuItem_Tools, listOf(
        SettingRow("⇅", R.string.importExportActivity_title, R.id.rowImportExport),
        SettingRow("◎", R.string.menuItem_localeOverride, R.id.rowLanguage),
        SettingRow("❓", R.string.menuItem_reportBug, R.id.rowReportBug),
        SettingRow("ⓘ", R.string.menuItem_licenses, R.id.rowLicenses)
    ))
)

@Composable
private fun SettingsContent(actions: HomeActions, landscape: Boolean) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { firstFocus.requestFocus() }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (landscape) 2 else 1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(28.dp, 18.dp, 28.dp, 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        settingGroups.forEachIndexed { groupIndex, group ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(group.title),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Gx.Text,
                    modifier = Modifier.padding(top = if (groupIndex == 0) 0.dp else 10.dp)
                )
            }
            group.rows.forEachIndexed { rowIndex, row ->
                item {
                    val modifier = if (groupIndex == 0 && rowIndex == 0) Modifier.focusRequester(firstFocus) else Modifier
                    SettingTile(row, modifier) { actions.onSettingsRow(row.rowId) }
                }
            }
        }
    }
}

@Composable
private fun SettingTile(row: SettingRow, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var rawFocus by remember { mutableStateOf(false) }
    val focused = rawFocus && LocalGamepadUi.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .background(if (focused) Gx.SurfaceHi else Gx.Surface)
            .border(if (focused) 3.dp else 1.dp, if (focused) Gx.Accent else Gx.Outline, shape)
            .onFocusChanged { rawFocus = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(row.glyph, fontSize = 22.sp, color = if (focused) Gx.Accent else Gx.TextDim)
        Text(stringResource(row.label), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Gx.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
