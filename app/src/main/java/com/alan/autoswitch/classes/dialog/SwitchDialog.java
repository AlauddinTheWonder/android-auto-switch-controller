package com.alan.autoswitch.classes.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.NumberPicker;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;

import com.alan.autoswitch.R;
import com.alan.autoswitch.classes.interfaces.SwitchListener;
import com.alan.autoswitch.classes.model.SwitchModel;

public class SwitchDialog {

    private AlertDialog alertDialog;

    private Context context;

    private NumberPicker onPicker;
    private NumberPicker offPicker;
    private Spinner switchPin;

    private SwitchListener.OnSaveListener positiveListener;
    private SwitchModel currentModel;
    private int currentPosition;

    public SwitchDialog(Context context) {
        this.context = context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;

        View layout = inflater.inflate(R.layout.switch_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        builder.setView(layout);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int pin = switchPin.getSelectedItemPosition();
                SwitchModel switchModel = new SwitchModel(pin, onPicker.getValue(), offPicker.getValue(), currentModel.getIndex());

                positiveListener.onClick(dialog, switchModel, currentPosition);

                currentModel = null;
                currentPosition = -1;
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentModel = null;
                currentPosition = -1;
                dialog.dismiss();
            }
        });

        alertDialog = builder.create();

        switchPin = layout.findViewById(R.id.switch_pin);
        onPicker = layout.findViewById(R.id.on_picker);
        offPicker = layout.findViewById(R.id.off_picker);

        onPicker.setMinValue(0);
        onPicker.setMaxValue(23);

        offPicker.setMinValue(0);
        offPicker.setMaxValue(23);
    }

    public void setPinSize(int size) {
        String[] items = new String[]{"Choose", "Switch 1", "Switch 2", "Switch 3"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.spinner_item, items);

        switchPin.setAdapter(adapter);
    }

    public void show(SwitchModel switchModel, int position, SwitchListener.OnSaveListener listener) {
        positiveListener = listener;
        currentModel = switchModel;
        currentPosition = position;
        alertDialog.show();

        switchPin.setSelection(switchModel.getPin());
        onPicker.setValue(switchModel.getOn());
        offPicker.setValue(switchModel.getOff());
    }
}
