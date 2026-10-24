package com.example.aimeeting;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

final class PremiumUi {
    private PremiumUi() {
    }

    static void applyShell(Activity activity, View root, View header) {
        if (root != null) {
            root.setBackgroundResource(R.drawable.app_background);
        }
        if (header != null) {
            header.setBackgroundResource(R.drawable.premium_header);
            header.setElevation(dp(activity, 8));
        }
    }

    static void elevateCard(Activity activity, View card) {
        if (card != null) {
            card.setBackgroundResource(R.drawable.panel_background);
            card.setElevation(dp(activity, 2));
        }
    }

    static void primaryButton(Button button) {
        if (button == null) {
            return;
        }
        button.setAllCaps(false);
        button.setTextColor(0xffffffff);
        button.setBackgroundResource(R.drawable.primary_button);
    }

    static void secondaryButton(Activity activity, Button button) {
        if (button == null) {
            return;
        }
        button.setAllCaps(false);
        button.setTextColor(activity.getColor(R.color.meeting_primary_dark));
        button.setBackgroundResource(R.drawable.secondary_button);
    }

    static void dangerButton(Activity activity, Button button) {
        if (button == null) {
            return;
        }
        button.setAllCaps(false);
        button.setTextColor(activity.getColor(R.color.meeting_danger));
        button.setBackgroundResource(R.drawable.destructive_button);
    }

