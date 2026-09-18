package com.example.food_saver.ngo;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.food_saver.databinding.ActivityMyCollectionsBinding;
import com.example.food_saver.models.FoodPost;
import com.example.food_saver.repository.FoodRepository;
import com.example.food_saver.utils.InsetsHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class MyCollectionsActivity extends AppCompatActivity {

    private ActivityMyCollectionsBinding binding;
    private final FoodRepository repository = new FoodRepository();
    private FoodPostNgoAdapter adapter;
    private ListenerRegistration listener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyCollectionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.header);
        binding.btnBack.setOnClickListener(v -> finish());

        String myNgoId = FirebaseAuth.getInstance().getUid();

        adapter = new FoodPostNgoAdapter(myNgoId, post -> {
            Intent intent = new Intent(this, FoodDetailNgoActivity.class);
            intent.putExtra(NgoDashboardActivity.EXTRA_FOOD_ID, post.getFoodId());
            startActivity(intent);
        });

        binding.rvCollections.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCollections.setAdapter(adapter);

        BottomNavHelper.setup(this, binding.bottomNav, binding.navHome, binding.navHistory,
                binding.navTransparency, binding.navProfile, BottomNavHelper.Tab.HISTORY);

        listener = repository.listenMyCollections(myNgoId, new FoodRepository.PostsCallback() {
            @Override
            public void onPosts(List<FoodPost> posts) {
                adapter.submitList(posts);
                binding.tvEmpty.setVisibility(posts.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                binding.rvCollections.setVisibility(posts.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
            }

            @Override
            public void onError(Exception e) {
                android.widget.Toast.makeText(MyCollectionsActivity.this,
                        "Couldn't load your collections: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}
