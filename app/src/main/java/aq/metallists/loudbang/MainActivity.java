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

        TextView tw = findViewById(R.id.main_helloworld);

        byte[] test = {0x00, 0x10, 0x20};
        WSPRMessage[] msg = CJarInterface.WSPRDecodeFromPcm(test, 0, false);

        StringBuilder out = new StringBuilder();

        for (WSPRMessage w : msg) {
            out.append(w.out).append("\n");
        }

        tw.setText(out.toString());
    }
}
