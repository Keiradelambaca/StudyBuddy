package com.example.studybuddy.tasks;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studybuddy.R;
import com.example.studybuddy.adapter.Module;
import com.example.studybuddy.adapter.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskDetailActivity extends AppCompatActivity {

    private EditText etTitle, etDescription;
    private Spinner spModule, spPriority, spTaskType;
    private Button btnPickDueDate, btnSave, btnDelete;
    private TextView tvError;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private DocumentReference taskDoc;
    private CollectionReference modulesRef;

    private final List<Module> modules = new ArrayList<>();
    private final List<String> moduleTitles = new ArrayList<>();
    private ArrayAdapter<String> moduleAdapter;

    private String taskId;
    private Task currentTask;

    private Long selectedDueAt = null;

    private static final List<String> PRIORITIES = Arrays.asList("None", "High", "Medium", "Low");
    private static final List<String> TASK_TYPES = Arrays.asList(
            "Select type...", "Task", "Assignment", "Exam", "Demo", "Presentation"
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        bindViews();

        taskId = getIntent().getStringExtra("TASK_ID");
        if (taskId == null || taskId.trim().isEmpty()) {
            showError("Missing task id.");
            finish();
            return;
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showError("You must be logged in.");
            finish();
            return;
        }

        taskDoc = db.collection("users").document(user.getUid()).collection("tasks").document(taskId);
        modulesRef = db.collection("users").document(user.getUid()).collection("modules");

        setupPrioritySpinner();
        setupTaskTypeSpinner();
        setupModuleSpinnerPlaceholder();
        setupDueDatePicker();
        setupButtons();

        loadModulesThenTask();
    }

    private void bindViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        spModule = findViewById(R.id.spModule);
        spPriority = findViewById(R.id.spPriority);
        spTaskType = findViewById(R.id.spTaskType);
        btnPickDueDate = findViewById(R.id.btnPickDueDate);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        tvError = findViewById(R.id.tvError);
    }

    private void setupPrioritySpinner() {
        spPriority.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                PRIORITIES
        ));
    }

    private void setupTaskTypeSpinner() {
        spTaskType.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                TASK_TYPES
        ));
    }

    private void setupModuleSpinnerPlaceholder() {
        moduleTitles.clear();
        moduleTitles.add("No module (optional)");
        moduleAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                moduleTitles
        );
        spModule.setAdapter(moduleAdapter);
    }

    private void setupDueDatePicker() {
        btnPickDueDate.setOnClickListener(v -> openDatePicker());
    }

    private void openDatePicker() {
        Calendar c = Calendar.getInstance();
        if (selectedDueAt != null) c.setTimeInMillis(selectedDueAt);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(Calendar.YEAR, year);
                    picked.set(Calendar.MONTH, month);
                    picked.set(Calendar.DAY_OF_MONTH, day);
                    // Default to 9am, adjust if you add a time picker later
                    picked.set(Calendar.HOUR_OF_DAY, 9);
                    picked.set(Calendar.MINUTE, 0);
                    picked.set(Calendar.SECOND, 0);
                    picked.set(Calendar.MILLISECOND, 0);

                    selectedDueAt = picked.getTimeInMillis();
                    btnPickDueDate.setText("Due: " + formatDueFull(selectedDueAt));
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void setupButtons() {
        btnSave.setOnClickListener(v -> saveTask());
        btnDelete.setOnClickListener(v -> deleteTask());
    }

    private void loadModulesThenTask() {
        modulesRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    modules.clear();
                    moduleTitles.clear();
                    moduleTitles.add("No module (optional)");

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Module m = doc.toObject(Module.class);
                        if (m != null) {
                            m.setId(doc.getId());
                            modules.add(m);
                            moduleTitles.add(safe(m.getTitle()));
                        }
                    }

                    moduleAdapter.notifyDataSetChanged();
                    loadTask();
                })
                .addOnFailureListener(e -> {
                    // still load task even if modules fail
                    loadTask();
                });
    }

    private void loadTask() {
        taskDoc.get()
                .addOnSuccessListener(doc -> {
                    currentTask = doc.toObject(Task.class);
                    if (currentTask == null) {
                        showError("Task not found.");
                        finish();
                        return;
                    }
                    currentTask.setId(doc.getId());
                    populateUI();
                })
                .addOnFailureListener(e -> {
                    showError("Failed to load task: " + e.getMessage());
                    finish();
                });
    }

    private void populateUI() {
        hideError();

        etTitle.setText(safe(currentTask.getTitle()));
        etDescription.setText(safe(currentTask.getDescription()));

        // Due date
        selectedDueAt = currentTask.getDueAt();
        if (selectedDueAt != null) {
            btnPickDueDate.setText("Due: " + formatDueFull(selectedDueAt));
        } else {
            btnPickDueDate.setText("Pick due date (optional)");
        }

        // Priority
        spPriority.setSelection(priorityToIndex(currentTask.getPriority()));

        // Module
        spModule.setSelection(moduleIndexFor(currentTask.getModuleId()));

        // Task type (required)
        spTaskType.setSelection(typeToIndex(currentTask.getType()));
    }

    private int moduleIndexFor(String moduleId) {
        if (moduleId == null) return 0; // placeholder
        for (int i = 0; i < modules.size(); i++) {
            if (moduleId.equals(modules.get(i).getId())) {
                return i + 1; // +1 because placeholder at index 0
            }
        }
        return 0;
    }

    private int priorityToIndex(String p) {
        // spinner: None(0), High(1), Medium(2), Low(3)
        if (p == null) return 0;
        switch (p) {
            case "HIGH": return 1;
            case "MEDIUM": return 2;
            case "LOW": return 3;
            default: return 0;
        }
    }

    private String uiPriorityToStored(String ui) {
        if (ui == null) return "NONE";
        switch (ui.toLowerCase(Locale.ROOT)) {
            case "high": return "HIGH";
            case "medium": return "MEDIUM";
            case "low": return "LOW";
            default: return "NONE";
        }
    }

    private int typeToIndex(String storedType) {
        // TASK_TYPES: Select type...(0), Task(1), Assignment(2), Exam(3), Demo(4), Presentation(5)
        if (storedType == null) return 0;
        String t = storedType.trim().toLowerCase(Locale.ROOT);
        switch (t) {
            case "task": return 1;
            case "assignment": return 2;
            case "exam": return 3;
            case "demo": return 4;
            case "presentation": return 5;
            default: return 0;
        }
    }

    private String uiTypeToStored(String uiSelected) {
        if (uiSelected == null) return null;
        if ("Select type...".equals(uiSelected)) return null;
        return uiSelected.trim().toLowerCase(Locale.ROOT); // Task -> task, Assignment -> assignment...
    }

    private void saveTask() {
        hideError();

        String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();
        String desc = etDescription.getText() == null ? "" : etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            showError("Task title is required.");
            return;
        }

        // Required: task type
        String uiSelectedType = spTaskType.getSelectedItem() == null ? "Select type..." : spTaskType.getSelectedItem().toString();
        String storedType = uiTypeToStored(uiSelectedType);
        if (storedType == null) {
            Toast.makeText(this, "Please select a task type", Toast.LENGTH_SHORT).show();
            return;
        }

        // Module (optional)
        int modulePos = spModule.getSelectedItemPosition();
        String moduleId = null;
        String moduleTitle = null;

        if (modulePos > 0 && modulePos - 1 < modules.size()) {
            Module m = modules.get(modulePos - 1);
            moduleId = m.getId();
            moduleTitle = m.getTitle();
        }

        // Priority
        String storedPriority = uiPriorityToStored(
                spPriority.getSelectedItem() == null ? "None" : spPriority.getSelectedItem().toString()
        );

        btnSave.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", desc.isEmpty() ? null : desc);
        updates.put("moduleId", moduleId);
        updates.put("moduleTitle", moduleTitle);
        updates.put("priority", storedPriority);
        updates.put("dueAt", selectedDueAt);
        updates.put("type", storedType);

        taskDoc.update(updates)
                .addOnSuccessListener(unused -> {
                    btnSave.setEnabled(true);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    showError("Save failed: " + e.getMessage());
                });
    }

    private void deleteTask() {
        btnDelete.setEnabled(false);
        hideError();

        taskDoc.delete()
                .addOnSuccessListener(unused -> finish())
                .addOnFailureListener(e -> {
                    btnDelete.setEnabled(true);
                    showError("Delete failed: " + e.getMessage());
                });
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String formatDueFull(Long dueAt) {
        if (dueAt == null) return "";
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(dueAt));
    }
}