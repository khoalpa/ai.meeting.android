package com.example.aimeeting;

import android.os.Build;
import android.view.WindowInsets;

final class InsetsCompat {
    private InsetsCompat() {
    }

    static int systemBarTop(WindowInsets insets) {
        if (insets == null) {
            return 0;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return insets.getInsets(WindowInsets.Type.systemBars()).top;
        }
        return legacySystemWindowInsetTop(insets);
    }

    static int systemBarBottom(WindowInsets insets) {
        if (insets == null) {
            return 0;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return insets.getInsets(WindowInsets.Type.systemBars()).bottom;
        }
        return legacySystemWindowInsetBottom(insets);
    }

    @SuppressWarnings("deprecation")
    private static int legacySystemWindowInsetTop(WindowInsets insets) {
        return insets.getSystemWindowInsetTop();
    }

    @SuppressWarnings("deprecation")
    private static int legacySystemWindowInsetBottom(WindowInsets insets) {
        return insets.getSystemWindowInsetBottom();
    }
}
