# Static libpng + shared libfreetype (libfreetype.so) for ndkLibs prebuilts / GLideN64.

FREETYPE_JNI := $(call my-dir)
NDK_LIBS_ROOT := $(abspath $(FREETYPE_JNI)/../..)
PNG_DIR := $(NDK_LIBS_ROOT)/png
FT_DIR := $(NDK_LIBS_ROOT)/freetype

include $(CLEAR_VARS)
LOCAL_PATH := $(PNG_DIR)
LOCAL_MODULE := png_for_ft2
LOCAL_SRC_FILES := \
	png.c \
	pngerror.c \
	pngget.c \
	pngmem.c \
	pngpread.c \
	pngread.c \
	pngrio.c \
	pngrtran.c \
	pngrutil.c \
	pngset.c \
	pngtrans.c \
	pngwio.c \
	pngwrite.c \
	pngwtran.c \
	pngwutil.c

LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
# Single .c list: disable arch-specific filter opts (otherwise missing neon/SSE objects).
LOCAL_CFLAGS += -DPNG_ARM_NEON_OPT=0 -DPNG_INTEL_SSE_OPT=0
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_PATH := $(FT_DIR)
LOCAL_MODULE := freetype
LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES := \
	src/base/ftbbox.c \
	src/base/ftbitmap.c \
	src/base/ftfstype.c \
	src/base/ftglyph.c \
	src/base/ftlcdfil.c \
	src/base/ftstroke.c \
	src/base/fttype1.c \
	src/base/ftxf86.c \
	src/base/ftbase.c \
	src/base/ftsystem.c \
	src/base/ftinit.c \
	src/base/ftgasp.c \
	src/gzip/ftgzip.c \
	src/raster/raster.c \
	src/sfnt/sfnt.c \
	src/smooth/smooth.c \
	src/autofit/autofit.c \
	src/truetype/truetype.c \
	src/cff/cff.c \
	src/psnames/psnames.c \
	src/pshinter/pshinter.c

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/builds \
	$(LOCAL_PATH)/include \
	$(PNG_DIR)

LOCAL_CFLAGS += -W -Wall
LOCAL_CFLAGS += -fPIC -DPIC
LOCAL_CFLAGS += -DDARWIN_NO_CARBON
LOCAL_CFLAGS += -DFT2_BUILD_LIBRARY
LOCAL_CFLAGS += -O2

LOCAL_STATIC_LIBRARIES := png_for_ft2
LOCAL_LDLIBS := -lz -lm

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384
endif
ifeq ($(TARGET_ARCH_ABI),x86_64)
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384
endif

include $(BUILD_SHARED_LIBRARY)
