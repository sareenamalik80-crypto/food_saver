package com.example.food_saver.ngo;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.food_saver.databinding.ActivityTransparencyDashboardBinding;
import com.example.food_saver.models.FoodPost;
import com.example.food_saver.repository.FoodRepository;
import com.example.food_saver.utils.InsetsHelper;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class TransparencyDashboardActivity extends AppCompatActivity {

    private ActivityTransparencyDashboardBinding binding;
    private final FoodRepository repository = new FoodRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransparencyDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.header);
        binding.btnBack.setOnClickListener(v -> finish());

        BottomNavHelper.setup(this, binding.bottomNav, binding.navHome, binding.navHistory,
                binding.navTransparency, binding.navProfile, BottomNavHelper.Tab.TRANSPARENCY);

        String myNgoId = FirebaseAuth.getInstance().getUid();

        repository.getNgoStats(myNgoId, new FoodRepository.PostsCallback() {
            @Override
            public void onPosts(List<FoodPost> posts) {
                int totalRequested = posts.size();
                int handedOver = 0;
                int pending = 0;

                for (FoodPost post : posts) {
                    if ("handedOver".equals(post.getStatus())) {
                        handedOver++;
                    } else if (!"available".equals(post.getStatus())) {
                        pending++;
                    }
                }

                binding.tvTotalRequested.setText(String.valueOf(totalRequested));
                binding.tvTotalHandedOver.setText(String.valueOf(handedOver));
                binding.tvPending.setText(String.valueOf(pending));
            }

            @Override
            public void onError(Exception e) {
                android.widget.Toast.makeText(TransparencyDashboardActivity.this,
                        "Couldn't load stats", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }
}
