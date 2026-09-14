package com.example.food_saver.donor;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.auth.AuthRepository;
import com.example.food_saver.databinding.ActivityDonorHomeBinding;
import com.example.food_saver.utils.InsetsHelper;
import com.example.food_saver.models.FoodPost;
import com.example.food_saver.models.Request;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class DonorHomeActivity extends AppCompatActivity {

    private ActivityDonorHomeBinding binding;
    private FoodPostRepository foodPostRepository;
    private RequestRepository requestRepository;
    private AuthRepository authRepository;
    private ListenerRegistration postsListener;
    private ListenerRegistration requestsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDonorHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.header);

        foodPostRepository = new FoodPostRepository();
        requestRepository = new RequestRepository();
        authRepository = new AuthRepository();

        BottomNavHelper.setup(this, binding.bottomNav, binding.navHome, binding.navHistory,
                binding.navRequests, binding.navProfile, BottomNavHelper.Tab.HOME);

        binding.btnPostFood.setOnClickListener(v ->
                startActivity(new Intent(this, PostFoodActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        postsListener = foodPostRepository.listenToMyPosts(new FoodPostRepository.PostsListCallback() {
            @Override
            public void onUpdate(List<FoodPost> posts) {
                int total = posts.size();
                int active = 0;
                for (FoodPost post : posts) {
                    if (!FoodPost.STATUS_HANDED_OVER.equals(post.getStatus())) {
                        active++;
                    }
                }
                binding.tvTotalDonations.setText(String.valueOf(total));
                binding.tvActivePosts.setText(String.valueOf(active));
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(DonorHomeActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        requestsListener = requestRepository.listenToRequestsForMe(new RequestRepository.RequestsListCallback() {
            @Override
            public void onUpdate(List<Request> requests) {
                int pending = 0;
                for (Request request : requests) {
                    if (Request.STATUS_REQUESTED.equals(request.getStatus())) {
                        pending++;
                    }
                }
                binding.tvPendingRequests.setText(String.valueOf(pending));
            }

            @Override
            public void onError(String errorMessage) {
                // Non-critical for the dashboard stat — just leave it at "0".
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (postsListener != null) {
            postsListener.remove();
        }
        if (requestsListener != null) {
            requestsListener.remove();
        }
    }
}