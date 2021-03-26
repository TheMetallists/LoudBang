package aq.metallists.loudbang.cutil;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import aq.metallists.loudbang.R;

public class DBToXMLConverter {
    private final Context ctx;

    public DBToXMLConverter(Context _ctx) {
        if (_ctx == null) {
            throw new IllegalArgumentException("Context is NULL");
        }
        this.ctx = _ctx;
    }

    public void wipeOut() throws Exception {
        AlertDialog.Builder ab = new AlertDialog.Builder(ctx);
        ab.setTitle(R.string.localdb_wipe_title);
        ab.setMessage(R.string.localdb_wipe_text);

        ab.setPositiveButton(R.string.localdb_wipe_btn_yes, (dialog, which) -> {
            Toast.makeText(DBToXMLConverter.this.ctx,
                    R.string.localdb_wipe_there_is_no_cd, Toast.LENGTH_SHORT)
                    .show();

            DBHelper dh = new DBHelper(DBToXMLConverter.this.ctx);
            dh.getWritableDatabase().execSQL("DELETE FROM messages;");
            //dh.getWritableDatabase().execSQL("delete from sqlite_sequence where name='messages';");
            dh.getWritableDatabase().execSQL("DELETE FROM contacts;");
            //dh.getWritableDatabase().execSQL("delete from sqlite_sequence where name='contacts';");

            Toast.makeText(DBToXMLConverter.this.ctx,
                    R.string.localdb_wipe_done, Toast.LENGTH_SHORT)
                    .show();
        });

        ab.setNegativeButton(R.string.localdb_wipe_btn_no, (dialog, which) ->
                Toast.makeText(DBToXMLConverter.this.ctx,
                        R.string.localdb_wipe_good_choice, Toast.LENGTH_SHORT).show());

        ab.create().show();
    }

    public void exportToXML() throws Exception {
        // get export file
        File parrentFolder = this.ctx.getExternalFilesDir("xml");
        if (!parrentFolder.exists())
            parrentFolder.mkdirs();

        File xpf = new File(parrentFolder, "exports.xml");
        if (xpf.exists()) {
            Toast.makeText(this.ctx,
                    R.string.error_exportfile_exists,
                    Toast.LENGTH_LONG).show();
            return;
        }

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        Element root = doc.createElement("LoudBangBase");
        doc.appendChild(root);

        Element date = doc.createElement("CreatedAt");
        root.appendChild(date);
        date.setAttribute("unix", String.valueOf(new Date().getTime()));
        date.setTextContent(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date()));

        this.writeSettings(doc, root);

        this.writeContacts(doc, root);
        this.writeRawPackets(doc, root);

        // saving to file.
        this.exportDomFile(xpf, doc);

        Toast.makeText(this.ctx, R.string.localdb_export_successfull, Toast.LENGTH_LONG).show();
    }

    private void exportDomFile(File xpf, Document doc) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        Properties outFormat = new Properties();
        outFormat.setProperty(OutputKeys.INDENT, "yes");
        outFormat.setProperty(OutputKeys.METHOD, "xml");
        outFormat.setProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        outFormat.setProperty(OutputKeys.VERSION, "1.0");
        outFormat.setProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperties(outFormat);
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource domSource = new DOMSource(doc.getDocumentElement());
        OutputStream output = new FileOutputStream(xpf);
        StreamResult result = new StreamResult(output);
        transformer.transform(domSource, result);
        output.flush();
        output.close();
    }

    private void writeRawPackets(Document doc, Element root) {
        Element pax = doc.createElement("RawPackets");
        root.appendChild(pax);

        DBHelper dh = new DBHelper(this.ctx);
        Cursor c = dh.getReadableDatabase().query(
                "messages", new String[]{"*", "id AS _id"}, null,
                null, null, null, null);
        if (c.moveToFirst())
            do {
                Element p = doc.createElement("Packet");
                pax.appendChild(p);

                p.setAttribute("id", String.valueOf(c.getInt(c.getColumnIndex("_id"))));
                p.setAttribute("session", String.valueOf(c.getInt(c.getColumnIndex("session"))));
                p.setAttribute("date", c.getString(c.getColumnIndex("date")));
                p.setAttribute("snr", String.valueOf(c.getDouble(c.getColumnIndex("snr"))));
                p.setAttribute("freq", String.valueOf(c.getDouble(c.getColumnIndex("freq"))));
                p.setAttribute("dt", String.valueOf(c.getDouble(c.getColumnIndex("dt"))));
                p.setAttribute("drift", String.valueOf(c.getDouble(c.getColumnIndex("drift"))));
                p.setAttribute("proto", "2");

                p.setTextContent(c.getString(c.getColumnIndex("message")));

            } while (c.moveToNext());
    }

    private void writeContacts(Document doc, Element root) {
        Element pax = doc.createElement("ProcessedMessages");
        root.appendChild(pax);

        DBHelper dh = new DBHelper(this.ctx);
        Cursor c = dh.getReadableDatabase().query(
                "contacts", new String[]{"*"}, null,
                null, null, null, null);
        if (c.moveToFirst())
            do {
                Element p = doc.createElement("Message");
                pax.appendChild(p);

                p.setAttribute("id", String.valueOf(c.getInt(c.getColumnIndex("id"))));
                p.setAttribute("message", String.valueOf(c.getInt(c.getColumnIndex("message"))));
                p.setAttribute("call", c.getString(c.getColumnIndex("call")));
                p.setAttribute("nhash", String.valueOf(c.getInt(c.getColumnIndex("nhash"))));
                p.setAttribute("grid", c.getString(c.getColumnIndex("grid")));
                p.setAttribute("power", String.valueOf(c.getInt(c.getColumnIndex("power"))));
                p.setAttribute("mygrid", c.getString(c.getColumnIndex("mygrid")));
                p.setAttribute("uploaded", String.valueOf(
                        c.getInt(c.getColumnIndex("uploaded")) > 0
                ));
                //p.setAttribute("proto", "2");

                p.setTextContent(c.getString(c.getColumnIndex("message")));

            } while (c.moveToNext());
    }

    private void writeSettings(Document doc, Element root) {
        Element sets = doc.createElement("Settings");
        root.appendChild(sets);

        SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(this.ctx);
        Map<String, ?> allPref = pm.getAll();
        for (Map.Entry<String, ?> entry : allPref.entrySet()) {

            Element setting = doc.createElement("Setting");
            setting.setAttribute("name", entry.getKey());
            setting.setAttribute("type", entry.getValue().getClass().getName());
            setting.setTextContent(entry.getValue().toString());

            sets.appendChild(setting);
        }
    }

    public void importFromXML() {
        // TODO
    }
}
