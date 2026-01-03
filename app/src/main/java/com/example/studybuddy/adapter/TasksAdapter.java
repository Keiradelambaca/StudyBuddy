package com.example.studybuddy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studybuddy.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskVH> {

    public void setTasks(List<Task> tasks) {
    }

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    private final List<Task> tasks;
    private final OnTaskClickListener listener;

    public TasksAdapter(List<Task> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming_task, parent, false);
        return new TaskVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskVH h, int position) {
        Task t = tasks.get(position);

        String title = (t.getTitle() == null || t.getTitle().trim().isEmpty())
                ? "Untitled task"
                : t.getTitle().trim();

        h.tvTitle.setText(title);

        String dueText = formatDue(t.getDueAt());
        String prText  = formatPriorityLabel(t.getPriority());

        String meta = (dueText.isEmpty() ? "No due date" : ("Due: " + dueText))
                + " • " + prText;

        h.tvMeta.setText(meta);

        int colorRes = priorityToColorRes(t.getPriority());
        h.viewPriority.setBackgroundResource(colorRes);

        h.imgStatus.setVisibility(t.isCompleted() ? View.VISIBLE : View.GONE);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(t);
        });
    }


    @Override
    public int getItemCount() {
        return tasks == null ? 0 : tasks.size();
    }

    static class TaskVH extends RecyclerView.ViewHolder {
        View viewPriority;
        TextView tvTitle, tvMeta;
        ImageView imgStatus;

        TaskVH(@NonNull View itemView) {
            super(itemView);
            viewPriority = itemView.findViewById(R.id.viewPriority);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvMeta = itemView.findViewById(R.id.tvTaskMeta);
            imgStatus = itemView.findViewById(R.id.imgStatus);
        }
    }

    private String formatDue(Long dueAt) {
        if (dueAt == null) return "";
        SimpleDateFormat fmt = new SimpleDateFormat("dd MMM", Locale.getDefault());
        return fmt.format(new Date(dueAt));
    }

    private String formatPriorityLabel(String p) {
        if (p == null) return "None";
        switch (p) {
            case "HIGH": return "High";
            case "MEDIUM": return "Medium";
            case "LOW": return "Low";
            default: return "None";
        }
    }

    private int priorityToColorRes(String p) {
        // You can create nicer colours later. For now reuse your theme colours:
        if ("HIGH".equals(p)) return R.color.error_red;
        if ("MEDIUM".equals(p)) return R.color.primary_blue;
        if ("LOW".equals(p)) return R.color.secondary_teal;
        return R.color.surface_alt;
    }
}
