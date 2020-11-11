package com.alan.autoswitch.classes.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.alan.autoswitch.R;
import com.alan.autoswitch.classes.model.SwitchModel;

import java.util.ArrayList;

public class SwitchListAdapter extends RecyclerView.Adapter<SwitchListAdapter.ViewHolder> {

    private ArrayList<SwitchModel> listdata;
    private OnEditClickListener onEditClickListener;
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnEditClickListener {
        void onEdit(int position, SwitchModel switchModel);
    }

    public void setOnEditClickListener(OnEditClickListener onEditClickListener) {
        this.onEditClickListener = onEditClickListener;
    }

    public interface OnDeleteClickListener {
        void onDelete(int position, SwitchModel switchModel);
    }

    public void setOnDeleteClickListener(OnDeleteClickListener onDeleteClickListener) {
        this.onDeleteClickListener = onDeleteClickListener;
    }

    // RecyclerView recyclerView;
    public SwitchListAdapter(ArrayList<SwitchModel> listdata) {
        this.listdata = listdata;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.switch_list_item, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final SwitchModel item = listdata.get(position);
        holder.switch_no.setText(String.valueOf(item.getPin()));
        holder.on_txt.setText(String.valueOf(item.getOn()));
        holder.off_txt.setText(String.valueOf(item.getOff()));

        holder.del_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onDeleteClickListener != null)
                    onDeleteClickListener.onDelete(position, item);
            }
        });

        holder.info_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onEditClickListener != null)
                    onEditClickListener.onEdit(position, item);
            }
        });

    }

    public void deleteItem(int position) {
        listdata.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, listdata.size());
    }

    public void updateItem(int position, SwitchModel switchModel) {
        listdata.add(position, switchModel);
        notifyItemChanged(position, switchModel);
    }

    @Override
    public int getItemCount() {
        return listdata.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout info_view;
        TextView switch_no, on_txt, off_txt;
        ImageButton del_btn;

        ViewHolder(View itemView) {
            super(itemView);
            switch_no = itemView.findViewById(R.id.switch_txt);
            on_txt = itemView.findViewById(R.id.on_txt);
            off_txt = itemView.findViewById(R.id.off_txt);
            info_view = itemView.findViewById(R.id.info_view);
            del_btn = itemView.findViewById(R.id.del_btn);
        }
    }
}
