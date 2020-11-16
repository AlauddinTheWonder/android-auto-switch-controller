package com.alan.autoswitch.classes.interfaces;

import android.content.DialogInterface;

import com.alan.autoswitch.classes.model.SwitchModel;

public interface SwitchListener {
    interface OnSaveListener {
        void onClick(DialogInterface dialog, SwitchModel model, int position);
    }
}
