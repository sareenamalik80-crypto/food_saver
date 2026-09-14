package com.example.food_saver.models;

/**
 * A single chat message. Firestore collection
 * "chats/{requestId}/messages/{messageId}" — one chat thread per accepted
 * request, so the requestId doubles as the chat id.
 */
public class ChatMessage {

    private String messageId;
    private String senderId;
    private String text;
    private long timestamp;

    // Empty constructor required for Firestore deserialization
    public ChatMessage() {
    }

    public ChatMessage(String senderId, String text, long timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
