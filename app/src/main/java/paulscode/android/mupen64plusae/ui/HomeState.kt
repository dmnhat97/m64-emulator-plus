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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import paulscode.android.mupen64plusae.GalleryItem

enum class HomeTab { HOME, LIBRARY, SETTINGS }

/** How the player is currently controlling the UI. */
enum class InputMode { TOUCH, GAMEPAD }

/** How a game is started from the UI. */
object PlayMode {
    const val RESUME = 0
    const val START = 1
    const val NETPLAY_JOIN = 2
    const val NETPLAY_HOST = 3
}

/** Everything the Compose UI can ask the host activity to do. */
interface HomeActions {
    fun onSearch(query: String)
    fun onPlay(item: GalleryItem, mode: Int)
    fun onToggleFavorite(item: GalleryItem)
    fun onGameSettings(item: GalleryItem)
    fun onRemove(item: GalleryItem)
    fun onShortcut(item: GalleryItem)
    fun onAddGames()
    fun onSettingsRow(rowId: Int)
}

/** UI state; the (Java) host activity pushes library data in through [update]. */
class HomeState {
    var items by mutableStateOf<List<GalleryItem>>(emptyList())
    var recents by mutableStateOf<List<GalleryItem>>(emptyList())
    var query by mutableStateOf("")
    var tab by mutableStateOf(HomeTab.HOME)
    var favoritesOnly by mutableStateOf(false)
    var details by mutableStateOf<GalleryItem?>(null)

    /** GAMEPAD shows focus rings, button hints and auto-focus; TOUCH keeps the UI clean for fingers. */
    var inputMode by mutableStateOf(InputMode.TOUCH)

    /** Called by the host when a game controller is connected or removed. */
    fun setGamepadConnected(connected: Boolean) {
        inputMode = if (connected) InputMode.GAMEPAD else InputMode.TOUCH
    }

    /** The game card that currently has controller focus (target of the Y / X shortcuts). */
    var focused: GalleryItem? = null

    fun update(newItems: List<GalleryItem>, newRecents: List<GalleryItem>) {
        items = newItems.filter { !it.isHeading }
        recents = newRecents.filter { !it.isHeading }
        // Keep the open details panel in sync (for example after toggling a favorite)
        details = details?.let { open -> items.firstOrNull { it.md5 == open.md5 } ?: open }
    }
}
