package com.alan.autoswitch;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.alan.autoswitch.extra.Constants;

public class LaunchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
    }

    public void onUsbClick(View view) {
        this.startRelatedActivity(Constants.DEVICE_USB);
    }

    public void onBluetoothClick(View view) {
        this.startRelatedActivity(Constants.DEVICE_BLUETOOTH);
    }

    private void startRelatedActivity(String name) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Constants.EXTRA_DEVICE_TYPE, name);
        startActivity(intent);
    }

}
