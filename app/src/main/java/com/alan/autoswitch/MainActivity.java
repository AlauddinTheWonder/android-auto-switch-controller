package com.alan.autoswitch;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
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

    private Device myDevice;

    private Debounce scrollDebounce;
    private ProgressDialog progressDialog;
    private NumberPickerDialog numberPickerDialog;

    private SimpleDateFormat utcDF, localDF;

    private boolean gettingInfo = false;
    private boolean terminalMode = false;

    private int numOfPins = 0;
    private int currentPinGetCnt = 0;
    private int[] pinsValueArray;
    private long deviceTimestamp = 0;
    private int driftTimeValue = 0;

    // Views
    private ScrollView scrollView;
    private EditText terminalInput;
    private TextView logView, deviceNameView, currentTimeView, devTimeView, driftTimeView;
    private LinearLayout contentView, timeView, terminalView;
    // Switches views
    private int maxSwitchLayouts = 5;
    private LinearLayout[] switchLayoutsViews = new LinearLayout[maxSwitchLayouts];
    private TextView[] switchOnOffViews = new TextView[maxSwitchLayouts * 2];

    final Handler commandHandler = new Handler();
    Thread counterThread;
    int commandRetry = 0;


    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        terminalMode = getIntent().getBooleanExtra(Constants.EXTRA_TERMINAL_MODE, false);

        String deviceType = getIntent().getStringExtra(Constants.EXTRA_DEVICE_TYPE);
        if (deviceType != null && MyDevice.validateDevice(deviceType)) {
            this.myDevice = MyDevice.getDevice(this, deviceType);
        } else {
            Toast.makeText(this, "Invalid device", Toast.LENGTH_SHORT).show();
            finish();
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
        currentTimeView = findViewById(R.id.current_time);
        devTimeView = findViewById(R.id.dev_time);
        driftTimeView = findViewById(R.id.driftTime);
        contentView = findViewById(R.id.content_view);
        timeView = findViewById(R.id.time_view);

        // Switches view
        for (int i = 0; i < maxSwitchLayouts; i++) {
            int rLayout = getResources().getIdentifier("switch_" + i + "_view", "id", getApplicationContext().getPackageName());
            switchLayoutsViews[i] = findViewById(rLayout);

            for (int j = (i * 2); j < (i * 2 + 2); j++) {
                int rSwtOnOff = getResources().getIdentifier("pin_" + j + "_view", "id", getApplicationContext().getPackageName());
                switchOnOffViews[j] = findViewById(rSwtOnOff);
            }
        }
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

        if (terminalMode) {
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
        counterThread.interrupt();
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

        if (!terminalMode) {
            onDataReceived(data);
        }
    }

    public void onTerminalSendBtnClick(View view) {
        String text = terminalInput.getText().toString();
        log(">> " + text);
        myDevice.send(text);
        terminalInput.setText("");
    }

    public void onOnOffViewClick(View view) {
        final int index = Integer.parseInt(view.getTag().toString()) + 1;
        int value = pinsValueArray[index];

        numberPickerDialog.show(value,23, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                log("Updating switch value with " + which);
                runCommand(index, which);
            }
        });
    }

    public void onDriftTimeClick(View view) {
        numberPickerDialog.show(driftTimeValue,59, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                log("Updating drift time value with " + which);
                runCommand(Command.DRIFT_TIME_VALUE, which);
            }
        });
    }

    public void onSyncDeviceTimeClick(View view) {
        long timestampToUTC = Utils.getCurrentTimeUTC() + 1;
        log("Syncing device time with: " + timestampToUTC);
        runCommand(Command.SET_TIME, timestampToUTC);
    }

    private void log(final String text) {
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
        if (deviceTimestamp > 100000) {
            deviceTimestamp++;

            Date dt = new Date(deviceTimestamp * 1000);
            currentDateTimeString = utcDF.format(new Date(deviceTimestamp * 1000));
        }

        String str = getString(R.string.dev_time_str, currentDateTimeString);
        devTimeView.setText(str);
    }



    private void showDetailViews() {
        timeView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.VISIBLE);

        updateViewWithDetails();
        startCounter();
    }

    private void hideDetailView() {
        timeView.setVisibility(View.GONE);
        contentView.setVisibility(View.GONE);
    }

    private void updateViewWithDetails() {
        for (int i = 0; i < numOfPins; i++) {
            switchLayoutsViews[i].setVisibility(View.VISIBLE);

            for (int j = (i * 2); j < (i * 2 + 2); j++) {
                int rId = (j % 2 == 0) ? R.string.switch_on : R.string.switch_off;
                String onOffStr = getString(rId, pinsValueArray[j + 1]);
                switchOnOffViews[j].setText(onOffStr);
            }
        }
        driftTimeView.setText(String.valueOf(driftTimeValue));
    }

    private void runCommand(final int command, final long param) {
        progressDialog.show();
        Command.set(command, param);

        myDevice.send(command);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                myDevice.send(String.valueOf(param));
                log(">> " + command + ":" + param);
            }
        }, Constants.COMMAND_GAP_TIME);

        commandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (commandRetry >= Constants.COMMAND_MAX_RETRY) {
                    log("Command timeout");
                    Command.reset();
                    progressDialog.hide();
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

    private void getInfoFromDevice() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Getting info from device", Toast.LENGTH_LONG).show();
                Command.reset();
                startFetchingInfo();
            }
        }, 1000);
    }

    private void startFetchingInfo() {
        gettingInfo = true;
        progressDialog.show();

        switch (Command.COMMAND)
        {
            case Command.NO_COMMAND_VALUE:
                Command.set(Command.PING_BACK, Command.NO_COMMAND_VALUE);
                break;

            case Command.PING_BACK:
                Command.set(Command.GET_TIME, Command.NO_COMMAND_VALUE);
                break;

            case Command.GET_TIME:
                Command.set(Command.GET_SWITCH_NUM, Command.NO_COMMAND_VALUE);
                break;

            case Command.GET_SWITCH_NUM:
            case Command.GET_SWITCH_VALUE:
                currentPinGetCnt++;

                if (currentPinGetCnt <= numOfPins * 2) {
                    Command.set(Command.GET_SWITCH_VALUE, currentPinGetCnt);
                }
                else if (Command.PARAM != Command.DRIFT_TIME_VALUE) {
                    Command.set(Command.GET_SWITCH_VALUE, Command.DRIFT_TIME_VALUE);
                } else {
                    currentPinGetCnt = 0;
                    Command.reset(); // Finished here
                }
                break;

            default:
                Command.reset();
                break;
        }

        if (Command.COMMAND != Command.NO_COMMAND_VALUE) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    runCommand(Command.COMMAND, Command.PARAM);
                }
            }, Constants.COMMAND_GAP_TIME);
        } else {
            // stop getting values from device as all details are fetched
            Command.reset();
            gettingInfo = false;
            showDetailViews();
            progressDialog.hide();
        }
    }

    private void onDataReceived(String data) {
        if (data.isEmpty()) {
            return;
        }
        int command = Command.COMMAND;
        long param = Command.PARAM;

        commandHandler.removeCallbacksAndMessages(null);
        commandRetry = 0;
        boolean isFailed = false;

        switch (command) {

            case Command.PING_BACK:
                if (!data.equals(String.valueOf(Command.PING_BACK))) {
                    parseInitialConfigData(data);
                }
                break;

            case Command.GET_TIME:
                deviceTimestamp = Long.parseLong(data);
                break;

            case Command.SET_TIME:
                deviceTimestamp = Long.parseLong(data);
                break;

            case Command.GET_SWITCH_NUM:
                numOfPins = Integer.parseInt(data);

                if (numOfPins <= 0) {
                    log("No valid number of switch found: " + data);
                    isFailed = true;
                } else {
                    pinsValueArray = new int[numOfPins * 2 + 1];
                    pinsValueArray[0] = numOfPins;
                }
                break;

            case Command.GET_SWITCH_VALUE: // Get switch hours or drift time value
                int intData = Integer.parseInt(data);
                if (param == Command.DRIFT_TIME_VALUE) {
                    driftTimeValue = intData;
                } else {
                    pinsValueArray[(int) param] = intData;
                }
                break;

            default:
                if (command > 0 && command <= Command.DRIFT_TIME_VALUE) { // Set switch hours or drift time value
                    int intValue = Integer.parseInt(data);
                    if (command == Command.DRIFT_TIME_VALUE) {
                        driftTimeValue = intValue;
                    } else {
                        pinsValueArray[command] = intValue;
                    }
                }
                break;
        }

        if (gettingInfo && !isFailed) {
            startFetchingInfo();
        }
        else {
            progressDialog.hide();
            updateViewWithDetails();
        }
    }

    private void parseInitialConfigData(String data)
    {
        gettingInfo = false;
        String[] chunks = data.split("\\|");
        if (chunks.length > 0) {

            for (String chunk : chunks) {
                if (!chunk.isEmpty()) {
                    String[] commands = chunk.split(":");
                    int command = Integer.parseInt(commands[0]);
                    int intData = Integer.parseInt(commands[1]);

                    switch (command) {
                        case Command.GET_TIME:
                            deviceTimestamp = Long.parseLong(commands[1]);
                            break;

                        case Command.GET_SWITCH_NUM:
                            numOfPins = intData;

                            if (numOfPins > 0) {
                                pinsValueArray = new int[numOfPins * 2 + 1];
                                pinsValueArray[0] = numOfPins;
                            }
                            break;

                        case Command.DRIFT_TIME_VALUE:
                            driftTimeValue = intData;
                            break;

                        default:
                            pinsValueArray[command] = intData;
                            break;
                    }
                }
            }
            Command.reset();
            showDetailViews();
        } else {
            log("Invalid Device");
        }
    }

}
