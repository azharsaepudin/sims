package com.example.mqtt2024java;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.material.snackbar.Snackbar;
import info.mqtt.android.service.MqttAndroidClient;
import info.mqtt.android.service.QoS;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {
    private MqttAndroidClient mqttAndroidClient;

    private final String CHANNEL_ID = "channelID";
    private final String CHANNEL_NAME = "channelName";
    private final int NOTIF_ID = 0;

    Button btnPublish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnPublish = findViewById(R.id.btnPublish);

        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publishMessage();
            }
        });

        createNotifChannel();
        clientId += System.currentTimeMillis();
        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    subscribeToTopic();
                } else {
                    Log.d("MQTT", "Connected");
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d("MQTT", "connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                Log.d("MQTT", "Message arive 2 " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                disconnectedBufferOptions.setBufferEnabled(true);
                disconnectedBufferOptions.setBufferSize(100);
                disconnectedBufferOptions.setPersistBuffer(false);
                disconnectedBufferOptions.setDeleteOldestMessages(false);
                mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                subscribeToTopic();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.d("MQTT", "Failed");
            }
        });
    }

    private void subscribeToTopic() {
        mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Log.d("MQTT", "Subscribed");
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Log.d("MQTT", "on failure");
            }
        });
        // THIS DOES NOT WORK!
        mqttAndroidClient.subscribe(subscriptionTopic, 0, (topic, message) -> {
            Log.d("MQTT", "Message arive " + message);

            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            PendingIntent pendingIntent = TaskStackBuilder.create(MainActivity.this).addNextIntentWithParentStack(intent).getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, CHANNEL_ID)
                    .setContentTitle("Sample Title")
                    .setContentText(message.toString())
                    .setSmallIcon(R.drawable.alertaa)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            }
            notificationManager.notify(NOTIF_ID, builder.build());
        });
    }

    private void publishMessage() {
        MqttMessage message = new MqttMessage();
        message.setPayload(publishMessage.getBytes());
        if (mqttAndroidClient.isConnected()) {
            try {
                mqttAndroidClient.publish(publishTopic, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!mqttAndroidClient.isConnected()) {
            }
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Not connected", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
        }
    }

    private void createNotifChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLightColor(Color.BLUE);
            channel.enableLights(true);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    private static final String serverUri = "tcp://broker.hivemq.com:1883";
    private static final String subscriptionTopic = "TEST/1";
    private static final String publishTopic = "TEST/1";
    private static final String publishMessage = "Hello World";
    private static String clientId = "BasicSample";

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}


