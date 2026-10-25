package com.example.food_saver.donor;

import com.example.food_saver.models.FoodPost;
import com.example.food_saver.models.Request;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class RequestRepository {

    private static final String REQUESTS_COLLECTION = "requests";
    private static final String POSTS_COLLECTION = "foodPosts";

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public interface RequestsListCallback {
        void onUpdate(List<Request> requests);
        void onError(String errorMessage);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public RequestRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    public ListenerRegistration listenToRequestsForMe(RequestsListCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("Not logged in.");
            return null;
        }
        String donorId = auth.getCurrentUser().getUid();

        Query query = firestore.collection(REQUESTS_COLLECTION)
                .whereEqualTo("donorId", donorId)
                .orderBy("requestedAt", Query.Direction.DESCENDING);

        return query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                callback.onError(error.getMessage());
                return;
            }
            if (snapshots == null) {
                callback.onUpdate(new ArrayList<>());
                return;
            }

            List<Request> requests = new ArrayList<>();
            for (var doc : snapshots.getDocuments()) {
                Request request = doc.toObject(Request.class);
                if (request != null) {
                    request.setRequestId(doc.getId());
                    requests.add(request);
                }
            }

            enrichWithFoodPostDetails(requests, callback);
        });
    }

    /** Fetches each request's linked foodPosts doc to fill in name/photo for display. */
    private void enrichWithFoodPostDetails(List<Request> requests, RequestsListCallback callback) {
        if (requests.isEmpty()) {
            callback.onUpdate(requests);
            return;
        }

        List<Task<DocumentSnapshot>> fetchTasks = new ArrayList<>();
        for (Request request : requests) {
            fetchTasks.add(firestore.collection(POSTS_COLLECTION).document(request.getFoodPostId()).get());
        }

        Tasks.whenAllComplete(fetchTasks).addOnSuccessListener(completedTasks -> {
            for (int i = 0; i < requests.size(); i++) {
                Task<DocumentSnapshot> task = fetchTasks.get(i);
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    FoodPost post = task.getResult().toObject(FoodPost.class);
                    if (post != null) {
                        post.setFoodId(task.getResult().getId());
                        requests.get(i).setFoodName(post.getFoodName());
                        requests.get(i).setFoodImageBase64(post.getImageUrl());
                        requests.get(i).setFoodExpired(post.isExpired());
                        com.example.food_saver.utils.ExpiryHelper.expireIfNeeded(post);
                    }
                }
            }
            callback.onUpdate(requests);
        }).addOnFailureListener(e -> {
            // Still show the requests even if enrichment partially failed.
            callback.onUpdate(requests);
        });
    }
    public void acceptRequest(String requestId, String foodPostId, ActionCallback callback) {
        DocumentReference requestRef = firestore.collection(REQUESTS_COLLECTION).document(requestId);
        DocumentReference postRef = firestore.collection(POSTS_COLLECTION).document(foodPostId);
        long now = System.currentTimeMillis();

        firestore.runTransaction(transaction -> {
                    transaction.update(requestRef,
                            "status", Request.STATUS_APPROVED,
                            "respondedAt", now);
                    transaction.update(postRef, "status", FoodPost.STATUS_APPROVED);
                    return null;
                }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
    public void rejectRequest(String requestId, String foodPostId, ActionCallback callback) {
        DocumentReference requestRef = firestore.collection(REQUESTS_COLLECTION).document(requestId);
        DocumentReference postRef = firestore.collection(POSTS_COLLECTION).document(foodPostId);
        long now = System.currentTimeMillis();

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot postSnapshot = transaction.get(postRef);
                    transaction.update(requestRef,
                            "status", Request.STATUS_REJECTED,
                            "respondedAt", now);

                    if (postSnapshot.exists()) {
                        transaction.update(postRef,
                                "status", FoodPost.STATUS_AVAILABLE,
                                "claimedByNgoId", null,
                                "claimedByNgoName", null);
                    }
                    return null;
                }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
    public void markHandedOverByDonor(String requestId, String foodPostId, ActionCallback callback) {
        DocumentReference requestRef = firestore.collection(REQUESTS_COLLECTION).document(requestId);
        DocumentReference postRef = firestore.collection(POSTS_COLLECTION).document(foodPostId);

        firestore.runTransaction(transaction -> {
                    DocumentSnapshot requestSnapshot = transaction.get(requestRef);
                    DocumentSnapshot postSnapshot = transaction.get(postRef);
                    String postStatus = postSnapshot.getString("status");
                    Long expiresAt = postSnapshot.getLong("expiresAt");
                    if (FoodPost.STATUS_EXPIRED.equals(postStatus)
                            || (FoodPost.STATUS_APPROVED.equals(postStatus) && expiresAt != null && expiresAt > 0
                            && System.currentTimeMillis() > expiresAt)) {
                        throw new RuntimeException("POST_EXPIRED");
                    }
                    Boolean alreadyHandedOver = requestSnapshot.getBoolean("donorConfirmedHandover");
                    if (alreadyHandedOver != null && alreadyHandedOver) {
                        throw new RuntimeException("ALREADY_HANDED_OVER");
                    }
                    String status = requestSnapshot.getString("status");
                    if (!Request.STATUS_APPROVED.equals(status) && !Request.STATUS_COLLECTED.equals(status)) {
                        throw new RuntimeException("NOT_APPROVED");
                    }
                    String arrivalTime = requestSnapshot.getString("arrivalTime");
                    if (arrivalTime == null || arrivalTime.isEmpty()) {
                        throw new RuntimeException("NO_DELIVERY_DETAILS");
                    }
                    Long arrivalAt = requestSnapshot.getLong("arrivalAt");
                    if (arrivalAt != null && arrivalAt > 0 && System.currentTimeMillis() < arrivalAt) {
                        throw new RuntimeException("TOO_EARLY");
                    }

                    Boolean ngoConfirmed = requestSnapshot.getBoolean("ngoConfirmedReceived");
                    if (ngoConfirmed != null && ngoConfirmed) {
                        transaction.update(requestRef,
                                "donorConfirmedHandover", true,
                                "status", Request.STATUS_HANDED_OVER);
                        transaction.update(postRef, "status", FoodPost.STATUS_HANDED_OVER);
                    } else {
                        transaction.update(requestRef,
                                "donorConfirmedHandover", true,
                                "status", Request.STATUS_COLLECTED);
                        transaction.update(postRef, "status", FoodPost.STATUS_COLLECTED);
                    }
                    return null;
                }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    String code = e.getMessage() != null ? e.getMessage() : "";
                    if (code.contains("POST_EXPIRED")) {
                        callback.onFailure("This donation has expired, so it can't be handed over.");
                    } else if (code.contains("NO_DELIVERY_DETAILS")) {
                        callback.onFailure("The NGO hasn't added the rider details yet.");
                    } else if (code.contains("TOO_EARLY")) {
                        callback.onFailure("You can hand over the food only after the rider's arrival time.");
                    } else if (code.contains("ALREADY_HANDED_OVER")) {
                        callback.onFailure("You have already handed over this food.");
                    } else if (code.contains("NOT_APPROVED")) {
                        callback.onFailure("This request can't be handed over right now.");
                    } else {
                        callback.onFailure(e.getMessage());
                    }
                });
    }
}
