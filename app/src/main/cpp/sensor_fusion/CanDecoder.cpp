#include "CanDecoder.h"
#include <cstring>

bool CanDecoder::decode(const RawCanFrame& raw, DecodedState& state) {
    if (raw.can_id == 0x101) {
        if (raw.can_dlc >= 8) {
            std::memcpy(&state.speed, raw.data, 4);
            std::memcpy(&state.brake, raw.data + 4, 4);
            return true;
        }
    } else if (raw.can_id == 0x102) {
        if (raw.can_dlc >= 8) {
            std::memcpy(&state.steering, raw.data, 4);
            std::memcpy(&state.rpm, raw.data + 4, 4);
            return true;
        }
    }
    return false;
}
