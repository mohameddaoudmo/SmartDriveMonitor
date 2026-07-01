#pragma once

class KalmanFilter {
public:
    KalmanFilter(float processNoiseAcc = 0.5f, float measurementNoise = 0.1f);

    // Reset the filter with an initial speed measurement
    void init(float initialSpeed);

    // Prediction and correction step. Returns the smoothed speed.
    float update(float measuredSpeed, float dt);

    float getSpeed() const { return x_speed_; }
    float getAcceleration() const { return x_accel_; }

private:
    bool initialized_;

    // States: speed and acceleration
    float x_speed_;
    float x_accel_;

    // State Covariance Matrix P
    float P00_;
    float P01_;
    float P11_;

    // Covariance parameters
    float q_acc_var_;  // Variance of acceleration process noise
    float r_var_;      // Variance of speed measurement noise
};
