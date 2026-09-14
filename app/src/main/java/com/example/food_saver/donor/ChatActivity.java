package com.example.food_saver.donor;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.food_saver.databinding.ActivityChatBinding;
import com.example.food_saver.utils.InsetsHelper;
import com.example.food_saver.models.ChatMessage;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_REQUEST_ID = "extra_request_id";
    public static final String EXTRA_OTHER_PARTY_NAME = "extra_other_party_name";

    private ActivityChatBinding binding;
    private ChatRepository chatRepository;
    private MessageAdapter adapter;
    private ListenerRegistration messagesListener;
    private String requestId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        InsetsHelper.applyStatusBarTopInset(binding.header);

        // Modern Android draws edge-to-edge, so the classic
        // windowSoftInputMode="adjustResize" no longer physically resizes
        // the window when the keyboard opens — the app has to react to the
        // IME inset itself. This pushes the whole screen's bottom padding
        // up by exactly the keyboard's height while it's visible (0 when
        // it's hidden), which shrinks the weighted RecyclerView above and
        // keeps the message input bar sitting right above the keyboard.
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

        if (TextUtils.isEmpty(requestId)) {
            Toast.makeText(this, "Chat unavailable — missing request.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Always show a plain "Chat" title — showing the other party's
        // name here could be confusing/misleading, so keep it generic.
        binding.tvChatTitle.setText("Chat");

        chatRepository = new ChatRepository();
        adapter = new MessageAdapter(chatRepository.getCurrentUserId());

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = binding.etMessageInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        binding.etMessageInput.setText("");
        chatRepository.sendMessage(requestId, text, new ChatRepository.SendCallback() {
            @Override
            public void onSuccess() {
                // Real-time listener will refresh the list.
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (requestId == null) return;

        messagesListener = chatRepository.listenToMessages(requestId, new ChatRepository.MessagesListCallback() {
            @Override
            public void onUpdate(List<ChatMessage> messages) {
                adapter.submitList(messages);
                if (!messages.isEmpty()) {
                    binding.recyclerView.scrollToPosition(messages.size() - 1);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(ChatActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messagesListener != null) {
            messagesListener.remove();
        }
    }
}
