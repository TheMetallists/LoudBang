package aq.metallists.loudbang;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import aq.metallists.loudbang.cutil.DBHelper;
import aq.metallists.loudbang.cutil.WSPRNetSender;

public class LBSpotUploadService extends Service implements Runnable {
    public static final String NOTCH_ID = "LBSpotUploadServiceChannel";
    NotificationCompat.Builder ncb;
    Thread tht;
    boolean quitter = false; // TODO: decide how to quit properly.

    public LBSpotUploadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.createNotificationChannel();

        this.ncb = new NotificationCompat.Builder(this, NOTCH_ID);
        this.ncb = this.ncb
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.state_uploading_spots))
                .setSmallIcon(R.drawable.ic_bomb)
                .setNotificationSilent()
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        new Intent(this, LBMainWindow.class), 0))
                .setProgress(100, 0, true);
        startForeground(1, this.ncb.build());
        this.tht = new Thread(this);
        this.tht.start();

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel nc = new NotificationChannel(
                        NOTCH_ID, "Foreground SC", NotificationManager.IMPORTANCE_DEFAULT);
                NotificationManager nm = getSystemService(NotificationManager.class);
                nm.createNotificationChannel(nc);
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    private void getAndSendSpots() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        DBHelper dh = new DBHelper(this.getApplicationContext());
        Cursor c = dh.getReadableDatabase().rawQuery(
                "SELECT c.id,c.call,c.grid,c.power,c.mygrid,msg.date,msg.freq,msg.snr,msg.dt,msg.drift" +
                        " FROM contacts AS c INNER JOIN messages as msg ON msg.id = c.message WHERE uploaded < 1;"
                , null);

        this.ncb = this.ncb.setProgress(c.getCount(), 0, false);
        nm.notify(1, ncb.build());

        WSPRNetSender wsn = new WSPRNetSender(dh);
        DateFormat dfOld = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

        if (sp.getString("callsign", "xxxRT3STxxx").equals("xxxRT3STxxx")) {
            Toast.makeText(this,
                    R.string.error_spotupload_nocall,
                    Toast.LENGTH_LONG).show();
            return;
        }


        if (c.moveToFirst()) {
            int ctr = 0;
            do {
                ctr++;
                this.ncb = this.ncb
                        .setProgress(c.getCount(), ctr, false)
                        .setContentText(getString(R.string.state_uploading_spots_progress, ctr, c.getCount()));
                nm.notify(1, this.ncb.build());
                Date dt;
                try {
                    dt = dfOld.parse(c.getString(c.getColumnIndex("date")));
                } catch (Exception x) {
                    continue;
                }

                wsn.append(
                        c.getInt(c.getColumnIndex("id")),
                        c.getString(c.getColumnIndex("call")),
                        c.getString(c.getColumnIndex("grid")),
                        Integer.toString(c.getInt(c.getColumnIndex("power"))),
                        dt,
                        c.getDouble(c.getColumnIndex("freq")),
                        c.getFloat(c.getColumnIndex("snr")),
                        c.getFloat(c.getColumnIndex("dt")),
                        c.getFloat(c.getColumnIndex("drift"))
                );

                String band = "";
                try {
                    band = this.freq2band(c.getDouble(c.getColumnIndex("freq")));
                } catch (Exception x) {
                    continue;
                }

                wsn.send(
                        c.getString(c.getColumnIndex("mygrid")),
                        sp.getString("callsign", "RT3ST"),
                        band
                );

            } while (c.moveToNext() && !quitter);
        }

        c.close();
    }

    @Override
    public void onDestroy() {
        quitter = true;
    }

    private String freq2band(double freq) {
        String sfreq = Double.toString(freq);
        if (sfreq.startsWith("0.00")) {
            return "0.0072";
        }
        if (sfreq.startsWith("0.13")) {
            return "0.136";
        }
        if (sfreq.startsWith("0.47")) {
            return "0.4742";
        }
        if (sfreq.startsWith("1.83")) {
            return "1.8366";
        }
        if (sfreq.startsWith("3.59")) {
            return "3.5926";
        }
        if (sfreq.startsWith("3.56")) {
            return "3.5686";
        }
        if (sfreq.startsWith("5.28")) {
            return "5.2872";
        }
        if (sfreq.startsWith("5.36")) {
            return "5.3647";
        }
        if (sfreq.startsWith("7.0")) {
            return "7.0386";
        }
        if (sfreq.startsWith("10.1")) {
            return "10.1387";
        }
        if (sfreq.startsWith("14.0")) {
            return "14.0956";
        }
        if (sfreq.startsWith("18.1")) {
            return "18.1046";
        }
        if (sfreq.startsWith("21.")) {
            return "21.0946";
        }
        if (sfreq.startsWith("24.")) {
            return "24.9246";
        }
        if (sfreq.startsWith("28.")) {
            return "28.1246";
        }
        if (sfreq.startsWith("50.")) {
            return "50.293";
        }
        if (sfreq.startsWith("70.")) {
            return "70.091";
        }
        if (sfreq.startsWith("144.48")) {
            return "144.489";
        }
        if (sfreq.startsWith("432.30")) {
            return "432.300";
        }
        if (sfreq.startsWith("1296.5")) {
            return "1296.500";
        }
        Toast.makeText(this.getApplicationContext(),
                "FRQ. does not belongs to any known band.\n" + sfreq,
                Toast.LENGTH_LONG).show();
        throw new IllegalArgumentException("Frequency dows not belong to any known band.");
    }

    @Override
    public void run() {
        try {
            this.getAndSendSpots();
        } catch (Exception x) {
            x.printStackTrace();


            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.error_uploading_spots)
                                    .concat("\n")
                                    .concat(x.getMessage())
                            , Toast.LENGTH_LONG).show();
                }
            });

        }
        stopSelf();
    }
}
