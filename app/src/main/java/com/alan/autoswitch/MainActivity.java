package com.alan.autoswitch;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.alan.autoswitch.classes.Device;
import com.alan.autoswitch.classes.DeviceListener;
import com.alan.autoswitch.classes.MyDevice;
import com.alan.autoswitch.extra.Command;
import com.alan.autoswitch.extra.Constants;
import com.alan.autoswitch.extra.Debounce;
import com.alan.autoswitch.extra.NumberPickerDialog;
import com.alan.autoswitch.extra.ProgressDialog;
import com.alan.autoswitch.extra.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements DeviceListener {

    Context context;

    private Device myDevice;

    private Debounce scrollDebounce;
    private ProgressDialog progressDialog;
    private NumberPickerDialog numberPickerDialog;

    private SimpleDateFormat utcDF, localDF;

    private boolean TerminalMode = false;

    private int NumOfSwitches = 0;
    private int MaxSettingsCount = 0;
    private int[] PinSettingsArray;
    private long DeviceTimestamp = 0;

    final Handler commandHandler = new Handler();
    Thread counterThread = null;
    int commandRetry = 0;

    // Views
    private ScrollView scrollView;
    private EditText terminalInput;
    private TextView logView, deviceNameView, currentTimeView, deviceTimeView;
    private LinearLayout timeView, terminalView;


    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        TerminalMode = getIntent().getBooleanExtra(Constants.EXTRA_TERMINAL_MODE, false);

        String deviceType = getIntent().getStringExtra(Constants.EXTRA_DEVICE_TYPE);
        if (deviceType != null && MyDevice.validateDevice(deviceType)) {
            this.myDevice = MyDevice.getDevice(this, deviceType);
        } else {
            exitScreen("Invalid Device");
        }

        scrollDebounce = new Debounce(1000);
        progressDialog = new ProgressDialog(this);
        numberPickerDialog = new NumberPickerDialog(this,"Select Hour");

        localDF = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
        utcDF = new SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault());
        utcDF.setTimeZone(TimeZone.getTimeZone("GMT"));

        scrollView =  findViewById(R.id.log_scroll_view);
        logView = findViewById(R.id.log_view);

        terminalView = findViewById(R.id.terminal_view);
        terminalInput = findViewById(R.id.terminal_input);

        deviceNameView = findViewById(R.id.device_name_view);

        timeView = findViewById(R.id.time_view);
        currentTimeView = findViewById(R.id.current_time);
        deviceTimeView = findViewById(R.id.device_time);


    }

    @Override
    protected void onStart() {
        super.onStart();
        myDevice.setListener(this);
        myDevice.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        myDevice.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
        myDevice.onExit();
    }

    @Override
    public void onInfo(String text) {
        log(text);
    }

    @Override
    public void onDeviceConnect(String name) {
        String str = getString(R.string.connected_str, name);
        log(str);
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
        deviceNameView.setText(str);
        deviceNameView.setTextColor(getResources().getColor(R.color.deviceConnected));

        if (TerminalMode) {
            terminalView.setVisibility(View.VISIBLE);
        }
        else {
            getInfoFromDevice();
        }
    }

    @Override
    public void onDeviceDisconnect() {
        deviceNameView.setText(getString(R.string.not_connected_str));
        deviceNameView.setTextColor(getResources().getColor(R.color.grey));
        log("Device disconnected");
        Toast.makeText(this, "Device disconnected", Toast.LENGTH_SHORT).show();
        Command.reset();

        if (counterThread != null) {
            counterThread.interrupt();
        }
        hideDetailView();
    }

    @Override
    public void onExitRequest() {
        finish();
    }

    @Override
    public void onProgressStart() {
        progressDialog.show();
    }

    @Override
    public void onProgressStop() {
        progressDialog.hide();
    }

    @Override
    public void onReceivedFromDevice(String data) {
        log("<< " + data);

        if (!TerminalMode) {
            onDataReceived(data);
        }
    }

    public void onTerminalSendBtnClick(View view) {
        String text = terminalInput.getText().toString();
        log(">> " + text);
        myDevice.send(text);
        terminalInput.setText("");
    }

    public void onSyncDeviceTimeClick(View view) {
        long timestampToUTC = Utils.getCurrentTimeUTC() + 1;
        log("Syncing device time with: " + timestampToUTC);
        runCommand(Command.SET_TIME, timestampToUTC);
    }

    private void log(final String text) {
        Log.i(Constants.TAG, text);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logView.append(text);
                logView.append("\n");

                scrollDebounce.attempt(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    private void exitScreen(final String msg) {
        progressDialog.hide();
        Log.i(Constants.TAG, msg);
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        finish();
    }

    private void startCounter() {
        counterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted()){
                    try {
                        displayDateTime();
                        displayDeviceDateTime();
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        counterThread.start();
    }

    private void displayDateTime() {
        String currentDateTimeString = localDF.format(new Date());
        String str = getString(R.string.current_time_str, currentDateTimeString);
        currentTimeView.setText(str);
    }

    private void displayDeviceDateTime() {
        String currentDateTimeString = "Not connected";
        if (DeviceTimestamp > 100000) {
            DeviceTimestamp++;

            Date dt = new Date(DeviceTimestamp * 1000);
            currentDateTimeString = utcDF.format(new Date(DeviceTimestamp * 1000));
        }

        String str = getString(R.string.dev_time_str, currentDateTimeString);
        deviceTimeView.setText(str);
    }

    private void showDetailViews() {
        timeView.setVisibility(View.VISIBLE);
        // TODO: show content view

        startCounter();
    }

    private void hideDetailView() {
        timeView.setVisibility(View.GONE);
        // TODO: hide content view
    }

    private void updateViewWithDetails() {
        // TODO: update pin views

        for (int i = 0; i < MaxSettingsCount; i++) {
//            switchLayoutsViews[i].setVisibility(View.VISIBLE);

            int j = (i * 3) + 1;
            String str = PinSettingsArray[j] + " => " + PinSettingsArray[j+1] + ":" + PinSettingsArray[j+2];
            Log.i(Constants.TAG, str);

//            for (int j = (i * 3); j < (i * 3 + 3); j++) {
//                int rId = (j % 2 == 0) ? R.string.switch_on : R.string.switch_off;
//                String onOffStr = getString(rId, pinsValueArray[j + 1]);
//                switchOnOffViews[j].setText(onOffStr);
//            }
        }
    }



    private void getInfoFromDevice() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                log("Getting info from device");
                runCommand(Command.GET_ALL_DATA, Command.NO_COMMAND_VALUE);
            }
        }, 1000);
    }

    private void runCommand(final int command, final long param) {
        progressDialog.show();

        Command.set(command, param);
        String commandStr = command + ":" + param;

        myDevice.send(commandStr);
        log(">> " + command + ":" + param);

       commandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (commandRetry >= Constants.COMMAND_MAX_RETRY) {
                    Command.reset();
                    commandRetry = 0;
                    progressDialog.hide();

                    if (TerminalMode) {
                        log("Command timeout");
                    } else {
                        exitScreen("Command timeout. Please try again.");
                    }
                }
                else {
                    log("Retrying...");
                    commandRetry++;
                    long newParam = param;

                    if (command == Command.SET_TIME) {
                        newParam = Utils.getCurrentTimeUTC() + 1;
                    }
                    runCommand(command, newParam);
                }
            }
        }, Constants.COMMAND_TIMEOUT);
    }

    private void onDataReceived(String data) {
        if (data.isEmpty()) {
            log("Empty data received");
            return;
        }
        int command = Command.COMMAND;
        long param = Command.PARAM;

        commandHandler.removeCallbacksAndMessages(null);
        commandRetry = 0;

        switch (command) {

            case Command.GET_ALL_DATA:
                parseInitialConfigData(data);
                break;

            case Command.GET_TIME:
                DeviceTimestamp = Long.parseLong(data);
                break;

            case Command.SET_TIME:
                DeviceTimestamp = Long.parseLong(data);
                break;

            case Command.GET_SWITCH_NUM:
                NumOfSwitches = Integer.parseInt(data);;

                if (NumOfSwitches <= 0) {
                    exitScreen("No valid number of switch found: " + data);
                }
                break;

            case Command.GET_MAX_SETTINGS:
                MaxSettingsCount = Integer.parseInt(data);;

                if (MaxSettingsCount > 0) {
                    PinSettingsArray = new int[MaxSettingsCount * 3 + 1];
                    PinSettingsArray[0] = NumOfSwitches; // Pin setting starts from 1 index.
                }
                break;

            case Command.GET_SWITCH_VALUE: // Get switch hours or drift time value
                PinSettingsArray[(int) param] = Integer.parseInt(data);;
                break;

            default:
                if (command > 0) { // Set switch hours or drift time value
                    PinSettingsArray[command] = Integer.parseInt(data);;
                }
                break;
        }

        progressDialog.hide();
        updateViewWithDetails();
    }

    private void parseInitialConfigData(String data) {
        String[] chunks = data.split("\\|");
        if (chunks.length > 1) {

            for (String chunk : chunks) {
                if (!chunk.isEmpty()) {
                    String[] commands = chunk.split(":");

                    if (commands.length < 2) {
                        return;
                    }
                    int command = Integer.parseInt(commands[0]);
                    int intData = Integer.parseInt(commands[1]);

                    if (command > 0) {
                        switch (command) {
                            case Command.GET_TIME:
                                DeviceTimestamp = Long.parseLong(commands[1]);
                                break;

                            case Command.GET_SWITCH_NUM:
                                NumOfSwitches = intData;
                                break;

                            case Command.GET_MAX_SETTINGS:
                                MaxSettingsCount = intData;

                                if (MaxSettingsCount > 0) {
                                    PinSettingsArray = new int[MaxSettingsCount * 3 + 1];
                                    PinSettingsArray[0] = NumOfSwitches; // Pin setting starts from 1 index.
                                }
                                break;

                            default:
                                PinSettingsArray[command] = intData;
                                break;
                        }
                    }
                }
            }
            Command.reset();
            showDetailViews();
        } else {
            exitScreen("Invalid data received. Please try again!");
        }
    }

}
