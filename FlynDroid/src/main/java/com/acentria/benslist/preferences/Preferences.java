package com.acentria.benslist.preferences;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.acentria.benslist.Config;
import com.acentria.benslist.Lang;
import com.acentria.benslist.R;
import com.acentria.benslist.Utils;

public class Preferences extends AppCompatActivity {
    private static String[][] ListPref = new String[][] {
            new String[] {"pref_reset_domain", "android_user_pref_change_domain", "android_user_pref_change_domain_summary"},
            new String[] {"select_lang", "android_user_pref_change_language", "android_user_pref_change_language_summary"},
            new String[] {"preload_method", "android_user_pref_preload_method", "android_user_pref_preload_method_summary"},
            new String[] {"clear_cache", "android_user_pref_clear_cache", "android_user_pref_clear_cache_summary"}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new PreferencesFragment()).commit();

        /* enable back action */
		ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
	        case android.R.id.home:
	        	super.onBackPressed();
				return true;

	        default:
	            return super.onOptionsItemSelected(item);
        }
    }


    public static class PreferencesFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {


        @SuppressLint("NewApi") @SuppressWarnings("deprecation")
        @Override
        public void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            /* translate preferences */
            translate();

            /* remove reset domain option for privately labeled */
            if ( !Utils.getConfig("customer_domain").isEmpty() ) {
                ChangeSiteDialog reset_domain = (ChangeSiteDialog) findPreference("pref_reset_domain");
                PreferenceScreen screen = getPreferenceScreen();
                screen.removePreference(reset_domain);
            }

            /* select language */
            CharSequence[] entries = (CharSequence[]) Config.cacheLangNames.toArray(new CharSequence[Config.cacheLangNames.size()]);
            CharSequence[] entryValues = (CharSequence[]) Config.cacheLangCodes.toArray(new CharSequence[Config.cacheLangCodes.size()]);

            final ListPreference lang = (ListPreference) findPreference("select_lang");
            lang.setDialogTitle(Lang.get("android_toast_list"));

            lang.setEntries(entries);
            lang.setEntryValues(entryValues);
            lang.setValue(Lang.getSystemLang());
            if ( entries.length == 1 ) {
                lang.setEnabled(false);
            }

            lang.setOnPreferenceChangeListener(new ListPreference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference prf, Object newValue) {
                    if ( !newValue.toString().equals(Utils.getSPConfig("select_lang", null)) ) {
                        Config.changeLanguage(newValue.toString());
                        translate();
                        Config.restartApp();
                    }
                    return true;
                }
            });
        }

        private void translate() {
            for ( int i = 0; i < ListPref.length; i++ ) {
                Preference pref = findPreference(ListPref[i][0]);

                if ( pref == null )
                    continue;

                pref.setTitle(Lang.get(ListPref[i][1]));
                pref.setSummary(Lang.get(ListPref[i][2]));
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onResume(){
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    //		if ( key.equals("select_lang") ) {
    //			translate();
    //		}
        }


    }

}

