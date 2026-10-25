package com.example.food_saver.repository;

import com.example.food_saver.models.ChatMessage;
import com.example.food_saver.models.DeliveryDetails;
import com.example.food_saver.models.FoodPost;
import com.example.food_saver.models.FoodRequest;
import com.example.food_saver.models.Rating;
import com.example.food_saver.utils.ExpiryHelper;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FoodRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface PostsCallback {
        void onPosts(List<FoodPost> posts);
        void onError(Exception e);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface SinglePostCallback {
        void onPost(FoodPost post);
        void onError(Exception e);
    }

    public interface MessagesCallback {
        void onMessages(List<ChatMessage> messages);
        void onError(Exception e);
    }

    public interface RequestCallback {
        void onRequest(FoodRequest request);
        void onError(Exception e);
    }

    public interface ProfileCallback {
        void onProfile(String name, String phone, String address);
        void onError(String message);
    }

    public interface RatingStatsCallback {
        void onStats(float average, int count);
        void onError(String message);
    }
    public ListenerRegistration[] listenDashboardPosts(String myNgoId, PostsCallback callback) {
        final Map<String, FoodPost> availablePosts = new HashMap<>();
        final Map<String, FoodPost> myPosts = new HashMap<>();

        ListenerRegistration l1 = db.collection("foodPosts")
                .whereEqualTo("status", "available")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { callback.onError(e); return; }
                    if (snapshots != null) {
                        availablePosts.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            FoodPost post = doc.toObject(FoodPost.class);
                            if (post != null) { post.setFoodId(doc.getId()); ExpiryHelper.expireIfNeeded(post); availablePosts.put(doc.getId(), post); }
                        }
                        callback.onPosts(mergePosts(availablePosts, myPosts));
                    }
                });

        ListenerRegistration l2 = db.collection("foodPosts")
                .whereEqualTo("claimedByNgoId", myNgoId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { callback.onError(e); return; }
                    if (snapshots != null) {
                        myPosts.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            FoodPost post = doc.toObject(FoodPost.class);
                            if (post != null) { post.setFoodId(doc.getId()); ExpiryHelper.expireIfNeeded(post); myPosts.put(doc.getId(), post); }
                        }
                        callback.onPosts(mergePosts(availablePosts, myPosts));
                    }
                });

        return new ListenerRegistration[]{l1, l2};
    }
    private List<FoodPost> mergePosts(Map<String, FoodPost> available, Map<String, FoodPost> mine) {
        Map<String, FoodPost> merged = new HashMap<>(available);
        merged.putAll(mine);
        return new ArrayList<>(merged.values());
    }
    public void getPost(String foodId, SinglePostCallback callback) {
        db.collection("foodPosts").document(foodId).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        FoodPost post = doc.toObject(FoodPost.class);
                        if (post != null) {
                            post.setFoodId(doc.getId());
                            callback.onPost(post);
                            return;
                        }
                    }
                    callback.onError(new Exception("Food post not found"));
                })
                .addOnFailureListener(callback::onError);
    }

    public ListenerRegistration listenPost(String foodId, SinglePostCallback callback) {
        return db.collection("foodPosts").document(foodId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null) { callback.onError(e); return; }
                    if (doc != null && doc.exists()) {
                        FoodPost post = doc.toObject(FoodPost.class);
                        if (post != null) post.setFoodId(doc.getId());
                        callback.onPost(post);
                    }
                });
    }
    public void requestFood(String foodId, String ngoId, String ngoName, String donorId,
                            SimpleCallback callback) {
        if (ngoId == null) {
            callback.onError("Not logged in — please log in again.");
            return;
        }
        DocumentReference postRef = db.collection("foodPosts").document(foodId);
        DocumentReference requestRef = db.collection("requests").document();

        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(postRef);
                    String currentStatus = snapshot.getString("status");

                    if (!"available".equals(currentStatus)) {
                        throw new RuntimeException("ALREADY_CLAIMED");
                    }
                    Long expiresAt = snapshot.getLong("expiresAt");
                    if (expiresAt != null && expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
                        throw new RuntimeException("EXPIRED");
                    }

                    Map<String, Object> postUpdate = new HashMap<>();
                    postUpdate.put("status", "requested");
                    postUpdate.put("claimedByNgoId", ngoId);
                    postUpdate.put("claimedByNgoName", ngoName);
                    transaction.update(postRef, postUpdate);

                    FoodRequest request = new FoodRequest(requestRef.getId(), foodId, ngoId, ngoName,
                            donorId, System.currentTimeMillis());
                    transaction.set(requestRef, request);

                    return null;
                }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    if ("ALREADY_CLAIMED".equals(e.getMessage())) {
                        callback.onError("This food post has already been claimed by another NGO.");
                    } else if ("EXPIRED".equals(e.getMessage())) {
                        callback.onError("This food post has expired and can no longer be requested.");
                    } else {
                        callback.onError("Error while sending request: " + e.getMessage());
                    }
                });
    }


    public void cancelRequest(String requestId, String foodPostId, SimpleCallback callback) {
        DocumentReference requestRef = db.collection("requests").document(requestId);
        DocumentReference postRef = db.collection("foodPosts").document(foodPostId);

        db.runTransaction(transaction -> {
                    transaction.update(requestRef, "status", "cancelled");

                    Map<String, Object> postUpdate = new HashMap<>();
                    postUpdate.put("status", "available");
                    postUpdate.put("claimedByNgoId", null);
                    postUpdate.put("claimedByNgoName", null);
                    transaction.update(postRef, postUpdate);

                    return null;
                }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void markCollected(String foodId, SimpleCallback callback) {
        db.collection("foodPosts").document(foodId)
                .update("status", "collected")
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void markHandedOver(String foodId, SimpleCallback callback) {
        db.collection("foodPosts").document(foodId)
                .update("status", "handedOver")
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    public ListenerRegistration listenRequestForPost(String foodId, String ngoId, RequestCallback callback) {
        return db.collection("requests")
                .whereEqualTo("foodPostId", foodId)
                .whereEqualTo("ngoId", ngoId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { callback.onError(e); return; }
                    if (snapshots == null || snapshots.isEmpty()) { return; }
                    DocumentSnapshot doc = snapshots.getDocuments().get(0);
                    FoodRequest request = doc.toObject(FoodRequest.class);
                    if (request != null) {
                        request.setRequestId(doc.getId());
                        callback.onRequest(request);
                    }
                });
    }
    public void markReceivedByNgo(String requestId, String foodId, SimpleCallback callback) {
        DocumentReference requestRef = db.collection("requests").document(requestId);
        DocumentReference postRef = db.collection("foodPosts").document(foodId);

        db.runTransaction(transaction -> {
                    DocumentSnapshot requestSnapshot = transaction.get(requestRef);
                    Boolean donorConfirmed = requestSnapshot.getBoolean("donorConfirmedHandover");
                    if (donorConfirmed == null || !donorConfirmed) {
                        throw new RuntimeException("NOT_HANDED_OVER");
                    }
                    Long arrivalAt = requestSnapshot.getLong("arrivalAt");
                    if (arrivalAt != null && arrivalAt > 0 && System.currentTimeMillis() < arrivalAt) {
                        throw new RuntimeException("TOO_EARLY");
                    }

                    Map<String, Object> requestUpdate = new HashMap<>();
                    requestUpdate.put("ngoConfirmedReceived", true);
                    requestUpdate.put("status", "handedOver");
                    transaction.update(requestRef, requestUpdate);
                    transaction.update(postRef, "status", "handedOver");
                    return null;
                }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    String code = e.getMessage() != null ? e.getMessage() : "";
                    if (code.contains("NOT_HANDED_OVER")) {
                        callback.onError("The donor hasn't handed over the food yet. You can mark it as received after the donor presses Handover.");
                    } else if (code.contains("TOO_EARLY")) {
                        callback.onError("You can mark the food as received only after the rider's arrival time.");
                    } else {
                        callback.onError(e.getMessage());
                    }
                });
    }

    public void saveDeliveryDetails(String requestId, DeliveryDetails details, SimpleCallback callback) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("riderName", details.getRiderName());
        updates.put("riderPhone", details.getRiderPhone());
        updates.put("vehicleNumber", details.getVehicleNumber());
        updates.put("arrivalTime", details.getTimeOfArrival());
        updates.put("arrivalAt", details.getArrivalAt());

        db.collection("requests").document(requestId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    public ListenerRegistration listenMessages(String requestId, MessagesCallback callback) {
        return db.collection("chats").document(requestId).collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { callback.onError(e); return; }
                    List<ChatMessage> messages = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            ChatMessage msg = doc.toObject(ChatMessage.class);
                            if (msg != null) { msg.setMessageId(doc.getId()); messages.add(msg); }
                        }
                    }
                    callback.onMessages(messages);
                });
    }

    public void sendMessage(String requestId, ChatMessage message) {
        db.collection("chats").document(requestId).collection("messages")
                .document()
                .set(message);
    }

    // 7) RATING
    public void submitRating(Rating rating, SimpleCallback callback) {
        db.collection("ratings").document()
                .set(rating)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    public void getDonorRatingStats(String donorId, RatingStatsCallback callback) {
        db.collection("ratings")
                .whereEqualTo("donorId", donorId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    int count = snapshots.size();
                    float total = 0f;
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Double stars = doc.getDouble("stars");
                        if (stars != null) total += stars;
                    }
                    float average = count > 0 ? total / count : 0f;
                    callback.onStats(average, count);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    public void getNgoStats(String ngoId, PostsCallback callback) {
        db.collection("foodPosts")
                .whereEqualTo("claimedByNgoId", ngoId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<FoodPost> posts = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        FoodPost post = doc.toObject(FoodPost.class);
                        if (post != null) { post.setFoodId(doc.getId()); posts.add(post); }
                    }
                    callback.onPosts(posts);
                })
                .addOnFailureListener(callback::onError);
    }
    public ListenerRegistration listenMyCollections(String ngoId, PostsCallback callback) {
        return db.collection("foodPosts")
                .whereEqualTo("claimedByNgoId", ngoId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { callback.onError(e); return; }
                    List<FoodPost> posts = new ArrayList<>();
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            FoodPost post = doc.toObject(FoodPost.class);
                            if (post != null) { post.setFoodId(doc.getId()); posts.add(post); }
                        }
                    }
                    posts.sort((a, b) -> Long.compare(b.getPostedAt(), a.getPostedAt()));
                    callback.onPosts(posts);
                });
    }
    public void getNgoProfile(String ngoId, ProfileCallback callback) {
        db.collection("users").document(ngoId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onProfile(doc.getString("name"), doc.getString("phone"), doc.getString("address"));
                    } else {
                        callback.onProfile(null, null, null);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void saveNgoProfile(String ngoId, String name, String phone, String address, SimpleCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("phone", phone);
        data.put("address", address);
        db.collection("users").document(ngoId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
