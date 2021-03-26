package aq.metallists.loudbang;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import aq.metallists.loudbang.cutil.DBHelper;

public class MessageDetailsActivity extends AppCompatActivity {
    private BroadcastReceiver bs;
    private SimpleCursorAdapter sca;
    private Cursor c;
    private DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_details);
        Toolbar toolbar = findViewById(R.id.message_details_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ListView lv = findViewById(R.id.message_details_view);
        this.db = new DBHelper(this.getApplicationContext());
        this.c = db.getReadableDatabase().query("messages",
                new String[]{"*", "id AS _id"}, null,
                null, null, null, null);
        this.startManagingCursor(this.c);

        String[] from = new String[]{"message", "freq"};
        int[] to = new int[]{R.id.log_entry_title, R.id.log_entry_summary};
        sca = new SimpleCursorAdapter(this, R.layout.log_entry_layout, this.c, from, to) {
            @Override
            public void setViewText(TextView v, String text) {
                if (v.getId() == R.id.log_entry_summary) {
                    text = String.format(
                            "SNR: %f Freq: %f Date: %s",
                            this.getCursor().getDouble(this.getCursor().getColumnIndex("snr")),
                            this.getCursor().getDouble(this.getCursor().getColumnIndex("freq")),
                            this.getCursor().getString(this.getCursor().getColumnIndex("date"))
                    );
                }
                super.setViewText(v, text);
            }
        };

        lv.setAdapter(sca);


        this.bs = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                MessageDetailsActivity.this.c.requery();
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(bs,
                new IntentFilter("eme.eva.loudbang.message"));

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sp.getBoolean("msgdetails_dialog_shown", false)) {
            sp.edit().putBoolean("msgdetails_dialog_shown", true).apply();
            AlertDialog.Builder ab = new AlertDialog.Builder(this);
            ab.setTitle(R.string.detailmsg_title);
            ab.setMessage(R.string.detailmsg_text);

            ab.setPositiveButton(R.string.welcomdlg_button, null);

            ab.create().show();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        this.c.close();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bs);
    }
}
