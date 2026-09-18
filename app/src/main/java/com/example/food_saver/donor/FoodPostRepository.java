package com.example.food_saver.donor;

import com.example.food_saver.models.FoodPost;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodPostRepository {

    private static final String POSTS_COLLECTION = "foodPosts";

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public interface UploadCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface PostsListCallback {
        void onUpdate(List<FoodPost> posts);
        void onError(String errorMessage);
    }

    public interface SinglePostCallback {
        void onLoaded(FoodPost post);
        void onError(String errorMessage);
    }

    public FoodPostRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /**
     * Saves a food post with the photo already compressed to a Base64
     * string by the caller (see ImageUtils.compressImageToBase64 — must be
     * done on a background thread before calling this).
     */
    public void createFoodPost(String photoBase64, String donorName, String foodName,
                               String quantity, String description, long expiresAt,
                               String pickupLocation, String pickupWindow,
                               double pickupLat, double pickupLng,
                               UploadCallback callback) {

        if (auth.getCurrentUser() == null) {
            callback.onFailure("Not logged in.");
            return;
        }
        String donorId = auth.getCurrentUser().getUid();
        long now = System.currentTimeMillis();

        FoodPost post = new FoodPost(donorId, donorName, foodName, photoBase64, quantity,
                description, now, expiresAt, FoodPost.STATUS_AVAILABLE, pickupLocation,
                pickupWindow, pickupLat, pickupLng);

        firestore.collection(POSTS_COLLECTION)
                .add(post)
                .addOnSuccessListener(docRef ->
                        docRef.update("foodId", docRef.getId())
                                .addOnSuccessListener(unused -> callback.onSuccess())
                                .addOnFailureListener(e ->
                                        callback.onFailure("Post saved but failed to set foodId: " + e.getMessage())))
                .addOnFailureListener(e -> callback.onFailure("Failed to save food post: " + e.getMessage()));
    }

    /** Fetches a single post by ID — used to pre-fill the Edit screen. */
    public void getFoodPost(String foodId, SinglePostCallback callback) {
        firestore.collection(POSTS_COLLECTION).document(foodId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        callback.onError("This post no longer exists.");
                        return;
                    }
                    FoodPost post = doc.toObject(FoodPost.class);
                    callback.onLoaded(post);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Updates an existing post's editable fields. photoBase64 is optional —
     * pass null to leave the existing photo unchanged (donor didn't retake it).
     * Only posts still "available" should be editable — enforce that check
     * in the UI layer (FoodPost.isEditable()) before calling this.
     */
    public void updateFoodPost(String foodId, String photoBase64OrNull, String foodName,
                               String quantity, String description, long expiresAt,
                               String pickupLocation, String pickupWindow,
                               double pickupLat, double pickupLng,
                               UploadCallback callback) {

        Map<String, Object> updates = new HashMap<>();
        updates.put("foodName", foodName);
        updates.put("quantity", quantity);
        updates.put("description", description);
        updates.put("expiresAt", expiresAt);
        updates.put("pickupLocation", pickupLocation);
        updates.put("pickupWindow", pickupWindow);
        updates.put("pickupLat", pickupLat);
        updates.put("pickupLng", pickupLng);

        if (photoBase64OrNull != null) {
            updates.put("imageUrl", photoBase64OrNull);
        }

        firestore.collection(POSTS_COLLECTION).document(foodId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Failed to update food post: " + e.getMessage()));
    }

    /**
     * Real-time listener for the currently logged-in donor's own posts,
     * newest first. Call the returned registration's remove() in onStop()/
     * onDestroy() to avoid leaking the listener.
     */
    public ListenerRegistration listenToMyPosts(PostsListCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("Not logged in.");
            return null;
        }
        String donorId = auth.getCurrentUser().getUid();

        Query query = firestore.collection(POSTS_COLLECTION)
                .whereEqualTo("donorId", donorId)
                .orderBy("postedAt", Query.Direction.DESCENDING);

        return query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                callback.onError(error.getMessage());
                return;
            }
            List<FoodPost> posts = new ArrayList<>();
            if (snapshots != null) {
                for (var doc : snapshots.getDocuments()) {
                    FoodPost post = doc.toObject(FoodPost.class);
                    if (post != null) {
                        posts.add(post);
                    }
                }
            }
            callback.onUpdate(posts);
        });
    }

    public interface RatingStatsCallback {
        void onStats(float average, int count);
        void onError(String message);
    }

    /** Average stars + count of all ratings NGOs have given this donor. */
    public void getMyRatingStats(RatingStatsCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("Not logged in.");
            return;
        }
        String donorId = auth.getCurrentUser().getUid();

        firestore.collection("ratings")
                .whereEqualTo("donorId", donorId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    int count = snapshots.size();
                    float total = 0f;
                    for (var doc : snapshots.getDocuments()) {
                        Double stars = doc.getDouble("stars");
                        if (stars != null) total += stars;
                    }
                    float average = count > 0 ? total / count : 0f;
                    callback.onStats(average, count);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Records that this donor chose to post despite the AI photo check
     * flagging it (not real food / AI-generated / screenshot). Written to
     * its own "policyViolations" collection so admin can review it and
     * decide whether to flag the donor's account — this call never blocks
     * or fails the actual food post upload; it's fire-and-forget logging.
     */
    public void reportPolicyViolation(String donorName, String foodName, String aiReason,
                                       boolean isRealFood, boolean isAiGenerated, boolean isScreenshot) {
        if (auth.getCurrentUser() == null) return;

        Map<String, Object> violation = new HashMap<>();
        violation.put("donorId", auth.getCurrentUser().getUid());
        violation.put("donorName", donorName);
        violation.put("foodName", foodName);
        violation.put("aiReason", aiReason);
        violation.put("isRealFood", isRealFood);
        violation.put("isAiGenerated", isAiGenerated);
        violation.put("isScreenshot", isScreenshot);
        violation.put("timestamp", System.currentTimeMillis());
        violation.put("reviewed", false);

        firestore.collection("policyViolations").document().set(violation);
        // No success/failure callback needed — this is best-effort logging
        // and must never block the donor's actual post from going through.
    }
}