package com.example.studybuddy.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;

import com.example.studybuddy.BaseBottomNavActivity;
import com.example.studybuddy.LoginActivity;
import com.example.studybuddy.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.studybuddy.FirestoreRepo;



public class ProfileActivity extends BaseBottomNavActivity {

    private TextView emailValue, uidValue;
    private EditText nameInput, dobInput;
    private Button saveProfileBtn, signOutBtn;
    private final FirestoreRepo repo = new FirestoreRepo();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        setupBottomNav(R.id.nav_modules);

        emailValue = findViewById(R.id.emailValue);
        uidValue   = findViewById(R.id.uidValue);
        nameInput  = findViewById(R.id.nameInput);
        dobInput   = findViewById(R.id.dobInput);
        saveProfileBtn = findViewById(R.id.saveProfileBtn);
        signOutBtn     = findViewById(R.id.signOutBtn);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        emailValue.setText("Email: " + (user.getEmail() == null ? "(none)" : user.getEmail()));
        uidValue.setText("Profile ID: " + user.getUid());

        repo.getProfile()
                .addOnSuccessListener(d -> {
                    if (d != null && d.exists()) {
                        nameInput.setText(d.getString("name"));
                        dobInput.setText(d.getString("dob"));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );

        // Save button
        saveProfileBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String dob  = dobInput.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(dob)) {
                Toast.makeText(this, "Name and DOB required", Toast.LENGTH_SHORT).show();
                return;
            }

            saveProfileBtn.setEnabled(false);
            String emailCopy = user.getEmail();
            repo.saveProfile(name, dob, emailCopy)
                    .addOnSuccessListener(ignored -> {
                        Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
                        saveProfileBtn.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        saveProfileBtn.setEnabled(true);
                    });
        });

        // Sign out button
        signOutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();

            // Return to login screen
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }
}
