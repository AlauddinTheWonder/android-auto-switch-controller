package com.alan.autoswitch.classes;

public interface DeviceListener {
    void onInfo(String text);
    void onReceivedFromDevice(String data);
    void onDeviceConnect(String name);
    void onDeviceDisconnect();
    void onExitRequest();
    void onProgressStart();
    void onProgressStop();
}