    static void compactTextButton(Activity activity, Button button, String description) {
        if (button == null) {
            return;
        }
        button.setContentDescription(description);
        button.setGravity(android.view.Gravity.CENTER);
        button.setTextSize(13);
        button.setSingleLine(true);
        button.setMaxLines(1);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setIncludeFontPadding(false);
        button.setAutoSizeTextTypeUniformWithConfiguration(10, 13, 1, TypedValue.COMPLEX_UNIT_SP);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(activity, 8), 0, dp(activity, 8), 0);
    }

    static void primaryIconButton(Activity activity, Button button, int iconRes, String description) {
        primaryButton(button);
        iconOnlyButton(activity, button, iconRes, description, android.R.color.white);
    }

    static void secondaryIconButton(Activity activity, Button button, int iconRes, String description) {
        secondaryButton(activity, button);
        iconOnlyButton(activity, button, iconRes, description, R.color.meeting_primary_dark);
    }

    static void dangerIconButton(Activity activity, Button button, int iconRes, String description) {
        dangerButton(activity, button);
        iconOnlyButton(activity, button, iconRes, description, R.color.meeting_danger);
    }

    static void primaryIconTextButton(Activity activity, Button button, int iconRes, String label) {
        primaryIconTextButton(activity, button, iconRes, label, label);
    }

    static void primaryIconTextButton(Activity activity, Button button, int iconRes, String label, String description) {
        primaryButton(button);
        iconTextButton(activity, button, iconRes, label, description, android.R.color.white);
    }

    static void secondaryIconTextButton(Activity activity, Button button, int iconRes, String label) {
        secondaryIconTextButton(activity, button, iconRes, label, label);
    }

    static void secondaryIconTextButton(Activity activity, Button button, int iconRes, String label, String description) {
        secondaryButton(activity, button);
        iconTextButton(activity, button, iconRes, label, description, R.color.meeting_primary_dark);
    }

    static void dangerIconTextButton(Activity activity, Button button, int iconRes, String label) {
        dangerIconTextButton(activity, button, iconRes, label, label);
    }

    static void dangerIconTextButton(Activity activity, Button button, int iconRes, String label, String description) {
        dangerButton(activity, button);
        iconTextButton(activity, button, iconRes, label, description, R.color.meeting_danger);
    }

    private static void iconOnlyButton(Activity activity, Button button, int iconRes, String description, int colorRes) {
        if (button == null) {
            return;
        }
        button.setText("");
        button.setContentDescription(description);
        button.setGravity(android.view.Gravity.CENTER);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(activity, 8), 0, dp(activity, 8), 0);
        Drawable icon = AppCompatResources.getDrawable(activity, iconRes);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(activity.getColor(colorRes));
            icon.setBounds(0, 0, dp(activity, 20), dp(activity, 20));
        }
        button.setCompoundDrawables(icon, null, null, null);
        button.setCompoundDrawablePadding(0);
    }

    private static void iconTextButton(Activity activity, Button button, int iconRes, String label,
                                       String description, int colorRes) {
        if (button == null) {
            return;
        }
        button.setText(label);
        button.setContentDescription(description == null ? label : description);
        button.setGravity(android.view.Gravity.CENTER);
        button.setTextSize(13);
        button.setSingleLine(true);
        button.setMaxLines(1);
        button.setEllipsize(TextUtils.TruncateAt.END);
        button.setIncludeFontPadding(false);
        button.setAutoSizeTextTypeUniformWithConfiguration(10, 13, 1, TypedValue.COMPLEX_UNIT_SP);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(activity, 10), 0, dp(activity, 10), 0);
        Drawable icon = AppCompatResources.getDrawable(activity, iconRes);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(activity.getColor(colorRes));
            icon.setBounds(0, 0, dp(activity, 18), dp(activity, 18));
        }
        button.setCompoundDrawables(icon, null, null, null);
        button.setCompoundDrawablePadding(dp(activity, 6));
    }

    static void sectionTitle(Activity activity, TextView text) {
        if (text == null) {
            return;
        }
        text.setTextColor(activity.getColor(R.color.meeting_text));
        text.setTextSize(18);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    static void addScreenNavigation(Activity activity, LinearLayout header, String currentScreenLabel) {
        if (header == null) {
            return;
        }
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
        rowParams.setMargins(0, dp(activity, 10), 0, 0);
        header.addView(row, rowParams);

        Button meetingButton = new Button(activity);
        meetingButton.setTextSize(13);
        secondaryIconButton(activity, meetingButton, R.drawable.ic_nav_meeting, "Meeting");
        meetingButton.setOnClickListener(v -> activity.finish());
        row.addView(meetingButton, new LinearLayout.LayoutParams(0, dp(activity, 36), 1));

        TextView current = new TextView(activity);
        current.setText(currentScreenLabel);
        current.setTextColor(activity.getColor(R.color.meeting_primary_dark));
        current.setTextSize(13);
        current.setTypeface(null, android.graphics.Typeface.BOLD);
        current.setGravity(android.view.Gravity.CENTER);
        current.setBackgroundResource(R.drawable.status_chip_neutral);
        LinearLayout.LayoutParams currentParams = new LinearLayout.LayoutParams(0, dp(activity, 36), 1);
        currentParams.setMargins(dp(activity, 8), 0, 0, 0);
        row.addView(current, currentParams);
    }

    static LinearLayout addBottomNavigation(Activity activity, LinearLayout root, String activeKey,
                                           String uiLanguageCode, String languageOneName, String languageOneCode,
                                           String languageTwoName, String languageTwoCode) {
        LinearLayout footer = new LinearLayout(activity);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(android.view.Gravity.CENTER);
        footer.setBackgroundResource(R.drawable.premium_header);
        footer.setElevation(dp(activity, 10));
        footer.setPadding(dp(activity, 14), dp(activity, 10), dp(activity, 14), dp(activity, 10));

        addFooterItem(activity, footer, activeKey, "meeting", R.drawable.ic_nav_meeting, "Meeting",
                v -> {
                    Intent intent = new Intent(activity, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(intent);
                });
        addFooterItem(activity, footer, activeKey, "assistant", R.drawable.ic_nav_assistant,
                LocalizedText.get(uiLanguageCode, "assistant"),
                v -> {
                    Intent intent = new Intent(activity, AssistantActivity.class);
                    intent.putExtra("language_name", languageOneName);
                    intent.putExtra("language_code", languageOneCode);
                    activity.startActivity(intent);
                });
        addFooterItem(activity, footer, activeKey, "models", R.drawable.ic_nav_models,
                LocalizedText.get(uiLanguageCode, "manage_models"),
                v -> {
                    Intent intent = new Intent(activity, ModelManagerActivity.class);
                    intent.putExtra("ui_language_code", uiLanguageCode);
                    intent.putExtra("ui_language_name", languageOneName);
                    activity.startActivity(intent);
                });
        addFooterItem(activity, footer, activeKey, "settings", R.drawable.ic_nav_settings,
                navLabel(uiLanguageCode, "settings"),
                v -> {
                    Intent intent = new Intent(activity, SettingsActivity.class);
                    putLanguageExtras(intent, uiLanguageCode, languageOneName, languageOneCode, languageTwoName, languageTwoCode);
                    activity.startActivity(intent);
                });
        addFooterItem(activity, footer, activeKey, "runtime", R.drawable.ic_nav_runtime,
                navLabel(uiLanguageCode, "runtime"),
                v -> {
                    Intent intent = new Intent(activity, RuntimeActivity.class);
                    putLanguageExtras(intent, uiLanguageCode, languageOneName, languageOneCode, languageTwoName, languageTwoCode);
                    activity.startActivity(intent);
                });

        root.addView(footer, new LinearLayout.LayoutParams(-1, -2));
        return footer;
    }

    static void styleFooterIconButton(Activity activity, ImageButton button, boolean active) {
        if (button == null) {
            return;
        }
        button.setBackgroundResource(active ? R.drawable.primary_button : R.drawable.secondary_button);
        button.setColorFilter(activity.getColor(active ? android.R.color.white : R.color.meeting_primary_dark));
        button.setScaleType(android.widget.ImageView.ScaleType.CENTER);
        button.setPadding(dp(activity, 9), dp(activity, 9), dp(activity, 9), dp(activity, 9));
        button.setEnabled(!active);
    }

    private static void addFooterItem(Activity activity, LinearLayout footer, String activeKey, String itemKey,
                                      int iconRes, String description, View.OnClickListener listener) {
        ImageButton button = new ImageButton(activity);
        button.setImageResource(iconRes);
        button.setContentDescription(description);
        boolean active = itemKey.equals(activeKey);
        styleFooterIconButton(activity, button, active);
        if (!active) {
            button.setOnClickListener(listener);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(activity, 40), 1);
        if (footer.getChildCount() > 0) {
            params.setMargins(dp(activity, 5), 0, 0, 0);
        }
        footer.addView(button, params);
    }

    private static void putLanguageExtras(Intent intent, String uiLanguageCode, String languageOneName,
                                          String languageOneCode, String languageTwoName, String languageTwoCode) {
        intent.putExtra("ui_language_code", uiLanguageCode);
        intent.putExtra("language_1_name", languageOneName);
        intent.putExtra("language_1_code", languageOneCode);
        intent.putExtra("language_2_name", languageTwoName);
        intent.putExtra("language_2_code", languageTwoCode);
    }

    private static String navLabel(String uiLanguageCode, String key) {
        boolean vi = "vi".equals(uiLanguageCode);
        if ("settings".equals(key)) {
            return vi ? "Cai dat" : "Settings";
        }
        if ("runtime".equals(key)) {
            return vi ? "Runtime" : "Runtime";
        }
        return key;
    }

    static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
