LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := SDL2_net
LOCAL_SRC_FILES := \
	../upstream/SDLnet.c \
	../upstream/SDLnetTCP.c \
	../upstream/SDLnetUDP.c \
	../upstream/SDLnetselect.c

LOCAL_C_INCLUDES += $(LOCAL_PATH) $(LOCAL_PATH)/../upstream
LOCAL_CFLAGS += -include $(LOCAL_PATH)/sdl_net_android_compat.h
LOCAL_LDLIBS := -llog

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384
endif
ifeq ($(TARGET_ARCH_ABI),x86_64)
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384
endif

include $(BUILD_SHARED_LIBRARY)
