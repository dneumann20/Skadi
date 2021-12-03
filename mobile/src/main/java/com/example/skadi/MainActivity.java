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
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

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

    boolean heartRateSensorActive = false;
    boolean gyroscopeSensorActive = false;
    boolean acceleratorSensorActive = false;
    boolean lightSensorActive = false;

    TextView heartRateValueView ;
    TextView gyroscopeValueView;
    TextView acceleratorValueView;
    TextView lightValueView;

    TextView honoConnectionStatus;

    // TODO remove after testing
    Button btnSendTestMessage;

    boolean honoConnectionActive = false;

    private static final String msgStart = "START_RECORDING";
    private static final String msgStop = "STOP_RECORDING";

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

    MemoryPersistence persistence = new MemoryPersistence();
    int qos = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set text views showing sensor values, default value: "Sensor off"
        heartRateValueView = findViewById(R.id.heartRateSensorValueView);
        gyroscopeValueView = findViewById(R.id.gyroscopeSensorValueView);
        acceleratorValueView = findViewById(R.id.acceleratorSensorValueView);
        lightValueView = findViewById(R.id.lightSensorValueView);

        honoConnectionStatus = findViewById(R.id.honoConnectionStatus); // Shows whether connected to Hono

        // Set buttons
        /*btnToggleSensor = findViewById(R.id.start_data_collection);
        btnToggleSensor.setOnClickListener(this::toggleDataCollection);*/

        btnToggleHeartRateSensor = findViewById(R.id.btnHeartRateSensor);
        btnToggleHeartRateSensor.setOnClickListener(this);
        btnToggleGyroscopeSensor = findViewById(R.id.btnGyroscopeSensor);
        btnToggleGyroscopeSensor.setOnClickListener(this);
        btnToggleAcceleratorSensor = findViewById(R.id.btnAcceleratorSensor);
        btnToggleAcceleratorSensor.setOnClickListener(this);
        btnToggleLightSensor = findViewById(R.id.btnLightSensor);
        btnToggleLightSensor.setOnClickListener(this);

        btnToggleMqttConnection = findViewById(R.id.toggle_hono_connection);
        btnToggleMqttConnection.setOnClickListener(v -> {
            try {
                toggleHonoConnection(v);
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
     * Receives Messages from Polar watch and displays sensor value on screen
     * TODO WIP: Sends the data to Hono
     */
    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived() A message from watch was received:"
                + messageEvent.getRequestId() + " " + messageEvent.getPath());
        String message = new String(messageEvent.getData());
        Log.d(TAG, "Main activity received message: " + message);
        Log.d(TAG, "Received data: " + message);
        //Check the message prefix which sensor sends the current data

        String newValue = message.substring(1);

        //data comes from heart rate sensor
        if(message.charAt(0) == 'h') {
            message = message.substring(1);
            // Display value without prefix
            heartRateValueView.setText(newValue);
        }
        //data comes from gyroscope
        if(message.charAt(0) == 'g') {
            gyroscopeValueView.setText(newValue);
        }
        //data comes from accelerator
        if(message.charAt(0) == 'a') {
            acceleratorValueView.setText(newValue);
        }
        //data comes from light sensor
        if(message.charAt(0) == 'l') {
            lightValueView.setText(newValue);
        }

        //TODO forward via MQTT Client
        /*try {
            pahoMqttClient.publishMessage(client, newValue, qos, topic+"heartRate");

            Log.i("MQTT", "Message published");
        } catch (MqttException | MqttPersistenceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/

    }

    @Override
    public void onClick(View v) {
        // toggle HeartRateSensor
        if(v == btnToggleHeartRateSensor) {
            //revert boolean value
            heartRateSensorActive = !heartRateSensorActive;
            if(heartRateSensorActive) {
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
            gyroscopeSensorActive = !gyroscopeSensorActive;
            if(gyroscopeSensorActive) {
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
            acceleratorSensorActive = !acceleratorSensorActive;
            if(acceleratorSensorActive) {
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
        // toggle LightSensor
        else if(v == btnToggleLightSensor) {
            //revert boolean value
            lightSensorActive = !lightSensorActive;
            if(lightSensorActive) {
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
    }

    private void toggleHonoConnection(View v) throws MqttException, UnsupportedEncodingException {
        if(!this.honoConnectionActive) {
            connectMQTTClient();
        }
        else {
            disconnectMQTTClient(client);
        }
    }

    /**
     * Trying connect to Hono, make sure that the receiver script is running!
     */
    private void connectMQTTClient() {

        try {
            this.client = pahoMqttClient.getMqttClient(getApplicationContext(),
                    MQTT_ADAPTER_IP_URI, CLIENT_DEVICE_ID, USERNAME, PASSWORD);

            honoConnectionActive = !honoConnectionActive;
            honoConnectionStatus.setText(R.string.connected_to_hono);
            btnToggleMqttConnection.setText(R.string.disconnect_from_hono);
            btnSendTestMessage.setEnabled(true);
        } catch (MqttException e) {
            Log.d(TAG, "Failure to connect."+e.toString());
        }
    }

    // TODO remove after test
    /*public void sendTestMessage(View v) {
        if(client.isConnected()){
            Log.d(TAG, "Client still connected");
                try {
                String message = "connectSuccess";
                pahoMqttClient.publishMessage(this.client, message, qos, topic+"sendTestMessage");

                testMessageToHono.setText(R.string.hono_message_test_success);
            } catch (MqttException | UnsupportedEncodingException e){
                Log.d(TAG, "Failure to send: "+e.toString());
                e.printStackTrace();
            }
        }
    }*/

    /**
     * Trying disconnect from MQTT Server
     */
    private void disconnectMQTTClient(@NonNull MqttAndroidClient client) {
        try {
            pahoMqttClient.disconnect(client);
            honoConnectionActive = !honoConnectionActive;
            honoConnectionStatus.setText(R.string.disconnected_from_hono);
            btnToggleMqttConnection.setText(R.string.connect_to_hono);
            btnSendTestMessage.setEnabled(false);
            Log.d(TAG, "Successfully disconnected");
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