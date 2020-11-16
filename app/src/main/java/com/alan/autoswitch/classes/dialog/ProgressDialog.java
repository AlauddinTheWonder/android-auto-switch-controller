package com.alan.autoswitch.classes.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.alan.autoswitch.R;

import java.util.Objects;

public class ProgressDialog {

    private AlertDialog dialog;
    private boolean showing = false;

    public ProgressDialog(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        assert inflater != null;
        View progressLayout = inflater.inflate(R.layout.progress_layout, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Please wait...");
        builder.setCancelable(false);
        builder.setView(progressLayout);
        dialog = builder.create();

        showing = false;
    }

    public void show() {
        if (showing) return;
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setLayout(600, 500);
        showing = true;
    }

    public void hide() {
        if (!showing) return;
        dialog.hide();
        showing = false;
    }

    public void dismiss() {
        dialog.dismiss();
    }

    public boolean isShowing() {
        return showing;
    }
}
