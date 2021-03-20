package aq.metallists.loudbang;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import aq.metallists.loudbang.cutil.CJarInterface;
import aq.metallists.loudbang.cutil.WSPRMessage;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tw = (TextView) findViewById(R.id.helloworld);

        byte[] test = {0x00, 0x10, 0x20};
        WSPRMessage[] msg = CJarInterface.WSPRDecodeFromPcm(test, 0, false);

        String out = "";

        for (WSPRMessage w : msg) {
            out = out.concat(w.out).concat("\n");
        }

        tw.setText(out);
    }
}
