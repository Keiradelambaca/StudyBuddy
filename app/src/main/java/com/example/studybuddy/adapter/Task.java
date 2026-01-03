package com.example.studybuddy.adapter;

public class Task {

    // Firestore document ID (not stored automatically)
    private String id;

    // Stored fields (must match Firestore keys exactly)
    private String title;
    private String description;

    private String moduleId;
    private String moduleTitle;

    // "NONE", "LOW", "MEDIUM", "HIGH"
    private String priority;

    // millis since epoch (nullable)
    private Long dueAt;

    private boolean completed;
    private long createdAt;

    // REQUIRED: no-arg constructor for Firestore
    public Task() {}

    // Optional convenience constructor
    public Task(String title, String description, String moduleId, String moduleTitle,
                String priority, Long dueAt) {
        this.title = title;
        this.description = description;
        this.moduleId = moduleId;
        this.moduleTitle = moduleTitle;
        this.priority = priority;
        this.dueAt = dueAt;
        this.completed = false;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getModuleId() { return moduleId; }
    public String getModuleTitle() { return moduleTitle; }
    public String getPriority() { return priority; }
    public Long getDueAt() { return dueAt; }
    public boolean isCompleted() { return completed; }
    public long getCreatedAt() { return createdAt; }

    // Setters (REQUIRED for Firestore mapping)
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }
    public void setModuleTitle(String moduleTitle) { this.moduleTitle = moduleTitle; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setDueAt(Long dueAt) { this.dueAt = dueAt; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}