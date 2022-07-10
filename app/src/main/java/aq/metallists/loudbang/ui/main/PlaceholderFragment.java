package aq.metallists.loudbang.ui.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import aq.metallists.loudbang.LBService;
import aq.metallists.loudbang.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String ARG_SECTION_NUMBER = "section_number";
    BroadcastReceiver bs;
    private PageViewModel pageViewModel;
    private SharedPreferences sp;

    public static PlaceholderFragment newInstance(int index) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    public void onResume() {
        super.onResume();

        try {
            final TextView state = this.getView().findViewById(R.id.statusLabel1);

            if (LBService.lastKnownState.length() > 0) {
                state.setText(LBService.lastKnownState);
            }
        } catch (Exception x) {
        }
    }

    public void onDestroyView() {
        if (this.bs != null) {
            try {
                LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(this.bs);
            } catch (Exception x) {

            }
        }

        if (this.sp != null)
            this.sp.unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroyView();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_status, container, false);

        final ProgressBar pbbm = root.findViewById(R.id.progressBar);
        final TextView state = root.findViewById(R.id.statusLabel1);
        final ToggleButton ltb = root.findViewById(R.id.launch_toggle_btn);
        final TextView lblTxNExt = root.findViewById(R.id.lblTransmitNextCount);

        if (LBService.lastKnownState.length() > 0) {
            state.setText(LBService.lastKnownState);
        }

        pbbm.setMax(32767);

        this.sp = PreferenceManager.getDefaultSharedPreferences(this.getActivity());
        this.bs = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().contains("eme.eva.loudbang.state")) {
                    boolean isRunnin = isMyServiceRunning(LBService.class);
                    ltb.setChecked(isRunnin);

                    state.setText(intent.getStringExtra("eme.eva.loudbang.state"));
                } else {
                    pbbm.setProgress(intent.getIntExtra("eme.eva.loudbang.level", 50));
                }
            }
        };

        // eme.eva.loudbang.state
        IntentFilter intentActionFilter = new IntentFilter();
        intentActionFilter.addAction("eme.eva.loudbang.state");
        intentActionFilter.addAction("eme.eva.loudbang.recordlevel");

        LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(bs, intentActionFilter);


        ltb.setOnClickListener(v -> {
            List<String> permissionsToRequest = new ArrayList<>();

            addPermissionToRequest(root,
                    permissionsToRequest, Manifest.permission.RECORD_AUDIO);

            if (sp.getBoolean("use_gps", false)) {
                addPermissionToRequest(root,
                        permissionsToRequest, Manifest.permission.ACCESS_FINE_LOCATION);
            }

            if (sp.getBoolean("use_celltowers", false)) {
                addPermissionToRequest(root,
                        permissionsToRequest, Manifest.permission.ACCESS_COARSE_LOCATION);
            }


            if (permissionsToRequest.size() > 0) {
                ActivityCompat.requestPermissions(PlaceholderFragment.this.getActivity(),
                        permissionsToRequest.toArray(new String[]{}),
                        0);

                ltb.setChecked(false);
                return;
            }


            if (!isMyServiceRunning(LBService.class)) {
                PlaceholderFragment.this.getActivity().startService(
                        new Intent(PlaceholderFragment.this.getActivity(), LBService.class));
                ltb.setChecked(true);
            } else {
                PlaceholderFragment.this.getActivity().stopService(
                        new Intent(PlaceholderFragment.this.getActivity(), LBService.class));
                ltb.setChecked(false);
            }
        });

        if (isMyServiceRunning(LBService.class)) {
            ltb.setChecked(true);
        }

        TextView bandname = root.findViewById(R.id.lbl_band);
        bandname.setText(this.getBandName(sp.getString("band", Double.toString(10.1387))));

        TextView callsign = root.findViewById(R.id.lbl_callsign);
        callsign.setText(this.sp.getString("callsign", "R0TST"));

        TextView grid = root.findViewById(R.id.lbl_grid);
        grid.setText(this.sp.getString("gridsq", "LO05io"));
        lblTxNExt.setText(Integer.toString(this.sp.getInt("tx_next_counter", 0)));

        TextView txstate = root.findViewById(R.id.lbl_txstate);
        String rxtx_state = "";
        if (this.sp.getBoolean("use_tx", false)) {
            rxtx_state = getString(R.string.lbl_tx_enabled);
        } else {
            rxtx_state = getString(R.string.lbl_tx_disabled);
        }

        switch (this.sp.getString("ptt_ctl", "none")) {
            case "none":
                rxtx_state = rxtx_state + getString(R.string.tbt_txptt_noptt);
                break;
            case "fbang_2":
                rxtx_state = rxtx_state + getString(R.string.tbt_txptt_fc);
                break;
            case "fbang_1":
                rxtx_state = rxtx_state + getString(R.string.tbt_txptt_rc);
                break;
            default:
                rxtx_state = rxtx_state + ", <ERR>";
        }

        rxtx_state = String.format(Locale.GERMAN, "%s, %d%%", rxtx_state,
                Integer.parseInt(this.sp.getString("tx_probability", "25"))
        );

        txstate.setText(rxtx_state);


        //set tracking timer forsome systems
        final TextView lblCurrentTime = root.findViewById(R.id.lblCurrentTime);
        final Timer tmr = new Timer();
        tmr.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Time today = new Time(Time.getCurrentTimezone());
                today.setToNow();
                final String out = String.format(Locale.GERMANY, "%02d.%02d.%04d %02d:%02d:%02d",
                        today.monthDay, today.month, today.year, today.hour, today.minute, today.second);

                Activity thisAct = getActivity();
                if (thisAct == null) {
                    tmr.cancel();
                    tmr.purge();
                    return;
                }

                thisAct.runOnUiThread(() -> {
                    lblCurrentTime.setText(out);

                    ltb.setChecked(isMyServiceRunning(LBService.class));
                });
            }
        }, 1000, 1000);


        sp.registerOnSharedPreferenceChangeListener(this);
        return root;
    }

    private void addPermissionToRequest(final View view, final List<String> permissionsToRequest,
                                        final String permission) {
        if (ContextCompat.checkSelfPermission(view.getContext(), permission)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(permission);
        }
    }

    private String getBandName(String freq) {
        String[] freqs = this.getActivity().getResources().getStringArray(R.array.sets_bandarr_value);
        String[] bands = this.getActivity().getResources().getStringArray(R.array.sets_bandarr_name);

        if (freqs.length != bands.length) {
            return "<ERROR>";
        }

        for (int i = 0; i < freqs.length; i++) {
            if (freqs[i].equals(freq)) {
                return bands[i];
            }
        }

        return freq;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        Context ctx = this.getActivity();
        if (ctx == null) {
            return false;
        }

        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null)
            return false;

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        try {
            switch (key) {
                case "tx_next_counter":
                    TextView lblTxNExt = this.getView().findViewById(R.id.lblTransmitNextCount);
                    if (lblTxNExt != null)
                        lblTxNExt.setText(Integer.toString(this.sp.getInt("tx_next_counter", 0)));
                    break;
                case "band":
                    TextView bandname = this.getView().findViewById(R.id.lbl_band);
                    if (bandname != null)
                        bandname.setText(this.getBandName(
                                sp.getString("band", Double.toString(10.1387))));
                    break;
                case "callsign":
                    TextView callsign = this.getView().findViewById(R.id.lbl_callsign);
                    if (callsign != null)
                        callsign.setText(this.sp.getString("callsign", "R0TST"));
                    break;
                case "gridsq":
                    TextView grid = this.getView().findViewById(R.id.lbl_grid);
                    if (grid != null)
                        grid.setText(this.sp.getString("gridsq", "LO05io"));
                    break;
                case "use_tx":
                case "tx_probability":
                case "ptt_ctl":
                    TextView txstate = this.getView().findViewById(R.id.lbl_txstate);
                    if (txstate == null)
                        break;
                    String rxtx_state = "";
                    if (this.sp.getBoolean("use_tx", false)) {
                        rxtx_state = getString(R.string.lbl_tx_enabled);
                    } else {
                        rxtx_state = getString(R.string.lbl_tx_disabled);
                    }

                    switch (this.sp.getString("ptt_ctl", "none")) {
                        case "none":
                            rxtx_state = rxtx_state + getString(R.string.tbt_txptt_noptt);
                            break;
                        case "fbang_2":
                            rxtx_state = rxtx_state + getString(R.string.tbt_txptt_fc);
                            break;
                        case "fbang_1":
                            rxtx_state = rxtx_state + getString(R.string.tbt_txptt_rc);
                            break;
                        default:
                            rxtx_state = rxtx_state + ", <ERR>";
                    }

                    rxtx_state = String.format(Locale.GERMAN, "%s, %d%%", rxtx_state,
                            Integer.parseInt(this.sp.getString("tx_probability", "25"))
                    );

                    txstate.setText(rxtx_state);
                    break;
                default:
            }
        } catch (Exception x) {
            Log.e("ERROR", "Exception:", x);
        }
    }
}
