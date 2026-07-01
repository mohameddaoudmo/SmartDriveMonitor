#include "KalmanFilter.h"

KalmanFilter::KalmanFilter(float processNoiseAcc, float measurementNoise)
    : initialized_(false),
      x_speed_(0.0f),
      x_accel_(0.0f),
      P00_(1.0f),
      P01_(0.0f),
      P11_(1.0f),
      q_acc_var_(processNoiseAcc * processNoiseAcc),
      r_var_(measurementNoise * measurementNoise) {}

void KalmanFilter::init(float initialSpeed) {
    x_speed_ = initialSpeed;
    x_accel_ = 0.0f;
    P00_ = 1.0f;
    P01_ = 0.0f;
    P11_ = 1.0f;
    initialized_ = true;
}

float KalmanFilter::update(float measuredSpeed, float dt) {
    if (!initialized_) {
        init(measuredSpeed);
        return x_speed_;
    }

    if (dt <= 0.0f) dt = 0.01f;

    // --- 1. Predict Step ---
    // F = [1 dt; 0 1]
    // x = F * x
    float x_speed_pred = x_speed_ + x_accel_ * dt;
    float x_accel_pred = x_accel_;

    // Q covariance of process noise
    float dt2 = dt * dt;
    float dt3 = dt2 * dt;
    float dt4 = dt3 * dt;

    float Q00 = 0.25f * dt4 * q_acc_var_;
    float Q01 = 0.5f * dt3 * q_acc_var_;
    float Q11 = dt2 * q_acc_var_;

    // P = F * P * F^T + Q
    float P00_pred = P00_ + 2.0f * P01_ * dt + P11_ * dt2 + Q00;
    float P01_pred = P01_ + P11_ * dt + Q01;
    float P11_pred = P11_ + Q11;

    // --- 2. Update Step ---
    // y = z - H * x_pred
    float y = measuredSpeed - x_speed_pred;

    // S = H * P_pred * H^T + R
    float S = P00_pred + r_var_;

    // K = P_pred * H^T * S^-1
    float K0 = P00_pred / S;
    float K1 = P01_pred / S;

    // x = x_pred + K * y
    x_speed_ = x_speed_pred + K0 * y;
    x_accel_ = x_accel_pred + K1 * y;

    // P = (I - K * H) * P_pred
    P00_ = (1.0f - K0) * P00_pred;
    P01_ = (1.0f - K0) * P01_pred;
    P11_ = P11_pred - K1 * P01_pred;

    return x_speed_;
}
