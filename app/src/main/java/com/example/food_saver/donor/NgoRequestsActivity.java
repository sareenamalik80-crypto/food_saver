package com.example.food_saver.donor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.food_saver.databinding.ActivityNgoRequestsBinding;
import com.example.food_saver.utils.InsetsHelper;
import com.example.food_saver.models.Request;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class NgoRequestsActivity extends AppCompatActivity {

    private ActivityNgoRequestsBinding binding;
    private RequestRepository requestRepository;
    private RequestAdapter adapter;
    private ListenerRegistration requestsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNgoRequestsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.header);

        requestRepository = new RequestRepository();

        adapter = new RequestAdapter(new RequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(Request request) {
                requestRepository.acceptRequest(request.getRequestId(), request.getFoodPostId(),
                        new RequestRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(NgoRequestsActivity.this, "Request approved", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(NgoRequestsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
            }

            @Override
            public void onReject(Request request) {
                requestRepository.rejectRequest(request.getRequestId(), request.getFoodPostId(),
                        new RequestRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(NgoRequestsActivity.this, "Request rejected", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(NgoRequestsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
            }

            @Override
            public void onOpenChat(Request request) {
                Intent intent = new Intent(NgoRequestsActivity.this, ChatActivity.class);
                intent.putExtra(ChatActivity.EXTRA_REQUEST_ID, request.getRequestId());
                intent.putExtra(ChatActivity.EXTRA_OTHER_PARTY_NAME, request.getNgoName());
                startActivity(intent);
            }

            @Override
            public void onMarkHandedOver(Request request) {
                requestRepository.markHandedOverByDonor(request.getRequestId(), request.getFoodPostId(),
                        new RequestRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(NgoRequestsActivity.this,
                                        "Marked as handed over", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(NgoRequestsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());

        BottomNavHelper.setup(this, binding.bottomNav, binding.navHome, binding.navHistory,
                binding.navRequests, binding.navProfile, BottomNavHelper.Tab.REQUESTS);
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestsListener = requestRepository.listenToRequestsForMe(new RequestRepository.RequestsListCallback() {
            @Override
            public void onUpdate(List<Request> requests) {
                binding.tvEmptyState.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
                adapter.submitList(requests);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(NgoRequestsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (requestsListener != null) {
            requestsListener.remove();
        }
    }
}