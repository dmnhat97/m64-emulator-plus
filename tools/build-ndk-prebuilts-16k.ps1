# Rebuild key ndkLibs prebuilt .so files with NDK r28+ and 16 KB ELF alignment.
# Usage (PowerShell):  cd <repo>; .\tools\build-ndk-prebuilts-16k.ps1
# Requires: Android NDK 28.x (match build_common/version_common.gradle), same ABIs as the app.

$ErrorActionPreference = "Stop"

function Resolve-Ndk {
    if ($env:ANDROID_NDK_HOME) { return $env:ANDROID_NDK_HOME.TrimEnd('\') }
    $sdk = $env:ANDROID_SDK_ROOT
    if (-not $sdk) { $sdk = "$env:LOCALAPPDATA\Android\Sdk" }
    $cand = Join-Path $sdk "ndk\28.0.13004108"
    if (Test-Path $cand) { return $cand }
    $latest = Get-ChildItem (Join-Path $sdk "ndk") -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
    if ($latest) { return $latest.FullName }
    throw "Set ANDROID_NDK_HOME or install NDK 28.x under $sdk\ndk\"
}

function Copy-PrebuiltLibs {
    param([string]$LibsDir, [string]$NdkLibsRoot, [string[]]$OnlyNames = @())
    if (-not (Test-Path $LibsDir)) { Write-Warning "Missing libs dir: $LibsDir"; return }
    foreach ($abi in @("armeabi-v7a", "arm64-v8a", "x86", "x86_64")) {
        $src = Join-Path $LibsDir $abi
        if (-not (Test-Path $src)) { continue }
        foreach ($dest in @("release", "debug")) {
            $dst = Join-Path $NdkLibsRoot "libs\$dest\$abi"
            New-Item -ItemType Directory -Force -Path $dst | Out-Null
            Get-ChildItem $src -Filter "*.so" | ForEach-Object {
                if ($OnlyNames.Count -gt 0 -and ($OnlyNames -notcontains $_.Name)) { return }
                Copy-Item $_.FullName (Join-Path $dst $_.Name) -Force
            }
        }
    }
}

$ndk = Resolve-Ndk
$ndkBuild = Join-Path $ndk "ndk-build.cmd"
if (-not (Test-Path $ndkBuild)) { throw "ndk-build not found: $ndkBuild" }

$repoRoot = Split-Path -Parent $PSScriptRoot
$ndkLibs = Join-Path $repoRoot "ndkLibs"

$args16 = @("APP_SUPPORT_FLEXIBLE_PAGE_SIZES:=true", "-j8")

Write-Host "Using NDK: $ndk"

# --- miniupnpc (libminiupnp-build.so) ---
$jniMini = Join-Path $ndkLibs "miniupnp-build\jni"
Push-Location $jniMini
try {
    & $ndkBuild @args16 "NDK_APPLICATION_MK=Application.mk"
} finally { Pop-Location }
Copy-PrebuiltLibs (Join-Path $ndkLibs "miniupnp-build\libs") $ndkLibs

# --- libnatpmp ---
$jniNat = Join-Path $ndkLibs "libnatpmp\jni"
Push-Location $jniNat
try {
    & $ndkBuild @args16 "NDK_APPLICATION_MK=Application.mk"
} finally { Pop-Location }
Copy-PrebuiltLibs (Join-Path $ndkLibs "libnatpmp\libs") $ndkLibs

# --- SoundTouch (ndk-build installs libs under Android-lib/libs, not jni/libs) ---
$jniSt = Join-Path $ndkLibs "soundtouch\source\Android-lib\jni"
Push-Location $jniSt
try {
    & $ndkBuild @args16 "NDK_APPLICATION_MK=Application.mk"
} finally { Pop-Location }
Copy-PrebuiltLibs (Join-Path $ndkLibs "soundtouch\source\Android-lib\libs") $ndkLibs

# --- SDL2_net (libSDL2_net.so) ---
$jniSdlNet = Join-Path $ndkLibs "SDL2_net\jni"
Push-Location $jniSdlNet
try {
    & $ndkBuild @args16 "NDK_APPLICATION_MK=Application.mk"
} finally { Pop-Location }
Copy-PrebuiltLibs (Join-Path $jniSdlNet "..\libs") $ndkLibs

# --- FreeType + libpng (libfreetype.so) ---
$jniFt = Join-Path $ndkLibs "freetype-build\jni"
Push-Location $jniFt
try {
    & $ndkBuild @args16 "NDK_APPLICATION_MK=Application.mk"
} finally { Pop-Location }
Copy-PrebuiltLibs (Join-Path $jniFt "..\libs") $ndkLibs

# --- hidapi (libhidapi.so only; also rebuilds libusb1.0 in this subproject output) ---
$jniHid = Join-Path $ndkLibs "hidapi-build\jni"
Push-Location $jniHid
try {
    & $ndkBuild @args16 "NDK_APPLICATION_MK=Application.mk"
} finally { Pop-Location }
Copy-PrebuiltLibs (Join-Path $jniHid "..\libs") $ndkLibs -OnlyNames @("libhidapi.so")

Write-Host ""
Write-Host "Copied rebuilt .so into ndkLibs/libs/release and ndkLibs/libs/debug for each ABI."
Write-Host ""
Write-Host "Oboe: use com.google.oboe:oboe:1.10.0 in mupen64plus-audio-android (see that module build.gradle)."
