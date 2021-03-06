package aq.metallists.loudbang;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import android.text.format.Time;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;


import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import aq.metallists.loudbang.cutil.DBToXMLConverter;
import aq.metallists.loudbang.ui.main.SectionsPagerAdapter;

public class LBMainWindow extends AppCompatActivity {
    private SharedPreferences sp;
    private boolean isAwake = false;
    private MenuItem awakeItem = null;

    private void checkTheWakelock() {
        if (isAwake) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (awakeItem != null) {
                awakeItem.setChecked(true);
            }
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (awakeItem != null) {
                awakeItem.setChecked(false);
            }
        }
    }

    protected void onResume() {
        super.onResume();

        this.checkTheWakelock();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lbmain_window);

        Toolbar lb = findViewById(R.id.toolbarx);
        setSupportActionBar(lb);

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);

        this.sp = PreferenceManager.getDefaultSharedPreferences(this);
        /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/

        if (!this.sp.getBoolean("welcome_dialog_shown", false)) {
            this.sp.edit().putBoolean("welcome_dialog_shown", true).apply();
            AlertDialog.Builder ab = new AlertDialog.Builder(this);
            ab.setTitle(R.string.welcomdlg_title);
            ab.setMessage(R.string.welcomdlg_text);

            ab.setPositiveButton(R.string.welcomdlg_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });

            ab.create().show();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);

        // wakelock status
        awakeItem = menu.findItem(R.id.mnu_wake_lock);

        return true;
    }

    private void uploadAbortRequested() {
        AlertDialog.Builder db = new AlertDialog.Builder(this);
        db.setCancelable(true);
        db.setTitle(R.string.uploadabort_title);
        db.setMessage(R.string.uploadabort_text);
        db.setPositiveButton(R.string.uploadabort_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                stopService(new Intent(LBMainWindow.this, LBSpotUploadService.class));
            }
        });

        db.setNegativeButton(R.string.uploadabort_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(LBMainWindow.this, R.string.spot_upload_aborted, Toast.LENGTH_LONG).show();
            }
        });

        db.create().show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection

        if (this.menuDealWithBandSwitch(item.getItemId()))
            return true;

        switch (item.getItemId()) {
            case R.id.mnu_go_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                break;
            case R.id.mnu_go_msgdetails:
                Intent i2 = new Intent(this, MessageDetailsActivity.class);
                startActivity(i2);
                break;

            case R.id.mnu_wake_lock:
                this.isAwake = !this.isAwake;
                this.checkTheWakelock();
                break;

            case R.id.mnu_switch_mode_next:
                sp.edit().putBoolean("switch_mode_next", true).apply();
                if (sp.getBoolean("use_tx", false)) {
                    Toast.makeText(getApplicationContext(), R.string.status_lbl_mode_switched_rx, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.status_lbl_mode_switched_tx, Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.mnu_upload_remaining_items:
                if (!this.isMyServiceRunning(LBSpotUploadService.class)) {
                    startService(new Intent(this, LBSpotUploadService.class));
                } else {
                    uploadAbortRequested();
                }
                break;

            case R.id.mnu_database_export:
                try {
                    (new DBToXMLConverter(this.getApplicationContext())).exportToXML();
                } catch (Exception x) {
                    x.printStackTrace();
                    Toast.makeText(getApplicationContext(), R.string.error_exporting, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.mnu_database_wipe:
                try {
                    (new DBToXMLConverter(this)).wipeOut();
                } catch (Exception x) {
                    x.printStackTrace();
                    Toast.makeText(getApplicationContext(), R.string.error_wiping, Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.mnu_about:
                Intent AboutWndIntent = new Intent(this, AboutWindow.class);
                startActivity(AboutWndIntent);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private boolean menuDealWithBandSwitch(int menuItemId) {
        switch (menuItemId) {
            case R.id.mnu_setband_b33400m:
                sp.edit().putString("band", "0.0072").apply();
                break;
            case R.id.mnu_setband_b2200m:
                sp.edit().putString("band", "0.136").apply();
                break;
            case R.id.mnu_setband_b600m:
                sp.edit().putString("band", "0.4742").apply();
                break;
            case R.id.mnu_setband_b160m:
                sp.edit().putString("band", "1.8366").apply();
                break;
            case R.id.mnu_setband_b80m:
                sp.edit().putString("band", "3.5926").apply();
                break;
            case R.id.mnu_setband_b80m_jp:
                sp.edit().putString("band", "3.5686").apply();
                break;
            case R.id.mnu_setband_b60m:
                sp.edit().putString("band", "5.2872").apply();
                break;
            case R.id.mnu_setband_b60m_eu:
                sp.edit().putString("band", "5.3647").apply();
                break;
            case R.id.mnu_setband_b40m:
                sp.edit().putString("band", "7.0386").apply();
                break;
            case R.id.mnu_setband_b30m:
                sp.edit().putString("band", "10.1387").apply();
                break;
            case R.id.mnu_setband_b20m:
                sp.edit().putString("band", "14.0956").apply();
                break;
            case R.id.mnu_setband_b17m:
                sp.edit().putString("band", "18.1046").apply();
                break;
            case R.id.mnu_setband_b15m:
                sp.edit().putString("band", "21.0946").apply();
                break;
            case R.id.mnu_setband_b12m:
                sp.edit().putString("band", "24.9246").apply();
                break;
            case R.id.mnu_setband_b10m:
                sp.edit().putString("band", "28.1246").apply();
                break;
            case R.id.mnu_setband_b6m:
                sp.edit().putString("band", "50.293").apply();
                break;
            case R.id.mnu_setband_b4m:
                sp.edit().putString("band", "70.091").apply();
                break;
            case R.id.mnu_setband_b2m:
                sp.edit().putString("band", "144.489").apply();
                break;
            case R.id.mnu_setband_b70cm:
                sp.edit().putString("band", "432.300").apply();
                break;
            case R.id.mnu_setband_b23cm:
                sp.edit().putString("band", "1296.500").apply();
                break;
            default:
                return false;
        }
        return true;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}