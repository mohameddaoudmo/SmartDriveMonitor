#include <jni.h>
#include <string>
#include <cstring>
#include "../sensor_fusion/KalmanFilter.h"
#include "../anfis_engine/AnfisEngine.h"
#include "../daemon/DaemonCore.h"

// Static instances for the execution paths
static KalmanFilter g_kalman_filter;
static AnfisEngine g_anfis_engine;
static DaemonCore g_daemon;

extern "C" JNIEXPORT void JNICALL
Java_com_smartcabin_data_source_NativeBridge_initFilter(
        JNIEnv* env, jobject thiz, jfloat initial_speed) {
    g_kalman_filter.init(initial_speed);
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_smartcabin_data_source_NativeBridge_updateFilter(
        JNIEnv* env, jobject thiz, jfloat measured_speed, jfloat dt) {
    return g_kalman_filter.update(measured_speed, dt);
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_smartcabin_data_source_NativeBridge_getAcceleration(
        JNIEnv* env, jobject thiz) {
    return g_kalman_filter.getAcceleration();
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_smartcabin_data_source_NativeBridge_evaluateAnfis(
        JNIEnv* env, jobject thiz, jfloat speed_variance, jfloat steering_angle, jfloat acceleration) {
    
    AnfisOutput out = g_anfis_engine.evaluate(speed_variance, steering_angle, acceleration);
    
    jclass resultClass = env->FindClass("com/example/smartdrivemonitor/data/source/NativeAnfisResult");
    if (resultClass == nullptr) {
        return nullptr;
    }
    
    jmethodID ctor = env->GetMethodID(resultClass, "<init>", "(FILjava/lang/String;F)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    
    jstring jRuleText = env->NewStringUTF(out.rule_text.c_str());
    jobject obj = env->NewObject(resultClass, ctor, 
                                 static_cast<jfloat>(out.safety_score), 
                                 static_cast<jint>(out.dominant_rule_index), 
                                 jRuleText, 
                                 static_cast<jfloat>(out.confidence));
    
    return obj;
}

// Daemon control JNI functions
extern "C" JNIEXPORT jboolean JNICALL
Java_com_smartcabin_data_source_NativeBridge_startDaemon(
        JNIEnv* env, jobject thiz, jint port) {
    return g_daemon.start(port) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_smartcabin_data_source_NativeBridge_stopDaemon(
        JNIEnv* env, jobject thiz) {
    g_daemon.stop();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_smartcabin_data_source_NativeBridge_isDaemonRunning(
        JNIEnv* env, jobject thiz) {
    return g_daemon.is_running() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_smartcabin_data_source_NativeBridge_injectDaemonSensorData(
        JNIEnv* env, jobject thiz, jfloat speed, jfloat brake, jfloat steering, jfloat rpm) {
    g_daemon.inject_raw_data(speed, brake, steering, rpm);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_smartcabin_data_source_NativeBridge_getLatestDaemonState(
        JNIEnv* env, jobject thiz) {
    
    DaemonState ds = g_daemon.get_latest_state();
    
    jclass stateClass = env->FindClass("com/example/smartdrivemonitor/data/source/NativeDaemonState");
    if (stateClass == nullptr) {
        return nullptr;
    }
    
    jmethodID ctor = env->GetMethodID(stateClass, "<init>", "(FFFFFFFILjava/lang/String;FJ)V");
    if (ctor == nullptr) {
        return nullptr;
    }
    
    jstring jRuleText = env->NewStringUTF(ds.rule_text);
    jobject obj = env->NewObject(stateClass, ctor,
                                 static_cast<jfloat>(ds.raw_speed),
                                 static_cast<jfloat>(ds.fused_speed),
                                 static_cast<jfloat>(ds.acceleration),
                                 static_cast<jfloat>(ds.brake),
                                 static_cast<jfloat>(ds.steering_angle),
                                 static_cast<jfloat>(ds.rpm),
                                 static_cast<jfloat>(ds.safety_score),
                                 static_cast<jint>(ds.dominant_rule_index),
                                 jRuleText,
                                 static_cast<jfloat>(ds.confidence),
                                 static_cast<jlong>(ds.timestamp));
    
    return obj;
}
