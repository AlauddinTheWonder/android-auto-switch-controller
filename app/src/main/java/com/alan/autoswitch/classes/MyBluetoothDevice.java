package com.alan.autoswitch.classes;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.alan.autoswitch.extra.Constants;
import com.alan.autoswitch.extra.ProgressDialog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MyBluetoothDevice implements Device {
    private Activity context;

    private BluetoothAdapter btAdapter;
    private BluetoothDevice btDevice;
    private BluetoothSocket btSocket;

    private InputStream btInputStream;
    private OutputStream btOutputStream;

    private AlertDialog.Builder btListBuilder;

    private boolean listenBT = false;
    private byte[] readBuffer;
    private int readBufferPosition;

    private DeviceListener listener;


    public MyBluetoothDevice(Activity context) {
        this.context = context;
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.context.registerReceiver(receiver, filter);

        btListBuilder = new AlertDialog.Builder(context);
        btListBuilder.setTitle("Choose a Device");
        btListBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (listener != null) {
                    listener.onExitRequest();
                }
            }
        });
    }

    @Override
    public void setListener(DeviceListener listener) {
        this.listener = listener;
    }

    @Override
    public void connect() {
        checkBluetoothState();
    }

    @Override
    public void disconnect() {
        closeSocket();
    }

    @Override
    public void onExit() {
        closeSocket();
        disableBluetooth();
        this.context.unregisterReceiver(receiver);
    }

    @Override
    public void send(String data) {
        try {
            if (isConnected()) {
                btOutputStream.write(data.getBytes());
            }
        } catch (IOException ignored) {}
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

    private boolean isEnabled() {
        if (btAdapter == null) {
            return false;
        }
        return btAdapter.isEnabled();
    }

    private boolean isConnected() {
        return isEnabled() && btSocket != null && btSocket.isConnected();
    }

    private void enableBluetooth() {
        if (!isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            context.startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
            btAdapter.enable();
            Toast.makeText(context, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void disableBluetooth() {
        if (isEnabled()) {
            btAdapter.disable();
            Toast.makeText(context, "Bluetooth disabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkBluetoothState() {
        if (btAdapter == null) {
            Toast.makeText(context, "Device not supported", Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onExitRequest();
            }
        } else {
            if (isEnabled()) {
                listBtDevices();
            }
            else {
                enableBluetooth();
            }
        }
    }

    private void closeSocket() {
        try {
            if (listenBT) {
                listenBT = false;
                if (listener != null) {
                    listener.onDeviceDisconnect();
                }
            }
            if (btSocket != null) {
                btSocket.close();
            }
            if (btInputStream != null) {
                btInputStream.close();
            }
            if (btOutputStream != null) {
                btOutputStream.close();
            }
        } catch (IOException ignored) {}
    }

    private List<BluetoothDevice> getDeviceList() {
        if (isEnabled()) {
            Set<BluetoothDevice> devices = btAdapter.getBondedDevices();
            return new ArrayList<>(devices);
        }
        return new ArrayList<BluetoothDevice>();
    }

    private void listBtDevices() {
        if (!isEnabled()) {
            return;
        }

        final List<BluetoothDevice> btList = getDeviceList();

        if (btList.size() > 0) {
            String[] lists = new String[btList.size()];

            for (int i = 0; i < btList.size(); i++) {
                BluetoothDevice device = btList.get(i);
                lists[i] = device.getName() + "(" + device.getAddress() + ")";
            }

            btListBuilder.setItems(lists, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    onDeviceSelect(btList.get(which));
                }
            });

            AlertDialog dialog = btListBuilder.create();
            dialog.show();
        }
        else {
            info("No paired device found.");
        }
    }

    private void onDeviceSelect(BluetoothDevice device) {
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            info("Selected device: " + device.getName());
            info("Connecting to device");

            if (listener != null) {
                listener.onProgressStart();
            }
            connectToSocket(device);
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON) {
                    checkBluetoothState();
                }
                else if (state == BluetoothAdapter.STATE_OFF) {
                    disconnect();
                    info("Bluetooth disabled");
                }
            }
        }
    };

    private void connectToSocket(final BluetoothDevice bluetoothDevice) {
        closeSocket();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    btDevice = bluetoothDevice;
                    btSocket = btDevice.createRfcommSocketToServiceRecord(Constants.BT_DEV_UUID);
                    btAdapter.cancelDiscovery();
                    btSocket.connect();
                    btInputStream = btSocket.getInputStream();
                    btOutputStream = btSocket.getOutputStream();

                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null) {
                                listener.onDeviceConnect(btDevice.getName());
                                listener.onProgressStop();
                            }
                            beginListenForData();
                        }
                    });

                    Thread.currentThread().interrupt();

                } catch (IOException e) {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeSocket();
                            Toast.makeText(context, "Error in connecting device", Toast.LENGTH_SHORT).show();
                            if (listener != null) {
                                listener.onExitRequest();
                            }
                        }
                    });

                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    private void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        listenBT = true;
        readBufferPosition = 0;
        readBuffer = new byte[1024];

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while(!Thread.currentThread().isInterrupted())
                {
                    if (!listenBT) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    try
                    {
                        int bytesAvailable = btInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];

                            btInputStream.read(packetBytes);

                            for(int i = 0; i < bytesAvailable; i++)
                            {
                                byte b = packetBytes[i];

                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, StandardCharsets.US_ASCII);
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            if (listener != null) {
                                                listener.onReceivedFromDevice(data.trim());
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        handler.post(new Runnable() {
                            public void run() {
                                closeSocket();
                            }
                        });
                    }
                }
            }
        }).start();
    }

}
