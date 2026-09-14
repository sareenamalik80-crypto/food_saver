package com.example.food_saver.auth;

import com.example.food_saver.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Single place that talks to FirebaseAuth + the "users" Firestore collection.
 * Verification documents are stored as Base64 strings directly in the
 * Firestore user doc (see ImageUtils) since Firebase Storage requires the
 * paid Blaze plan.
 */
public class AuthRepository {

    private static final String USERS_COLLECTION = "users";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public interface AuthCallback {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }

    public AuthRepository() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    /**
     * Creates a FirebaseAuth account, then writes the Firestore user doc
     * with the verification document already compressed to a Base64 string
     * by the caller (see ImageUtils.compressImageToBase64 — must be done
     * on a background thread before calling this).
     * role must be User.ROLE_DONOR or User.ROLE_NGO (admins are not self-registered).
     * New accounts always start as STATUS_PENDING until admin verifies them.
     */
    public void signUp(String name, String email, String password, String phone,
                       String address, String role, String documentBase64, AuthCallback callback) {

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        callback.onFailure("Account creation failed. Try again.");
                        return;
                    }
                    saveUserDocument(firebaseUser.getUid(), name, email, phone, address,
                            role, documentBase64, callback);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private void saveUserDocument(String uid, String name, String email, String phone,
                                  String address, String role, String documentBase64,
                                  AuthCallback callback) {

        long now = System.currentTimeMillis();
        User newUser = new User(uid, name, email, phone, address,
                role, User.STATUS_PENDING, documentBase64, now);

        firestore.collection(USERS_COLLECTION)
                .document(uid)
                .set(newUser)
                .addOnSuccessListener(unused -> callback.onSuccess(newUser))
                .addOnFailureListener(e ->
                        callback.onFailure("Signup saved auth but failed to save profile: " + e.getMessage()));
    }

    /**
     * Logs in with email/password, then fetches the Firestore user doc so the
     * caller can route by role + check verification status.
     */
    public void login(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        callback.onFailure("Login failed. Try again.");
                        return;
                    }
                    fetchUserProfile(firebaseUser.getUid(), callback);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void fetchUserProfile(String uid, AuthCallback callback) {
        firestore.collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onFailure("No profile found for this account.");
                        return;
                    }
                    User user = doc.toObject(User.class);
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /** Call this on app start to auto-route an already-logged-in user. */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void logout() {
        auth.signOut();
    }
}