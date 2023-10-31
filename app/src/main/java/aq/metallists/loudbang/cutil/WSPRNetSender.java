package aq.metallists.loudbang.cutil;

import android.database.sqlite.SQLiteStatement;
import android.net.Uri;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class WSPRNetSender {
    private final List<WNetMessage> msgs;
    private final DBHelper dh;

    public WSPRNetSender(DBHelper _dh) {
        this.msgs = new ArrayList<>();
        this.dh = _dh;
    }

    public void append(long contactID, String call, String grid, String power, Date date,
                       double frequency, float snr, float dt, float drift) {
        this.msgs.add(new WNetMessage(contactID, call, grid, power, date, frequency, snr, dt, drift));
    }

    public void send(String rxgrid, String callsign, String band) {
        SQLiteStatement ps = dh.getWritableDatabase().compileStatement(
                "UPDATE contacts SET uploaded=1 WHERE id=?;");
        for (WNetMessage msg : this.msgs) {
            try {
                msg.send(rxgrid, callsign, band);
                ps.bindLong(1, msg.getDbID());
                ps.executeUpdateDelete();
            } catch (Exception x) {
                x.printStackTrace();
            }
        }
        ps.close();
        this.msgs.clear();
    }

    static class WNetMessage {
        String call;
        String grid;
        String power;
        Date date;
        double frequency;
        float snr;
        float dt;
        float drift;
        long dbID;

        public WNetMessage(long dbID, String call, String grid, String power, Date date,
                           double frequency, float snr, float dt, float drift) {
            this.call = call;
            this.grid = grid;
            this.power = power;
            this.date = date;
            this.frequency = frequency;
            this.snr = snr;
            this.dt = dt;
            this.drift = drift;
            this.dbID = dbID;
        }

        public void send(String rxgrid, String callsign, String band) throws Exception {
            DateFormat df = new SimpleDateFormat("yyMMdd");
            DateFormat df2 = new SimpleDateFormat("HHmm");
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            df2.setTimeZone(TimeZone.getTimeZone("UTC"));

            Uri.Builder ub = new Uri.Builder();
            URL url = new URL(ub.scheme("https")
                    .authority("wsprnet.org")
                    .appendPath("post")
                    .appendQueryParameter("function", "wspr")
                    .appendQueryParameter("rcall", callsign)
                    .appendQueryParameter("rgrid", rxgrid)
                    .appendQueryParameter("rqrg", band)
                    .appendQueryParameter("date", df.format(date))
                    .appendQueryParameter("time", df2.format(date))
                    .appendQueryParameter("sig", Float.toString(Math.round(snr)))

                    .appendQueryParameter("dt", Float.toString(dt))
                    .appendQueryParameter("drift", Float.toString(drift))

                    .appendQueryParameter("tqrg", String.format(Locale.ROOT, "%.05f", frequency)) //?
                    .appendQueryParameter("tcall", call)
                    .appendQueryParameter("tgrid", grid)
                    .appendQueryParameter("dbm", power)
                    .appendQueryParameter("version", "libQSc0.49")
                    .appendQueryParameter("mode", "2")
                    .build().toString());

            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            huc.setRequestMethod("GET");
            huc.setRequestProperty("User-Agent", "libQuietScream 0.0.49");
            huc.connect();

            InputStream is = huc.getInputStream();

            while (is.available() > 0) {
                is.read();
            }
            is.close();
        }

        public long getDbID() {
            return this.dbID;
        }
    }
}
