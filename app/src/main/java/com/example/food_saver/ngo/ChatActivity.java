package com.example.food_saver.ngo;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.food_saver.databinding.ActivityNgoChatBinding;
import com.example.food_saver.models.ChatMessage;
import com.example.food_saver.repository.FoodRepository;
import com.example.food_saver.utils.InsetsHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class ChatActivity extends AppCompatActivity {

    // Chat is scoped per accepted request (matches the Donor module's
    // schema): "chats/{requestId}/messages/{messageId}" — requestId doubles
    // as the chat thread's id, NOT the foodId.
    public static final String EXTRA_REQUEST_ID = "extra_request_id";

    private ActivityNgoChatBinding binding;
    private final FoodRepository repository = new FoodRepository();
    private ChatAdapter adapter;
    private ListenerRegistration listener;
    private String requestId;
    private String myUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNgoChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsHelper.applyStatusBarTopInset(binding.header);
        binding.btnBack.setOnClickListener(v -> finish());

        // Modern Android draws edge-to-edge, so the classic
        // windowSoftInputMode="adjustResize" no longer physically resizes
        // the window when the keyboard opens — react to the IME inset
        // directly so the message input bar always sits above the keyboard.
        int rootBasePadding = binding.getRoot().getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int bottom = Math.max(ime.bottom, systemBars.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    rootBasePadding + bottom);
            return insets;
        });

        requestId = getIntent().getStringExtra(EXTRA_REQUEST_ID);
        myUserId = FirebaseAuth.getInstance().getUid();

        if (TextUtils.isEmpty(requestId)) {
            Toast.makeText(this, "Chat unavailable — missing request.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new ChatAdapter(myUserId);
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMessages.setAdapter(adapter);

        binding.btnSend.setOnClickListener(v -> sendMessage());

        listener = repository.listenMessages(requestId, new FoodRepository.MessagesCallback() {
            @Override
            public void onMessages(List<ChatMessage> messages) {
                adapter.submitList(messages);
                binding.rvMessages.scrollToPosition(Math.max(0, messages.size() - 1));
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ChatActivity.this, "Couldn't load chat", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String text = binding.etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        ChatMessage message = new ChatMessage(myUserId, "ngo", text, System.currentTimeMillis());
        repository.sendMessage(requestId, message);
        binding.etMessage.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}
