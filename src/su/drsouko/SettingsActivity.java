package su.drsouko;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

public class SettingsActivity extends Activity {
	public static final String PREF_CTRLPLACE="control_button_placement";
	public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener{
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs);
			onSharedPreferenceChanged(null,PREF_CTRLPLACE);
		}
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if(key.equals(PREF_CTRLPLACE)) {
				ListPreference ctrlplacePrefs=(ListPreference)findPreference(PREF_CTRLPLACE);
				ctrlplacePrefs.setSummary(ctrlplacePrefs.getEntry());
			}
		}
		@Override
		public void onResume() {
			super.onResume();
			SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
			sharedPreferences.registerOnSharedPreferenceChangeListener(this);
		}
		@Override
		public void onPause() {
			super.onPause();
			SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
			sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction()
		.replace(android.R.id.content, new SettingsFragment())
		.commit();
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}
	public boolean onMenuItemSelected(int featureId,MenuItem item) {
		int itemid=item.getItemId();
		if(itemid==android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}
}
