package com.example.testdoorbellapi28;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.widget.Toast;
import android.util.Log;
import android.app.Activity;
import android.media.ToneGenerator;

public class UltrasoundManager {
    private SimpleMqttClient mqttClient;
    private Handler handler;
    private Context context;
    private String ultrasoundTopic = "Ultrasound_ESP32_MCI_JJ"; // MQTT topic
    private UltrasoundDataListener dataListener;
    private ToneGenerator toneGenerator;
    private static final double MAX_EXPECTED_DISTANCE = 180;
    private final long[] beepRates = new long[] {
            100, // Highest frequency for closest distance
            200,
            300,
            400,
            500,
            600, // Lowest frequency for farthest distance
    };

    public interface UltrasoundDataListener {
        void onUltrasoundDataReceived(double distance);
    }

    public UltrasoundManager(Context context, UltrasoundDataListener dataListener) {
        this.context = context;
        this.handler = new Handler();
        this.dataListener = dataListener;
        this.toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100); // Max volume
        initializeMqttClient();
    }

    private void initializeMqttClient() {
        mqttClient = new SimpleMqttClient("Ultrasound_ESP32_MCI_JJ");
        connectToMqttBroker();
    }

    private void connectToMqttBroker() {
        mqttClient.connect(new SimpleMqttClient.MqttConnection(context) {
            @Override
            public void onSuccess() {
                Toast.makeText(context, "MQTT Connection Successful", Toast.LENGTH_SHORT).show();
                subscribeToUltrasoundTopic();
            }

            @Override
            public void onError(Throwable error) {
                Toast.makeText(context, "MQTT Connection Failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void subscribeToUltrasoundTopic() {
        mqttClient.subscribe(new SimpleMqttClient.MqttSubscription(context, ultrasoundTopic) {
            @Override
            public void onMessage(String topic, String payload) {
                Log.d("MQTT", "Message arrived on topic: " + topic);
                double[] distanceContainer = new double[1];

                try {
                    distanceContainer[0] = Double.parseDouble(payload.trim());
                    Log.d("MQTT", "Distance: " + distanceContainer[0]);

                    if (distanceContainer[0] >= 0 && distanceContainer[0] <= MAX_EXPECTED_DISTANCE) {
                        Log.d("MQTT", "Valid distance: " + distanceContainer[0]);
                        if (dataListener != null) {
                            ((Activity) context).runOnUiThread(() -> dataListener.onUltrasoundDataReceived(distanceContainer[0]));
                        }
                    } else {
                        Log.w("MQTT", "Distance outside expected range: " + distanceContainer[0]);
                    }
                } catch (NumberFormatException e) {
                    Log.e("MQTT", "Failed to parse payload as double: " + payload, e);
                }
                adjustBeepFrequency(distanceContainer[0]);
            }
        });
    }

    private void adjustBeepFrequency(double distance) {
        int index = (int) ((distance * beepRates.length) / MAX_EXPECTED_DISTANCE);
        index = Math.min(Math.max(index, 0), beepRates.length - 1);
        long rate = beepRates[index];

        handler.removeCallbacksAndMessages(null);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (toneGenerator != null) {
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100); // Beep for 100 ms
                }
                handler.postDelayed(this, rate);
            }
        }, rate);
    }

    public void startBeeping() {
        handler.post(beepRunnable);
    }

    private Runnable beepRunnable = new Runnable() {
        @Override
        public void run() {
            if (toneGenerator != null) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100); // Beep for 100 ms
            }
            handler.postDelayed(this, 500); // Default beep interval, adjust as necessary
        }
    };

    public void stopBeeping() {
        handler.removeCallbacksAndMessages(null);
        if (toneGenerator != null) {
            toneGenerator.stopTone(); // Stop any ongoing tone immediately
        }
    }

    public void releaseResources() {
        stopBeeping();
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }
}

