package com.example.studybuddy.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studybuddy.R;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> tasks;

    public TaskAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);

        holder.tvTaskTitle.setText(task.getTitle());
        holder.tvTaskMeta.setText(
                "Due: " + task.getDueDate() + " • " + task.getPriority()
        );

        // Set priority colour strip
        int priorityColor = getPriorityColor(holder.itemView.getContext(), task.getPriority());
        holder.viewPriority.setBackgroundColor(priorityColor);

        // Optional: show completed icon (future use)
        holder.imgStatus.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return tasks == null ? 0 : tasks.size();
    }

    // -------------------------
    // Helper methods
    // -------------------------

    private int getPriorityColor(Context context, String priority) {
        if (priority == null) {
            return ContextCompat.getColor(context, R.color.secondary_teal);
        }

        switch (priority.toLowerCase()) {
            case "high":
                return ContextCompat.getColor(context, R.color.error_red);
            case "medium":
                return ContextCompat.getColor(context, R.color.warning_orange);
            case "low":
            default:
                return ContextCompat.getColor(context, R.color.secondary_teal);
        }
    }

    // -------------------------
    // ViewHolder
    // -------------------------

    static class TaskViewHolder extends RecyclerView.ViewHolder {

        TextView tvTaskTitle;
        TextView tvTaskMeta;
        View viewPriority;
        ImageView imgStatus;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskMeta  = itemView.findViewById(R.id.tvTaskMeta);
            viewPriority = itemView.findViewById(R.id.viewPriority);
            imgStatus = itemView.findViewById(R.id.imgStatus);
        }
    }
}