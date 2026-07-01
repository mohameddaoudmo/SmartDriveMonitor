package com.smartcabin.nativecore;

parcelable DriverStateParcelable {
    float rawSpeed;
    float fusedSpeed;
    float acceleration;
    float brake;
    float steeringAngle;
    float rpm;
    float safetyScore;
    int dominantRuleIndex;
    String ruleText;
    float confidence;
    long timestamp;
}
