package com.example.food_saver.utils;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Applies top padding for the status bar only.
 *
 * We deliberately avoid the XML "android:fitsSystemWindows=true" attribute
 * here. On this codebase's target SDK, a view with fitsSystemWindows can
 * end up being padded using the *combined* system-window-insets rect,
 * which — while the on-screen keyboard is open and the window is resized
 * for it — includes the keyboard's height. A header pinned to the top of
 * the screen would then gain a huge bottom padding equal to the keyboard's
 * height, visually ballooning downward. Using
 * WindowInsetsCompat.Type.statusBars() explicitly excludes the IME, so
 * opening the keyboard never affects this padding.
 */
public final class InsetsHelper {

    private InsetsHelper() {
    }

    /** Pads `view` at the top by the status bar height, on top of whatever top padding it already has. */
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
