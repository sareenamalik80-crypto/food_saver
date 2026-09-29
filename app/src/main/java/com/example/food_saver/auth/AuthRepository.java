package com.example.food_saver.auth;

import com.example.food_saver.models.User;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthRepository {

    private static final String USERS_COLLECTION = "users";

    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public interface AuthCallback {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }


    public interface GoogleAuthCallback {
        void onExistingUser(User user);
        void onNewUser(String uid, String displayName, String email);
        void onFailure(String errorMessage);
    }

    public AuthRepository() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

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

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public void logout() {
        auth.signOut();
    }

    public void sendPasswordReset(String email, SimpleCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }


    public void continueWithGoogleCredential(AuthCredential credential, GoogleAuthCallback callback) {
        auth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser firebaseUser = result.getUser();
                    if (firebaseUser == null) {
                        callback.onFailure("Google sign-in failed. Try again.");
                        return;
                    }
                    String uid = firebaseUser.getUid();
                    firestore.collection(USERS_COLLECTION).document(uid).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    callback.onExistingUser(doc.toObject(User.class));
                                } else {
                                    callback.onNewUser(uid, firebaseUser.getDisplayName(), firebaseUser.getEmail());
                                }
                            })
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }


    public void completeGoogleSignUp(String uid, String name, String email, String phone,
                                     String address, String role, String documentBase64,
                                     AuthCallback callback) {
        saveUserDocument(uid, name, email, phone, address, role, documentBase64, callback);
    }
}