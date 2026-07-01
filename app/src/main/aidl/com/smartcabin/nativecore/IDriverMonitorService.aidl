package com.smartcabin.nativecore;

import com.smartcabin.nativecore.IDriverMonitorListener;
import com.smartcabin.nativecore.DriverStateParcelable;

interface IDriverMonitorService {
    void registerListener(IDriverMonitorListener listener);
    void unregisterListener(IDriverMonitorListener listener);
    void injectSensorData(float speed, float brake, float steeringAngle, float rpm);
    DriverStateParcelable getLatestState();
}
