package com.example.studybuddy.tasks;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.studybuddy.R;
import com.example.studybuddy.adapter.Module;
import com.example.studybuddy.adapter.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class TaskDetailActivity extends AppCompatActivity {

    private EditText etTitle, etDescription;
    private Spinner spModule, spPriority;
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
        btnPickDueDate = findViewById(R.id.btnPickDueDate);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        tvError = findViewById(R.id.tvError);
    }

    private void setupPrioritySpinner() {
        List<String> priorities = Arrays.asList("None", "High", "Medium", "Low");
        spPriority.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, priorities));
    }

    private void setupModuleSpinnerPlaceholder() {
        moduleTitles.clear();
        moduleTitles.add("No module (optional)");
        moduleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, moduleTitles);
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
                    picked.set(Calendar.HOUR_OF_DAY, 9);
                    picked.set(Calendar.MINUTE, 0);
                    picked.set(Calendar.SECOND, 0);
                    picked.set(Calendar.MILLISECOND, 0);

                    selectedDueAt = picked.getTimeInMillis();
                    btnPickDueDate.setText("Due: " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            .format(new Date(selectedDueAt)));
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
                            moduleTitles.add(m.getTitle());
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
        etTitle.setText(safe(currentTask.getTitle()));
        etDescription.setText(safe(currentTask.getDescription()));

        // due date
        selectedDueAt = currentTask.getDueAt();
        if (selectedDueAt != null) {
            btnPickDueDate.setText("Due: " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(new Date(selectedDueAt)));
        } else {
            btnPickDueDate.setText("Pick due date (optional)");
        }

        // priority spinner
        spPriority.setSelection(priorityToIndex(currentTask.getPriority()));

        // module spinner
        int idx = moduleIndexFor(currentTask.getModuleId());
        spModule.setSelection(idx);
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

    private void saveTask() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            showError("Task title is required.");
            return;
        }

        // module
        int modulePos = spModule.getSelectedItemPosition();
        String moduleId = null;
        String moduleTitle = null;

        if (modulePos > 0 && modulePos - 1 < modules.size()) {
            Module m = modules.get(modulePos - 1);
            moduleId = m.getId();
            moduleTitle = m.getTitle();
        }

        // priority
        String storedPriority = uiPriorityToStored(spPriority.getSelectedItem().toString());

        btnSave.setEnabled(false);
        hideError();

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", desc);
        updates.put("moduleId", moduleId);
        updates.put("moduleTitle", moduleTitle);
        updates.put("priority", storedPriority);
        updates.put("dueAt", selectedDueAt);

        taskDoc.update(updates)
                .addOnSuccessListener(unused -> {
                    btnSave.setEnabled(true);
                    finish(); // goes back to Tasks page (or Home page) automatically
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
}

