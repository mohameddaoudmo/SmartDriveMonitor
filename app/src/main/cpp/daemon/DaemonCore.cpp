#include "DaemonCore.h"
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <chrono>
#include <cstring>
#include <android/log.h>

#define LOG_TAG "DriverMonitorDaemon"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

DaemonCore::DaemonCore() : running_(false), last_speed_time_ms_(0) {
    std::memset(&latest_state_, 0, sizeof(latest_state_));
}

DaemonCore::~DaemonCore() {
    stop();
}

bool DaemonCore::start(int port) {
    if (running_) return true;

    running_ = true;
    variance_tracker_.reset();
    
    // Start UDP socket listener thread
    listener_thread_ = std::thread(&DaemonCore::udp_listener_loop, this, port);
    
    // Start processing pipeline thread
    processor_thread_ = std::thread(&DaemonCore::processing_loop, this);

    LOGD("DaemonCore successfully started native threads");
    return true;
}

void DaemonCore::stop() {
    if (!running_) return;

    running_ = false;
    
    if (listener_thread_.joinable()) {
        listener_thread_.join();
    }
    
    if (processor_thread_.joinable()) {
        processor_thread_.join();
    }

    LOGD("DaemonCore successfully stopped native threads");
}

void DaemonCore::set_on_state_updated_callback(std::function<void(const DaemonState&)> cb) {
    std::lock_guard<std::mutex> lock(state_mutex_);
    on_state_updated_ = cb;
}

DaemonState DaemonCore::get_latest_state() const {
    std::lock_guard<std::mutex> lock(state_mutex_);
    return latest_state_;
}

void DaemonCore::inject_raw_data(float speed, float brake, float steering, float rpm) {
    // Pack into two simulated Raw CAN frames
    RawCanFrame frame_101;
    frame_101.can_id = 0x101;
    frame_101.can_dlc = 8;
    std::memcpy(frame_101.data, &speed, 4);
    std::memcpy(frame_101.data + 4, &brake, 4);
    queue_.enqueue(frame_101);

    RawCanFrame frame_102;
    frame_102.can_id = 0x102;
    frame_102.can_dlc = 8;
    std::memcpy(frame_102.data, &steering, 4);
    std::memcpy(frame_102.data + 4, &rpm, 4);
    queue_.enqueue(frame_102);
}

void DaemonCore::udp_listener_loop(int port) {
    int sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sockfd < 0) {
        LOGE("Failed to create UDP socket");
        return;
    }

    // Allow address reuse
    int reuse = 1;
    setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &reuse, sizeof(reuse));

    // Set receive timeout to prevent blocking indefinitely on stop()
    struct timeval tv;
    tv.tv_sec = 0;
    tv.tv_usec = 200000; // 200ms
    setsockopt(sockfd, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));

    struct sockaddr_in servaddr;
    std::memset(&servaddr, 0, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = INADDR_ANY;
    servaddr.sin_port = htons(port);

    if (bind(sockfd, (const struct sockaddr *)&servaddr, sizeof(servaddr)) < 0) {
        LOGE("Failed to bind socket on port %d", port);
        close(sockfd);
        return;
    }

    LOGD("UDP Socket listener thread listening on port %d", port);
    RawCanFrame frame;

    while (running_) {
        struct sockaddr_in cliaddr;
        socklen_t len = sizeof(cliaddr);
        ssize_t n = recvfrom(sockfd, &frame, sizeof(frame), 0, (struct sockaddr *)&cliaddr, &len);
        if (n == sizeof(frame)) {
            queue_.enqueue(frame);
        }
    }

    close(sockfd);
    LOGD("UDP Socket listener thread shutting down");
}

void DaemonCore::processing_loop() {
    RawCanFrame raw;
    DecodedState state;
    uint64_t last_time_ms = 0;

    while (running_) {
        bool got_frame = false;
        while (queue_.dequeue(raw)) {
            got_frame = true;
            CanDecoder::decode(raw, state);
        }

        if (got_frame) {
            uint64_t now_ms = std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::steady_clock::now().time_since_epoch()
            ).count();

            float dt = 0.1f; // Default 100ms
            if (last_time_ms > 0) {
                dt = (now_ms - last_time_ms) / 1000.0f;
            }
            last_time_ms = now_ms;

            // Update Kalman filter on speed
            float raw_speed = state.speed;
            float fused_speed = kalman_.update(raw_speed, dt);
            float acceleration = kalman_.getAcceleration();

            // Track variance
            variance_tracker_.add_sample(fused_speed);
            float speed_var = variance_tracker_.get_variance();

            // Run ANFIS Inference
            AnfisOutput fuzzy_out = anfis_.evaluate(speed_var, state.steering, acceleration);

            // Construct state
            DaemonState ds;
            ds.raw_speed = raw_speed;
            ds.fused_speed = fused_speed;
            ds.acceleration = acceleration;
            ds.brake = state.brake;
            ds.steering_angle = state.steering;
            ds.rpm = state.rpm;
            ds.safety_score = fuzzy_out.safety_score;
            ds.dominant_rule_index = fuzzy_out.dominant_rule_index;
            std::strncpy(ds.rule_text, fuzzy_out.rule_text.c_str(), sizeof(ds.rule_text) - 1);
            ds.rule_text[sizeof(ds.rule_text) - 1] = '\0';
            ds.confidence = fuzzy_out.confidence;
            ds.timestamp = now_ms;

            {
                std::lock_guard<std::mutex> lock(state_mutex_);
                latest_state_ = ds;
            }

            if (on_state_updated_) {
                on_state_updated_(ds);
            }
        }

        // Sleep to avoid high CPU usage
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
    LOGD("Processing loop thread shutting down");
}
