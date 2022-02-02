package com.example.skadi;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Receives its own events using a listener API designed for foreground activities. Updates a data
 * item every second while it is open. Also allows user to take a photo and send that as an asset to
 * the paired wearable.
 */
public class MainActivity extends AppCompatActivity implements
        MessageClient.OnMessageReceivedListener, View.OnClickListener {

    String dataPath = "/message_path";
    String TAG = "Mobile MainActivity";

    Button btnToggleMqttConnection;

    //Buttons to toggle Sensor activation
    Button btnToggleHeartRateSensor;
    Button btnToggleGyroscopeSensor;
    Button btnToggleAcceleratorSensor;
    Button btnToggleLightSensor;
    Button btnResetSensors;

    boolean heartRateSensorActivated = false;
    boolean gyroscopeSensorActivated = false;
    boolean acceleratorSensorActivated = false;
    boolean lightSensorActivated = false;

    TextView heartRateValueView ;
    TextView gyroscopeValueView;
    TextView acceleratorValueView;
    TextView lightValueView;

    TextView honoConnectionStatus;

    boolean honoConnectionActive = false;

    /**
     * Hono related variables
     */
    //Instance of the helper class for the connection and message sending functions
    PahoMqttClient pahoMqttClient = new PahoMqttClient();
    MqttAndroidClient client;

    /**
     * IPs and IDs generated via setup.sh file from Smadis honoScripts
     * Should be moved as environment variables for security reasons
     */
    String MQTT_ADAPTER_IP_URI = "tcp://20.103.90.35:1883";
    String TENANT_ID = "13848e8d-cb99-469f-a686-747c6dace783";
    String CLIENT_DEVICE_ID = "a33a0a20-6c10-4b80-a359-b88b5c70971e";
    String USERNAME = CLIENT_DEVICE_ID +"@"+ TENANT_ID;
    String PASSWORD = "j2lZb0qWYWFZAAe";

    String topic =  "event"+"/"+TENANT_ID+"/"+CLIENT_DEVICE_ID+"/";

    int qos = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set text views
        heartRateValueView = findViewById(R.id.heartRateSensorValueView);
        gyroscopeValueView = findViewById(R.id.gyroscopeSensorValueView);
        acceleratorValueView = findViewById(R.id.acceleratorSensorValueView);
        lightValueView = findViewById(R.id.lightSensorValueView);
        honoConnectionStatus = findViewById(R.id.honoConnectionStatus); // Shows whether connected to Hono

        // Set buttons
        btnToggleHeartRateSensor = findViewById(R.id.btnHeartRateSensor);
        btnToggleHeartRateSensor.setOnClickListener(this);
        btnToggleGyroscopeSensor = findViewById(R.id.btnGyroscopeSensor);
        btnToggleGyroscopeSensor.setOnClickListener(this);
        btnToggleAcceleratorSensor = findViewById(R.id.btnAcceleratorSensor);
        btnToggleAcceleratorSensor.setOnClickListener(this);
        btnToggleLightSensor = findViewById(R.id.btnLightSensor);
        btnToggleLightSensor.setOnClickListener(this);

        btnResetSensors = findViewById(R.id.resetSensors);
        btnResetSensors.setOnClickListener(this);

        btnToggleMqttConnection = findViewById(R.id.toggle_hono_connection);
        btnToggleMqttConnection.setOnClickListener(v -> {
            try {
                toggleHonoConnection();
            }
            catch (MqttException | UnsupportedEncodingException e){
                Log.d(TAG, "Connect via Button Click "+e.toString());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Wearable.getMessageClient(this).removeListener(this);
    }

    /**
     * Receives Messages from Polar watch, sends to Hono and displays sensor value on screen
     */
    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived() A message from watch was received:"
                + messageEvent.getRequestId() + " " + messageEvent.getPath());
        String message = new String(messageEvent.getData());
        Log.d(TAG, "Main activity received message: " + message);
        Log.d(TAG, "Received data: " + message);

        try {
            // New string without prefix
            String newValue = message.substring(1);

            //Check the message prefix which sensor sends the current data
            //data comes from heart rate sensor
            if(message.charAt(0) == 'h') {
                pahoMqttClient.publishMessage(client, newValue, qos, topic+"heartRate");
                heartRateValueView.setText(newValue);
            }
            //data comes from gyroscope
            if(message.charAt(0) == 'g') {
                pahoMqttClient.publishMessage(client, newValue, qos, topic+"gyroscope");
                gyroscopeValueView.setText(newValue);
            }
            //data comes from accelerator
            if(message.charAt(0) == 'a') {
                pahoMqttClient.publishMessage(client, newValue, qos, topic+"accelerator");
                acceleratorValueView.setText(newValue);
            }
            //data comes from light sensor
            if(message.charAt(0) == 'l') {
                pahoMqttClient.publishMessage(client, newValue, qos, topic+"light");
                lightValueView.setText(newValue);
            }

            Log.d("MQTT", "Message published: "+newValue);
        } catch (MqttException | UnsupportedEncodingException e) {
            Log.d("MQTT", "Message error sending message");
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        // toggle HeartRateSensor
        if(v == btnToggleHeartRateSensor) {
            //revert boolean value
            heartRateSensorActivated = !heartRateSensorActivated;
            if(heartRateSensorActivated) {
                //Initial value after activation is 0 until value gets updated by received value
                heartRateValueView.setText(R.string.plain_zero);
                btnToggleHeartRateSensor.setText(R.string.stop);

            } else {
                heartRateValueView.setText(R.string.sensorOff);
                btnToggleHeartRateSensor.setText(R.string.start);
            }
            // Send message to toggle Sensor on watch
            new SendThread(dataPath, "heartRate").start();
        }
        // toggle Gyroscope Sensor
        else if(v == btnToggleGyroscopeSensor) {
            //revert boolean value
            gyroscopeSensorActivated = !gyroscopeSensorActivated;
            if(gyroscopeSensorActivated) {
                //Initial value after activation is 0 until value gets updated by received value
                gyroscopeValueView.setText(R.string.plain_zero);
                btnToggleGyroscopeSensor.setText(R.string.stop);
            } else {
                gyroscopeValueView.setText(R.string.sensorOff);
                btnToggleGyroscopeSensor.setText(R.string.start);
            }

            new SendThread(dataPath, "gyroscope").start();
        }
        // toggle Accelerator Sensor
        else if(v == btnToggleAcceleratorSensor) {
            //revert boolean value
            acceleratorSensorActivated = !acceleratorSensorActivated;
            if(acceleratorSensorActivated) {
                //Initial value after activation is 0 until value gets updated by received value
                acceleratorValueView.setText(R.string.plain_zero);
                btnToggleAcceleratorSensor.setText(R.string.stop);
            } else {
                acceleratorValueView.setText(R.string.sensorOff);
                btnToggleAcceleratorSensor.setText(R.string.start);
            }
            // Send message to toggle Sensor on watch
            new SendThread(dataPath, "accelerator").start();
        }
        // toggle Light Sensor
        else if(v == btnToggleLightSensor) {
            //revert boolean value
            lightSensorActivated = !lightSensorActivated;
            if(lightSensorActivated) {
                //Initial value after activation is 0 until value gets updated by received sensor data
                lightValueView.setText(R.string.plain_zero);
                btnToggleLightSensor.setText(R.string.stop);
            } else {
                lightValueView.setText(R.string.sensorOff);
                btnToggleLightSensor.setText(R.string.start);
            }
            // Send message to toggle Sensor on watch
            new SendThread(dataPath, "light").start();
        }
        // Reset sensors
        else if(v == btnResetSensors) {
            // Send message to reset all sensors
            new SendThread(dataPath, "reset").start();

            resetButtonsAndTextFields();
        }
    }

    private void toggleHonoConnection() throws MqttException, UnsupportedEncodingException {
        if(!this.honoConnectionActive) {
            connectMQTTClient();
        }
        else {
            disconnectMQTTClient(client);
        }
    }

    private void resetButtonsAndTextFields() {
        Log.d(TAG, "RESET TO DEFAULT");

        btnToggleHeartRateSensor.setText(R.string.start);
        btnToggleGyroscopeSensor.setText(R.string.start);
        btnToggleAcceleratorSensor.setText(R.string.start);
        btnToggleLightSensor.setText(R.string.start);

        heartRateValueView.setText(R.string.sensorOff);
        gyroscopeValueView.setText(R.string.sensorOff);
        acceleratorValueView.setText(R.string.sensorOff);
        lightValueView.setText(R.string.sensorOff);

        this.heartRateSensorActivated = false;
        this.gyroscopeSensorActivated = false;
        this.acceleratorSensorActivated = false;
        this.lightSensorActivated = false;
    }

    /**
     * Trying connect to Hono, make sure that the smadis receiver script is running!
     *
     * Connection is handled here instead to make sure that all the buttons and texts only update on a successful
     * connection try
     */
    private void connectMQTTClient() {

        try {
            this.client = new MqttAndroidClient(getApplicationContext(), MQTT_ADAPTER_IP_URI, CLIENT_DEVICE_ID);

            IMqttToken token = this.client.connect(pahoMqttClient.getMqttConnectionOption(USERNAME, PASSWORD));
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    client.setBufferOpts(pahoMqttClient.getDisconnectedBufferOptions());
                    Log.d(TAG, "Successfully connected");

                    honoConnectionActive = true;
                    honoConnectionStatus.setText(R.string.connected_to_hono);
                    btnToggleMqttConnection.setText(R.string.disconnect_from_hono);

                    btnToggleHeartRateSensor.setEnabled(true);
                    btnToggleGyroscopeSensor.setEnabled(true);
                    btnToggleAcceleratorSensor.setEnabled(true);
                    btnToggleLightSensor.setEnabled(true);
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Failure " + exception.toString());
                }
            });
        } catch (MqttException e) {
                e.printStackTrace();
        }
    }

    /**
     * Trying disconnect from MQTT Server
     */
    private void disconnectMQTTClient(@NonNull MqttAndroidClient client) {
        try {

            IMqttToken mqttToken = client.disconnect();
            mqttToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    Log.d(TAG, "Successfully disconnected");

                    //Deactivate sensors when disconnected.
                    new SendThread(dataPath, "reset").start();

                    honoConnectionActive = false;
                    honoConnectionStatus.setText(R.string.disconnected_from_hono);
                    btnToggleMqttConnection.setText(R.string.connect_to_hono);

                    btnToggleHeartRateSensor.setEnabled(false);
                    btnToggleGyroscopeSensor.setEnabled(false);
                    btnToggleAcceleratorSensor.setEnabled(false);
                    btnToggleLightSensor.setEnabled(false);

                    resetButtonsAndTextFields();
                }
                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    Log.d(TAG, "Failed to disconnected " + throwable.toString());
                }
            });

        } catch (MqttException me) {
            Log.d(TAG, "Failure to disconnect: " + me.toString());
        }
    }

    // Thread to send the message to the wearable device. Thread is destroyed afterwards.
    class SendThread extends Thread {
        String path;
        String message;

        //constructor
        SendThread(String p, String msg) {
            path = p;
            message = msg;
        }

        // sends the message via the thread. this will send to all wearables connected.
        // Currently it's assumed the Polar watch is the only connected device.
        public void run() {

            //first get all the nodes, ie connected wearable devices.
            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                List<Node> nodes = Tasks.await(nodeListTask);

                //Now send the message to each device.
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());

                    try {
                        // Block on a task and get the result synchronously (because this is on a background
                        // thread).
                        Integer result = Tasks.await(sendMessageTask);
                        Log.v(TAG, "SendThread: message sent ("+result+") to " + node.getDisplayName());

                    } catch (ExecutionException exception) {
                        //sendMessage("SendThread: message failed to" + node.getDisplayName());
                        Log.e(TAG, "Send Task failed: " + exception);

                    } catch (InterruptedException exception) {
                        Log.e(TAG, "Send Interrupt occurred: " + exception);
                    }

                }

            } catch (ExecutionException exception) {
                //sendMessage("Node Task failed: " + exception);
                Log.e(TAG, "Node Task failed: " + exception);

            } catch (InterruptedException exception) {
                Log.e(TAG, "Node Interrupt occurred: " + exception);
            }

        }
    }
}
