package aq.metallists.loudbang;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import aq.metallists.loudbang.cutil.CJarInterface;
import aq.metallists.loudbang.cutil.DBHelper;
import aq.metallists.loudbang.ui.main.PageViewModel;

public class LogaFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;
    private BroadcastReceiver bs;
    private SimpleCursorAdapter sca;
    private Cursor c;
    private DBHelper db;

    public static LogaFragment newInstance(int index) {
        LogaFragment fragment = new LogaFragment();
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

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_loga, container, false);

        final ListView lv = (ListView) root.findViewById(R.id.loggerLV);

        this.db = new DBHelper(this.getActivity().getApplicationContext());
        this.c = db.getReadableDatabase().rawQuery(
                "SELECT c.id AS _id,c.call,c.grid,c.power,c.mygrid,m.freq,m.date,m.snr,c.uploaded " +
                        "FROM contacts AS c " +
                        "INNER JOIN messages AS m " +
                        "ON c.message = m.id;"
                , null);
        //this.getActivity().startManagingCursor(this.c);

        String[] from = new String[]{
                "call", "grid", "power", "snr", "freq", "date", "uploaded"
        };
        int[] to = new int[]{
                R.id.lt_message, R.id.log_distance, R.id.log_power, R.id.log_snr,
                R.id.log_frequency, R.id.log_date, R.id.log_uploaded
        };
        sca = new SimpleCursorAdapter(this.getActivity(), R.layout.msglog_layout, this.c, from, to) {
            @Override
            public void setViewText(TextView v, String text) {
                switch (v.getId()) {
                    case R.id.lt_message:
                        text = String.format(Locale.GERMAN,
                                "%s %s %d",
                                this.getCursor().getString(this.getCursor().getColumnIndex("call")),
                                this.getCursor().getString(this.getCursor().getColumnIndex("grid")),
                                this.getCursor().getInt(this.getCursor().getColumnIndex("power"))
                        );
                        break;
                    case R.id.log_distance:
                        text = Integer.toString((int) CJarInterface.WSPRGetDistanceBetweenLocators(
                                this.getCursor().getString(this.getCursor().getColumnIndex("grid")),
                                this.getCursor().getString(this.getCursor().getColumnIndex("mygrid"))
                        ));
                        break;
                    case R.id.log_power:
                        int wattage = this.getCursor().getInt(this.getCursor().getColumnIndex("power"));
                        String wtg = Integer.toString(wattage);

                        String[] powervals = LogaFragment.this.getActivity()
                                .getResources().getStringArray(R.array.sets_powerarr_value);
                        String[] wattages = LogaFragment.this.getActivity()
                                .getResources().getStringArray(R.array.sets_wattages);
                        if (powervals.length != wattages.length) {
                            text = "ERROR!";
                            break;
                        }

                        text = wtg.concat(" dBm");

                        for (int i = 0; i < powervals.length; i++) {
                            if (powervals[i].equals(wtg)) {
                                text = wattages[i];
                                break;
                            }
                        }

                        break;
                    case R.id.log_snr:
                        text = String.format(Locale.GERMAN, "%.2f",
                                this.getCursor().getFloat(
                                        this.getCursor().getColumnIndex("snr")));
                        break;
                    case R.id.log_frequency:
                        text = String.format(Locale.GERMAN, "%.6f",
                                this.getCursor().getFloat(
                                        this.getCursor().getColumnIndex("freq")));
                        break;
                    case R.id.log_date:
                        String date = this.getCursor().getString(
                                this.getCursor().getColumnIndex("date"));
                        try {
                            DateFormat dfOld = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                            text = df.format(dfOld.parse(date));
                        } catch (Exception x) {
                            text = "ERROR";
                        }
                        break;
                    case R.id.log_uploaded:
                        if (this.getCursor().getInt(
                                this.getCursor().getColumnIndex("uploaded")) > 0) {
                            text = getString(R.string.lbl_log_uploaded_yes);
                        } else {
                            text = getString(R.string.lbl_log_uploaded_no);
                        }
                        break;
                    default:
                }
                super.setViewText(v, text);
            }
        };

        this.installSelectBoxes(root);

        sca.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence charSequence) {
                String sql = "SELECT " +
                        "c.id AS _id,c.call,c.grid,c.power,c.mygrid,m.freq,m.date,m.snr,c.uploaded " +
                        "FROM contacts AS c " +
                        "INNER JOIN messages AS m " +
                        "ON c.message = m.id";
                sql = sql.concat(" WHERE ");
                String[] filters = charSequence.toString().split("/");

                if (filters.length == 2) {
                    int band = Integer.parseInt(filters[0]);
                    if (band > 0) {
                        String[] bandarr = getActivity().getApplicationContext()
                                .getResources().getStringArray(R.array.filter_bandarr_value);
                        double freq = Double.parseDouble(bandarr[band]);
                        if (freq > 0.001) {
                            sql += String.format(Locale.ENGLISH,
                                    "m.freq > %f AND m.freq < %f AND ", (freq - 0.006), (freq + 0.006));
                        }
                    }

                    int date = Integer.parseInt(filters[1]);
                    if (date > 0) {
                        String[] datearr = getActivity().getApplicationContext()
                                .getResources().getStringArray(R.array.filter_datearr_value);
                        date = Integer.parseInt(datearr[date]);
                        if (date > 0)
                            sql += String.format(Locale.GERMANY,
                                    "m.date > date('now','-%d seconds') AND ", date);
                    }
                }

                sql = sql.concat(" 1=1 ;");
                //Toast.makeText(LogaFragment.this.getContext(), sql, Toast.LENGTH_SHORT).show();

                return db.getReadableDatabase().rawQuery(sql, null);
            }
        });

        lv.setAdapter(sca);

        this.bs = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (!LogaFragment.this.sca.getCursor().isClosed())
                    LogaFragment.this.sca.getCursor().requery();
            }
        };

        if (this.getActivity() != null)
            LocalBroadcastManager.getInstance(this.getActivity()).registerReceiver(bs,
                    new IntentFilter("eme.eva.loudbang.message"));


        pageViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
            }
        });
        return root;
    }

    private void installSelectBoxes(View root) {
        final Spinner spBandFilter = (Spinner) root.findViewById(R.id.bandFilterSelect);
        final Spinner spDateFilter = (Spinner) root.findViewById(R.id.dateFilterSelect);
        spBandFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                sca.getFilter().filter(
                        String.format("%d/%d",
                                spBandFilter.getSelectedItemPosition(),
                                spDateFilter.getSelectedItemPosition()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        spDateFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                sca.getFilter().filter(
                        String.format("%d/%d",
                                spBandFilter.getSelectedItemPosition(),
                                spDateFilter.getSelectedItemPosition()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    public void onDestroy() {
        super.onDestroy();
        this.c.close();
        if (this.getActivity() != null)
            LocalBroadcastManager.getInstance(this.getActivity()).unregisterReceiver(bs);
    }
}
