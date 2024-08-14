package com.example.testdoorbellapi28;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class FindActivity extends AppCompatActivity implements UltrasoundManager.UltrasoundDataListener {

    private UltrasoundManager ultrasoundManager;
    private TextView distanceTextView;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find);

        distanceTextView = findViewById(R.id.distanceTextView);

        // Initialize the UltrasoundManager
        ultrasoundManager = new UltrasoundManager(this, this);

        // Start the ultrasound beeping
        ultrasoundManager.startBeeping();
    }

    @Override
    public void onUltrasoundDataReceived(double distance) {
        runOnUiThread(() -> {
            if (distanceTextView != null) {
                String distanceAsString = String.format("Distance: %.2f cm", distance);
                distanceTextView.setText(distanceAsString);
                if (distance <= 10.0) {
                    ultrasoundManager.stopBeeping();
                    returnToMainActivity();
                }
            }
        });
    }

    private void returnToMainActivity() {
        Intent intent = new Intent(FindActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void vibratePhone() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {1, 100};
            vibrator.vibrate(pattern, -1);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ultrasoundManager.stopBeeping();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ultrasoundManager.stopBeeping();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ultrasoundManager.stopBeeping();
        ultrasoundManager.releaseResources();
    }
}
