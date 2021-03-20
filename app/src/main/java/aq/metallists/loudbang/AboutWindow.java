package aq.metallists.loudbang;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AboutWindow extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_window);
        Toolbar toolbar = findViewById(R.id.about_appbar);
        setSupportActionBar(toolbar);
    }
}
