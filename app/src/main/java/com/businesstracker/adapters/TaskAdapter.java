package com.businesstracker.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.businesstracker.R;
import com.businesstracker.models.Task;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    public interface OnTaskActionListener {
        void onToggle(String id, boolean checked);
        void onDelete(String id);
        void onEdit(Task task);
    }

    private final List<Task> tasks;
    private final OnTaskActionListener listener;

    public TaskAdapter(List<Task> tasks, OnTaskActionListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task t = tasks.get(position);

        holder.cbDone.setOnCheckedChangeListener(null);
        holder.cbDone.setChecked(t.isCompleted());

        holder.tvTitle.setText(t.getTitle());
        if (t.isCompleted()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(0.5f);
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(1.0f);
        }

        holder.tvDescription.setText(t.getDescription());
        holder.tvDescription.setVisibility(
                (t.getDescription() == null || t.getDescription().isEmpty()) ? View.GONE : View.VISIBLE);

        holder.tvDueDate.setText("Due: " + (t.getDueDate() != null ? t.getDueDate() : "—"));

        holder.cbDone.setOnCheckedChangeListener((b, checked) ->
                listener.onToggle(t.getId(), checked));

        holder.btnDelete.setOnClickListener(v -> listener.onDelete(t.getId()));
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(t));
    }

    @Override
    public int getItemCount() { return tasks.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox       cbDone;
        TextView       tvTitle, tvDescription, tvDueDate;
        MaterialButton btnDelete, btnEdit;

        ViewHolder(View v) {
            super(v);
            cbDone        = v.findViewById(R.id.cbTaskDone);
            tvTitle       = v.findViewById(R.id.tvTaskTitle);
            tvDescription = v.findViewById(R.id.tvTaskDescription);
            tvDueDate     = v.findViewById(R.id.tvTaskDueDate);
            btnDelete     = v.findViewById(R.id.btnDeleteTask);
            btnEdit       = v.findViewById(R.id.btnEditTask);
        }
    }
}
