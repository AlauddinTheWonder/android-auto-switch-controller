package com.alan.autoswitch.extra;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import androidx.appcompat.app.AlertDialog;

import com.alan.autoswitch.R;


public class NumberPickerDialog {

    private AlertDialog alertDialog;
    private NumberPicker numberPicker;

    private DialogInterface.OnClickListener positiveListener;

    public NumberPickerDialog(final Context context, String title) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        assert inflater != null;
        View progressLayout = inflater.inflate(R.layout.number_picker, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setCancelable(false);
        builder.setView(progressLayout);
        builder.setPositiveButton("Select", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int value = numberPicker.getValue();
                positiveListener.onClick(dialog, value);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog = builder.create();

        numberPicker = progressLayout.findViewById(R.id.numberPicker);
    }

    public void setMinValue(int min) {
        numberPicker.setMinValue(min);
    }

    public void setMaxValue(int max) {
        numberPicker.setMaxValue(max);
    }

    public void show(int value, DialogInterface.OnClickListener listener) {
        positiveListener = listener;
        alertDialog.show();
        numberPicker.setValue(value);
    }

    public void show(int value, int max, DialogInterface.OnClickListener listener) {
        positiveListener = listener;
        setMinValue(0);
        setMaxValue(max);
        alertDialog.show();
        numberPicker.setValue(value);
    }

    public void show(int value, int min, int max, DialogInterface.OnClickListener listener) {
        positiveListener = listener;
        setMinValue(min);
        setMaxValue(max);
        alertDialog.show();
        numberPicker.setValue(value);
    }

}
