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

import com.example.studybuddy.home.HomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView validationText, signupText;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Views
        emailEditText    = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton      = findViewById(R.id.loginButton);
        validationText   = findViewById(R.id.validationText);
        signupText       = findViewById(R.id.signupText);

        // Firebase
        auth = FirebaseAuth.getInstance();

        // Auto-route if already signed in
        FirebaseUser current = auth.getCurrentUser();
        if (current != null) {
            goToHome();
            return;
        }

        // If coming back from sign up: prefill email and prompt to log in
        boolean fromSignup = getIntent().getBooleanExtra("from_signup", false);
        if (fromSignup) {
            String prefill = getIntent().getStringExtra("prefill_email");
            if (prefill != null) {
                emailEditText.setText(prefill);
                emailEditText.setSelection(prefill.length());
            }
            validationText.setText("Account created. Please log in with your new credentials.");
            passwordEditText.requestFocus();
        }

        // Sign in
        loginButton.setOnClickListener(v -> {
            validationText.setText("");

            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            boolean validEmail = email.length() > 0 && Patterns.EMAIL_ADDRESS.matcher(email).matches();
            boolean validPassword = password.length() >= 6;

            if (!validEmail && !validPassword) {
                validationText.setText("Invalid email and password.");
                return;
            }
            if (!validEmail) {
                validationText.setText("Invalid email.");
                return;
            }
            if (!validPassword) {
                validationText.setText("Password must be at least 6 characters.");
                return;
            }

            setLoading(true);

            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            goToHome();
                        } else {
                            String msg = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Login failed.";
                            validationText.setText("Login failed: " + msg);
                        }
                    });
        });

        // Go to Signup screen
        signupText.setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class))
        );
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        validationText.setVisibility(View.VISIBLE);
        validationText.setText(loading ? "Signing in..." : "");
    }

    private void goToHome() {
        Intent i = new Intent(this, HomeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}
