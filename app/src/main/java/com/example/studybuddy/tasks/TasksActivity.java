package com.example.studybuddy.tasks;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studybuddy.BaseBottomNavActivity;
import com.example.studybuddy.R;
import com.example.studybuddy.adapter.Module;
import com.example.studybuddy.adapter.Task;
import com.example.studybuddy.adapter.TasksAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;
import com.example.studybuddy.util.SimpleItemSelectedListener;


public class TasksActivity extends BaseBottomNavActivity {

    // UI
    private RecyclerView rvTasks;
    private LinearLayout emptyTasksState;
    private Spinner spPriorityFilter;

    private EditText etTaskTitle, etTaskDescription;
    private Spinner spTaskModule, spTaskPriority;
    private Button btnPickDueDate, btnAddTask;
    private TextView tvTaskValidation;

    // Data
    private final List<Task> allTasks = new ArrayList<>();
    private final List<Task> filteredTasks = new ArrayList<>();
    private TasksAdapter adapter;

    private final List<Module> modules = new ArrayList<>();
    private final List<String> moduleTitles = new ArrayList<>();
    private ArrayAdapter<String> moduleSpinnerAdapter;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CollectionReference tasksRef;
    private CollectionReference modulesRef;
    private ListenerRegistration tasksListener;
    private ListenerRegistration modulesListener;

