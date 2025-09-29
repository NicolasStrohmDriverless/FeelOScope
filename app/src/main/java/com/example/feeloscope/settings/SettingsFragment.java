package com.example.feeloscope.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;

import com.example.feeloscope.R;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String KEY_OVERLAY_OPACITY = "pref_overlay_opacity";
    private SeekBarPreference overlayPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        overlayPreference = findPreference(KEY_OVERLAY_OPACITY);
        updateOverlaySummary();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        if (preferences != null) {
            preferences.registerOnSharedPreferenceChangeListener(this);
        }
        updateOverlaySummary();
    }

    @Override
    public void onPause() {
        SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        if (preferences != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(this);
        }
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (KEY_OVERLAY_OPACITY.equals(key)) {
            updateOverlaySummary();
        }
    }

    private void updateOverlaySummary() {
        if (overlayPreference == null) {
            return;
        }
        int value = overlayPreference.getValue();
        String summary = getString(R.string.pref_summary_overlay_opacity) + "\n"
                + getString(R.string.pref_overlay_opacity_value, value);
        overlayPreference.setSummary(summary);
    }
}
