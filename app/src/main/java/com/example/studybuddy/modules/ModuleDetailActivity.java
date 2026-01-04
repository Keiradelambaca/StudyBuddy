package com.example.studybuddy.modules;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.studybuddy.BaseBottomNavActivity;
import com.example.studybuddy.R;
import com.example.studybuddy.adapter.Module;
import com.example.studybuddy.adapter.TimetableEvent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.*;

public class ModuleDetailActivity extends BaseBottomNavActivity {

    private EditText etTitle, etDescription;
    private Spinner spYear, spSemester, spDayOfWeek;
    private Button btnPickStart, btnPickEnd, btnSave;

    private int selectedStartMin = -1;
    private int selectedEndMin = -1;

    private FirebaseUser user;
    private FirebaseFirestore db;

    private String moduleId;
    private DocumentReference moduleDoc;

    private String timetableEventId = null; // we’ll query it by moduleId

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_detail);
        setupBottomNav(R.id.nav_modules);

        moduleId = getIntent().getStringExtra("MODULE_ID");
        if (moduleId == null) {
            finish();
            return;
        }

        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        spYear = findViewById(R.id.spYear);
        spSemester = findViewById(R.id.spSemester);
        spDayOfWeek = findViewById(R.id.spDayOfWeek);
        btnPickStart = findViewById(R.id.btnPickStart);
        btnPickEnd = findViewById(R.id.btnPickEnd);
        btnSave = findViewById(R.id.btnSave);

        setupSpinners();

        if (user == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        moduleDoc = db.collection("users").document(user.getUid())
                .collection("modules").document(moduleId);

        btnPickStart.setOnClickListener(v -> pickTime(true));
        btnPickEnd.setOnClickListener(v -> pickTime(false));
        btnSave.setOnClickListener(v -> saveChanges());

        loadModule();
        loadTimetableEventForModule();
    }

    private void setupSpinners() {
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Year 1", "Year 2", "Year 3", "Year 4")
        );
        spYear.setAdapter(yearAdapter);

        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Semester 1", "Semester 2")
        );
        spSemester.setAdapter(semAdapter);

        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        );
        spDayOfWeek.setAdapter(dayAdapter);
    }

    private void loadModule() {
        moduleDoc.get().addOnSuccessListener(doc -> {
            Module m = doc.toObject(Module.class);
            if (m == null) return;

            etTitle.setText(m.getTitle());
            etDescription.setText(m.getDescription() == null ? "" : m.getDescription());

            // Best-effort: pick year/semester from metaText if your Module stores ints, update below.
            // If you already have getYear()/getSemester(), replace this section with direct selection.
        });
    }

    private void loadTimetableEventForModule() {
        CollectionReference ref = db.collection("users").document(user.getUid())
                .collection("timetable_events");

        ref.whereEqualTo("moduleId", moduleId)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) return;

                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    timetableEventId = doc.getId();

                    TimetableEvent e = doc.toObject(TimetableEvent.class);
                    if (e == null) return;

                    // Day selection
                    spDayOfWeek.setSelection(calendarDayToSpinnerIndex(e.getDayOfWeek()));

                    selectedStartMin = e.getStartMin();
                    selectedEndMin = e.getEndMin();

                    btnPickStart.setText("Start: " + formatTime(selectedStartMin));
                    btnPickEnd.setText("End: " + formatTime(selectedEndMin));
                });
    }

    private void pickTime(boolean isStart) {
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);

        new TimePickerDialog(this, (view, h, m) -> {
            int minsFromMidnight = h * 60 + m;

            if (isStart) {
                selectedStartMin = minsFromMidnight;
                btnPickStart.setText("Start: " + formatTime(selectedStartMin));
            } else {
                selectedEndMin = minsFromMidnight;
                btnPickEnd.setText("End: " + formatTime(selectedEndMin));
            }
        }, hour, min, true).show();
    }

    private void saveChanges() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Title required");
            return;
        }

        // Update module doc
        Map<String, Object> moduleUpdates = new HashMap<>();
        moduleUpdates.put("title", title);
        moduleUpdates.put("description", desc.isEmpty() ? null : desc);

        moduleDoc.update(moduleUpdates)
                .addOnSuccessListener(v -> {
                    // Update timetable event too (if exists)
                    if (timetableEventId != null && selectedStartMin >= 0 && selectedEndMin >= 0) {
                        updateTimetableEvent(title);
                    } else {
                        Toast.makeText(this, "Saved.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void updateTimetableEvent(String title) {
        int dayOfWeekConst = spinnerIndexToCalendarDay(spDayOfWeek.getSelectedItemPosition());

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("dayOfWeek", dayOfWeekConst);
        updates.put("startMin", selectedStartMin);
        updates.put("endMin", selectedEndMin);

        db.collection("users").document(user.getUid())
                .collection("timetable_events")
                .document(timetableEventId)
                .update(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Saved.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Timetable update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private String formatTime(int mins) {
        int h = mins / 60;
        int m = mins % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", h, m);
    }

    private int calendarDayToSpinnerIndex(int calendarConst) {
        // Calendar.SUNDAY..SATURDAY == 1..7, spinner is 0..6
        return Math.max(0, Math.min(6, calendarConst - 1));
    }

    private int spinnerIndexToCalendarDay(int idx) {
        return idx + 1; // back to Calendar const
    }
}
