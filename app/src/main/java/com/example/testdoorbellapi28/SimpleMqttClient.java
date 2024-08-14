package com.example.testdoorbellapi28;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.hivemq.client.mqtt.mqtt3.message.subscribe.suback.Mqtt3SubAck;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SimpleMqttClient {

    private static final String SERVER_HOST = "broker.hivemq.com";
    private static final int SERVER_PORT = 1883;
    private static final String TOPIC = "Ultrasound_ESP32_MCI_Jackermaier_Out"; // Adjusted topic to match Arduino

    private Mqtt3AsyncClient client;
    private String identifier;

    public SimpleMqttClient(String clientIdentifier) {
        this.identifier = clientIdentifier;
        this.client = Mqtt3Client.builder()
                .serverHost(SERVER_HOST)
                .serverPort(SERVER_PORT)
                .identifier(clientIdentifier)
                .buildAsync();
    }

    // Connect method (asynchronous)
    public void connect(MqttConnection conn) {
        this.client.connect().whenComplete(conn);
    }

    // Disconnect method (blocking)
    public void disconnect() {
        client.toBlocking().disconnect();
    }

    // Subscribe method (asynchronous)
    public void subscribe(MqttSubscription sub) {
        client.subscribeWith()
                .topicFilter(TOPIC) // Use constant TOPIC for subscription
                .callback(sub)
                .send()
                .whenComplete(sub);
    }

    // Unsubscribe method (blocking)
    public void unsubscribe(String topic) {
        client.toBlocking().unsubscribeWith().topicFilter(topic).send();
    }

    // Publish method (asynchronous)
    public void publish(MqttPublish pub) {
        client.publishWith()
                .topic(pub.getTopic())
                .qos(MqttQos.AT_LEAST_ONCE)
                .payload(pub.getPayload().getBytes())
                .send()
                .whenComplete(pub);
    }

    //region Inner Classes for MQTT Operations
    private static abstract class MqttOperationResult<T> implements BiConsumer<T, Throwable> {
        protected Context context;
        protected Activity activity;

        public MqttOperationResult(Context context) {
            this.context = context;
        }

        @Override
        public void accept(T ack, Throwable throwable) {
            if (throwable == null) {
                logSuccess();
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(this::onSuccess);
                }
            } else {
                logError(throwable);
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(() -> onError(throwable));
                }
            }
        }

        protected abstract void logSuccess();
        protected abstract void logError(Throwable error);
        public void onSuccess() { /* Do nothing by default */ }
        public void onError(Throwable error) { /* Do nothing by default */ }
    }

    public static abstract class MqttConnection extends MqttOperationResult<Mqtt3ConnAck> {
        public MqttConnection(Context context) {
            super(context);
        }

        @Override
        protected void logSuccess() {
            Log.d("MQTT", "Connection established");
        }

        @Override
        protected void logError(Throwable error) {
            Log.e("MQTT", "Unable to connect", error);
        }
    }

    public static abstract class MqttSubscription extends MqttOperationResult<Mqtt3SubAck> implements Consumer<Mqtt3Publish> {
        private final String topic;

        public MqttSubscription(Context context, String topic) {
            super(context);
            this.topic = topic;
        }

        public String getTopic() {
            return topic;
        }

        @Override
        protected void logSuccess() {
            Log.d("MQTT", String.format("Subscribed to %s", topic));
        }

        @Override
        protected void logError(Throwable error) {
            Log.e("MQTT", String.format("Unable to subscribe to %s", topic), error);
        }

        @Override
        public void accept(Mqtt3Publish mqtt3Publish) {
            String topic = mqtt3Publish.getTopic().toString();
            String payload = new String(mqtt3Publish.getPayloadAsBytes());
            Log.d("MQTT", String.format("Received message from topic %s: %s", topic, payload));
            onMessage(topic, payload);
        }

        public abstract void onMessage(String topic, String payload);

    }

    public static abstract class MqttPublish extends MqttOperationResult<Mqtt3Publish> {
        private final String topic;
        private final String payload;

        public MqttPublish(Activity activity, String topic, String payload) {
            super(activity);
            this.topic = topic;
            this.payload = payload;
        }

        public String getTopic() {
            return topic;
        }

        public String getPayload() {
            return payload;
        }

        @Override
        protected void logSuccess() {
            Log.d("MQTT", String.format("Published to %s: %s", topic, payload));
        }

        @Override
        protected void logError(Throwable error) {
            Log.e("MQTT", String.format("Unable to publish to %s", topic), error);
        }
    }
    //endregion
}

