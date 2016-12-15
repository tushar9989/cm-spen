LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := EventInjector
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_LDLIBS := \
	-llog \

LOCAL_SRC_FILES := \
	C:\Users\Tushar\AndroidstudioProjects\EventInjector\CMSPen\src\main\jni\EventInjector.c \
	C:\Users\Tushar\AndroidstudioProjects\EventInjector\CMSPen\src\main\jni\nativeTerminal.cpp \

LOCAL_C_INCLUDES += C:\Users\Tushar\AndroidstudioProjects\EventInjector\CMSPen\src\main\jni
LOCAL_C_INCLUDES += C:\Users\Tushar\AndroidstudioProjects\EventInjector\CMSPen\src\release\jni

include $(BUILD_SHARED_LIBRARY)
