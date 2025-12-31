package com.example.studybuddy.adapter;
/**
 * Task model used for:
 * - Home dashboard (Upcoming Tasks)
 * - Tasks list (RecyclerView)
 * - Room database later
 */
public class Task {

    private String title;
    private String dueDate;
    private String priority;

    // Constructor
    public Task(String title, String dueDate, String priority) {
        this.title = title;
        this.dueDate = dueDate;
        this.priority = priority;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getPriority() {
        return priority;
    }

    // Setters (useful later for editing)
    public void setTitle(String title) {
        this.title = title;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}