    // Due date
    private Long selectedDueAt = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);
        setupBottomNav(R.id.nav_modules);

        bindViews();
        setupRecyclerView();
        setupSpinners();
        setupFirebase();
        setupDueDatePicker();
        setupAddTask();

        listenForModules(); // fill module spinner
        listenForTasks();   // live tasks list
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tasksListener != null) tasksListener.remove();
        if (modulesListener != null) modulesListener.remove();
    }

    private void bindViews() {
        rvTasks = findViewById(R.id.rvTasks);
        emptyTasksState = findViewById(R.id.emptyTasksState);
        spPriorityFilter = findViewById(R.id.spPriorityFilter);

        etTaskTitle = findViewById(R.id.etTaskTitle);
        etTaskDescription = findViewById(R.id.etTaskDescription);
        spTaskModule = findViewById(R.id.spTaskModule);
        spTaskPriority = findViewById(R.id.spTaskPriority);
        btnPickDueDate = findViewById(R.id.btnPickDueDate);
        btnAddTask = findViewById(R.id.btnAddTask);
        tvTaskValidation = findViewById(R.id.tvTaskValidation);
    }

    private void setupRecyclerView() {
        adapter = new TasksAdapter(filteredTasks, task -> {
            Intent i = new Intent(TasksActivity.this, TaskDetailActivity.class);
            i.putExtra("TASK_ID", task.getId());
            startActivity(i);
        });
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(adapter);
    }

    private void setupSpinners() {
        // Filter spinner (includes "All")
        List<String> filters = Arrays.asList("All", "High", "Medium", "Low", "None");
        spPriorityFilter.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, filters));
        spPriorityFilter.setSelection(0);
        spPriorityFilter.setOnItemSelectedListener(new SimpleItemSelectedListener(this::applyFilter));

        // Priority input spinner (stored on task)
        List<String> priorities = Arrays.asList("None", "High", "Medium", "Low");
        spTaskPriority.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, priorities));
        spTaskPriority.setSelection(0);

        // Module spinner (loaded from Firestore). Start with placeholder.
        moduleTitles.clear();
        moduleTitles.add("No module (optional)");
        moduleSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, moduleTitles);
        spTaskModule.setAdapter(moduleSpinnerAdapter);
    }

    private void setupFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        Log.d("TasksActivity", "currentUser=" + (user == null ? "null" : user.getUid()));

        if (user == null) {
            showValidation("You must be logged in to manage tasks.");
            btnAddTask.setEnabled(false);
            return;
        }

        modulesRef = db.collection("users").document(user.getUid()).collection("modules");
        tasksRef   = db.collection("users").document(user.getUid()).collection("tasks");
    }

    private void listenForModules() {
        if (modulesRef == null) return;

        modulesListener = modulesRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        Log.e("TasksActivity", "modules listen err", err);
                        return;
                    }
                    modules.clear();

                    // reset titles but keep placeholder
                    moduleTitles.clear();
                    moduleTitles.add("No module (optional)");

                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Module m = doc.toObject(Module.class);
                            if (m != null) {
                                m.setId(doc.getId());
                                modules.add(m);
                                moduleTitles.add(m.getTitle());
                            }
                        }
                    }

                    moduleSpinnerAdapter.notifyDataSetChanged();
                });
    }

        private void listenForTasks() {
            tasksListener = tasksRef
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .addSnapshotListener((snap, err) -> {

                        if (err != null) {
                            showValidation("Failed to load tasks: " + err.getMessage());
                            return;
                        }

                        allTasks.clear();

                        if (snap != null) {
                            for (DocumentSnapshot doc : snap.getDocuments()) {

                                Task t = doc.toObject(Task.class);
                                if (t != null) {
                                    t.setId(doc.getId());
                                    allTasks.add(t);
                                }
                            }
                        }

                        applyFilter();
                        updateTasksUI();
                    });
        }


        private void applyFilter() {
        String selected = safeSpinnerValue(spPriorityFilter); // All / High / Medium / Low / None

        filteredTasks.clear();

        String wanted = mapUiPriorityToStored(selected); // null for All, else "HIGH"/"MEDIUM"/"LOW"/"NONE"
        for (Task t : allTasks) {
            if (wanted == null) {
                filteredTasks.add(t);
            } else {
                String p = (t.getPriority() == null) ? "NONE" : t.getPriority();
                if (p.equals(wanted)) filteredTasks.add(t);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void updateTasksUI() {
        if (filteredTasks.isEmpty()) {
            rvTasks.setVisibility(View.GONE);
            emptyTasksState.setVisibility(View.VISIBLE);
        } else {
            rvTasks.setVisibility(View.VISIBLE);
            emptyTasksState.setVisibility(View.GONE);
        }
    }

    private void setupDueDatePicker() {
        btnPickDueDate.setOnClickListener(v -> openDatePicker());
    }

    private void openDatePicker() {
        Calendar c = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(Calendar.YEAR, year);
                    picked.set(Calendar.MONTH, month);
                    picked.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    picked.set(Calendar.HOUR_OF_DAY, 9);
                    picked.set(Calendar.MINUTE, 0);
                    picked.set(Calendar.SECOND, 0);
                    picked.set(Calendar.MILLISECOND, 0);

                    selectedDueAt = picked.getTimeInMillis();
                    btnPickDueDate.setText("Due: " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(selectedDueAt)));
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }

    private void setupAddTask() {
        btnAddTask.setOnClickListener(v -> {
            String title = etTaskTitle.getText().toString().trim();
            String desc = etTaskDescription.getText().toString().trim();

            if (title.isEmpty()) {
                showValidation("Task title is required");
                return;
            }

            if (tasksRef == null) {
                showValidation("Not connected. Please log in again.");
                return;
            }

            hideValidation();
            btnAddTask.setEnabled(false);

            // Module selection
            int modulePos = spTaskModule.getSelectedItemPosition();
            String moduleId = null;
            String moduleTitle = null;

            if (modulePos > 0 && modulePos - 1 < modules.size()) {
                Module m = modules.get(modulePos - 1);
                moduleId = m.getId();
                moduleTitle = m.getTitle();
            }

            // Priority selection
            String uiPriority = safeSpinnerValue(spTaskPriority); // None/High/Medium/Low
            String storedPriority = mapUiPriorityToStored(uiPriority); // "NONE"/"HIGH"/...

            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("description", desc);
            data.put("moduleId", moduleId);
            data.put("moduleTitle", moduleTitle);
            data.put("priority", storedPriority);
            data.put("dueAt", selectedDueAt); // can be null
            data.put("completed", false);
            data.put("createdAt", System.currentTimeMillis());

            tasksRef.add(data)
                    .addOnSuccessListener(doc -> {
                        btnAddTask.setEnabled(true);
                        clearForm();
                    })
                    .addOnFailureListener(e -> {
                        btnAddTask.setEnabled(true);
                        showValidation("Failed to save task: " + e.getMessage());
                    });
        });
    }

    private void clearForm() {
        etTaskTitle.setText("");
        etTaskDescription.setText("");
        spTaskModule.setSelection(0);
        spTaskPriority.setSelection(0);
        selectedDueAt = null;
        btnPickDueDate.setText("Pick due date (optional)");
        tvTaskValidation.setVisibility(View.GONE);
    }

    private String safeSpinnerValue(Spinner spinner) {
        if (spinner == null || spinner.getSelectedItem() == null) return "";
        return spinner.getSelectedItem().toString();
    }

    private String mapUiPriorityToStored(String ui) {
        if (ui == null) return "NONE";
        switch (ui.toLowerCase(Locale.ROOT)) {
            case "high": return "HIGH";
            case "medium": return "MEDIUM";
            case "low": return "LOW";
            case "none": return "NONE";
            case "all": return null; // filter-only
            default: return "NONE";
        }
    }

    private void showValidation(String msg) {
        tvTaskValidation.setText(msg);
        tvTaskValidation.setVisibility(View.VISIBLE);
    }

    private void hideValidation() {
        tvTaskValidation.setVisibility(View.GONE);
    }
}