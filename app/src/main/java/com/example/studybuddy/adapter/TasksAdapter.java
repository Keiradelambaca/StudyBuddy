package com.example.studybuddy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studybuddy.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for showing tasks (used on Home "Upcoming Tasks" list).
 * Inflates: item_upcoming_task.xml
 *
 * Features:
 * - setTasks(List<Task>) updates list safely + notifies
 * - Clicking a task calls OnTaskClickListener
 * - Shows meta text: due date + priority
 * - Shows completed tick if completed
 * - Uses LIGHT colors from colors.xml based on task type:
 *   task, assignment, exam, demo, presentation
 * - Uses priority strip color (left bar) based on priority
 */
public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskVH> {

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
    }

    private final List<Task> tasks = new ArrayList<>();
    private final OnTaskClickListener listener;

    public TasksAdapter(List<Task> initialTasks, OnTaskClickListener listener) {
        if (initialTasks != null) this.tasks.addAll(initialTasks);
        this.listener = listener;
    }

    /** Replace the adapter data and refresh the list. */
    public void setTasks(List<Task> newTasks) {
        tasks.clear();
        if (newTasks != null) tasks.addAll(newTasks);
        notifyDataSetChanged();
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

        // Title
        String title = (t.getTitle() == null || t.getTitle().trim().isEmpty())
                ? "Untitled task"
                : t.getTitle().trim();
        h.tvTitle.setText(title);

        // Meta: Due + Priority label
        String dueText = formatDue(t.getDueAt());
        String prText = formatPriorityLabel(t.getPriority());

        String meta = (dueText.isEmpty() ? "No due date" : ("Due: " + dueText))
                + " • " + prText;
        h.tvMeta.setText(meta);

        // Completed icon
        h.imgStatus.setVisibility(t.isCompleted() ? View.VISIBLE : View.GONE);

        // Card background: LIGHT colors from your theme based on task TYPE
        applyTypeCardColor(h, t);

        // Left strip color: based on PRIORITY (keeps your existing meaning)
        applyPriorityStripColor(h, t);

        // Click
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(t);
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class TaskVH extends RecyclerView.ViewHolder {
        CardView cardTaskItem;
        View viewPriority;
        TextView tvTitle, tvMeta;
        ImageView imgStatus;

        TaskVH(@NonNull View itemView) {
            super(itemView);
            cardTaskItem = itemView.findViewById(R.id.cardTaskItem);
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
            case "HIGH":
                return "High";
            case "MEDIUM":
                return "Medium";
            case "LOW":
                return "Low";
            default:
                return "None";
        }
    }

    /**
     * Task TYPE -> light card background (from your colors.xml).
     * Expected stored values: task, assignment, exam, demo, presentation
     * Falls back to "task".
     */
    private void applyTypeCardColor(@NonNull TaskVH h, @NonNull Task t) {
        String type = t.getType();
        if (type == null) type = "task";
        type = type.trim().toLowerCase(Locale.ROOT);

        int bgRes;
        switch (type) {
            case "assignment":
                bgRes = R.color.info_blue_soft;
                break;
            case "exam":
                bgRes = R.color.warning_orange_soft;
                break;
            case "demo":
                bgRes = R.color.accent_amber_soft;
                break;
            case "presentation":
                bgRes = R.color.accent_coral_soft;
                break;
            case "task":
            default:
                bgRes = R.color.success_green_soft;
                break;
        }

        if (h.cardTaskItem != null) {
            h.cardTaskItem.setCardBackgroundColor(
                    ContextCompat.getColor(h.itemView.getContext(), bgRes)
            );
        }
    }

    /**
     * Priority -> left strip accent color.
     * If you want priority strip to also be type-based, tell me and I’ll swap it.
     */
    private void applyPriorityStripColor(@NonNull TaskVH h, @NonNull Task t) {
        String p = t.getPriority();

        int stripRes;
        if ("HIGH".equals(p)) stripRes = R.color.error_red;
        else if ("MEDIUM".equals(p)) stripRes = R.color.primary_blue;
        else if ("LOW".equals(p)) stripRes = R.color.secondary_teal;
        else stripRes = R.color.surface_alt;

        if (h.viewPriority != null) {
            h.viewPriority.setBackgroundColor(
                    ContextCompat.getColor(h.itemView.getContext(), stripRes)
            );
        }
    }
}
