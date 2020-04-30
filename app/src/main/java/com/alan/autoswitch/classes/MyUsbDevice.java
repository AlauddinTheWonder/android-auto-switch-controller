package com.alan.autoswitch.classes;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.alan.autoswitch.extra.Constants;
import com.alan.autoswitch.extra.Utils;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

public class MyUsbDevice implements Device {
    private Activity context;

    private DeviceListener listener;

    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbSerialDevice serial;

    public MyUsbDevice(Activity context) {
        this.context = context;
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

            IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        this.context.registerReceiver(receiver, filter);
    }

    @Override
    public void setListener(DeviceListener listener) {
        this.listener = listener;
    }

    @Override
    public void connect() {
        UsbDevice device = getAttachedArduino();
        if (device != null) {
            startUsbConnection(device);
        } else {
            info("Please plug an Arduino via OTG.");
        }
    }

    @Override
    public void disconnect() {
        if (serial != null) {
            serial.close();
        }
    }

    @Override
    public void onExit() {
        if (serial != null) {
            serial.close();
        }
        this.context.unregisterReceiver(receiver);
    }

    @Override
    public void send(String data) {
        if (serial != null) {
            serial.write(data.getBytes());
        }
    }

    @Override
    public void send(int data) {
        send(String.valueOf(data));
    }

    private void info(String text) {
        if (listener != null) {
            listener.onInfo(text);
        }
    }

    private void startUsbConnection(UsbDevice device) {
        usbDevice = device;

        Intent intent = new Intent(Constants.ACTION_USB_PERMISSION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        usbManager.requestPermission(usbDevice, pendingIntent);
        info("Initiating access permission...");
    }

    private UsbDevice getAttachedArduino() {
        HashMap<String, UsbDevice> map = usbManager.getDeviceList();
        for (UsbDevice device : map.values()) {
            return device;
        }
        return null;
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (Constants.ACTION_USB_PERMISSION.equals(action)) {
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);

                if (granted && usbDevice != null) {
                    UsbDeviceConnection connection = usbManager.openDevice(usbDevice);
                    serial = UsbSerialDevice.createUsbSerialDevice(usbDevice, connection);

                    if (serial != null) {
                        if (serial.open()) {
                            serial.setBaudRate(Constants.DEFAULT_BAUD_RATE);
                            serial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serial.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serial.setParity(UsbSerialInterface.PARITY_NONE);
                            serial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serial.read(serialCallback);

                            if (listener != null) {
                                String devName = usbDevice.getProductName();
                                if (devName == null) {
                                    devName = usbDevice.getDeviceName();
                                }
                                listener.onDeviceConnect(devName);
                            }
                            info("Device connected via Serial port: " + Constants.DEFAULT_BAUD_RATE);
                        } else {
                            info("Can't open Serial");
                        }
                    } else {
                        info("Invalid Device");
                    }
                } else {
                    if (listener != null) {
                        listener.onExitRequest();
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    info("Device attached");
                    startUsbConnection(device);
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                disconnect();
                if (listener != null) {
                    listener.onDeviceDisconnect();
                }
            }

        }
    };

    private UsbSerialInterface.UsbReadCallback serialCallback = new UsbSerialInterface.UsbReadCallback() {
        ByteArrayOutputStream readStream = new ByteArrayOutputStream();

        @Override
        public void onReceivedData(byte[] bytes) {
            if (bytes.length > 0) {
                int idx = Utils.getIndexByDelim(bytes);

                if (idx == -1) {
                    readStream.write(bytes, 0, bytes.length);
                } else {
                    if (idx > 0) {
                        readStream.write(bytes, 0, idx);
                    }

                    if (listener != null) {
                        listener.onReceivedFromDevice(readStream.toString().trim());
                    }
                    readStream.reset();

                    if (idx < bytes.length - 1) {
                        readStream.write(bytes, idx + 1, bytes.length - (idx + 1));
                    }
                }
            }
        }
    };
}
