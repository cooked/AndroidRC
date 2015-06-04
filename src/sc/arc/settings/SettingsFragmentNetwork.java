package sc.arc.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import sc.arc.R;

public class SettingsFragmentNetwork extends PreferenceFragment {
	
	public static final String KEY_CONN_TYPE = "connection_type";
    public SettingsFragmentNetwork() {
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO define the overall approach to prefrences
        //WifiManager wfm = (WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);
        //startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.pref_connection);
    }
    
    @Override
	public void onResume() {
	    super.onResume();
	    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(ospcl);
        findPreference(KEY_CONN_TYPE).setSummary(
        		getPreferenceScreen().getSharedPreferences().getString(KEY_CONN_TYPE, "Some Default Text"));
	}

	@Override
	public void onPause() {
	    getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(ospcl);
	    super.onPause();
	}
	
	OnSharedPreferenceChangeListener ospcl = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

			    Preference pref = findPreference(key);

			    if (pref instanceof ListPreference) {
			        ListPreference listPref = (ListPreference) pref;
			        pref.setSummary(listPref.getEntry());
			    }
			}
	};
	
}