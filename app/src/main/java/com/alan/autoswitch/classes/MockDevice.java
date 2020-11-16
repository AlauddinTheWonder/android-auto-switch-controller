package com.alan.autoswitch.classes;

import android.app.Activity;
import android.os.Handler;

import com.alan.autoswitch.classes.interfaces.Device;
import com.alan.autoswitch.classes.interfaces.DeviceListener;
import com.alan.autoswitch.extra.Command;
import com.alan.autoswitch.extra.Utils;

public class MockDevice implements Device {
    private DeviceListener listener;

    private int TOTAL_SWT = 3;
    private int MAX_SETTINGS = 10;
    private int MAX_ROM_VAL = 255;

    private int[] EEPROM = new int[MAX_SETTINGS * 3];

    MockDevice(Activity context) {
    }

    @Override
    public void setListener(DeviceListener listener) {
        this.listener = listener;
    }

    @Override
    public void connect() {
        if (listener != null) {
            listener.onProgressStart();
        }

        onDeviceConnected();
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void onExit() {

    }

    @Override
    public void send(String data) {
        if (data.isEmpty()) {
            return;
        }
        String[] commandList = data.split("\\|");

        if (commandList.length == 0) {
            return;
        }

        for (String s : commandList) {
            if (!s.isEmpty()) {
                String[] commands = s.split((":"));

                if (commands.length == 2) {
                    int command = Integer.parseInt(commands[0]);
                    long value = Long.parseLong(commands[1]);

                    if (command > 0 && value >= 0) {
                        executeCommand(command, value);
                    }
                }
            }
        }
    }

    @Override
    public void send(int data) {
        send(String.valueOf(data));
    }

    private long getTimeNow() {
        return Utils.getCurrentTimeUTC();
    }

    private int getROMvalue(int index) {
        return EEPROM[index];
    }

    private void setROMvalue(int addr, int val) {
        EEPROM[addr] = val;
    }

    private void info(String text) {
        if (listener != null) {
            listener.onInfo(text);
        }
    }

    private void onDeviceConnected() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onDeviceConnect("Mock Device 007");
                    listener.onProgressStop();
                }
            }
        }, 2000);
    }

    private void SerialPrint(String data) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onReceivedFromDevice(data.trim());
                }
            }
        }, 1000);
    }

    private void SerialPrint(long data) {
        SerialPrint(String.valueOf(data));
    }

    private void executeCommand(int command, long value) {
        if (command == Command.PING_BACK) {
            SerialPrint(Command.PING_BACK);
        }
        else if (command == Command.GET_ALL_DATA) {
            StringBuilder res = new StringBuilder();
            res.append(Command.GET_TIME + ":").append(getTimeNow()).append("|");
            res.append(Command.GET_SWITCH_NUM + ":").append(TOTAL_SWT).append("|");
            res.append(Command.GET_MAX_SETTINGS + ":").append(MAX_SETTINGS).append("|");

            int cnt = 0;
            for (int i = 0; i < MAX_SETTINGS; i++) {
                for (int j = 0; j <= 2; j++) {
                    res.append(cnt).append(":").append(getROMvalue(cnt)).append("|");
                }
            }
            res.append("\n");

            SerialPrint(res.toString());
        }
        else if (command == Command.GET_TIME) {
            SerialPrint(getTimeNow());
        }
        else if (command == Command.SET_TIME && value > 0) {
            SerialPrint(value);
        }
        else if (command == Command.GET_SWITCH_NUM) {
            SerialPrint(TOTAL_SWT);
        }
        else if (command == Command.GET_MAX_SETTINGS) {
            SerialPrint(MAX_SETTINGS);
        }
        else if (command == Command.GET_SWITCH_VALUE && value >= 0 && value <= MAX_ROM_VAL) {
            SerialPrint(getROMvalue((int) value));
        }
        else if (command >= 0 && command <= MAX_ROM_VAL && value >= 0 && value <= MAX_ROM_VAL) {
            setROMvalue(command, (int) value);
            SerialPrint(getROMvalue(command));
        }
        else {
            SerialPrint(-1);
        }
    }
}
