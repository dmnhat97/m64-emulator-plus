# Wrapper so ndk-build runs from jni/ with a proper Application.mk (16 KB page size).
JNI_LOCAL_PATH := $(call my-dir)/..
include $(JNI_LOCAL_PATH)/Android.mk
