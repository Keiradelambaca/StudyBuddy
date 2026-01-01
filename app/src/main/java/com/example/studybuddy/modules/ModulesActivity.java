package com.example.studybuddy.modules;

import android.os.Bundle;
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
import com.example.studybuddy.adapter.ModulesAdapter;
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

            if (modulesRef == null) {
                showValidation("Not connected. Please log in again.");
                return;
            }

            hideValidation();

            String year = safeSpinnerValue(spYear);
            String semester = safeSpinnerValue(spSemester);
            String dayOfWeek = safeSpinnerValue(spDayOfWeek);

            // Build Firestore map
            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("description", description);
            data.put("year", year);
            data.put("semester", semester);
            data.put("dayOfWeek", dayOfWeek);
            data.put("createdAt", System.currentTimeMillis());

            btnAddModule.setEnabled(false);

            // Add new module document
            modulesRef.add(data)
                    .addOnSuccessListener(ref -> {
                        btnAddModule.setEnabled(true);
                        clearForm();

                        // No need to manually add to list:
                        // snapshot listener will refresh automatically.
                    })
                    .addOnFailureListener(e -> {
                        btnAddModule.setEnabled(true);
                        showValidation("Failed to save module: " + e.getMessage());
                    });
        });
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
}