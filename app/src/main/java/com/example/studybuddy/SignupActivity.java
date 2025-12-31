package com.example.studybuddy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText nameEditText, dobEditText, emailEditText, passwordEditText;
    private TextView validationText, loginText;
    private Button signupButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // Views
        nameEditText = findViewById(R.id.nameEditText);
        dobEditText = findViewById(R.id.dobEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        validationText = findViewById(R.id.validationText);
        loginText = findViewById(R.id.loginText);
        signupButton = findViewById(R.id.signupButton);

        // Firebase
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() != null) {
            goToLoginPrefilled(auth.getCurrentUser().getEmail());
            return;
        }

        signupButton.setOnClickListener(v -> doSignup());

        // back to Login text
        loginText.setOnClickListener(v -> {
            Intent i = new Intent(this,LoginActivity.class);
            // prefill
            i.putExtra("from_signup", true);
            i.putExtra("prefill_email", emailEditText.getText().toString().trim());
            startActivity(i);
            finish();
        });
    }

    private void doSignup() {
        validationText.setText("");

        final String name     = nameEditText.getText().toString().trim();
        final String dob      = dobEditText.getText().toString().trim(); // keep as simple string per brief
        final String email    = emailEditText.getText().toString().trim();
        final String password = passwordEditText.getText().toString().trim();

        // Basic validation
        if (name.isEmpty()) {
            validationText.setText("Please enter your name.");
            return;
        }
        if (dob.isEmpty()) {
            validationText.setText("Please enter your date of birth.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            validationText.setText("Please enter a valid email.");
            return;
        }
        if (password.length() < 6) {
            validationText.setText("Password must be at least 6 characters.");
            return;
        }

        setLoading(true);

        // Create account with Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        setLoading(false);
                        String msg = (task.getException() != null) ? task.getException().getMessage() : "Sign up failed.";
                        validationText.setText("Sign up failed: " + msg);
                        return;
                    }

                    // Save profile to Firestore
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        setLoading(false);
                        validationText.setText("Sign up completed, but user is null. Try logging in.");
                        goToLoginPrefilled(email);
                        return;
                    }

                    String uid = user.getUid();

                    Map<String, Object> profile = new HashMap<>();
                    profile.put("name", name);
                    profile.put("dob", dob);
                    profile.put("email", email);

                    db.collection("users").document(uid)
                            .set(profile)
                            .addOnSuccessListener(ignored -> {
                                setLoading(false);
                                // Login with prefilled email and message
                                goToLoginPrefilled(email);
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                // User is created but profile save failed, still let them log in
                                validationText.setText("Account created, but profile save failed: " + e.getMessage());
                                goToLoginPrefilled(email);
                            });
                });
    }

    private void setLoading(boolean loading) {
        signupButton.setEnabled(!loading);
        validationText.setVisibility(View.VISIBLE);
        validationText.setText(loading ? "Creating your account..." : "");
    }

    private void saveProfileLocally(String name) {
        getSharedPreferences("studybuddy_prefs", MODE_PRIVATE)
                .edit()
                .putString("profile_name", name)
                .apply();
    }


    private void goToLoginPrefilled(String email) {
        Intent i = new Intent(this, LoginActivity.class);
        i.putExtra("from_signup", true);
        i.putExtra("prefill_email", email);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
