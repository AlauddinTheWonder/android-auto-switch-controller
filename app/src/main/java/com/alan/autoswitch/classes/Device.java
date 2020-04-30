package com.alan.autoswitch.classes;

public interface Device {
    void setListener(DeviceListener listener);
    void connect();
    void disconnect();
    void onExit();
    void send(String data);
    void send(int data);
}
