package com.example.skadi;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.skadi.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity implements SensorEventListener, MessageClient.OnMessageReceivedListener {

    private final static String TAG = "Wear MainActivity";
    String dataPath = "/message_path";

    private Button btnHeartRateRecording;
    private TextView heartRateView;
    private ActivityMainBinding binding;
    private boolean buttonActivated = false;

    //TODO remove after testing
    private TextView messageTest;
    int num = 1;

    MessageClient messageClient;

    //TODO look up proper use or remove?
    DataClient dataClient;

    private SensorManager sensorManager;
    private Sensor heartRateSensor;
    private Integer heartRateValue = 0;
    private ConnectivityManager connectivityManager;
    private static final String MESSAGE_TRANSFER_CAPABILITY_NAME = "message_transfer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        //Set Button
        btnHeartRateRecording = findViewById(R.id.btnHeartRateRecording);
        btnHeartRateRecording.setOnClickListener(toggleHeartRate);

        //Set TextViews
        heartRateView = findViewById(R.id.heartRateView);
        messageTest = findViewById(R.id.messageTest);

        //Set HeartRate sensor
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        if (heartRateSensor  == null) {
            Log.d(TAG, "Warning: no heartRateSensor");
        } else {
            // Needs testing how much the battery drains on FASTEST mode
            sensorManager.registerListener((SensorEventListener) this, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        messageClient = Wearable.getMessageClient(getApplicationContext());

        // TODO check data client usage
        //dataClient = Wearable.getDataClient(getApplicationContext());
    }

    /*private void setupVoiceTranscription() {
        try {
            CapabilityInfo capabilityInfo = Tasks.await(
                    Wearable.getCapabilityClient(this).getCapability(
                            MESSAGE_TRANSFER_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE));

            CapabilityClient.OnCapabilityChangedListener capabilityListener =
                    capabilityInfo1 -> { updateTransferionCapability(capabilityInfo); };
            Wearable.getCapabilityClient(this).addListener(
                    capabilityListener,
                    MESSAGE_TRANSFER_CAPABILITY_NAME);


        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // capabilityInfo has the reachable nodes with the transcription capability
    }

    private void setupVoiceTranscription() {
        // This example uses a Java 8 Lambda. You can use named or anonymous classes.
        CapabilityClient.OnCapabilityChangedListener capabilityListener =
                capabilityInfo -> { updateTransferCapability(capabilityInfo); };
        Wearable.getCapabilityClient(context).addListener(
                capabilityListener,
                VOICE_TRANSCRIPTION_CAPABILITY_NAME);
    }*/


    public View.OnClickListener toggleHeartRate = v -> {
        this.buttonActivated = !this.buttonActivated;
        if (this.buttonActivated) {
            btnHeartRateRecording.setText(R.string.button_stop);
            activateSensor();
        }
        else {
            btnHeartRateRecording.setText(R.string.button_start);
            deactivateSensor();
        }
    };

    private void activateSensor() {
        if (sensorManager != null) {
            if (heartRateSensor != null) {
                sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
            }
            heartRateView.setText(String.valueOf(heartRateValue));
        }
    }

    private void deactivateSensor() {
        if (sensorManager != null) {
            if (heartRateSensor != null) {
                sensorManager.unregisterListener((SensorEventListener) this);
                //TODO change layout -> nur heartrate, kein Text davor(?)
                heartRateView.setText(R.string.heartRateDisplay_off);
                //heartRateView.setText("Button deactivated");
                heartRateValue = 0;
                //heartRateView.setText("Off: "+ heartRateValue.toString());
            }
            else {
                heartRateView.setText("No HeartRate sensor");
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //Update your data. This check is very raw. You should improve it when the sensor is unable to calculate the heart rate
        if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            if (heartRateSensor != null) {
                sensorManager.registerListener((SensorEventListener) this, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
            } else {
                heartRateValue = (int)sensorEvent.values[0];
                heartRateView.setText(new StringBuilder().append(R.string.heartRateDisplay_off).append(heartRateValue).toString());
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.d(TAG, "onAccuracyChanged - accuracy: " + i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getMessageClient(this).removeListener(this);
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        // TODO send Message with current Sensor data to mobile device

        Log.d(TAG, "onMessageReceived() A message from watch was received:"
                + messageEvent.getRequestId() + " " + messageEvent.getPath());
        String message =new String(messageEvent.getData());
        Log.v(TAG, "Wear activity received message: " + message);
        // Display message in UI
        messageTest.setText("PING: " + num);
        //Send a message back.
        // TODO STOP THREAD (siehe Mobile)
        message = new String("YEET");
        new SendThread(dataPath, message).start();
    }

    // Send the Data back to Mobile Phone
    class SendThread extends Thread {
        String path;
        String message;

        //constructor
        SendThread(String p, String msg) {
            path = p;
            message = msg;
        }

        //sends the message via the thread.  this will send to all wearables connected, but
        //since there is (should only?) be one, so no problem.
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
                        Log.v(TAG, "SendThread: message send to " + node.getDisplayName());

                    } catch (ExecutionException exception) {
                        Log.e(TAG, "Task failed: " + exception);

                    } catch (InterruptedException exception) {
                        Log.e(TAG, "Interrupt occurred: " + exception);
                    }

                }

            } catch (ExecutionException exception) {
                Log.e(TAG, "Task failed: " + exception);

            } catch (InterruptedException exception) {
                Log.e(TAG, "Interrupt occurred: " + exception);
            }
        }
    }

}