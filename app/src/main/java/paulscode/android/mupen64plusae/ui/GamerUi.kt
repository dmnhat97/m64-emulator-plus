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

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy

/** Entry point used by the Java host activity. */
object GamerUi {
    @JvmStatic
    fun attach(view: ComposeView, state: HomeState, actions: HomeActions) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent { GamerApp(state, actions) }
    }
}
