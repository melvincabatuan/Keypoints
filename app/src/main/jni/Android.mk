# Build ImageProcessing

LOCAL_PATH := $(call my-dir)

# Build ImageProcessing library

include $(CLEAR_VARS)
 
OPENCV_LIB_TYPE:=STATIC
OPENCV_INSTALL_MODULES:=on

include /home/cobalt/Android/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk

LOCAL_MODULE    := ImageProcessing
LOCAL_SRC_FILES := ImageProcessing.cpp
LOCAL_SRC_FILES += $(wildcard $(LOCAL_PATH)/xfeatures2d_src/*.cpp) 
LOCAL_LDLIBS 	+=  -llog -ldl -ljnigraphics

include $(BUILD_SHARED_LIBRARY)
