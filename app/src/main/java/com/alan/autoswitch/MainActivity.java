package com.alan.autoswitch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.alan.autoswitch.classes.Device;
import com.alan.autoswitch.classes.DeviceListener;
import com.alan.autoswitch.classes.MyDevice;
import com.alan.autoswitch.classes.adapter.SwitchListAdapter;
import com.alan.autoswitch.classes.model.SwitchModel;
import com.alan.autoswitch.extra.Command;
import com.alan.autoswitch.extra.Constants;
import com.alan.autoswitch.extra.Debounce;
import com.alan.autoswitch.extra.NumberPickerDialog;
import com.alan.autoswitch.extra.ProgressDialog;
import com.alan.autoswitch.extra.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private LinearLayout timeView, bottomView, terminalView;
    private RecyclerView switchListView;

    private ArrayList<SwitchModel> switchList = new ArrayList<>();

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

        bottomView = findViewById(R.id.bottom_view);
        scrollView =  findViewById(R.id.log_scroll_view);
        logView = findViewById(R.id.log_view);

        terminalView = findViewById(R.id.terminal_view);
        terminalInput = findViewById(R.id.terminal_input);

        deviceNameView = findViewById(R.id.device_name_view);

        timeView = findViewById(R.id.time_view);
        currentTimeView = findViewById(R.id.current_time);
        deviceTimeView = findViewById(R.id.device_time);

        switchListView = findViewById(R.id.switch_list_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        switchListView.setLayoutManager(layoutManager);
        switchListView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        if (TerminalMode) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            params.addRule(RelativeLayout.BELOW, R.id.top_view);

            bottomView.setLayoutParams(params);
        }

        // TODO: remove dummy data function
        loadDummyData();
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
        exitScreen("");
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
        if (!msg.isEmpty()) {
            Log.i(Constants.TAG, msg);
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
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
        switchListView.setVisibility(View.VISIBLE);

        final SwitchListAdapter switchListAdapter = new SwitchListAdapter(switchList);
        switchListView.setAdapter(switchListAdapter);
        switchListAdapter.setOnEditClickListener(new SwitchListAdapter.OnEditClickListener() {
            @Override
            public void onEdit(int position, SwitchModel switchModel) {

                log("Clicked on " + position);
                // TODO: show popup for edit
                switchListAdapter.updateItem(position, switchModel);
            }
        });

        switchListAdapter.setOnDeleteClickListener(new SwitchListAdapter.OnDeleteClickListener() {
            @Override
            public void onDelete(int position, SwitchModel switchModel) {

                log(String.valueOf(switchModel.getIndex()));

                SwitchModel sModel = new SwitchModel(0, 0, 0, switchModel.getIndex());
                switchListAdapter.updateItem(position, sModel);

//                switchListAdapter.deleteItem(position);
            }
        });

        startCounter();
    }

    private void hideDetailView() {
        timeView.setVisibility(View.GONE);
        switchListView.setVisibility(View.GONE);
    }

    private void getInfoFromDevice() {
        progressDialog.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                log("Getting info from device");
                runCommand(Command.GET_ALL_DATA, Command.NO_COMMAND_VALUE);
            }
        }, 3000);
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
        boolean reParseSettings = true;

        switch (command) {

            case Command.GET_ALL_DATA:
                reParseSettings = false;
                parseInitialConfigData(data);
                break;

            case Command.GET_TIME:
            case Command.SET_TIME:
                DeviceTimestamp = Long.parseLong(data);
                break;

            case Command.GET_SWITCH_VALUE:
                PinSettingsArray[(int) param] = Integer.parseInt(data);;
                break;

            default:
                if (command > 0 && MaxSettingsCount > 0 && command <= (MaxSettingsCount * 3)) {
                    PinSettingsArray[command] = Integer.parseInt(data);;
                }
                break;
        }

        if (reParseSettings) {
            parseSettingsToSwitchList();
        }
        progressDialog.hide();
        Command.reset();
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
                                if (MaxSettingsCount > 0 && command <= (MaxSettingsCount * 3)) {
                                    PinSettingsArray[command] = intData;
                                }
                                break;
                        }
                    }
                }
            }

            parseSettingsToSwitchList();
            showDetailViews();
        } else {
            exitScreen("Invalid data received. Please try again!");
        }
    }

    private void parseSettingsToSwitchList() {
        if (switchList.size() > 0) {
            switchList.clear();
        }
        for (int i = 0; i < MaxSettingsCount; i++) {
            int pos = (i * 3) + 1;
            int pin = PinSettingsArray[pos];
            int on = PinSettingsArray[pos + 1];
            int off = PinSettingsArray[pos + 2];

            SwitchModel switchModel = new SwitchModel(pin, on, off, pos);
            switchList.add(switchModel);
        }
    }

    private void loadDummyData() {
        if (TerminalMode) {
            terminalView.setVisibility(View.VISIBLE);
        } else {
            NumOfSwitches = 3;
            MaxSettingsCount = 10;

            DeviceTimestamp = Utils.getCurrentTimeUTC();

            PinSettingsArray = new int[MaxSettingsCount * 3 + 1];
            PinSettingsArray[0] = NumOfSwitches;

            for (int i = 0; i < MaxSettingsCount; i++) {
                int pos = (i * 3) + 1;

                PinSettingsArray[pos] = i + 1;
                PinSettingsArray[pos + 1] = Utils.getRandomNumberInRange(0, 23);
                PinSettingsArray[pos + 2] = Utils.getRandomNumberInRange(0, 23);
            }

            parseSettingsToSwitchList();
            showDetailViews();
        }
    }
}
