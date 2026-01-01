package com.example.studybuddy.adapter;

import java.io.Serializable;

public class Module implements Serializable {

    // Firestore document ID (not stored automatically)
    private String id;

    private String title;
    private String description;
    private String year;
    private String semester;
    private String dayOfWeek;
    private long createdAt;

    // REQUIRED: no-arg constructor for Firestore
    public Module() {
    }

    // Convenience constructor (optional – not used by Firestore)
    public Module(String title, String description, String year, String semester, String dayOfWeek) {
        this.title = title;
        this.description = description;
        this.year = year;
        this.semester = semester;
        this.dayOfWeek = dayOfWeek;
        this.createdAt = System.currentTimeMillis();
    }

    // -------- Getters --------
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getYear() {
        return year;
    }

    public String getSemester() {
        return semester;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // -------- Setters (REQUIRED for Firestore) --------
    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    // -------- UI helper --------
    public String getMetaText() {
        // Example: "Year 3 • Semester 2 • Monday"
        String day = (dayOfWeek == null || dayOfWeek.isEmpty()) ? "" : " • " + dayOfWeek;
        return "Year " + year + " • Semester " + semester + day;
    }
}