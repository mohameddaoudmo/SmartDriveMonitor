#include <gtest/gtest.h>
#include "../../main/cpp/anfis_engine/AnfisEngine.h"

TEST(AnfisEngineTest, DetectsHighDrowsinessWithSlowReaction) {
    AnfisEngine engine;
    // High speed variance, high steering angle, low acceleration
    auto result = engine.evaluate(/*speed_variance=*/80.0f, /*steering_angle=*/35.0f, /*acceleration=*/-0.5f);
    
    // Safety score should be low (high risk)
    EXPECT_LT(result.safety_score, 50.0f);
    EXPECT_NE(result.rule_text, "");
}

TEST(AnfisEngineTest, HandlesStableInputGracefully) {
    AnfisEngine engine;
    // Low speed variance, zero steering, steady acceleration
    auto result = engine.evaluate(/*speed_variance=*/5.0f, /*steering_angle=*/0.0f, /*acceleration=*/0.0f);
    
    // Safety score should be high
    EXPECT_GT(result.safety_score, 80.0f);
}

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}
