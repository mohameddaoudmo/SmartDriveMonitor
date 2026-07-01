import csv
import socket
import struct
import time
import sys
import os
import argparse
import subprocess

# CAN frame struct format:
# canid_t (4 bytes) + can_dlc (1 byte) + pad/res (3 bytes) + data (8 bytes) = 16 bytes
CAN_FRAME_FORMAT = "<IB3x8s"
UDP_IP = "127.0.0.1"
UDP_PORT = 5005

def pack_can_frame(can_id, data_bytes):
    # data_bytes should be exactly 8 bytes
    return struct.pack(CAN_FRAME_FORMAT, can_id, len(data_bytes), data_bytes)

def inject_vhal_speed(speed):
    # VehicleProperty::PERF_VEHICLE_SPEED = 291504644 (0x11600207)
    cmd = [
        "adb", "shell", "dumpsys",
        "android.hardware.automotive.vehicle.IVehicle/default",
        "--inject-event", f"0x11600207,0,0,{speed}"
    ]
    try:
        subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    except Exception as e:
        print(f"Failed to inject VHAL event: {e}")

def replay_csv(csv_path, vhal_mode=False):
    if not os.path.exists(csv_path):
        print(f"Error: CSV file not found at {csv_path}")
        sys.exit(1)

    print(f"Starting replay from {csv_path}...")
    if vhal_mode:
        print("Mode: VHAL Injection via ADB")
    else:
        print(f"Mode: Raw UDP CAN Injection to {UDP_IP}:{UDP_PORT}")
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    with open(csv_path, mode='r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        last_timestamp = None
        
        for row in reader:
            try:
                # Parse inputs
                ts = int(row['timestamp'])
                speed = float(row['speed'])
                rpm = float(row['rpm'])
                steering = float(row['steeringAngle'])
                brake = float(row['brake'])
            except (ValueError, KeyError) as e:
                print(f"Skipping malformed row: {row}. Error: {e}")
                continue

            # Sleep to match real-world timing
            if last_timestamp is not None:
                delta_ms = ts - last_timestamp
                if 0 < delta_ms < 1000: # Ignore negative deltas or massive gaps
                    time.sleep(delta_ms / 1000.0)
                else:
                    time.sleep(0.01) # Default to 10ms (100 fps)
            
            last_timestamp = ts

            if vhal_mode:
                inject_vhal_speed(speed)
                print(f"[{ts}] Injected VHAL Speed: {speed:.2f} m/s", end='\r')
            else:
                # Construct CAN ID 0x101: Speed (float) + Brake (float)
                data_101 = struct.pack("<ff", speed, brake)
                frame_101 = pack_can_frame(0x101, data_101)
                sock.sendto(frame_101, (UDP_IP, UDP_PORT))

                # Construct CAN ID 0x102: Steering (float) + RPM (float)
                data_102 = struct.pack("<ff", steering, rpm)
                frame_102 = pack_can_frame(0x102, data_102)
                sock.sendto(frame_102, (UDP_IP, UDP_PORT))

                print(f"[{ts}] Sent CAN 0x101 (Speed: {speed:.2f}, Brake: {brake:.2f}) | 0x102 (Steer: {steering:.2f}, RPM: {rpm:.0f})", end='\r')

    print("\nReplay finished successfully.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="SmartDriveMonitor Data Replayer")
    parser.add_argument("csv_file", nargs='?', default="perfect_driving_data.csv", help="Path to driving data CSV")
    parser.add_argument("--vhal-mode", action="store_true", help="Inject directly into AAOS VHAL via adb")
    
    args = parser.parse_args()
    replay_csv(args.csv_file, args.vhal_mode)
