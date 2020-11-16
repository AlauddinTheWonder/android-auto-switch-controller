package com.alan.autoswitch;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;

import com.alan.autoswitch.extra.Constants;

public class LaunchActivity extends AppCompatActivity {

    private boolean terminalMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        terminalMode = ((CheckBox)findViewById(R.id.mode)).isChecked();
    }

    public void onUsbClick(View view) {
        this.startRelatedActivity(Constants.DEVICE_USB);
    }

    public void onBluetoothClick(View view) {
        this.startRelatedActivity(Constants.DEVICE_BLUETOOTH);
    }

    public void onMockDevClick(View view) {
        this.startRelatedActivity(Constants.DEVICE_MOCK_DEVICE);
    }

    public void onModeClick(View view) {
        terminalMode = ((CheckBox) view).isChecked();
    }

    private void startRelatedActivity(String name) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Constants.EXTRA_DEVICE_TYPE, name);
        intent.putExtra(Constants.EXTRA_TERMINAL_MODE, terminalMode);
        startActivity(intent);
    }

}
