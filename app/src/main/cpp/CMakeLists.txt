cmake_minimum_required(VERSION 3.22.1)

project("smartdrivemonitor")

# Add native shared library
add_library(smartdrivemonitor SHARED
        jni/native-lib.cpp
        sensor_fusion/KalmanFilter.cpp
        sensor_fusion/CanDecoder.cpp
        anfis_engine/AnfisEngine.cpp
        daemon/DaemonCore.cpp
)

# Include standard NDK logging support
find_library(log-lib log)
find_library(android-lib android)

# Link libs
target_link_libraries(smartdrivemonitor
        ${log-lib}
        ${android-lib}
)
