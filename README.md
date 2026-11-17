# XM64 Emulator Plus

XM64 Emulator Plus is an Android Nintendo 64 emulator. It is a **modified fork of
[Mupen64Plus AE](https://github.com/mupen64plus-ae/mupen64plus-ae)** (Android Edition) by Paul Lamb
and contributors, and is **not** the official Mupen64Plus AE nor endorsed by its authors.

## License

This program is free software: you can redistribute it and/or modify it under the terms of the
GNU General Public License as published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version. It comes with ABSOLUTELY NO WARRANTY.

- Full license text: [LICENSE](LICENSE)
- Fork attribution, summary of modifications and third-party licenses: [NOTICE](NOTICE)
- In the app: drawer menu → About → Open source licenses

Source code for each released version is available in this repository (see the release tags).
No ROMs or BIOS images are included; you must supply your own legally obtained game files.

## What is different from the original Mupen64Plus AE

Improvements
- New gamer UI (Jetpack Compose): home with a "Continue playing" card, library with search and favorites, game details panel and settings grid. With a controller connected (or when you press the D-pad / a gamepad button) it switches to controller mode: landscape, full screen, focus rings and button hints (A select, B back, X details, Y favorite, L1/R1 switch tab). Touching the screen brings back the clean touch mode, in portrait or landscape; long-press a game to favorite it.
- Favorites and per-game play time.
- Every settings screen uses the same Compose list: cards with icon, title and value, switches, dependent items dimmed, D-pad / A / B navigation with visible focus. Detailed pickers (sliders, lists) still open the classic dialogs.
- New in-game pause menu (same controller / touch modes): Resume, Screenshot, Save / Load slot, speed, frame limiter, controller paks, save files and Exit. Open it with the Menu key (or by swiping from the left edge); B or Back closes it.
- Settings dialogs (list choices, sliders, text entry, confirmations, emulation-profile picker) use the same Compose style with D-pad / A / B and touch support.
- Redesigned dark user interface: gallery, new Settings hub, preference screens with toolbar, themed dialogs, import screen and splash.
- Netplay: fixed a client crash that kept a device out of a session after a failed connection to a stale mDNS entry.
- New "Open source licenses" screen in the app (Settings → Tools).
- RetroAchievements (softcore) support, from upstream and hardened (Settings → Tools → RetroAchievements). It only connects to retroachievements.org when enabled and logged in.
- Backported from upstream Mupen64Plus AE: support for ROMs larger than 64 MB, resource-leak fixes, locale tag fix, duplicate vibration call removed.
- Old profiles that used the removed angrylion-plus plugin switch automatically to the default plugin.

Removed
- The angrylion-plus software renderer (its MAME license forbids commercial use and is not GPL-compatible).
- Upstream FAQ, help-forum and credits menu entries and links.

Note: this fork is based on an earlier upstream snapshot. Upstream's manual ROM-path entry (which needs the restricted "All files access" permission) is not included. Full details: [NOTICE](NOTICE) and `docs/project_customization_and_merge_notes.md`.

## Build Instructions

1. Download and install the prerequisites
   - [Android Studio](https://developer.android.com/studio/index.html)
   - During the installation, make sure the latest SDK and NDK
   - If running Windows, make sure you install Git, Python, awk and required Microsoft Visual C++ Redistributable (i.e. cmake 3.18.1 requires Microsoft Visual C++ Redistributable 2015) and that the binaries are in your path environment variable.
2. Clone this repository
   - `git clone https://github.com/dmnhat97/m64-emulator-plus.git`
3. Open the project using Android Studio
4. Build and run the app from Android Studio
   - Select Build --> Make Project to build
   - Select Run --> Run app to run

Release signing keys are not part of this repository; build with your own keystore.

Setting up a clean clone (these files are not tracked here):
- `ndkLibs/libs/*`: take them from the upstream mupen64plus-ae repository.
- `ndkLibs/libs/*/*/librcheevos.a`: build with `ndkLibs/rcheevos-build/build.sh <path-to-ndk>` (sources are in `ndkLibs/rcheevos`).
- `gradle.properties` (present upstream) and `local.properties` (your SDK path).
- On macOS, generate the `asm_defines` headers with `mupen64plus-core/upstream/tools/gen_asm_script.sh`, because the BSD awk shipped with macOS cannot run `gen_asm_defines.awk`.

## Project-Specific Maintenance Notes

- Before syncing from upstream, read: `docs/project_customization_and_merge_notes.md`
- `app/src/main/assets/licenses/` holds copies of `NOTICE` and `LICENSE` shown inside the app;
  update them whenever the root files change.
