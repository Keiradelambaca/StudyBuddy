package com.example.studybuddy;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirestoreRepo {

    private static final String COLLECTION_PROFILES = "profiles";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Returns the current user's profile document snapshot.
     * profiles/{uid}
     */
    public Task<DocumentSnapshot> getProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Return a failed Task so your onFailureListener runs cleanly
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalStateException("No logged-in user")
            );
        }

        return db.collection(COLLECTION_PROFILES)
                .document(user.getUid())
                .get();
    }

    /**
     * Saves/updates the current user's profile data.
     * profiles/{uid}
     */
    public Task<Void> saveProfile(String name, String dob, @Nullable String email) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return com.google.android.gms.tasks.Tasks.forException(
                    new IllegalStateException("No logged-in user")
            );
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("dob", dob);
        data.put("email", email);

        // merge = update fields without overwriting other fields
        return db.collection(COLLECTION_PROFILES)
                .document(user.getUid())
                .set(data, com.google.firebase.firestore.SetOptions.merge());
    }
}

