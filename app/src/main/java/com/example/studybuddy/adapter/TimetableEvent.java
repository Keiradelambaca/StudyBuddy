package com.example.studybuddy.adapter;

public class TimetableEvent {
    private String id;
    private String moduleId;
    private String title;
    private int dayOfWeek;   // Calendar.SUNDAY..SATURDAY (1..7)
    private int startMin;    // minutes from midnight
    private int endMin;
    private String rrule;    // "WEEKLY"
    private long createdAt;

    public TimetableEvent() {}

    public TimetableEvent(String moduleId, String title, int dayOfWeek, int startMin, int endMin) {
        this.moduleId = moduleId;
        this.title = title;

        this.dayOfWeek = dayOfWeek;
        this.startMin = startMin;
        this.endMin = endMin;
        this.rrule = "WEEKLY";
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getModuleId() { return moduleId; }
    public String getTitle() { return title; }
    public int getDayOfWeek() { return dayOfWeek; }
    public int getStartMin() { return startMin; }
    public int getEndMin() { return endMin; }
    public String getRrule() { return rrule; }
    public long getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }
    public void setTitle(String title) { this.title = title; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public void setStartMin(int startMin) { this.startMin = startMin; }
    public void setEndMin(int endMin) { this.endMin = endMin; }
    public void setRrule(String rrule) { this.rrule = rrule; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}

