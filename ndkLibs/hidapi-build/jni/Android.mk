# Builds libhidapi.so (libusb backend) + libusb1.0.so using the same libusb
# tree as mupen64plus-input-raphnet. Output: libs/<abi>/libhidapi.so

# $(call my-dir) after including libusb.mk points at libusb's jni; capture this jni dir first.
HIDAPI_JNI := $(call my-dir)

include $(HIDAPI_JNI)/../../../mupen64plus-input-raphnet/libusb/android/jni/libusb.mk

include $(CLEAR_VARS)
LOCAL_PATH := $(HIDAPI_JNI)
LOCAL_MODULE := hidapi
LOCAL_SRC_FILES := $(HIDAPI_JNI)/../../hidapi/upstream/libusb/hid.c
LOCAL_C_INCLUDES += \
	$(HIDAPI_JNI)/../../hidapi/upstream/hidapi \
	$(HIDAPI_JNI)/../../hidapi/upstream/libusb \
	$(HIDAPI_JNI)/../../../mupen64plus-input-raphnet/libusb/libusb \
	$(HIDAPI_JNI)/../../../mupen64plus-input-raphnet/libusb

LOCAL_SHARED_LIBRARIES := libusb1.0
LOCAL_LDLIBS := -llog

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384
endif
ifeq ($(TARGET_ARCH_ABI),x86_64)
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384
endif

include $(BUILD_SHARED_LIBRARY)
