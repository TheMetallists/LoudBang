package aq.metallists.loudbang.cutil;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public DBHelper(Context context) {
        super(context, "contactdb.db3", null, 3);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE messages\n" +
                "(id INTEGER PRIMARY KEY NOT NULL, \n" +
                "session INTEGER, \n" +
                "date DATETIME DEFAULT CURRENT_TIMESTAMP, \n" +
                "snr REAL, freq REAL, dt REAL, drift REAL, message TEXT);");

        db.execSQL("CREATE TABLE contacts\n" +
                "(id INTEGER PRIMARY KEY NOT NULL,\n" +
                "message INTEGER, call TEXT, nhash INTEGER, grid TEXT, power INTEGER," +
                " mygrid TEXT, uploaded INTEGER DEFAULT 0);");
    }

    public int getLastSessionID() {
        Cursor c = this.getReadableDatabase()
                .query("messages", null, null, null,
                        "session", null, "session DESC", "0,1");
        int sid = 1;
        if (c.moveToNext()) {
            sid = c.getInt(c.getColumnIndex("session"));
        }

        return sid;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE contacts ADD COLUMN uploaded INTEGER DEFAULT 0;");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE messages ADD COLUMN dt REAL;");
            db.execSQL("ALTER TABLE messages ADD COLUMN drift REAL;");
        }
    }
}
