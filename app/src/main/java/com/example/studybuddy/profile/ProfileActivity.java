package com.example.studybuddy.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.studybuddy.BaseBottomNavActivity;
import com.example.studybuddy.LoginActivity;
import com.example.studybuddy.R;
import com.example.studybuddy.FirestoreRepo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends BaseBottomNavActivity {

    private TextView emailValue, uidValue, dobInput;
    private EditText nameInput;
    private Button saveProfileBtn, signOutBtn;

    private final FirestoreRepo repo = new FirestoreRepo();

    private String currentDobFromDb = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        setupBottomNav(R.id.nav_profile);

        emailValue = findViewById(R.id.emailValue);
        uidValue   = findViewById(R.id.uidValue);
        nameInput  = findViewById(R.id.nameInput);
        dobInput   = findViewById(R.id.dobInput);
        saveProfileBtn = findViewById(R.id.saveProfileBtn);
        signOutBtn     = findViewById(R.id.signOutBtn);

        // Extra safety: enforce DOB read-only even if XML changes later
        dobInput.setEnabled(false);
        dobInput.setFocusable(false);
        dobInput.setClickable(false);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        emailValue.setText(user.getEmail() == null ? "—" : user.getEmail());
        uidValue.setText(user.getUid());

        // Load profile from Firestore
        repo.getProfile()
                .addOnSuccessListener(d -> {
                    if (d == null || !d.exists()) {
                        nameInput.setText("");
                        dobInput.setText("—");
                        currentDobFromDb = null;
                        return;
                    }

                    Log.d("PROFILE", "data=" + d.getData());

                    // Try common field names (in case your DB uses different keys)
                    String name = d.getString("name");
                    if (name == null) name = d.getString("fullName");
                    if (name == null) name = d.getString("username");

                    String dob = d.getString("dob");
                    if (dob == null) dob = d.getString("dateOfBirth");
                    if (dob == null) dob = d.getString("DOB");

                    // Show values nicely
                    if (name != null) nameInput.setText(name);
                    else nameInput.setText("");

                    currentDobFromDb = dob;
                    dobInput.setText((dob == null || dob.trim().isEmpty()) ? "—" : dob);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );





        // Save ONLY name (DOB is read-only)
        saveProfileBtn.setOnClickListener(v -> {
            String newName = nameInput.getText().toString().trim();

            if (TextUtils.isEmpty(newName)) {
                nameInput.setError("Name required");
                nameInput.requestFocus();
                return;
            }

            saveProfileBtn.setEnabled(false);

            // Preserve DOB from DB (don’t allow changing)
            repo.updateName(newName)
                    .addOnSuccessListener(ignored -> {
                        Toast.makeText(this, "Name updated", Toast.LENGTH_SHORT).show();
                        saveProfileBtn.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        saveProfileBtn.setEnabled(true);
                    });

        });

        signOutBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }


}