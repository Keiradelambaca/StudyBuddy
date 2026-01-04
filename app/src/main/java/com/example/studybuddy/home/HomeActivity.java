package com.example.studybuddy.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.studybuddy.BaseBottomNavActivity;
import com.example.studybuddy.R;
import com.example.studybuddy.adapter.TasksAdapter;
import com.example.studybuddy.adapter.Task;
import com.example.studybuddy.focus.FocusActivity;
import com.example.studybuddy.tasks.TaskDetailActivity;
import com.example.studybuddy.tasks.TasksActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


import com.android.volley.DefaultRetryPolicy;
import com.android.volley.toolbox.JsonArrayRequest;
import org.json.JSONObject;

/**
 * Matches activity_home.xml
 * - Greeting + summary
 * - Focus card button
 * - Upcoming Tasks Card (RecyclerView + empty state)
 * - Quote card + refresh button (optional; if you removed refresh from XML, delete that part)
 * - Bottom navigation (5 pages)
 */
public class HomeActivity extends BaseBottomNavActivity {

    private RecyclerView rvTasks;
    private View emptyTasksState;
    private TasksAdapter taskAdapter;
    private List<Task> tasks = new ArrayList<>();
    private TextView tvGreeting, tvSummary, tvQuoteText, tvQuoteAuthor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setupBottomNav(R.id.nav_home);

        bindViews();
        loadUserNameFromFirestoreAndSetGreeting();
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
        taskAdapter = new TasksAdapter(tasks, task -> {
            Intent i = new Intent(HomeActivity.this, TaskDetailActivity.class);
            i.putExtra("TASK_ID", task.getId());
            startActivity(i);
        });

        rvTasks.setAdapter(taskAdapter);
    }

    private void setupClicks() {
        // Focus button
        findViewById(R.id.btnStartFocus).setOnClickListener(v ->
                startActivity(new Intent(this, FocusActivity.class))
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

    private void loadUpcomingTasks() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("tasks")
                .orderBy("dueAt", Query.Direction.ASCENDING)
                .limit(20) // grab some then filter
                .get()
                .addOnSuccessListener(snap -> {

                    long now = System.currentTimeMillis();
                    List<Task> upcoming = new ArrayList<>();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Task t = doc.toObject(Task.class);
                        if (t == null) continue;

                        t.setId(doc.getId());

                        long dueMillis = 0;

                        Object raw = doc.get("dueAt");
                        if (raw instanceof Long) {
                            dueMillis = (Long) raw;
                        } else if (raw instanceof com.google.firebase.Timestamp) {
                            dueMillis = ((com.google.firebase.Timestamp) raw).toDate().getTime();
                        }

                        if (dueMillis >= now) upcoming.add(t);
                        if (upcoming.size() == 3) break;
                    }

                    taskAdapter.setTasks(upcoming);
                    updateTasksEmptyState(upcoming);
                    tvSummary.setText("You have " + upcoming.size() + " upcoming tasks");
                });

    }

    private void updateTasksEmptyState(List<Task> tasks) {
        boolean isEmpty = (tasks == null || tasks.isEmpty());
        rvTasks.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyTasksState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    // QUOTE OF THE DAY
    private void loadDailyQuote() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://zenquotes.io/api/today";

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, url, null,
                arr -> {
                    try {
                        JSONObject obj = arr.getJSONObject(0);
                        String quote = obj.optString("q");
                        String author = obj.optString("a");

                        tvQuoteText.setText("“" + quote + "”");
                        tvQuoteAuthor.setText("— " + author);
                    } catch (Exception e) {
                        tvQuoteText.setText("“Small progress is still progress.”");
                        tvQuoteAuthor.setText("— StudyBuddy");
                    }
                },
                err -> {
                    String msg = (err.getMessage() != null) ? err.getMessage() : err.toString();
                    Log.e("QOTD", "Volley error: " + msg, err);

                    tvQuoteText.setText("“Small progress is still progress.”");
                    tvQuoteAuthor.setText("— StudyBuddy");
                }
        );

        // optional: avoid hanging forever
        req.setRetryPolicy(new DefaultRetryPolicy(
                8000,
                2,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(req);
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadUpcomingTasks();
    }

}