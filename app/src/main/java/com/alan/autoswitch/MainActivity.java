package com.alan.autoswitch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.alan.autoswitch.classes.dialog.ConfirmDialog;
import com.alan.autoswitch.classes.dialog.SwitchDialog;
import com.alan.autoswitch.classes.interfaces.Device;
import com.alan.autoswitch.classes.interfaces.DeviceListener;
import com.alan.autoswitch.classes.MyDevice;
import com.alan.autoswitch.classes.adapter.SwitchListAdapter;
import com.alan.autoswitch.classes.model.CommandList;
import com.alan.autoswitch.classes.model.CommandType;
import com.alan.autoswitch.classes.model.SwitchModel;
import com.alan.autoswitch.extra.Command;
import com.alan.autoswitch.extra.Constants;
import com.alan.autoswitch.extra.Debounce;
import com.alan.autoswitch.classes.dialog.ProgressDialog;
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
    private SwitchDialog switchDialog;
    private SwitchListAdapter switchListAdapter;

    private SimpleDateFormat utcDF, localDF;

    private boolean TerminalMode = false;
    private boolean ShowLogs = false;

    private int NumOfSwitches = 0;
    private int MaxSettingsCount = 0;
    private int[] PinSettingsArray;
    private long DeviceTimestamp = 0;

    final Handler commandHandler = new Handler();
    Thread counterThread = null;
    int commandRetry = 0;
    CommandType currentCommand;

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

