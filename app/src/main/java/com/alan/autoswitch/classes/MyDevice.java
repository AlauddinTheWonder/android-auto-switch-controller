package com.alan.autoswitch.classes;

import android.app.Activity;

import com.alan.autoswitch.extra.Constants;

public class MyDevice {

    public static Device getDevice(Activity context, String deviceType) {
        if (validateDevice(deviceType)) {
            if (deviceType.equals(Constants.DEVICE_USB)) {
                return new MyUsbDevice(context);
            }
            else if (deviceType.equals(Constants.DEVICE_BLUETOOTH)) {
                return new MyBluetoothDevice(context);
            }
        }
        return null;
    }

    public static Boolean validateDevice(String deviceType) {
        return deviceType.equals(Constants.DEVICE_USB) || deviceType.equals(Constants.DEVICE_BLUETOOTH);
    }

}
