package com.alan.autoswitch;

import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
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

    private SimpleDateFormat utcDF;
    private SimpleDateFormat localDF;

    private boolean gettingInfo = false;

    private int numOfPins = 0;
    private int currentPinGetCnt = 0;
    private int[] pinsValueArray;
    private long btTimestamp = 0;
    private int driftTimeValue = 0;

    // Views
    private ScrollView scrollView;
    private TextView logView;
    private TextView deviceNameView;
    private TextView currentTimeView;
    private TextView btTimeView;
    private TextView syncTimeView;
    private TextView driftTimeView;
    private LinearLayout contentView;
    private LinearLayout timeView;
    // Switches views
    private int maxSwitchLayouts = 5;
    private LinearLayout[] switchLayoutsViews = new LinearLayout[maxSwitchLayouts];
    private TextView[] switchOnOffViews = new TextView[maxSwitchLayouts * 2];

    final Handler commandHandler = new Handler();
    Thread counterThread;
    int commandRetry = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        deviceNameView = findViewById(R.id.device_name_view);
        currentTimeView = findViewById(R.id.current_time);
        btTimeView = findViewById(R.id.bt_time);
        syncTimeView = findViewById(R.id.sync_time);
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
        deviceNameView.setTextColor(getResources().getColor(R.color.btConnected));

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Getting info from BT device", Toast.LENGTH_LONG).show();
                getInfoFromBT();
            }
        }, 1000);
    }

    @Override
    public void onDeviceDisconnect() {
        deviceNameView.setText(getString(R.string.not_connected_str));
        deviceNameView.setTextColor(getResources().getColor(R.color.grey));
        log("Device disconnected");
        Toast.makeText(this, "Device disconnected", Toast.LENGTH_SHORT).show();
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
        onReceivedFromBT(data);
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
                runCommand(Constants.DRIFT_TIME_VALUE, which);
            }
        });
    }

    public void onSyncDeviceTimeClick(View view) {
        long timestampToUTC = Utils.getCurrentTimeUTC() + 1;
        log("Syncing BT device time with: " + timestampToUTC);
        runCommand(Constants.SET_TIME, timestampToUTC);
    }

    private void log(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logView.append("> ");
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
                        displayBtDateTime();
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

    private void displayBtDateTime() {
        String currentDateTimeString = "Not connected";
        if (btTimestamp > 100000) {
            btTimestamp++;

            Date dt = new Date(btTimestamp * 1000);
            currentDateTimeString = utcDF.format(new Date(btTimestamp * 1000));
        }

        String str = getString(R.string.bt_time_str, currentDateTimeString);
        btTimeView.setText(str);
    }



    private void resetCommand() {
        Command.set(Constants.NO_COMMAND_VALUE, Constants.NO_COMMAND_VALUE);
    }

    private void initView() {
        timeView.setVisibility(View.VISIBLE);
        contentView.setVisibility(View.VISIBLE);
        syncTimeView.setVisibility(View.VISIBLE);

        updateViewWithBTDetails();
        startCounter();
    }

    private void updateViewWithBTDetails() {
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
                    resetCommand();
                    progressDialog.hide();
                }
                else {
                    log("Retrying...");
                    commandRetry++;
                    long newParam = param;

                    if (command == Constants.SET_TIME) {
                        newParam = Utils.getCurrentTimeUTC() + 1;
                    }
                    runCommand(command, newParam);
                }
            }
        }, Constants.COMMAND_TIMEOUT);
    }

    private void getInfoFromBT() {
        gettingInfo = true;
        progressDialog.show();

        switch (Command.COMMAND)
        {
            case Constants.NO_COMMAND_VALUE:
                Command.set(Constants.PING_BACK, Constants.NO_COMMAND_VALUE);
                break;

            case Constants.PING_BACK:
                Command.set(Constants.GET_TIME, Constants.NO_COMMAND_VALUE);
                break;

            case Constants.GET_TIME:
                Command.set(Constants.GET_SWITCH_NUM, Constants.NO_COMMAND_VALUE);
                break;

            case Constants.GET_SWITCH_NUM:
            case Constants.GET_SWITCH_VALUE:
                currentPinGetCnt++;

                if (currentPinGetCnt <= numOfPins * 2) {
                    Command.set(Constants.GET_SWITCH_VALUE, currentPinGetCnt);
                }
                else if (Command.PARAM != Constants.DRIFT_TIME_VALUE) {
                    Command.set(Constants.GET_SWITCH_VALUE, Constants.DRIFT_TIME_VALUE);
                } else {
                    currentPinGetCnt = 0;
                    resetCommand(); // Finished here
                }
                break;

            default:
                resetCommand();
                break;
        }

        if (Command.COMMAND != Constants.NO_COMMAND_VALUE) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    runCommand(Command.COMMAND, Command.PARAM);
                }
            }, Constants.COMMAND_GAP_TIME);
        } else {
            // stop getting values from BT as all details are fetched
            gettingInfo = false;
            initView();
            progressDialog.hide();
        }
    }

    private void onReceivedFromBT(String data) {
        int command = Command.COMMAND;
        long param = Command.PARAM;
        int intData = Integer.parseInt(data);

        commandHandler.removeCallbacksAndMessages(null);
        commandRetry = 0;
        boolean isFailed = false;

        log("<< " + command + ":" + param + " << " + data);

        switch (command) {

            case Constants.PING_BACK:
                if (intData != Constants.PING_BACK) {
                    log("Invalid BT device.");
                    isFailed = true;
                }
                break;

            case Constants.GET_TIME:
                btTimestamp = Long.parseLong(data);
                break;

            case Constants.SET_TIME:
                btTimestamp = Long.parseLong(data);
                break;

            case Constants.GET_SWITCH_NUM:
                numOfPins = intData;

                if (numOfPins <= 0) {
                    log("No valid number of switch found: " + data);
                    isFailed = true;
                } else {
                    pinsValueArray = new int[numOfPins * 2 + 1];
                    pinsValueArray[0] = numOfPins;
                }
                break;

            case Constants.GET_SWITCH_VALUE: // Get switch hours or drift time value
                if (param == Constants.DRIFT_TIME_VALUE) {
                    driftTimeValue = intData;
                } else {
                    pinsValueArray[(int) param] = intData;
                }
                break;

            default:
                if (command > 0 && command <= Constants.DRIFT_TIME_VALUE) { // Set switch hours or drift time value
                    if (command == Constants.DRIFT_TIME_VALUE) {
                        driftTimeValue = intData;
                    } else {
                        pinsValueArray[command] = intData;
                    }
                }
                break;
        }

        if (gettingInfo && !isFailed) {
            getInfoFromBT();
        }
        else {
            progressDialog.hide();
            updateViewWithBTDetails();
        }
    }

}
