#pragma once
#include <thread>
#include <atomic>
#include <mutex>
#include <vector>
#include <functional>
#include "../utils/SpscRingBuffer.h"
#include "../sensor_fusion/CanDecoder.h"
#include "../sensor_fusion/KalmanFilter.h"
#include "../anfis_engine/AnfisEngine.h"

// Simple tracker for speed variance
class SpeedVarianceTracker {
public:
    void add_sample(float speed) {
        if (history_.size() >= 50) {
            history_.erase(history_.begin());
        }
        history_.push_back(speed);
    }

    float get_variance() const {
        if (history_.size() < 2) return 0.0f;
        float sum = 0.0f;
        for (float val : history_) sum += val;
        float mean = sum / history_.size();
        float sq_sum = 0.0f;
        for (float val : history_) {
            sq_sum += (val - mean) * (val - mean);
        }
        return sq_sum / (history_.size() - 1);
    }

    void reset() {
        history_.clear();
    }

private:
    std::vector<float> history_;
};

// Represents the output states propagated from C++ daemon
struct DaemonState {
    float raw_speed;
    float fused_speed;
    float acceleration;
    float brake;
    float steering_angle;
    float rpm;
    float safety_score;
    int dominant_rule_index;
    char rule_text[256];
    float confidence;
    uint64_t timestamp;
};

class DaemonCore {
public:
    DaemonCore();
    ~DaemonCore();

    bool start(int port = 5005);
    void stop();
    bool is_running() const { return running_; }

    // Register a callback for state updates (used for JNI or native Binder service)
    void set_on_state_updated_callback(std::function<void(const DaemonState&)> cb);

    // Explicitly inject raw sensor data (fallback interface)
    void inject_raw_data(float speed, float brake, float steering, float rpm);

    // Get the latest processed state
    DaemonState get_latest_state() const;

private:
    void udp_listener_loop(int port);
    void processing_loop();

    std::atomic<bool> running_;
    std::thread listener_thread_;
    std::thread processor_thread_;

    // Lock-free queue for CAN frames
    SpscRingBuffer<RawCanFrame, 1024> queue_;

    // Core engines
    KalmanFilter kalman_;
    AnfisEngine anfis_;
    SpeedVarianceTracker variance_tracker_;

    // Thread-safe state storage
    mutable std::mutex state_mutex_;
    DaemonState latest_state_;
    uint64_t last_speed_time_ms_;

    std::function<void(const DaemonState&)> on_state_updated_;
};
