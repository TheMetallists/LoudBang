package aq.metallists.loudbang;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Pattern;

import aq.metallists.loudbang.cutil.CJarInterface;
import aq.metallists.loudbang.cutil.DBHelper;
import aq.metallists.loudbang.cutil.WSPRMessage;
import aq.metallists.loudbang.cutil.WSPRNetSender;

public class LBService extends Service implements Runnable,
        SharedPreferences.OnSharedPreferenceChangeListener, LocationListener {
    public static final String NSC_ID = "NotAServiceChannel";
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int SAMPLE_RATE = 12000; // Hz
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
    private static final int BUFFER_SIZE = 2 * AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING);
    public static String lastKnownState = "";
    private final double dialfreq = 14.0;
    Thread t;
    boolean quitter = false;
    boolean setStatusWithNotification = false;
    SharedPreferences sp;
    int settings_version = 0;
    PowerManager.WakeLock wake;
    private LocationManager glm;
    private AudioTrack audio = null;
    private AudioRecord ar = null;
    private int sessionID = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.sp = PreferenceManager.getDefaultSharedPreferences(this);
        this.setStatus(getString(R.string.sv_status_startin));

        this.createNotificationChannel();

        Intent ni = new Intent(this, LBMainWindow.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, ni, 0);
        int icon = R.drawable.ic_bomb_colorful;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            icon = R.drawable.ic_bomb;
        }
        Notification nt = new NotificationCompat.Builder(this, NSC_ID)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getString(R.string.sv_status_startin))
                .setSmallIcon(icon)
                .setContentIntent(pi)
                .build();

        startForeground(1, nt);
        this.setStatusWithNotification = true;

        this.sp.registerOnSharedPreferenceChangeListener(this);

        this.glm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (sp.getBoolean("use_gps", false)) {
            try {
                glm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 10, 0, this);
            } catch (SecurityException sx) {
                Toast.makeText(this,
                        getString(R.string.error_fine_loca), Toast.LENGTH_LONG)
                        .show();
            }
        }

        if (sp.getBoolean("use_celltowers", false)) {
            try {
                glm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 10, 0, this);
            } catch (SecurityException sx) {
                Toast.makeText(this,
                        getString(R.string.error_crap_loca), Toast.LENGTH_LONG)
                        .show();
            }
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wake = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LoudBang::BangBang");

        t = new Thread(this);
        this.quitter = false;
        t.start();
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel nc = new NotificationChannel(
                        NSC_ID, "Foreground SC", NotificationManager.IMPORTANCE_DEFAULT);
                NotificationManager nm = getSystemService(NotificationManager.class);
                nm.createNotificationChannel(nc);
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        this.quitter = true;

        Intent itt = new Intent("eme.eva.loudbang.state");
        itt.putExtra("eme.eva.loudbang.alive", false);
        itt.putExtra("eme.eva.loudbang.state", getString(R.string.sv_status_quittin));
        LocalBroadcastManager.getInstance(this).sendBroadcast(itt);

        this.sp.unregisterOnSharedPreferenceChangeListener(this);

        try {
            if (audio != null) {
                if (audio.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    audio.stop();
                }
                audio.release();
                audio = null;
            }

        } catch (Exception x) {
            x.printStackTrace();
        }

        try {
            if (ar != null) {
                if (ar.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    ar.stop();
                }
                ar.release();
                ar = null;
            }
        } catch (Exception x) {
            x.printStackTrace();
        }

        try {
            switch (this.sp.getString("ptt_ctl", "none")) {
                case "fbang_1":
                    this.setFlashbangMode(0, false);
                    break;
                case "fbang_2":
                    this.setFlashbangMode(1, false);
                    break;
                default:
            }
        } catch (Exception x) {
            Toast.makeText(getApplicationContext(), getString(R.string.lbl_cannot_disable)
                    .concat("\n\n")
                    .concat(x.getMessage()), Toast.LENGTH_LONG
            ).show();
        }
        try {
            if (wake != null)
                wake.release();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    private void setStatus(String status) {
        lastKnownState = status;

        Intent itt = new Intent("eme.eva.loudbang.state");
        itt.putExtra("eme.eva.loudbang.alive", !this.quitter);
        itt.putExtra("eme.eva.loudbang.state", status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(itt);

        if (setStatusWithNotification) {
            int icon = R.drawable.ic_bomb_colorful;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                icon = R.drawable.ic_bomb;
            }

            Intent ni = new Intent(this, LBMainWindow.class);
            PendingIntent pi = PendingIntent.getActivity(this, 0, ni, 0);
            Notification nt = new NotificationCompat.Builder(this, NSC_ID)
                    .setContentTitle(getText(R.string.app_name))
                    .setContentText(status)
                    .setSmallIcon(icon)
                    .setContentIntent(pi)
                    .build();

            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(1, nt);
        }

    }

    protected void setFlashbangMode(int flashbangID, boolean doBang) {
        CameraManager cmm = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            cmm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                String cameraID = cmm.getCameraIdList()[flashbangID];
                cmm.setTorchMode(cameraID, doBang);
            } catch (Exception x) {
                Toast.makeText(this,
                        getString(R.string.sv_error_flashbang), Toast.LENGTH_LONG);
            }
        } else {
            Toast.makeText(this,
                    getString(R.string.sv_error_flashbang_oldjunk), Toast.LENGTH_LONG);
        }

    }

    @Override
    public void run() {
        DBHelper dh = new DBHelper(this.getApplicationContext());
        this.sessionID = dh.getLastSessionID() + 1;

        boolean doTx = false;
        int probability = 25;
        byte[] txsound = new byte[]{};
        byte[] txsound2 = new byte[]{};
        boolean use_txsound2 = false;
        boolean next_is_txsound2 = false;
        int setsVersion = -1;
        Random rnd = new Random();

        wake.acquire();
        while (!quitter) {
            if (setsVersion != this.settings_version) {
                setsVersion = this.settings_version;
                doTx = this.sp.getBoolean("use_tx", false);

                boolean doSwitchMode = this.sp.getBoolean("switch_mode_next", false);
                if (doSwitchMode) {
                    this.sp.edit()
                            .putBoolean("use_tx", !doTx)
                            .putBoolean("switch_mode_next", false)
                            .apply();
                    doTx = !doTx;
                    this.setStatus(getString(R.string.status_lbl_mode_switched));
                }

                try {
                    probability = Integer.parseInt(this.sp.getString("tx_probability", "25"));
                    if (probability > 100 || probability < 0) {
                        throw new Exception("Bullshit!");
                    }
                } catch (Exception x) {
                    probability = 25;
                }

                if (doTx) {
                    // update txsound
                    boolean lsb_mode = this.sp.getBoolean("lsb_mode", false);

                    String callsign = this.sp.getString("callsign", "R0TST");
                    if (callsign.length() <= 6 && !callsign.contains("/")) {
                        if (this.sp.getBoolean("use_6letter", true)) {
                            use_txsound2 = true;
                            next_is_txsound2 = false;

                            String locator = this.sp.getString("gridsq", "LO05io");
                            int power = Integer.parseInt(this.sp.getString("outpower", "0"));

                            txsound = CJarInterface.WSPREncodeToPCM(
                                    callsign, locator.substring(0, 4), power, 0, lsb_mode);

                            txsound2 = CJarInterface.WSPREncodeToPCM(
                                    callsign, locator, power, 0, lsb_mode);
                        } else {
                            use_txsound2 = false;
                            next_is_txsound2 = false;
                            txsound2 = new byte[]{};

                            String locator = this.sp.getString("gridsq", "LO05io");
                            int power = Integer.parseInt(this.sp.getString("outpower", "0"));


                            txsound = CJarInterface.WSPREncodeToPCM(
                                    callsign, locator.substring(0, 4), power, 0, lsb_mode);
                        }
                    } else {
                        use_txsound2 = true;
                        next_is_txsound2 = false;

                        String locator = this.sp.getString("gridsq", "LO05io");
                        int power = Integer.parseInt(this.sp.getString("outpower", "0"));

                        txsound = CJarInterface.WSPREncodeToPCM(callsign, "", power, 0, lsb_mode);

                        txsound2 = CJarInterface.WSPREncodeToPCM(callsign, locator, power, 0, lsb_mode);
                    }

                } else {
                    if (txsound.length > 0) {
                        txsound = new byte[]{};
                    }
                    if (txsound2.length > 0) {
                        txsound2 = new byte[]{};
                    }
                }
            }

            /*final byte[] music;
            music = CJarInterface.WSPREncodeToPCM("RA0TES", "JO56", 30, 0);
            setStatus("START 0");
            decodersRun(music);


            switch (testIter) {
                case 0:
                    music = CJarInterface.WSPREncodeToPCM("RA0TES", "JO56", 30, 0);
                    setStatus("START 0");
                    break;
                case 1:
                    music = CJarInterface.WSPREncodeToPCM("RA0TES", "JO52wi", 30, 0);
                    setStatus("START 1");
                    break;
                case 2:
                    music = CJarInterface.WSPREncodeToPCM("RA0TES/P", "", 30, 0);
                    setStatus("START 2");
                    break;
                case 3:
                    music = CJarInterface.WSPREncodeToPCM("RA0TES/P", "IO24xh", 30, 0);
                    setStatus("START 3");
                    break;
                default:
                    setStatus("DEFAULT");
                    testIter = 0;
                    continue;
            }

            decodersRun(music);

            testIter++;
            if (true)
                continue;*/

            Time today = new Time(Time.getCurrentTimezone());
            today.setToNow();

            if (today.minute % 2 != 0 || today.second > 0) {
                setStatus(String.format(Locale.getDefault(),
                        getString(R.string.sv_uneven_minute_msg), today.minute));
                try {
                    Thread.sleep((59 - today.second) * 1000);
                    if (quitter) {
                        return;
                    }
                } catch (Exception e) {
                }
                continue;
            }

            //prepairing buffersfor recording...
            ByteArrayOutputStream baos = new ByteArrayOutputStream(); // 12000 * 60 * 2
            this.ar = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_MASK, ENCODING, BUFFER_SIZE);
            //final preparation
            boolean run = true;
            byte[] buffer = new byte[BUFFER_SIZE];
            int read = 0, total = 0;


            this.setStatus(getString(R.string.sv_second_remaining));


            while (today.second < 1) {
                try {
                    Thread.sleep(10);
                } catch (Exception e) {
                }
                today = new Time(Time.getCurrentTimezone());
                today.setToNow();
            }

            if (quitter) {
                return;
            }


            if (doTx && (rnd.nextInt(100) < probability || next_is_txsound2)) {
                this.setStatus(getString(R.string.sv_status_playingbk));

                switch (this.sp.getString("ptt_ctl", "none")) {
                    case "fbang_1":
                        this.setFlashbangMode(0, true);
                        break;
                    case "fbang_2":
                        this.setFlashbangMode(1, true);
                        break;
                    default:
                }

                int length = 0;
                if (next_is_txsound2) {
                    length = txsound2.length;
                } else {
                    length = txsound.length;
                }

                int output_line = AudioManager.STREAM_VOICE_CALL;

                switch (this.sp.getString("tx_output", "call")) {
                    case "call":
                        output_line = AudioManager.STREAM_VOICE_CALL;
                        break;
                    case "ring":
                        output_line = AudioManager.STREAM_RING;
                        break;
                    case "music":
                        output_line = AudioManager.STREAM_MUSIC;
                        break;
                    case "alarm":
                        output_line = AudioManager.STREAM_ALARM;
                        break;
                    default:
                        output_line = AudioManager.STREAM_VOICE_CALL;
                        break;
                }

                this.audio = new AudioTrack(output_line,
                        SAMPLE_RATE, //sample rate
                        AudioFormat.CHANNEL_OUT_MONO, //2 channel
                        ENCODING, // 16-bit
                        length,
                        AudioTrack.MODE_STATIC);

                int slength = 0;

                if (next_is_txsound2) {
                    audio.write(txsound2, 0, txsound2.length);
                    next_is_txsound2 = false;
                    slength = txsound2.length;
                } else {
                    audio.write(txsound, 0, txsound.length);
                    slength = txsound.length;
                    if (use_txsound2)
                        next_is_txsound2 = true;
                }

                audio.play();
                try {
                    Thread.sleep(slength / (SAMPLE_RATE * 2) * 1000);
                    if (quitter) {
                        return;
                    }
                } catch (Exception e) {
                }

                //possible NULLPTR resolution
                if (quitter) {
                    return;
                }

                //NULLPTR:
                if (audio.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    try {
                        Thread.sleep(500);
                        if (quitter) {
                            return;
                        }
                        audio.stop();
                    } catch (Exception e) {
                    }
                }

                audio.release();

                switch (this.sp.getString("ptt_ctl", "none")) {
                    case "fbang_1":
                        this.setFlashbangMode(0, false);
                        break;
                    case "fbang_2":
                        this.setFlashbangMode(1, false);
                        break;
                    default:
                }

                if (!next_is_txsound2) {
                    this.setStatus(this.getString(R.string.sv_status_pbkok));
                } else {
                    this.setStatus(this.getString(R.string.sv_status_pbkok2));
                }

                try {
                    Thread.sleep(1000);
                    if (quitter) {
                        return;
                    }
                } catch (Exception e) {
                }

            } else {

                //recording

                this.setStatus(getString(R.string.sv_status_recording));

                boolean bDoReportVolume = this.sp.getBoolean("report_volume", false);

                ar.startRecording();
                final Date recordTimestamp = new Date(System.currentTimeMillis());
                while (run && !this.quitter) {
                    read = ar.read(buffer, 0, buffer.length);

                    ///AMPT CALC
                    if (bDoReportVolume) {
                        int sum = 0;
                        for (int i = 0; i < read; i++) {
                            sum += Math.abs(buffer[i]);
                        }

                        if (read > 0) {
                            sum = sum / read;
                        }

                        Intent itt = new Intent("eme.eva.loudbang.recordlevel");
                        itt.putExtra("eme.eva.loudbang.level", sum);


                        LocalBroadcastManager.getInstance(this).sendBroadcast(itt);
                    }
                    //END AMPT CALC


                    if (total + read > 12000 * 2 * 114) {
                        // Write as many bytes as we can before hitting the max size
                        for (int i = 0; i < read && total <= 12000 * 2 * 114; i++, total++) {
                            baos.write(buffer[i]);
                        }
                        run = false;
                    } else {
                        // Write out the entire read buffer
                        baos.write(buffer, 0, read);
                        total += read;
                    }
                }

                if (this.quitter) {
                    stopSelf();
                    return;
                }

                //cleaning up
                try {
                    if (ar.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        ar.stop();
                    }
                } catch (IllegalStateException ex) {
                    //
                }
                if (ar.getState() == AudioRecord.STATE_INITIALIZED) {
                    ar.release();
                }


                final byte[] record = baos.toByteArray();
                try {
                    baos.close();
                } catch (Exception x) {

                }
                baos = null;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        decodersRun(record, recordTimestamp);
                    }
                }).start();
            }
        }

        if (wake != null && wake.isHeld())
            wake.release();

        stopSelf();
    }

    private double getBand() {
        String band = sp.getString("band", "band");
        try {
            return Double.parseDouble(band);
        } catch (Exception x) {
            sp.edit().putString("band", Double.toString(10.1387)).apply();
            return 10.1387;
        }
    }

    private void decodersRun(byte[] record, Date recordTimestamp) {
        this.setStatus(getString(R.string.sv_status_decoding));

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        DBHelper dh = new DBHelper(this.getApplicationContext());
        boolean lsb_mode = this.sp.getBoolean("lsb_mode", false);

        WSPRMessage[] wsm = CJarInterface.WSPRDecodeFromPcm(record, this.getBand(), lsb_mode);

        WSPRNetSender sender = new WSPRNetSender(dh);

        for (WSPRMessage wm : wsm) {
            // filling a simplemessage
            ContentValues cv = new ContentValues();
            cv.put("session", this.sessionID);
            cv.put("snr", wm.getSNR());
            cv.put("freq", wm.getFREQ());
            cv.put("message", wm.getMSG());
            cv.put("dt", wm.getDT());
            cv.put("drift", wm.getDRIFT());
            cv.put("date", dateFormat.format(recordTimestamp));
            long nmid = dh.getWritableDatabase().insert("messages", null, cv);

            // determine message type
            Pattern pt1 = Pattern.compile("^[A-Z0-9]?[A-Z0-9][0-9][A-Z0-9]{0,3} [A-Z0-9]{4} \\d+$");
            Pattern pt2 = Pattern.compile("[A-Z0-9/]{1,13} \\d+$");
            Pattern pt3 = Pattern.compile("^#[0-9]{1,6} [A-Z0-9]{6} \\d+$");

            if (pt1.matcher(wm.getMSG()).matches()) {
                Log.e("MSGPARSE", "Got lvl1 message: ".concat(wm.getMSG()));
                // level 1
                String[] parst = wm.getMSG().split(" ");
                if (parst.length != 3) {
                    Log.e("MSGPARSE", "Error parsing message: ".concat(wm.getMSG()));
                    Toast.makeText(this,
                            "Error parsing message: ".concat(wm.getMSG()),
                            Toast.LENGTH_LONG).show();
                    continue;
                }

                ContentValues cv2 = new ContentValues();
                cv2.put("message", nmid);
                cv2.put("call", parst[0]);
                cv2.put("nhash", CJarInterface.WSPRNhash(parst[0]));
                cv2.put("grid", parst[1]);
                try {
                    cv2.put("power", Integer.parseInt(parst[2]));
                } catch (Exception x) {
                    cv2.put("power", -1);
                }
                cv2.put("mygrid", sp.getString("gridsq", "LO05io"));
                long cID = dh.getWritableDatabase().insert("contacts", null, cv2);

                sender.append(cID, parst[0], parst[1], parst[2],
                        new Date(), wm.getFREQ(), wm.getSNR(), wm.getDT(), wm.getDRIFT());

            } else if (pt2.matcher(wm.getMSG()).matches()) {
                Log.e("MSGPARSE", "Got lvl2 message: ".concat(wm.getMSG()));
                String[] parst = wm.getMSG().split(" ");
                if (parst.length != 2) {
                    Log.e("MSGPARSE", "Error parsing message: ".concat(wm.getMSG()));
                    Toast.makeText(this,
                            "Error parsing message: ".concat(wm.getMSG()),
                            Toast.LENGTH_LONG).show();
                    continue;
                }

                ContentValues cv2 = new ContentValues();
                cv2.put("message", nmid);
                cv2.put("call", parst[0]);
                cv2.put("nhash", CJarInterface.WSPRNhash(parst[0]));
                cv2.put("grid", "");
                try {
                    cv2.put("power", Integer.parseInt(parst[1]));
                } catch (Exception x) {
                    cv2.put("power", -1);
                }
                cv2.put("mygrid", sp.getString("gridsq", "LO05io"));
                long cID = dh.getWritableDatabase().insert("contacts", null, cv2);

                sender.append(cID, parst[0], "", parst[1],
                        new Date(), wm.getFREQ(), wm.getSNR(), wm.getDT(), wm.getDRIFT());
            } else if (pt3.matcher(wm.getMSG()).matches()) {
                Log.e("MSGPARSE", "Got lvl3 message: ".concat(wm.getMSG()));
                // level 3
                String[] parst = wm.getMSG().split(" ");
                if (parst.length != 3) {
                    Log.e("MSGPARSE", "Error parsing message: ".concat(wm.getMSG()));
                    Toast.makeText(this,
                            "Error parsing message: ".concat(wm.getMSG()),
                            Toast.LENGTH_LONG).show();
                    continue;
                }

                long nhash = 0;

                try {
                    String[] nhParts = parst[0].split("#");
                    if (nhParts.length != 2) {
                        throw new Exception("invalid HN length!");
                    }

                    nhash = Long.parseLong(nhParts[1]);
                } catch (Exception x) {
                    x.printStackTrace();
                    Log.e("MSGPARSE", "Error parsing message: ".concat(wm.getMSG()));
                    Toast.makeText(this,
                            "Error parsing message: ".concat(wm.getMSG()),
                            Toast.LENGTH_LONG).show();
                    continue;
                }


                Cursor c = dh.getReadableDatabase().rawQuery(
                        "SELECT id,grid,message,call FROM contacts WHERE message IN" +
                                "(SELECT id FROM messages WHERE session = CAST(? AS INTEGER))" +
                                "AND nhash = CAST(? AS INTEGER) ;"
                        , new String[]{String.valueOf(this.sessionID), String.valueOf(nhash)});

                List<Long> toUpdate = new ArrayList<Long>();

                if (c.moveToFirst())
                    do {
                        sender.append(c.getLong(c.getColumnIndex("id")),
                                "<".concat(c.getString(c.getColumnIndex("call")))
                                        .concat(">"), parst[1], parst[2],
                                new Date(), wm.getFREQ(), wm.getSNR(), wm.getDT(), wm.getDRIFT());

                        if (c.getString(c.getColumnIndex("grid")).length() < parst[1].length()) {
                            toUpdate.add(c.getLong(c.getColumnIndex("id")));
                        }
                    } while (c.moveToNext());
                c.close();

                Long[] toupd = toUpdate.toArray(new Long[]{});
                SQLiteStatement ps = dh.getWritableDatabase()
                        .compileStatement("UPDATE contacts SET grid=? WHERE id=CAST(? AS INTEGER) ;");

                Log.e("MSGPARSE", String.format("Got msg2u: %d", toupd.length));

                for (long id : toupd) {
                    ps.bindString(1, parst[1]);
                    ps.bindLong(2, id);
                    int afc = ps.executeUpdateDelete();
                    Log.e("MSGPARSE", String.format("UPDATE TOUCHED %d rows.", afc));
                }

                ps.close();

            } else {
                Log.e("MSGPARSE", "Error parsing message: ".concat(wm.getMSG()));
                Toast.makeText(this,
                        "Error parsing message: ".concat(wm.getMSG()),
                        Toast.LENGTH_LONG).show();
            }


            if (this.sp.getBoolean("use_network", false)) {
                String rxgrid = this.sp.getString("gridsq", "LO05io");
                if (!this.sp.getBoolean("use_6letter", true)) {
                    rxgrid = rxgrid.substring(0, 4);
                }

                String callsign = this.sp.getString("callsign", "RT3ST");
                String band = this.sp.getString("band", Double.toString(10.1387));

                sender.send(rxgrid, callsign, band);
            }

            Intent itt = new Intent("eme.eva.loudbang.message");
            itt.putExtra("eme.eva.loudbang.msgid", nmid);


            LocalBroadcastManager.getInstance(this).sendBroadcast(itt);
        }

        this.setStatus(String.format(getString(R.string.sv_decoding_ended), wsm.length));

        if (this.quitter) {
            stopSelf();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        this.settings_version++;
    }

    private void saveLocation(double lat, double lon) {
        try {
            String newLoca = CJarInterface.WSPRLatLonToGSQ(lat, lon);
            if (!sp.getString("gridsq", "LO05io").equals(newLoca)) {
                sp.edit().putString("gridsq", newLoca).apply();
            }
        } catch (Exception x) {
            Log.e("ERROR", "Got ConversionException: ", x);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            if (sp.getBoolean("use_gps", false)) {
                this.saveLocation(location.getLatitude(), location.getLongitude());
            }
        }

        if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
            if (sp.getBoolean("use_celltowers", false)) {
                this.saveLocation(location.getLatitude(), location.getLongitude());
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
}
