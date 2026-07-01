#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>
#include <android/binder_manager.h>
#include <android/log.h>
#include <mutex>
#include <vector>
#include <algorithm>
#include "DaemonCore.h"

// Generate-includes for compiled AIDL bindings
#include <aidl/com/smartcabin/nativecore/BnDriverMonitorService.h>
#include <aidl/com/smartcabin/nativecore/IDriverMonitorListener.h>
#include <aidl/com/smartcabin/nativecore/DriverStateParcelable.h>

#define LOG_TAG "DriverMonitorDaemonMain"
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace aidl::com::smartcabin::nativecore;
using ndk::ScopedAStatus;

class DriverMonitorService : public BnDriverMonitorService {
public:
    DriverMonitorService() {
        daemon_.start(5005);
        daemon_.set_on_state_updated_callback([this](const DaemonState& ds) {
            notify_listeners(ds);
        });
        ALOGI("Native DriverMonitorService initialized");
    }

    ~DriverMonitorService() {
        daemon_.stop();
        ALOGI("Native DriverMonitorService destroyed");
    }

    ScopedAStatus registerListener(const std::shared_ptr<IDriverMonitorListener>& listener) override {
        std::lock_guard<std::mutex> lock(listeners_mutex_);
        if (listener) {
            listeners_.push_back(listener);
            ALOGI("Registered new listener. Total listeners: %zu", listeners_.size());
        }
        return ScopedAStatus::ok();
    }

    ScopedAStatus unregisterListener(const std::shared_ptr<IDriverMonitorListener>& listener) override {
        std::lock_guard<std::mutex> lock(listeners_mutex_);
        if (listener) {
            auto it = std::find(listeners_.begin(), listeners_.end(), listener);
            if (it != listeners_.end()) {
                listeners_.erase(it);
                ALOGI("Unregistered listener. Total listeners: %zu", listeners_.size());
            }
        }
        return ScopedAStatus::ok();
    }

    ScopedAStatus injectSensorData(float speed, float brake, float steeringAngle, float rpm) override {
        daemon_.inject_raw_data(speed, brake, steeringAngle, rpm);
        return ScopedAStatus::ok();
    }

    ScopedAStatus getLatestState(DriverStateParcelable* _aidl_return) override {
        DaemonState ds = daemon_.get_latest_state();
        _aidl_return->rawSpeed = ds.raw_speed;
        _aidl_return->fusedSpeed = ds.fused_speed;
        _aidl_return->acceleration = ds.acceleration;
        _aidl_return->brake = ds.brake;
        _aidl_return->steeringAngle = ds.steering_angle;
        _aidl_return->rpm = ds.rpm;
        _aidl_return->safetyScore = ds.safety_score;
        _aidl_return->dominantRuleIndex = ds.dominant_rule_index;
        _aidl_return->ruleText = std::string(ds.rule_text);
        _aidl_return->confidence = ds.confidence;
        _aidl_return->timestamp = static_cast<int64_t>(ds.timestamp);
        return ScopedAStatus::ok();
    }

private:
    void notify_listeners(const DaemonState& ds) {
        std::lock_guard<std::mutex> lock(listeners_mutex_);
        
        DriverStateParcelable parcel;
        parcel.rawSpeed = ds.raw_speed;
        parcel.fusedSpeed = ds.fused_speed;
        parcel.acceleration = ds.acceleration;
        parcel.brake = ds.brake;
        parcel.steeringAngle = ds.steering_angle;
        parcel.rpm = ds.rpm;
        parcel.safetyScore = ds.safety_score;
        parcel.dominantRuleIndex = ds.dominant_rule_index;
        parcel.ruleText = std::string(ds.rule_text);
        parcel.confidence = ds.confidence;
        parcel.timestamp = static_cast<int64_t>(ds.timestamp);

        for (auto it = listeners_.begin(); it != listeners_.end(); ) {
            auto status = (*it)->onDriverStateUpdated(parcel);
            if (!status.isOk()) {
                // If callback fails, connection is dead; erase the listener
                it = listeners_.erase(it);
                ALOGI("Removed dead listener callback context");
            } else {
                ++it;
            }
        }
    }

    DaemonCore daemon_;
    std::mutex listeners_mutex_;
    std::vector<std::shared_ptr<IDriverMonitorListener>> listeners_;
};

int main() {
    ALOGI("Starting Standalone Native DriverMonitor Daemon...");
    
    // Start binder threadpool
    android::ProcessState::self()->startThreadPool();
    
    // Instantiate our implementation class
    auto service = ndk::SharedRefBase::make<DriverMonitorService>();
    
    // Add service to system Service Manager
    binder_status_t status = AServiceManager_addService(
        service->asBinder().get(), 
        "com.smartcabin.IDriverMonitorService"
    );
    
    if (status != STATUS_OK) {
        ALOGE("CRITICAL: Failed to register com.smartcabin.IDriverMonitorService binder service (Status: %d)", status);
        return 1;
    }
    
    ALOGI("Binder Service registered. Running IPC daemon loop...");
    android::IPCThreadState::self()->joinThreadPool();
    return 0;
}
