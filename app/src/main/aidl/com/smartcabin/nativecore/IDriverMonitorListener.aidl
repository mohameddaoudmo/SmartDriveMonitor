package com.smartcabin.nativecore;

import com.smartcabin.nativecore.DriverStateParcelable;

interface IDriverMonitorListener {
    void onDriverStateUpdated(in DriverStateParcelable state);
}
