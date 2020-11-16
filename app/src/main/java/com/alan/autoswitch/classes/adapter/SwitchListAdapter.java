package com.alan.autoswitch.classes.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.switch_list_item, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final SwitchModel item = listdata.get(position);

        if (item.getPin() == 0) {
            holder.addBtn.setVisibility(View.VISIBLE);
            holder.editBtn.setVisibility(View.GONE);
            holder.delBtn.setVisibility(View.GONE);

            holder.addView.setVisibility(View.VISIBLE);
            holder.infoView.setVisibility(View.GONE);

            holder.addBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onEditClickListener != null)
                        onEditClickListener.onEdit(position, item);
                }
            });
        }
        else {
            holder.addBtn.setVisibility(View.GONE);
            holder.editBtn.setVisibility(View.VISIBLE);
            holder.delBtn.setVisibility(View.VISIBLE);

            holder.addView.setVisibility(View.GONE);
            holder.infoView.setVisibility(View.VISIBLE);

            holder.switchNum.setText(String.valueOf(item.getPin()));
            holder.onTxt.setText(String.valueOf(item.getOn()));
            holder.offTxt.setText(String.valueOf(item.getOff()));

            holder.delBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onDeleteClickListener != null)
                        onDeleteClickListener.onDelete(position, item);
                }
            });

            holder.editBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onEditClickListener != null)
                        onEditClickListener.onEdit(position, item);
                }
            });
        }
    }

    public void deleteItem(int position) {
        listdata.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, listdata.size());
    }

    public void updateItem(int position, SwitchModel switchModel) {
        listdata.set(position, switchModel);
        notifyItemChanged(position, switchModel);
    }

    @Override
    public int getItemCount() {
        return listdata.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout infoView;
        TextView addView, switchNum, onTxt, offTxt;
        ImageButton addBtn, editBtn, delBtn;

        ViewHolder(View itemView) {
            super(itemView);

            infoView = itemView.findViewById(R.id.info_view);
            addView = itemView.findViewById(R.id.add_view);

            switchNum = itemView.findViewById(R.id.switch_txt);
            onTxt = itemView.findViewById(R.id.on_txt);
            offTxt = itemView.findViewById(R.id.off_txt);

            addBtn = itemView.findViewById(R.id.add_btn);
            editBtn = itemView.findViewById(R.id.edit_btn);
            delBtn = itemView.findViewById(R.id.del_btn);
        }
    }
}
