package com.example.studybuddy.focus;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.studybuddy.BaseBottomNavActivity;
import com.example.studybuddy.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class FocusActivity extends BaseBottomNavActivity {

    private TextInputEditText etStudyMins, etBreakMins, etIterations;
    private Button btnStartFocus, btnFindStudySpot;
    private TextView tvWeeklyFocusHours;
    private androidx.gridlayout.widget.GridLayout gridGarden;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_focus);
        setupBottomNav(R.id.nav_focus);

        tvWeeklyFocusHours = findViewById(R.id.tvWeeklyFocusHours);
        gridGarden = findViewById(R.id.gridGarden);

        etStudyMins = findViewById(R.id.etStudyMins);
        etBreakMins = findViewById(R.id.etBreakMins);
        etIterations = findViewById(R.id.etIterations);

        btnStartFocus = findViewById(R.id.btnStartFocus);
        btnFindStudySpot = findViewById(R.id.btnFindStudySpot);

        btnStartFocus.setOnClickListener(v -> {
            int study = parseOrDefault(etStudyMins, 25);
            int brk   = parseOrDefault(etBreakMins, 5);
            int iters = parseOrDefault(etIterations, 4);

            // basic validation
            if (study <= 0) study = 25;
            if (brk < 0) brk = 5;
            if (iters <= 0) iters = 1;

            Intent i = new Intent(this, FocusTimerActivity.class);
            i.putExtra("studyMins", study);
            i.putExtra("breakMins", brk);
            i.putExtra("iterations", iters);
            startActivity(i);
        });

        btnFindStudySpot.setOnClickListener(v -> {
            startActivity(new Intent(this, StudySpotActivity.class));
        });
        
        loadWeeklyFocusGarden();
    }

    private void loadWeeklyFocusGarden() {
        com.google.firebase.auth.FirebaseUser user =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            tvWeeklyFocusHours.setText("0.0 hours");
            renderGarden(0);
            return;
        }

        long now = System.currentTimeMillis();
        long sevenDaysAgo = now - java.util.concurrent.TimeUnit.DAYS.toMillis(7);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("focus_sessions")
                .whereGreaterThanOrEqualTo("completedAt", sevenDaysAgo)
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    long totalMins = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        Long mins = doc.getLong("studyMins");
                        if (mins != null) totalMins += mins;
                    }

                    double hours = totalMins / 60.0;
                    tvWeeklyFocusHours.setText(String.format(java.util.Locale.getDefault(), "%.1f hours", hours));

                    int flowers = (int) Math.floor(hours);
                    renderGarden(flowers);
                })
                .addOnFailureListener(e -> {
                    tvWeeklyFocusHours.setText("0.0 hours");
                    renderGarden(0);
                });
    }

    private void renderGarden(int flowers) {
        if (gridGarden == null) return;
        gridGarden.removeAllViews();

        int max = Math.min(flowers, 16); // 2 rows x 8
        int sizePx = (int) (24 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < max; i++) {
            android.widget.ImageView iv = new android.widget.ImageView(this);
            iv.setImageResource(com.example.studybuddy.R.drawable.ic_flower);

            androidx.gridlayout.widget.GridLayout.LayoutParams lp =
                    new androidx.gridlayout.widget.GridLayout.LayoutParams();
            lp.width = sizePx;
            lp.height = sizePx;
            lp.setMargins(8, 8, 8, 8);

            iv.setLayoutParams(lp);
            gridGarden.addView(iv);
        }
    }



    private int parseOrDefault(TextInputEditText et, int def) {
        if (et == null) return def;
        String s = et.getText() == null ? "" : et.getText().toString().trim();
        if (TextUtils.isEmpty(s)) return def;
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}