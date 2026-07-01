#pragma once
#include <cstdint>

#pragma pack(push, 1)
struct RawCanFrame {
    uint32_t can_id;
    uint8_t can_dlc;
    uint8_t padding[3];
    uint8_t data[8];
};
#pragma pack(pop)

struct DecodedState {
    float speed = 0.0f;
    float brake = 0.0f;
    float steering = 0.0f;
    float rpm = 0.0f;
    uint64_t timestamp = 0;
};

class CanDecoder {
public:
    // Decodes raw CAN frames, updating the corresponding fields in state.
    // Returns true if the frame ID was recognized and decoded.
    static bool decode(const RawCanFrame& raw, DecodedState& state);
};
