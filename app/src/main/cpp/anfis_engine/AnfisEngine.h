#pragma once
#include <string>
#include <vector>

struct AnfisOutput {
    float safety_score;        // 0 to 100 (0 = Safe, 100 = Dangerous)
    int dominant_rule_index;   // The rule ID that has the highest firing strength
    std::string rule_text;     // Rule description (XAI)
    float confidence;          // Normalized firing strength (0.0 to 1.0)
};

class AnfisEngine {
public:
    AnfisEngine();

    // Runs inference on fuzzy inputs.
    AnfisOutput evaluate(float speed_variance, float steering_angle, float acceleration);

private:
    // Membership function shapes
    float trimf(float x, float a, float b, float c);
    float trapmf(float x, float a, float b, float c, float d);

    std::vector<std::string> speed_labels_;
    std::vector<std::string> steering_labels_;
    std::vector<std::string> accel_labels_;
};
