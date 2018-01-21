LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := arthook_native
LOCAL_CFLAGS := -DANDROID_NDK -std=c99
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_LDLIBS := \
	-llog \

LOCAL_SRC_FILES := \
	/Users/timtoheus/Projects/ghforks/ArtHook/lib/src/main/jni/empty.c \
	/Users/timtoheus/Projects/ghforks/ArtHook/lib/src/main/jni/hook.c \

LOCAL_C_INCLUDES += /Users/timtoheus/Projects/ghforks/ArtHook/lib/src/main/jni
LOCAL_C_INCLUDES += /Users/timtoheus/Projects/ghforks/ArtHook/lib/src/debug/jni

include $(BUILD_SHARED_LIBRARY)
