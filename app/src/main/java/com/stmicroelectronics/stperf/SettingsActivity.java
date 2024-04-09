package com.stmicroelectronics.stperf;

import android.os.Bundle;
import android.text.InputType;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

/**
 * SettingsActivity
 */
public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_app, rootKey);
            initSummary(getPreferenceScreen());
        }

        /**
         * Walks through all preferences.
         *
         * @param p The starting preference to search from.
         */
        private void initSummary(Preference p) {
            if (p instanceof PreferenceGroup) {
                PreferenceGroup pGrp = (PreferenceGroup) p;
                for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                    initSummary(pGrp.getPreference(i));
                }
            } else {
                setPreferenceSummary(p);
            }
        }

        /**
         * Sets up summary providers for the preferences.
         *
         * @param p The preference to set up summary provider.
         */
        private void setPreferenceSummary(Preference p) {
            // No need to set up preference summaries for checkbox preferences because
            // they can be set up in xml using summaryOff and summary On
            if (p instanceof ListPreference) {
                p.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
            } else if (p instanceof EditTextPreference) {
                p.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());

                if (p.hasKey() && p.getKey().equals(getString(R.string.pref_period_label))) {
                    // set input type (xml inputType not taken into account)
                    ((EditTextPreference) p).setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
                }
            }
        }
    }
}

