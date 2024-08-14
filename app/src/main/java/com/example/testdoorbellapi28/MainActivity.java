package com.example.testdoorbellapi28;

import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the "Find" button by ID and set OnClickListener
        Button findButton = findViewById(R.id.btn_find);
        findButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchFindActivity();
            }
        });

        findButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                vibratePhone();
                launchFindActivity();
                return true;
            }
        });

        // Find the "Read" button by ID and set OnClickListener
        Button readButton = findViewById(R.id.btn_read);
        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchReadActivity();
            }
        });

        readButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                vibratePhone();
                launchReadActivity();
                return true;
            }
        });
    }

    private void launchFindActivity() {
        Intent intent = new Intent(MainActivity.this, FindActivity.class);
        startActivity(intent);
    }

    private void launchReadActivity() {
        Intent intent = new Intent(MainActivity.this, ReadActivity.class);
        startActivity(intent);
    }

    private void vibratePhone() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {1, 100};
            vibrator.vibrate(pattern, -1);
        }
    }
}
