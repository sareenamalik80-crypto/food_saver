package com.example.food_saver.utils;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
public final class InsetsHelper {

    private InsetsHelper() {
    }
    public static void applyStatusBarTopInset(View view) {
        int basePaddingTop = view.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), basePaddingTop + statusBars.top,
                    v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
    }
}
