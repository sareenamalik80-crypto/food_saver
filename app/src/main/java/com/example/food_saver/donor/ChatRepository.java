package com.example.food_saver.donor;

import com.example.food_saver.models.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Chat is scoped per accepted request: Firestore path
 * "chats/{requestId}/messages/{messageId}".
 */
public class ChatRepository {

    private static final String CHATS_COLLECTION = "chats";
    private static final String MESSAGES_SUBCOLLECTION = "messages";

    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;

    public interface MessagesListCallback {
        void onUpdate(List<ChatMessage> messages);
        void onError(String errorMessage);
    }

    public interface SendCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public ChatRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public ListenerRegistration listenToMessages(String requestId, MessagesListCallback callback) {
        Query query = firestore.collection(CHATS_COLLECTION)
                .document(requestId)
                .collection(MESSAGES_SUBCOLLECTION)
                .orderBy("timestamp", Query.Direction.ASCENDING);

        return query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                callback.onError(error.getMessage());
                return;
            }
            List<ChatMessage> messages = new ArrayList<>();
            if (snapshots != null) {
                for (var doc : snapshots.getDocuments()) {
                    ChatMessage message = doc.toObject(ChatMessage.class);
                    if (message != null) {
                        message.setMessageId(doc.getId());
                        messages.add(message);
                    }
                }
            }
            callback.onUpdate(messages);
        });
    }

    public void sendMessage(String requestId, String text, SendCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onFailure("Not logged in.");
            return;
        }
        String senderId = auth.getCurrentUser().getUid();
        ChatMessage message = new ChatMessage(senderId, text, System.currentTimeMillis());

        firestore.collection(CHATS_COLLECTION)
                .document(requestId)
                .collection(MESSAGES_SUBCOLLECTION)
                .add(message)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
}
