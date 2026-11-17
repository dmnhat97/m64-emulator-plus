# Project Customization And Merge Notes

This repository is no longer a vanilla upstream build.
It contains product-specific customization that must be preserved when syncing from upstream.

## Current Product Identity

- App display name: `XM64 Emulator Plus`
- Package/theme code names may still include legacy identifiers (for compatibility), but user-facing branding must remain `XM64 Emulator Plus`.

## High-Risk Custom Areas (Do Not Overwrite Blindly)

### 1) App Branding And User-Facing Text

- `app/src/main/res/values/strings-no-translate.xml`
  - `app_name` changed to `XM64 Emulator Plus`
  - Legacy support/forum/credits URLs removed (`uri_forum`, `uri_bugReport`, `uri_credits`)
- `app/src/main/res/values*/strings.xml`
  - Multiple strings updated from old product naming to `XM64 Emulator Plus`
  - Legacy FAQ/help/credits flow removed from active UI path

### 2) Navigation / Menu Structure

- `app/src/main/res/menu/gallery_drawer.xml`
  - FAQ / Help Forum / Credits menu items removed from current product menu
- `app/src/main/java/paulscode/android/mupen64plusae/GalleryActivity.java`
  - Removed handlers for `menuItem_faq`, `menuItem_helpForum`, `menuItem_credits`

### 3) Popup/Dialog Legacy Help Content

- `app/src/main/java/paulscode/android/mupen64plusae/dialog/Popups.java`
  - Removed FAQ popup state/handler and related helper

### 4) Network Security Domains

- `app/src/main/res/xml/network_security_config.xml`
  - Removed `paulscode.com` cleartext domain
  - Keep this file under review during upstream merges

### 5) Theme And Modern UI Overhaul

- Extensive custom visual system exists across:
  - `app/src/main/res/values/colors.xml`
  - `app/src/main/res/values/styles.xml`
  - `app/src/main/res/drawable/*` (new dark theme/card/tabbar/dialog assets)
  - `app/src/main/res/layout/*` (gallery, settings, import, profile-related screens)
- Upstream resource changes can easily regress current UX if merged without manual review.

### 6) Preference Shell And Back Navigation Refactor

- `app/src/main/java/paulscode/android/mupen64plusae/compat/AppCompatPreferenceActivity.java`
- `app/src/main/res/layout/activity_preference_shell.xml`
- Multiple preference activities migrated to `NoActionBar` + custom toolbar behavior.

### 7) Dialog Theming Modernization

- Alert dialog usage and theme overlay behavior was customized to avoid legacy style fallback.
- Key files:
  - `app/src/main/res/values/styles.xml`
  - `app/src/main/java/paulscode/android/mupen64plusae/dialog/*.java`
  - `app/src/main/res/layout/progress_dialog.xml`

### 8) Splash Branding De-Legacy

- `app/src/main/res/layout/splash_activity.xml`
  - Splash visual switched to app launcher icon + app name text
  - Legacy splash artwork references removed
- `app/src/main/java/paulscode/android/mupen64plusae/SplashActivity.java`
  - Big-screen splash image source switched to `@mipmap/ic_launcher`
- `app/src/main/AndroidManifest.xml`
  - `android:logo` and `game_icon` metadata switched from legacy drawable to `@mipmap/ic_launcher`
  - Prevents old N64/legacy publisher branding from reappearing in splash/launcher-integrated surfaces

## Merge Workflow (Recommended)

When pulling upstream updates:

1. **Merge upstream branch without auto conflict resolution** for files listed above.
2. **Review conflicts file-by-file** in high-risk areas.
3. **Preserve local product behavior first**, then selectively apply upstream fixes.
4. Build immediately:
   - `./gradlew :app:processDebugResources :app:compileDebugJavaWithJavac`
5. Smoke-check these flows:
   - Gallery menu and settings navigation
   - In-game exit confirmation dialog theme
   - Import ROM screen
   - Preference screens back button + toolbar shell
   - App name and key dialogs/toasts

## Practical Guardrails

- Do not bulk replace `app/src/main/res/values*/strings*.xml` from upstream.
- Do not bulk replace `gallery_drawer.xml`, `styles.xml`, or `network_security_config.xml`.
- Do not bulk replace `Popups.java`.
- `AppMupen64Plus.java` is intentionally an empty `MultiDexApplication`: the first-open analytics hook was removed. Do not reintroduce it when merging upstream.
- Do not reintroduce legacy splash/logo drawables (`publisherlogo`, `hireslogo`) into splash or manifest branding fields.
- If upstream introduces new menu/help entries, manually decide whether they fit product scope.

## If You Need To Re-Enable Upstream Help/Credits

Re-enable only intentionally:

- Re-add menu entries in `gallery_drawer.xml`
- Re-add handlers in `GalleryActivity.java`
- Restore URL strings in `strings-no-translate.xml`
- Restore popup logic in `Popups.java`
- Re-add required domains in `network_security_config.xml` if needed

## Notes

- Some legacy keys are intentionally kept as default fallback strings to avoid resource merge warnings when locale packs define those keys.
- This document should be updated every time product-specific behavior diverges from upstream.

## Upstream Sync Status (as of 2026-11)

Upstream: https://github.com/mupen64plus-ae/mupen64plus-ae (GPL-3.0, active; last checked commit 33bf70215, 2026-08-28).

- Adopted: `32e19db5a` Allow loading 64MB+ ROMs; `33bf70215` Fix resource leaks (all files except `gradle.properties`); `-dontwarn com.github.luben.zstd.**` in `proguard-rules.pro`.
- Deliberately NOT adopted: `62ba760e3` manual ROM path entry. It adds `MANAGE_EXTERNAL_STORAGE` (restricted by Google Play) and only helps Android TV 11+.
- Adopted with changes: `03b4c0de3` RetroAchievements via rcheevos (MIT). Changes vs upstream: removed the exported `RetroAchievementsHostOverrideReceiver` (any app could redirect the user's token to its own server), no token in logcat, own User-Agent (`XM64EmulatorPlus (Android)`), RA settings screen uses the `NoActionBar` shell theme, and a "RetroAchievements" row in Settings → Tools (the drawer entry is unreachable in the new UI). Vendored in `ndkLibs/rcheevos`; rebuild `librcheevos.a` with `ndkLibs/rcheevos-build/build.sh`. Verified on device only up to "enabled, not logged in" (no RA account/ROM available to test unlocking).
- Do not bulk-apply upstream `strings.xml` hunks: they also escape `<small>` as `&lt;small>`, which changes how our labels render.

