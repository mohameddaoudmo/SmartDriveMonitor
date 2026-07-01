#include "AnfisEngine.h"
#include <cmath>
#include <algorithm>
#include <android/trace.h>

// Sugeno output constant weights for each of the 27 rules
// Format is: Index = speed_idx * 9 + steer_idx * 3 + accel_idx
// 0 = Low/Stable/Smooth, 1 = Medium/Swerving/Moderate, 2 = High/Sharp/Harsh
static const float RULE_OUTPUTS[27] = {
    // Speed = STEADY (0)
    // Steer=STABLE (0)
    0.0f,   15.0f,  60.0f,  // Accel = SMOOTH (0), MODERATE (1), HARSH (2)
    // Steer=SWERVING (1)
    20.0f,  30.0f,  65.0f,
    // Steer=SHARP (2)
    65.0f,  70.0f,  80.0f,

    // Speed = MODERATE (1)
    // Steer=STABLE (0)
    20.0f,  30.0f,  65.0f,
    // Steer=SWERVING (1)
    30.0f,  45.0f,  75.0f,
    // Steer=SHARP (2)
    70.0f,  75.0f,  85.0f,

    // Speed = ERRATIC (2)
    // Steer=STABLE (0)
    70.0f,  75.0f,  85.0f,
    // Steer=SWERVING (1)
    75.0f,  80.0f,  90.0f,
    // Steer=SHARP (2)
    85.0f,  95.0f,  100.0f
};

AnfisEngine::AnfisEngine() {
    speed_labels_ = {"STEADY", "MODERATE", "ERRATIC"};
    steering_labels_ = {"STABLE", "SWERVING", "SHARP"};
    accel_labels_ = {"SMOOTH", "MODERATE", "HARSH"};
}

float AnfisEngine::trimf(float x, float a, float b, float c) {
    if (x <= a || x >= c) return 0.0f;
    if (x > a && x < b) return (x - a) / (b - a);
    if (x >= b && x < c) return (c - x) / (c - b);
    return 0.0f;
}

float AnfisEngine::trapmf(float x, float a, float b, float c, float d) {
    if (x <= a || x >= d) return 0.0f;
    if (x >= b && x <= c) return 1.0f;
    if (x > a && x < b) return (x - a) / (b - a);
    if (x > c && x < d) return (d - x) / (d - c);
    return 0.0f;
}

AnfisOutput AnfisEngine::evaluate(float speed_variance, float steering_angle, float acceleration) {
    ATrace_beginSection("AnfisEngine_Inference");
    
    // 1. Fuzzify Speed Variance
    float mu_speed[3];
    mu_speed[0] = trapmf(speed_variance, -1.0f, 0.0f, 2.0f, 5.0f);     // STEADY
    mu_speed[1] = trimf(speed_variance, 2.0f, 5.0f, 10.0f);           // MODERATE
    mu_speed[2] = trapmf(speed_variance, 8.0f, 12.0f, 100.0f, 200.0f); // ERRATIC

    // 2. Fuzzify Steering Angle
    float abs_steer = std::abs(steering_angle);
    float mu_steer[3];
    mu_steer[0] = trapmf(abs_steer, -1.0f, 0.0f, 8.0f, 18.0f);         // STABLE
    mu_steer[1] = trimf(abs_steer, 12.0f, 22.0f, 35.0f);               // SWERVING
    mu_steer[2] = trapmf(abs_steer, 28.0f, 40.0f, 360.0f, 720.0f);     // SHARP

    // 3. Fuzzify Acceleration Magnitude
    float abs_accel = std::abs(acceleration);
    float mu_accel[3];
    mu_accel[0] = trapmf(abs_accel, -0.1f, 0.0f, 0.6f, 1.6f);          // SMOOTH
    mu_accel[1] = trimf(abs_accel, 1.2f, 2.2f, 3.2f);                  // MODERATE
    mu_accel[2] = trapmf(abs_accel, 2.8f, 4.0f, 25.0f, 50.0f);         // HARSH

    float sum_w = 0.0f;
    float sum_w_z = 0.0f;
    float max_w = -1.0f;
    
    int dominant_rule_idx = 0;
    int speed_idx_max = 0;
    int steer_idx_max = 0;
    int accel_idx_max = 0;

    // Evaluate Sugeno Rules
    for (int s = 0; s < 3; ++s) {
        for (int st = 0; st < 3; ++st) {
            for (int a = 0; a < 3; ++a) {
                int rule_idx = s * 9 + st * 3 + a;
                
                // Product inference for ANFIS rule weight
                float w = mu_speed[s] * mu_steer[st] * mu_accel[a];
                float z = RULE_OUTPUTS[rule_idx];

                sum_w += w;
                sum_w_z += w * z;

                if (w > max_w) {
                    max_w = w;
                    dominant_rule_idx = rule_idx;
                    speed_idx_max = s;
                    steer_idx_max = st;
                    accel_idx_max = a;
                }
            }
        }
    }

    float final_score = 0.0f;
    if (sum_w > 0.0001f) {
        final_score = sum_w_z / sum_w;
    }

    AnfisOutput out;
    out.safety_score = final_score;
    out.dominant_rule_index = dominant_rule_idx;
    
    // Construct XAI Explanation
    float rule_weight = RULE_OUTPUTS[dominant_rule_idx];
    std::string risk_level = "LOW";
    if (rule_weight > 60.0f) {
        risk_level = "HIGH";
    } else if (rule_weight > 25.0f) {
        risk_level = "MODERATE";
    }

    out.rule_text = "IF Speed Var is " + speed_labels_[speed_idx_max] +
                    " & Steer is " + steering_labels_[steer_idx_max] +
                    " & Accel is " + accel_labels_[accel_idx_max] +
                    " THEN Risk level is " + risk_level;
    
    out.confidence = std::fmax(0.1f, std::fmin(0.99f, 1.0f - (final_score / 200.0f)));
    
    ATrace_endSection();
    return out;
}
