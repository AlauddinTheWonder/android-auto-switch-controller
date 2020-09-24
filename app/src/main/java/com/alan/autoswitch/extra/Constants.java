package com.alan.autoswitch.extra;

import java.util.UUID;

public class Constants {

    public final static String TAG = "ALAN_AUTO_SWITCH";

    public static final String DEVICE_USB = "com.device.usb";
    public static final String DEVICE_BLUETOOTH = "com.device.bluetooth";
    public static final String EXTRA_DEVICE_TYPE = "com.alan.extra.device.type";
    public static final String EXTRA_TERMINAL_MODE = "com.alan.extra.terminal.mode";

    public static final String ACTION_USB_PERMISSION = "com.alan.usb.permission";
    public static final int DEFAULT_BAUD_RATE = 9600;
    public static final byte DEFAULT_DELIMITER = '\n';

    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_DISCOVERABLE_BT = 2;
    public static final UUID BT_DEV_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final int COMMAND_TIMEOUT = 5000;
    public static final int COMMAND_GAP_TIME = 500;
    public static final int COMMAND_MAX_RETRY = 2;

}
