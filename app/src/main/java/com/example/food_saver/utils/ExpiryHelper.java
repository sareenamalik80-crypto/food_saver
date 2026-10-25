package com.example.food_saver.utils;

import com.example.food_saver.models.FoodPost;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public final class ExpiryHelper {

    private ExpiryHelper() {}

    public static boolean canExpire(String status) {
        return FoodPost.STATUS_AVAILABLE.equals(status)
                || FoodPost.STATUS_REQUESTED.equals(status)
                || FoodPost.STATUS_APPROVED.equals(status);
    }

    public static void expireIfNeeded(FoodPost post) {
        if (post == null || post.getFoodId() == null) return;
        if (!canExpire(post.getStatus())) return;
        if (post.getExpiresAt() <= 0 || System.currentTimeMillis() <= post.getExpiresAt()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String foodId = post.getFoodId();
        DocumentReference postRef = db.collection("foodPosts").document(foodId);

        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(postRef);
                    Long expiresAt = snapshot.getLong("expiresAt");
                    if (!snapshot.exists() || !canExpire(snapshot.getString("status"))
                            || expiresAt == null || expiresAt <= 0
                            || System.currentTimeMillis() <= expiresAt) {
                        return false;
                    }
                    transaction.update(postRef, "status", FoodPost.STATUS_EXPIRED);
                    return true;
                })
                .addOnSuccessListener(expired -> {
                    if (Boolean.TRUE.equals(expired)) expireOpenRequests(db, foodId);
                });
    }

    private static void expireOpenRequests(FirebaseFirestore db, String foodId) {
        db.collection("requests").whereEqualTo("foodPostId", foodId).get()
                .addOnSuccessListener(snapshot -> {
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = doc.getString("status");
                        if ("requested".equals(status) || "approved".equals(status)) {
                            doc.getReference().update("status", "expired");
                        }
                    }
                });
    }
}
