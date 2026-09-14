package com.example.food_saver.ngo;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Shared wiring for the bottom navigation bar that appears on Home,
 * History, Transparency, and Profile. Keeps clicks and edge-to-edge bottom
 * padding consistent across all four screens without repeating the same
 * boilerplate in every Activity.
 */
public final class BottomNavHelper {

    public enum Tab { HOME, HISTORY, TRANSPARENCY, PROFILE }

    private BottomNavHelper() {
    }

    public static void setup(Activity activity, View bottomNav, View navHome, View navHistory,
                              View navTransparency, View navProfile, Tab current) {

        // Push the bar up above the gesture/navigation bar instead of
        // letting it sit underneath it (edge-to-edge is on by default for
        // apps targeting Android 15+).
        int basePadding = bottomNav.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    basePadding + systemBars.bottom);
            return insets;
        });

        navHome.setOnClickListener(v -> {
            if (current != Tab.HOME) {
                activity.startActivity(new Intent(activity, NgoDashboardActivity.class));
            }
        });
        navHistory.setOnClickListener(v -> {
            if (current != Tab.HISTORY) {
                activity.startActivity(new Intent(activity, MyCollectionsActivity.class));
            }
        });
        navTransparency.setOnClickListener(v -> {
            if (current != Tab.TRANSPARENCY) {
                activity.startActivity(new Intent(activity, TransparencyDashboardActivity.class));
            }
        });
        navProfile.setOnClickListener(v -> {
            if (current != Tab.PROFILE) {
                activity.startActivity(new Intent(activity, ProfileActivity.class));
            }
        });
    }
}
