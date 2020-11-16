package com.alan.autoswitch.classes;

import android.app.Activity;

import com.alan.autoswitch.classes.interfaces.Device;
import com.alan.autoswitch.extra.Constants;

public class MyDevice {

    public static Device getDevice(Activity context, String deviceType) {
        if (validateDevice(deviceType)) {
            if (deviceType.equals(Constants.DEVICE_USB)) {
                return new MyUsbDevice(context, Constants.DEFAULT_BAUD_RATE);
            }
            else if (deviceType.equals(Constants.DEVICE_BLUETOOTH)) {
                return new MyBluetoothDevice(context);
            }
            else if (deviceType.equals(Constants.DEVICE_MOCK_DEVICE)) {
                return new MockDevice((context));
            }
        }
        return null;
    }

    public static Boolean validateDevice(String deviceType) {
        return deviceType.equals(Constants.DEVICE_USB)
                || deviceType.equals(Constants.DEVICE_BLUETOOTH)
                || deviceType.equals(Constants.DEVICE_MOCK_DEVICE);
    }

}
