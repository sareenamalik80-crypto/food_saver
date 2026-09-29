package com.example.food_saver.donor;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.food_saver.databinding.ActivityMyDonationsBinding;
import com.example.food_saver.utils.InsetsHelper;
import com.example.food_saver.models.FoodPost;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class MyDonationsActivity extends AppCompatActivity {

    private ActivityMyDonationsBinding binding;
    private FoodPostRepository foodPostRepository;
    private FoodPostAdapter adapter;
    private ListenerRegistration postsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyDonationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.header);

        foodPostRepository = new FoodPostRepository();

        adapter = new FoodPostAdapter(post -> {
            if (post.isEditable()) {
                Intent intent = new Intent(this, EditFoodPostActivity.class);
                intent.putExtra(EditFoodPostActivity.EXTRA_FOOD_ID, post.getFoodId());
                startActivity(intent);
            } else {
                String state = post.isCompleted() ? "completed" : post.getStatus();
                Toast.makeText(this,
                        "Can't edit — this post is already " + state + ".",
                        Toast.LENGTH_SHORT).show();
            }
        }, this::confirmDelete);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());

        BottomNavHelper.setup(this, binding.bottomNav, binding.navHome, binding.navHistory,
                binding.navRequests, binding.navProfile, BottomNavHelper.Tab.DONATIONS);
    }

    @Override
    protected void onStart() {
        super.onStart();
        postsListener = foodPostRepository.listenToMyPosts(new FoodPostRepository.PostsListCallback() {
            @Override
            public void onUpdate(List<FoodPost> posts) {
                binding.tvEmptyState.setVisibility(posts.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                adapter.submitList(posts);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MyDonationsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (postsListener != null) {
            postsListener.remove();
        }
    }

    private void confirmDelete(FoodPost post) {
        new AlertDialog.Builder(this)
                .setTitle("Delete this post?")
                .setMessage("\"" + post.getFoodName() + "\" will be removed permanently. "
                        + "This can't be undone.")
                .setPositiveButton("Delete", (dialog, which) ->
                        foodPostRepository.deleteFoodPost(post.getFoodId(),
                                new FoodPostRepository.UploadCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Toast.makeText(MyDonationsActivity.this,
                                                "Post deleted.", Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        Toast.makeText(MyDonationsActivity.this,
                                                errorMessage, Toast.LENGTH_LONG).show();
                                    }
                                }))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
