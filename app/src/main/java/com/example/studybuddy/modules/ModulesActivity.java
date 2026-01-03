package com.example.studybuddy.modules;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studybuddy.BaseBottomNavActivity;
import com.example.studybuddy.R;
import com.example.studybuddy.adapter.Module;
import com.example.studybuddy.adapter.ModulesAdapter;
import com.example.studybuddy.adapter.TimetableEvent;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.TimePickerDialog;
import java.util.Calendar;
import java.util.Locale;


public class ModulesActivity extends BaseBottomNavActivity {

    // RecyclerView + empty state
    private RecyclerView rvModules;
    private LinearLayout emptyModulesState;

    // Add module form
    private EditText etModuleTitle;
    private EditText etModuleDescription;
    private Spinner spYear, spSemester, spDayOfWeek;
    private Button btnAddModule;
    private TextView tvModuleValidation;

    // Data
    private final List<Module> modules = new ArrayList<>();
    private ModulesAdapter adapter;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private CollectionReference modulesRef;
    private ListenerRegistration modulesListener;

    // Calendar
    private Button btnPickStartTime, btnPickEndTime;
    private int selectedStartMin = -1;
    private int selectedEndMin = -1;
    private CollectionReference timetableRef;
    private FrameLayout calendarContainer;
    private MaterialButtonToggleGroup toggleCalendarView;
    private View monthView;
    private View weekView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modules);

        setupBottomNav(R.id.nav_modules);

        bindViews();
        setupRecyclerView();
        setupSpinnersIfEmpty();   // safe helper (won't override if already set)
        setupFirebase();
        setupAddModule();
        setupTimePickers();

        // Load from DB and keep UI in sync
        listenForModules();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (modulesListener != null) {
            modulesListener.remove();
            modulesListener = null;
        }
    }

    private void bindViews() {
        rvModules = findViewById(R.id.rvModules);
        emptyModulesState = findViewById(R.id.emptyModulesState);

        etModuleTitle = findViewById(R.id.etModuleTitle);
        etModuleDescription = findViewById(R.id.etModuleDescription);
        spYear = findViewById(R.id.spYear);
        spSemester = findViewById(R.id.spSemester);
        spDayOfWeek = findViewById(R.id.spDayOfWeek);

        btnAddModule = findViewById(R.id.btnAddModule);
        tvModuleValidation = findViewById(R.id.tvModuleValidation);

        calendarContainer = findViewById(R.id.calendarContainer);
        toggleCalendarView = findViewById(R.id.toggleCalendarView);
        btnPickStartTime = findViewById(R.id.btnPickStartTime);
        btnPickEndTime = findViewById(R.id.btnPickEndTime);

    }

    private void setupRecyclerView() {
        adapter = new ModulesAdapter(modules, module -> {
            // TODO: open ModuleDetailActivity later if you want
            // Intent intent = new Intent(this, ModuleDetailActivity.class);
            // intent.putExtra("MODULE_ID", module.getId());
            // startActivity(intent);
        });

        rvModules.setLayoutManager(new LinearLayoutManager(this));
        rvModules.setAdapter(adapter);
    }

    /**
     * If you already set spinner adapters elsewhere, this won't override them.
     * This is just to prevent spinners being empty/null during early testing.
     */
    private void setupSpinnersIfEmpty() {
        if (spYear.getAdapter() == null) {
            List<String> years = Arrays.asList("1", "2", "3", "4");
            spYear.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, years));
        }

        if (spSemester.getAdapter() == null) {
            List<String> semesters = Arrays.asList("1", "2");
            spSemester.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, semesters));
        }

        if (spDayOfWeek.getAdapter() == null) {
            List<String> days = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
            spDayOfWeek.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, days));
        }
    }

    private void setupFirebase() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        Log.d("ModulesActivity", "currentUser=" + (user == null ? "null" : user.getUid()));

        if (user == null) {
            // If you want, redirect to Login here. For now show a message.
            showError("You must be logged in to manage modules.");
            btnAddModule.setEnabled(false);
            return;
        }


        // Store modules per user:
        // users/{uid}/modules/{moduleId}
        modulesRef = db.collection("users")
                .document(user.getUid())
                .collection("modules");

        timetableRef = db.collection("users")
                .document(user.getUid())
                .collection("timetable_events");

    }

    private void listenForModules() {
        if (modulesRef == null) {
            updateModulesUI();
            return;
        }

        // Live updates whenever modules change
        modulesListener = modulesRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        showError("Failed to load modules: " + err.getMessage());
                        return;
                    }
                    if (snap == null) {
                        updateModulesUI();
                        return;
                    }

                    modules.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        // Requires Module to have a public no-arg constructor + setters
                        Module m = doc.toObject(Module.class);

                        if (m != null) {
                            // IMPORTANT: make sure the module has its document id stored
                            // If your Module class has setId(), use it:
                            m.setId(doc.getId());
                            modules.add(m);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    updateModulesUI();
                });
    }

    private void setupAddModule() {
        btnAddModule.setOnClickListener(v -> {
            String title = etModuleTitle.getText().toString().trim();
            String description = etModuleDescription.getText().toString().trim();

            if (title.isEmpty()) {
                showValidation("Module title is required");
                return;
            }

            if (selectedStartMin < 0 || selectedEndMin < 0) {
                showValidation("Please pick start and end time");
                return;
            }

            if (selectedEndMin <= selectedStartMin) {
                showValidation("End time must be after start time");
                return;
            }

            if (modulesRef == null || timetableRef == null) {
                showValidation("Not connected. Please log in again.");
                return;
            }

            hideValidation();

            String year = safeSpinnerValue(spYear);
            String semester = safeSpinnerValue(spSemester);

            // Convert day string -> Calendar day-of-week int
            int dayOfWeek = mapDayToCalendarConstant(safeSpinnerValue(spDayOfWeek));

            btnAddModule.setEnabled(false);

            // 1) Save module
            Map<String, Object> moduleData = new HashMap<>();
            moduleData.put("title", title);
            moduleData.put("description", description);
            moduleData.put("year", year);
            moduleData.put("semester", semester);
            moduleData.put("createdAt", System.currentTimeMillis());

            modulesRef.add(moduleData)
                    .addOnSuccessListener(moduleDoc -> {
                        String moduleId = moduleDoc.getId();

                        // 2) Save timetable recurring event
                        TimetableEvent event = new TimetableEvent(
                                moduleId,
                                title,
                                dayOfWeek,
                                selectedStartMin,
                                selectedEndMin
                        );

                        timetableRef.add(event)
                                .addOnSuccessListener(eventDoc -> {
                                    btnAddModule.setEnabled(true);
                                    clearForm();
                                    resetTimeButtons();
                                    // listeners refresh UI automatically
                                })
                                .addOnFailureListener(e -> {
                                    btnAddModule.setEnabled(true);
                                    showValidation("Saved module, but failed to create timetable event: " + e.getMessage());
                                });

                    })
                    .addOnFailureListener(e -> {
                        btnAddModule.setEnabled(true);
                        showValidation("Failed to save module: " + e.getMessage());
                    });
        });
    }

    private void resetTimeButtons() {
        selectedStartMin = -1;
        selectedEndMin = -1;
        btnPickStartTime.setText("Start time");
        btnPickEndTime.setText("End time");
    }

    private int mapDayToCalendarConstant(String day) {
        if (day == null) return Calendar.MONDAY;
        switch (day.toLowerCase(Locale.ROOT)) {
            case "sunday": return Calendar.SUNDAY;
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            default: return Calendar.MONDAY;
        }
    }

    private void updateModulesUI() {
        if (modules.isEmpty()) {
            rvModules.setVisibility(View.GONE);
            emptyModulesState.setVisibility(View.VISIBLE);
        } else {
            rvModules.setVisibility(View.VISIBLE);
            emptyModulesState.setVisibility(View.GONE);
        }
    }

    private void clearForm() {
        etModuleTitle.setText("");
        etModuleDescription.setText("");
        spYear.setSelection(0);
        spSemester.setSelection(0);
        spDayOfWeek.setSelection(0);
        tvModuleValidation.setVisibility(View.GONE);
    }

    private String safeSpinnerValue(Spinner spinner) {
        if (spinner == null || spinner.getSelectedItem() == null) return "";
        return spinner.getSelectedItem().toString();
    }

    private void showValidation(String msg) {
        tvModuleValidation.setText(msg);
        tvModuleValidation.setVisibility(View.VISIBLE);
    }

    private void hideValidation() {
        tvModuleValidation.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        // simple UI error; you can replace with Snackbar if you want
        showValidation(msg);
    }

    private void setupCalendarSwitcher() {
        monthView = getLayoutInflater().inflate(R.layout.view_calendar_month, calendarContainer, false);
        weekView  = getLayoutInflater().inflate(R.layout.view_calendar_week, calendarContainer, false);

        // Default: Month
        showMonth();

        toggleCalendarView.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnMonth) showMonth();
            else if (checkedId == R.id.btnWeek) showWeek(false);
            else if (checkedId == R.id.btnDay) showWeek(true);
        });
    }

    private void showMonth() {
        calendarContainer.removeAllViews();
        calendarContainer.addView(monthView);

        // TODO: init month calendar + click date -> load events
    }

    private void showWeek(boolean singleDay) {
        calendarContainer.removeAllViews();
        calendarContainer.addView(weekView);

        // TODO: init WeekView
        // If singleDay=true, configure WeekView to show one day; else 5/7 days.
    }

    private void setupTimePickers() {
        btnPickStartTime.setOnClickListener(v -> openTimePicker(true));
        btnPickEndTime.setOnClickListener(v -> openTimePicker(false));
    }

    private void openTimePicker(boolean isStart) {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, pickedHour, pickedMinute) -> {
                    int mins = pickedHour * 60 + pickedMinute;

                    if (isStart) {
                        selectedStartMin = mins;
                        btnPickStartTime.setText(formatTime(mins));
                    } else {
                        selectedEndMin = mins;
                        btnPickEndTime.setText(formatTime(mins));
                    }
                },
                hour,
                minute,
                true
        );
        dialog.show();
    }

    private String formatTime(int mins) {
        int h = mins / 60;
        int m = mins % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", h, m);
    }
}