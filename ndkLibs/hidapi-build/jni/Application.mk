APP_ABI := armeabi-v7a arm64-v8a x86 x86_64
APP_STL := c++_shared
APP_OPTIM := release
APP_SUPPORT_FLEXIBLE_PAGE_SIZES := true
# Bionic headers define pthread_barrier_t; hidapi's API<24 fallback collides. Match core min if needed.
APP_PLATFORM := android-24
