#
# Build library bilaries for all supported architectures
#

APP_ABI := armeabi-v7a arm64-v8a x86 x86_64
APP_OPTIM := release
APP_STL := c++_static
APP_SUPPORT_FLEXIBLE_PAGE_SIZES := true
APP_CPPFLAGS := -fexceptions # -D SOUNDTOUCH_DISABLE_X86_OPTIMIZATIONS