//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        TerminalMode = getIntent().getBooleanExtra(Constants.EXTRA_TERMINAL_MODE, false);

        String deviceType = getIntent().getStringExtra(Constants.EXTRA_DEVICE_TYPE);
        if (deviceType != null && MyDevice.validateDevice(deviceType)) {
            this.myDevice = MyDevice.getDevice(this, deviceType);
        } else {
            exitScreen("Invalid Device");
        }

        scrollDebounce = new Debounce(1000);
        progressDialog = new ProgressDialog(this);
        switchDialog = new SwitchDialog(this);

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

        myDevice.setListener(this);
        myDevice.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        progressDialog.dismiss();
        myDevice.onExit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_show_logs);

        Drawable icon = item.getIcon();
        icon.setTint(getResources().getColor(R.color.menuIconTint));
        item.setIcon(icon);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_show_logs) {
            if (ShowLogs) {
                hideLogPanel();

                Drawable icon = item.getIcon();
                icon.setTint(getResources().getColor(R.color.menuIconTint));
                item.setIcon(icon);
            } else {
                showLogPanel();

                Drawable icon = item.getIcon();
                icon.setTint(getResources().getColor(R.color.menuIconTintActive));
                item.setIcon(icon);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onInfo(String text) {
        log(text);
    }

    @Override
    public void onDeviceConnect(String name) {
        String str = getString(R.string.connected_str, name);
        log(str, true);
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
        log("Device disconnected", true);
        CommandList.reset();

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

    private void log(String text) {
        log(text, false);
    }

    private void log(final String text, boolean showToast) {
        Log.i(Constants.TAG, text);

        if (showToast) {
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        }

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
            log(msg, true);
        }
        finish();
    }

    private void showLogPanel() {
        ShowLogs = true;

        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics());
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) bottomView.getLayoutParams();
        params.height = height;

        bottomView.setLayoutParams(params);
    }

    private void hideLogPanel() {
        ShowLogs = false;

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) bottomView.getLayoutParams();
        params.height = 0;
        bottomView.setLayoutParams(params);
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

    private void onDataLoadedFromDevice() {
        timeView.setVisibility(View.VISIBLE);
        switchListView.setVisibility(View.VISIBLE);
        switchDialog.setPinSize(NumOfSwitches);

        switchListAdapter = new SwitchListAdapter(switchList);
        switchListView.setAdapter(switchListAdapter);

        switchListAdapter.setOnEditClickListener(new SwitchListAdapter.OnEditClickListener() {
            @Override
            public void onEdit(int position, SwitchModel switchModel) {

                switchDialog.show(switchModel, position, (dialog, model, position1) -> {

                    int idx = model.getIndex();
                    if (switchModel.getPin() != model.getPin()) {
                        CommandList.add(idx, model.getPin());
                    }
                    if (switchModel.getOn() != model.getOn()) {
                        CommandList.add(idx + 1, model.getOn());
                    }
                    if (switchModel.getOff() != model.getOff()) {
                        CommandList.add(idx + 2, model.getOff());
                    }

                    runCommandList();
                });
            }
        });

        switchListAdapter.setOnDeleteClickListener(new SwitchListAdapter.OnDeleteClickListener() {
            @Override
            public void onDelete(final int position, final SwitchModel switchModel) {
                ConfirmDialog dialog = new ConfirmDialog(context, "Are you sure to delete thhis?");
                dialog.show((dialog1, which) -> {
                    int idx = switchModel.getIndex();

                    CommandList.add(idx, 0);
                    CommandList.add(idx + 1, 0);
                    CommandList.add(idx + 2, 0);

                    runCommandList();
                });
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

    private void runCommandList() {
        if (CommandList.size() == 0) {
            return;
        }
        CommandType command = CommandList.get(0);
        CommandList.remove(0);

        runCommand(command.getCommand(), command.getParam());
    }

    private void runCommand(final int command, final long param) {
        progressDialog.show();

        currentCommand = new CommandType(command, param);

        String commandStr = command + ":" + param;

        myDevice.send(commandStr);
        log(">> " + command + ":" + param);

       commandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (commandRetry >= Constants.COMMAND_MAX_RETRY) {
                    currentCommand = null;
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

        int command = currentCommand.getCommand();
        long param = currentCommand.getParam();

        currentCommand = null;

        commandHandler.removeCallbacksAndMessages(null);
        commandRetry = 0;

        switch (command) {

            case Command.PING_BACK:
                if (data.equals(String.valueOf(Command.PING_BACK))) {
                    runCommand(Command.GET_ALL_DATA, Command.NO_COMMAND_VALUE);
                } else {
                    exitScreen("Invalid device.");
                }
                break;

            case Command.GET_ALL_DATA:
                parseInitialConfigData(data);
                break;

            case Command.GET_TIME:
            case Command.SET_TIME:
                DeviceTimestamp = Long.parseLong(data);
                break;

            case Command.GET_SWITCH_VALUE:
                PinSettingsArray[(int) param] = Integer.parseInt(data);
                updateSwitchListByIndex((int) param, Integer.parseInt(data));
                break;

            default:
                if (command > 0 && MaxSettingsCount > 0 && command <= (MaxSettingsCount * Constants.SWITCH_SINGLE_ROW_CNT)) {
                    PinSettingsArray[command] = Integer.parseInt(data);
                    updateSwitchListByIndex(command, Integer.parseInt(data));
                }
                break;
        }

        if (CommandList.size() > 0) {
            runCommandList();
        } else {
            progressDialog.hide();
        }
    }

    private void parseInitialConfigData(String data) {
        String[] chunks = data.split("\\|");
        if (chunks.length > 1)
        {
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
                                    PinSettingsArray = new int[MaxSettingsCount * Constants.SWITCH_SINGLE_ROW_CNT + 1];
                                    PinSettingsArray[0] = NumOfSwitches; // Pin setting starts from 1 index.
                                }
                                break;

                            default:
                                if (MaxSettingsCount > 0 && command <= (MaxSettingsCount * Constants.SWITCH_SINGLE_ROW_CNT)) {
                                    PinSettingsArray[command] = intData;
                                }
                                break;
                        }
                    }
                }
            }

            parseSettingsToSwitchList();
            onDataLoadedFromDevice();
        } else {
            exitScreen("Invalid data received. Please try again!");
        }
    }

    private void parseSettingsToSwitchList() {
        if (switchList.size() > 0) {
            switchList.clear();
        }
        for (int i = 0; i < MaxSettingsCount; i++) {
            int index = (i * Constants.SWITCH_SINGLE_ROW_CNT) + 1;
            int pin = PinSettingsArray[index];
            int on = PinSettingsArray[index + 1];
            int off = PinSettingsArray[index + 2];

            SwitchModel switchModel = new SwitchModel(pin, on, off, index);
            switchList.add(switchModel);
        }
    }

    private void updateSwitchListByIndex(int command, int value) {
        int target = command - 1;
        int position = (int) Math.floor(target / (float) Constants.SWITCH_SINGLE_ROW_CNT);
        int sequence = target - (position * Constants.SWITCH_SINGLE_ROW_CNT); // 0 = pin, 1 = on, 2 = off

        SwitchModel switchModel = switchList.get(position);
        if (sequence == 0) {
            switchModel.setPin(value);
        }
        else if (sequence == 1) {
            switchModel.setOn(value);
        }
        else if (sequence == 2) {
            switchModel.setOff(value);
        }
        switchListAdapter.updateItem(position, switchModel);
    }

}
