package aq.metallists.loudbang;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.regex.Pattern;

public class SettingsActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        this.sp = PreferenceManager.getDefaultSharedPreferences(this);
        this.sp.registerOnSharedPreferenceChangeListener(this);

        this.stopService(new Intent(this, LBService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.stopService(new Intent(this, LBService.class));
    }

    protected void onDestroy() {
        this.sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    private void showFuckupDialog(int text) {
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setTitle(R.string.sets_error_title);
        ab.setMessage(text);

        ab.setPositiveButton(R.string.welcomdlg_button, (dialog, which) -> {
            Intent it = getIntent();
            finish();
            startActivity(it);
        });
        ab.setCancelable(false);
        ab.setOnDismissListener(dialog -> {
            Intent it = getIntent();
            finish();
            startActivity(it);
        });

        ab.create().show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case "tx_probability":
                try {
                    int txprob = Integer.parseInt(
                            sharedPreferences.getString("tx_probability", "25"));
                    if (txprob < 0 || txprob > 100) {
                        throw new NumberFormatException("WUT?");
                    }
                } catch (Exception x) {
                    sharedPreferences.edit().putString("tx_probability", "25").apply();
                    showFuckupDialog(R.string.sets_error_duty);
                }

                break;
            case "band":
                if (!sharedPreferences.getBoolean("bandwarn_msg_displayed", false)) {
                    sharedPreferences.edit().putBoolean("bandwarn_msg_displayed", true).apply();
                    showFuckupDialog(R.string.sets_error_bands);
                }
                break;
            case "gridsq":
                String grid = sharedPreferences.getString("gridsq", "LO05io");

                Pattern gpatrn = Pattern.compile("^[A-Z]{2}[0-9]{2}[a-z]{2}$");

                if (!gpatrn.matcher(grid).matches()) {
                    sharedPreferences.edit().putString("gridsq", "LO05io").apply();
                    showFuckupDialog(R.string.sets_error_gridsq);
                }

                break;

            case "callsign":
                String callsign = sharedPreferences.getString("callsign", "R0TES");

                Pattern cspatrn = Pattern.compile(
                        "^[A-Z0-9]{1,3}/[A-Z0-9]{1,2}[0-9][A-Z0-9]{1,3}$|^[A-Z0-9]{1,2}[0-9][A-Z0-9]{1,3}$|^[A-Z0-9]{1,2}[0-9][A-Z0-9]{1,3}/[A-Z0-9]$|^[A-Z0-9]{1,2}[0-9][A-Z0-9]{1,3}/[0-9]{2}$");

                if (!cspatrn.matcher(callsign).matches()) {
                    //sharedPreferences.edit().putString("callsign", "R0TES").apply();
                    showFuckupDialog(R.string.sets_error_callsign);
                }
                break;
            case "use_6letter":
                String callsign2 = sharedPreferences.getString("callsign", "R0TES");

                Pattern cspatrn2 = Pattern.compile(
                        "^[A-Z0-9]{1,3}/[A-Z0-9]{1,2}[0-9][A-Z0-9]{1,3}$|^[A-Z0-9]{1,2}[0-9][A-Z0-9]{1,3}/[A-Z0-9]$|^[A-Z0-9]{1,2}[0-9][A-Z0-9]{1,3}/[0-9]{2}$");

                if (cspatrn2.matcher(callsign2).matches() && !sharedPreferences.getBoolean("use_6letter", false)) {
                    sharedPreferences.edit().putBoolean("use_6letter", true).apply();
                    showFuckupDialog(R.string.sets_error_6letters);
                }
                break;

            default:
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }
    }
}
