package com.example.studybuddy.focus;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;

import com.example.studybuddy.BaseBottomNavActivity;
import com.example.studybuddy.R;

import java.util.Locale;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FocusTimerActivity extends BaseBottomNavActivity {

    private TextView tvPhase, tvIteration, tvTimer;
    private Button btnEndSession;
    private int studyMins, breakMins, iterations;
    private int currentIteration = 1;
    private boolean isStudyPhase = true;
    private CountDownTimer timer;
    private boolean sessionSaved = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus_timer);
        setupBottomNav(R.id.nav_focus);

        tvPhase = findViewById(R.id.tvPhase);
        tvIteration = findViewById(R.id.tvIteration);
        tvTimer = findViewById(R.id.tvTimer);
        btnEndSession = findViewById(R.id.btnEndSession);

        studyMins = getIntent().getIntExtra("studyMins", 25);
        breakMins = getIntent().getIntExtra("breakMins", 5);
        iterations = getIntent().getIntExtra("iterations", 4);

        btnEndSession.setOnClickListener(v -> finish());

        startPhase(true);
    }

    private void startPhase(boolean study) {
        isStudyPhase = study;

        tvPhase.setText(study ? "Study" : "Break");
        tvIteration.setText(String.format(Locale.getDefault(),
                "Cycle %d of %d", currentIteration, iterations));

        long durationMs = (long) (study ? studyMins : breakMins) * 60_000L;

        // If break is 0 mins, skip it automatically.
        if (!study && durationMs <= 0) {
            onPhaseFinished();
            return;
        }

        if (timer != null) timer.cancel();

        timer = new CountDownTimer(durationMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText(formatTime(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                onPhaseFinished();
            }
        }.start();

        tvTimer.setText(formatTime(durationMs));
    }

    private void onPhaseFinished() {
        if (isStudyPhase) {
            // finished study -> go to break
            startPhase(false);
        } else {
            // finished break -> next iteration study
            if (currentIteration >= iterations) {
                tvPhase.setText("Complete");
                tvIteration.setText(String.format(Locale.getDefault(),
                        "You finished %d cycle(s).", iterations));
                tvTimer.setText("🎉");

                saveCompletedSession();
                return;
            }
            currentIteration++;
            startPhase(true);
        }
    }

    private void saveCompletedSession() {
        if (sessionSaved) return;
        sessionSaved = true;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        long completedAt = System.currentTimeMillis();
        int totalStudyMins = studyMins * iterations;

        Map<String, Object> data = new HashMap<>();
        data.put("completedAt", completedAt);
        data.put("studyMins", totalStudyMins);
        data.put("studyMinsPerCycle", studyMins);
        data.put("breakMinsPerCycle", breakMins);
        data.put("iterations", iterations);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("focus_sessions")
                .add(data);
    }


    private String formatTime(long ms) {
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec);
    }

    @Override
    protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
}
