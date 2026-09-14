package com.example.food_saver.repository;

import com.example.food_saver.models.ChatMessage;
import com.example.food_saver.models.DeliveryDetails;
import com.example.food_saver.models.FoodPost;
import com.example.food_saver.models.FoodRequest;
import com.example.food_saver.models.Rating;
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

/**
 * Poore NGO module ka Firestore layer yahin se guzarta hai.
 * Collections:
 *   foodPosts/{foodId}
 *   foodPosts/{foodId}/chats/{messageId}
 *   foodPosts/{foodId}/delivery/details   (single doc)
 *   requests/{requestId}
 *   ratings/{ratingId}
 *   ngos/{ngoId}   (profile: name, phone, address)
 */
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

    // 1) DASHBOARD: available posts + posts jo is NGO ne claim ki hain
    public ListenerRegistration[] listenDashboardPosts(String myNgoId, PostsCallback callback) {
        final Map<String, FoodPost> merged = new HashMap<>();

        ListenerRegistration l1 = db.collection("foodPosts")
                .whereEqualTo("status", "available")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { callback.onError(e); return; }
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            FoodPost post = doc.toObject(FoodPost.class);
                            if (post != null) { post.setFoodId(doc.getId()); merged.put(doc.getId(), post); }
                        }
                        callback.onPosts(new ArrayList<>(merged.values()));
                    }
                });

        ListenerRegistration l2 = db.collection("foodPosts")
                .whereEqualTo("claimedByNgoId", myNgoId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { callback.onError(e); return; }
                    if (snapshots != null) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            FoodPost post = doc.toObject(FoodPost.class);
                            if (post != null) { post.setFoodId(doc.getId()); merged.put(doc.getId(), post); }
                        }
                        callback.onPosts(new ArrayList<>(merged.values()));
                    }
                });

        return new ListenerRegistration[]{l1, l2};
    }

    // 2) SINGLE POST live updates (Food Detail screen ke liye)
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

    // 3) REQUEST FOOD — transaction se atomic hota hai, isliye do NGOs
    //    ek sath request karein to sirf ek hi jeetegi ("claimed" race safe).
    public void requestFood(String foodId, String ngoId, String ngoName, String donorId,
                            SimpleCallback callback) {
        if (ngoId == null) {
            // Defense in depth — never write a request/claim with a null
            // ngoId, since this NGO could then never recognize it as "mine".
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
                    } else {
                        callback.onError("Error while sending request: " + e.getMessage());
                    }
                });
    }

    // 3b) CANCEL REQUEST — NGO changes its mind before the donor responds.
    // Reverts the FoodPost back to "available" and clears the claim, so
    // other NGOs can request it again.
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

    // 4) STATUS UPDATES (donor ki taraf se "approved" set hota hai — woh Donor module karega)
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

    // 4b) DUAL-CONFIRMATION HANDOVER (mirrors the Donor module's
    // markHandedOverByDonor, swapped). The Donor and this NGO each confirm
    // their own half on the shared "requests/{requestId}" document — only
    // once BOTH donorConfirmedHandover and ngoConfirmedReceived are true
    // does the request (and the linked FoodPost) move to "handedOver".

    /** Finds the single request document linking this NGO to this food post, with live updates. */
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

    /**
     * NGO's half of the dual-confirmation handover. Sets
     * ngoConfirmedReceived = true. If the donor has ALREADY confirmed their
     * side (donorConfirmedHandover == true), this completes the handover:
     * both the request and its linked FoodPost move to "handedOver".
     * Otherwise the request moves to "collected" — "one side confirmed,
     * waiting on the other". Runs as a transaction so two near-simultaneous
     * confirmations can't race each other into an inconsistent state.
     */
    public void markReceivedByNgo(String requestId, String foodId, SimpleCallback callback) {
        DocumentReference requestRef = db.collection("requests").document(requestId);
        DocumentReference postRef = db.collection("foodPosts").document(foodId);

        db.runTransaction(transaction -> {
                    DocumentSnapshot requestSnapshot = transaction.get(requestRef);
                    Boolean donorConfirmed = requestSnapshot.getBoolean("donorConfirmedHandover");
                    boolean donorAlreadyConfirmed = donorConfirmed != null && donorConfirmed;

                    if (donorAlreadyConfirmed) {
                        Map<String, Object> requestUpdate = new HashMap<>();
                        requestUpdate.put("ngoConfirmedReceived", true);
                        requestUpdate.put("status", "handedOver");
                        transaction.update(requestRef, requestUpdate);
                        transaction.update(postRef, "status", "handedOver");
                    } else {
                        Map<String, Object> requestUpdate = new HashMap<>();
                        requestUpdate.put("ngoConfirmedReceived", true);
                        requestUpdate.put("status", "collected");
                        transaction.update(requestRef, requestUpdate);
                    }
                    return null;
                }).addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 5) DELIVERY DETAILS (rider name, phone, vehicle, time of arrival)
    public void saveDeliveryDetails(String foodId, DeliveryDetails details, SimpleCallback callback) {
        db.collection("foodPosts").document(foodId)
                .collection("delivery").document("details")
                .set(details)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 6) CHAT — scoped per accepted request (matches the Donor module's
    // schema): "chats/{requestId}/messages/{messageId}". requestId doubles
    // as the chat thread's id, so both sides read/write the exact same path.
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

    /** Average stars + count of all ratings a donor has received — shown to an NGO before it requests their food. */
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

    // 8) TRANSPARENCY DASHBOARD (is NGO ki apni activity ka summary)
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

    // 9) MY COLLECTIONS / HISTORY — every post this NGO has ever claimed,
    // regardless of current status (requested through handedOver).
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

    // 10) NGO PROFILE
    public void getNgoProfile(String ngoId, ProfileCallback callback) {
        db.collection("ngos").document(ngoId).get()
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
        db.collection("ngos").document(ngoId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
