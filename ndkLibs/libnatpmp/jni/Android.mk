LOCAL_PATH := $(call my-dir)/..

include $(CLEAR_VARS)
LOCAL_MODULE := natpmp
LOCAL_SRC_FILES := natpmp.c getgateway.c
LOCAL_CFLAGS := -O3 -fPIC -fsigned-char -DENABLE_STRNATPMPERR -Wall
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
include $(BUILD_SHARED_LIBRARY)
