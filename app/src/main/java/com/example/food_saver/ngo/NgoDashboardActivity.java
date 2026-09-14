package com.example.food_saver.ngo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.food_saver.databinding.ActivityNgoDashboardBinding;
import com.example.food_saver.models.FoodPost;
import com.example.food_saver.repository.FoodRepository;
import com.example.food_saver.utils.InsetsHelper;
import com.example.food_saver.utils.LocationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NgoDashboardActivity extends AppCompatActivity {

    public static final String EXTRA_FOOD_ID = "extra_food_id";

    private ActivityNgoDashboardBinding binding;
    private final FoodRepository repository = new FoodRepository();
    private FoodPostNgoAdapter adapter;
    private ListenerRegistration[] listeners;

    // The full unfiltered list from Firestore — the search box filters a
    // copy of this into the adapter rather than mutating it directly, so
    // clearing the search always restores everything.
    private final List<FoodPost> allPosts = new ArrayList<>();

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    fetchLocationAndSort();
                } else {
                    android.widget.Toast.makeText(this,
                            "Location permission is needed to sort by nearest.", android.widget.Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNgoDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        InsetsHelper.applyStatusBarTopInset(binding.header);

        String myNgoId = FirebaseAuth.getInstance().getUid();

        String email = (FirebaseAuth.getInstance().getCurrentUser() != null)
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail()
                : "NGO";
        binding.tvWelcome.setText("Welcome, " + email);

        adapter = new FoodPostNgoAdapter(myNgoId, post -> {
            Intent intent = new Intent(this, FoodDetailNgoActivity.class);
            intent.putExtra(EXTRA_FOOD_ID, post.getFoodId());
            startActivity(intent);
        });

        binding.rvFoodPosts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFoodPosts.setAdapter(adapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearchFilter(s.toString());
            }

            @Override public void afterTextChanged(Editable s) { }
        });

        binding.btnSortNearby.setOnClickListener(v -> checkLocationPermissionAndSort());

        BottomNavHelper.setup(this, binding.bottomNav, binding.navHome, binding.navHistory,
                binding.navTransparency, binding.navProfile, BottomNavHelper.Tab.HOME);

        listeners = repository.listenDashboardPosts(myNgoId, new FoodRepository.PostsCallback() {
            @Override
            public void onPosts(List<FoodPost> posts) {
                allPosts.clear();
                allPosts.addAll(posts);
                applySearchFilter(binding.etSearch.getText().toString());
            }

            @Override
            public void onError(Exception e) {
                android.widget.Toast.makeText(NgoDashboardActivity.this,
                        "Couldn't load posts: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Filters allPosts by food name / description (case-insensitive) and pushes the result to the adapter. */
    private void applySearchFilter(String query) {
        String needle = query.trim().toLowerCase(Locale.getDefault());
        List<FoodPost> filtered;
        if (needle.isEmpty()) {
            filtered = allPosts;
        } else {
            filtered = new ArrayList<>();
            for (FoodPost post : allPosts) {
                String name = post.getFoodName() != null ? post.getFoodName().toLowerCase(Locale.getDefault()) : "";
                String desc = post.getDescription() != null ? post.getDescription().toLowerCase(Locale.getDefault()) : "";
                if (name.contains(needle) || desc.contains(needle)) {
                    filtered.add(post);
                }
            }
        }
        adapter.submitList(filtered);
        binding.tvEmpty.setVisibility(filtered.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.rvFoodPosts.setVisibility(filtered.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
    }

    private void checkLocationPermissionAndSort() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndSort();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void fetchLocationAndSort() {
        LocationHelper.fetchCurrentLocation(this, new LocationHelper.LocationCallback() {
            @Override
            public void onLocationFound(double latitude, double longitude, String address) {
                adapter.setMyLocation(latitude, longitude);
            }

            @Override
            public void onError(String errorMessage) {
                android.widget.Toast.makeText(NgoDashboardActivity.this, errorMessage, android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listeners != null) {
            for (ListenerRegistration l : listeners) {
                if (l != null) l.remove();
            }
        }
    }
}
