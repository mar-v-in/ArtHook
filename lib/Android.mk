LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := arthook_native
LOCAL_CFLAGS := -DANDROID_NDK -std=c99
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_LDLIBS := \
	-llog \

LOCAL_SRC_FILES := \
	./src/main/jni/hook.c \
	./src/main/jni/empty.c \

LOCAL_C_INCLUDES += ./src/debug/jni
LOCAL_C_INCLUDES += ./src/main/jni

include $(BUILD_SHARED_LIBRARY)
