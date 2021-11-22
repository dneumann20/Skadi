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
        MessageClient.OnMessageReceivedListener {

    String dataPath = "/message_path";
    String TAG = "Mobile MainActivity";

    Button btnToggleSensor;
    Button btnToggleMqttConnection;

    TextView currentHeartRate;
    TextView heartRateValue;
    TextView recordingStatus;
    TextView honoConnectionStatus;

    // TODO remove after testing
    TextView testMessageToHono;

    boolean dataCollectionActive = false;
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

        // Set text views
        currentHeartRate = findViewById(R.id.currentHeartRate); // Plain textView with static text
        heartRateValue = findViewById(R.id.heartRateValue); // textView that shows the actual heart rate
        recordingStatus = findViewById(R.id.recordingStatus); // Shows whether recording is on or off
        honoConnectionStatus = findViewById(R.id.honoConnectionStatus); // Shows whether connected to Hono

        //TODO Remove
        testMessageToHono = findViewById(R.id.testMessageToHono);

        // Set buttons
        btnToggleSensor = findViewById(R.id.start_data_collection);
        btnToggleSensor.setOnClickListener(this::toggleDataCollection);

        btnToggleMqttConnection = findViewById(R.id.toggle_hono_connection);
        btnToggleMqttConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    toggleHonoConnection(v);
                }
                catch (MqttException | UnsupportedEncodingException e){
                    Log.d(TAG, "Connect via Button Click "+e.toString());
                    e.printStackTrace();
                }

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
        // Display message in UI
        heartRateValue.setText(message);

        //TODO forward via MQTT Client
        /*try {
            pahoMqttClient.publishMessage(client, message, qos, topic+"heartRate");

            Log.i("MQTT", "Message published");
        } catch (MqttException | MqttPersistenceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/

    }

    public void toggleDataCollection(View v) {
        this.dataCollectionActive = !this.dataCollectionActive;
        if(this.dataCollectionActive) {

            heartRateValue.setText(R.string.plain_zero); // Initial value is 0
            btnToggleSensor.setText(R.string.stop_data_collection);
            recordingStatus.setText(R.string.data_collection_on);

            // Sends message to start heart rate Sensor
            new SendThread(dataPath, msgStart).start();
        }

        else {
            heartRateValue.setText("");
            btnToggleSensor.setText(R.string.start_data_collection);
            recordingStatus.setText(R.string.data_collection_off);

            // Sends message to stop heart rate Sensor
            new SendThread(dataPath, msgStop).start();
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
            client = pahoMqttClient.getMqttClient(getApplicationContext(),
                    MQTT_ADAPTER_IP_URI, CLIENT_DEVICE_ID, USERNAME, PASSWORD);
            honoConnectionActive = !honoConnectionActive;
            honoConnectionStatus.setText(R.string.disconnected_from_hono);
            btnToggleMqttConnection.setText(R.string.connect_to_hono);
        } catch (MqttException e) {
            Log.d(TAG, "Failure to connect."+e.toString());
        }

        //Send test message
        try {
            String message = "connectSuccess";
            pahoMqttClient.publishMessage(client, message, qos, topic+"sendTestMessage");

            testMessageToHono.setText(R.string.hono_message_test);
        } catch (MqttException | UnsupportedEncodingException e){
            Log.d(TAG, "Failure to send: "+e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Trying disconnect from MQTT Server
     */
    private void disconnectMQTTClient(@NonNull MqttAndroidClient client) {
        try {
            pahoMqttClient.disconnect(client);
            honoConnectionActive = !honoConnectionActive;
            honoConnectionStatus.setText(R.string.disconnected_from_hono);
            btnToggleMqttConnection.setText(R.string.connect_to_hono);
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

        // sends the message via the thread.  this will send to all wearables connected.
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