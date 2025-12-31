package com.example.studybuddy.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studybuddy.R;
import com.example.studybuddy.adapter.TaskAdapter;
import com.example.studybuddy.adapter.Task;
import com.example.studybuddy.focus.FocusActivity;
import com.example.studybuddy.modules.ModulesActivity;
import com.example.studybuddy.profile.ProfileActivity;
import com.example.studybuddy.tasks.TasksActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Matches activity_home.xml
 * - Greeting + summary
 * - Focus card button
 * - Upcoming Tasks Card (RecyclerView + empty state)
 * - Quote card + refresh button (optional; if you removed refresh from XML, delete that part)
 * - Bottom navigation (5 pages)
 */
public class HomeActivity extends AppCompatActivity {

    private RecyclerView rvTasks;
    private View emptyTasksState;
    private TaskAdapter taskAdapter;

    private TextView tvGreeting;
    private TextView tvSummary;

    private TextView tvQuoteText;
    private TextView tvQuoteAuthor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bindViews();
        loadUserNameFromFirestoreAndSetGreeting();
        setupBottomNav();
        setupTasksRecycler();
        setupClicks();

        setGreeting();
        loadUpcomingTasks();     // replace with Room/Repo later if needed
        loadDailyQuote();        // replace with API later if needed
    }

    private void bindViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        tvSummary = findViewById(R.id.tvSummary);

        rvTasks = findViewById(R.id.rvTasks);
        emptyTasksState = findViewById(R.id.emptyTasksState);

        // Quote views (if you kept them)
        tvQuoteText = findViewById(R.id.tvQuoteText);
        tvQuoteAuthor = findViewById(R.id.tvQuoteAuthor);
    }

    private void setupTasksRecycler() {
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(new ArrayList<>());
        rvTasks.setAdapter(taskAdapter);
    }

    private void setupClicks() {
        // Focus button
        findViewById(R.id.btnStartFocus).setOnClickListener(v ->
                startActivity(new Intent(this, FocusActivity.class))
        );

        // "See all" tasks
        findViewById(R.id.tvSeeAll).setOnClickListener(v ->
                startActivity(new Intent(this, TasksActivity.class))
        );
    }

    private void setGreeting() {
        String greeting = getTimeGreeting();
        tvGreeting.setText(greeting + " 👋");

        // You can replace this summary with real counts from DB later
        tvSummary.setText("You have tasks to review today");
    }

    @NonNull
    private String getTimeGreeting() {
        int hour = Integer.parseInt(new SimpleDateFormat("HH", Locale.getDefault()).format(new Date()));
        if (hour < 12) return "Good morning";
        if (hour < 18) return "Good afternoon";
        return "Good evening";
    }

    private void setGreetingText(String name) {
        String greeting = getTimeGreeting(); // "Good morning" etc.

        if (name != null) name = name.trim();

        if (name == null || name.isEmpty()) {
            tvGreeting.setText(greeting + " 👋");
        } else {
            tvGreeting.setText(greeting + ", " + name + " 👋");
        }
    }

    private void loadUserNameFromFirestoreAndSetGreeting() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            tvGreeting.setText(getTimeGreeting() + " 👋");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String name = "";
                    if (doc != null && doc.exists()) {
                        name = doc.getString("name");
                    }
                    setGreetingText(name);
                })
                .addOnFailureListener(e -> setGreetingText("")); // fallback
    }


    /**
     * TODO: Replace this with real Room query / repository call.
     * For now, it loads a small sample list so your UI is working end-to-end.
     */
    private void loadUpcomingTasks() {
        // ---- SAMPLE DATA (replace with DB) ----
        List<Task> tasks = new ArrayList<>();
        tasks.add(new Task("Mobile Dev CA2", "Tomorrow", "High"));
        tasks.add(new Task("Distributed Systems Lab", "Fri", "Medium"));
        tasks.add(new Task("Revise Design Patterns", "Sun", "Low"));
        // --------------------------------------

        taskAdapter.setTasks(tasks);
        updateTasksEmptyState(tasks);

        // Optional: update summary text based on list size
        tvSummary.setText("You have " + tasks.size() + " upcoming tasks");
    }

    private void updateTasksEmptyState(List<Task> tasks) {
        boolean isEmpty = (tasks == null || tasks.isEmpty());
        rvTasks.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyTasksState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    /**
     * TODO: Replace with your Quotes API call.
     */
    private void loadDailyQuote() {
        if (tvQuoteText == null || tvQuoteAuthor == null) return;

        // Simple placeholder (swap with API response)
        tvQuoteText.setText("“Success is the sum of small efforts repeated daily.”");
        tvQuoteAuthor.setText("— Robert Collier");
    }

    /**
     * Bottom navigation for 5 pages.
     * This matches bottom_nav_menu.xml ids:
     * nav_home, nav_tasks, nav_modules, nav_focus, nav_profile
     */
    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;

            Intent intent = null;

            if (id == R.id.nav_tasks) intent = new Intent(this, TasksActivity.class);
            else if (id == R.id.nav_modules) intent = new Intent(this, ModulesActivity.class);
            else if (id == R.id.nav_focus) intent = new Intent(this, FocusActivity.class);
            else if (id == R.id.nav_profile) intent = new Intent(this, ProfileActivity.class);

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
            return true;
        });
    }
}


