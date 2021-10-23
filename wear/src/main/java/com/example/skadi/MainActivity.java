package com.example.skadi;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.skadi.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.core.app.ActivityCompat;

public class MainActivity extends Activity implements SensorEventListener, MessageClient.OnMessageReceivedListener {

    private final static String TAG = "Wear MainActivity";
    String dataPath = "/message_path";
    private static final String msgStart = "START_RECORDING";
    private static final String msgStop = "STOP_RECORDING";

    //private Button btnHeartRateRecording;
    private TextView heartRateView;
    private TextView recording_on_off;

    MessageClient messageClient;

    private SensorManager sensorManager;
    private Sensor heartRateSensor;

    private Integer heartRateValue = null;

    ExecutorService executor = Executors.newFixedThreadPool(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        /*
          Set Button;
          The button is for debugging purposes and shows current data in the textView below
          is either toggled manually or by signal from the mobile phone
         */
        //btnHeartRateRecording = findViewById(R.id.btnHeartRateRecording);
        //btnHeartRateRecording.setOnClickListener(toggleButton);

        //Set TextViews
        heartRateView = findViewById(R.id.heartRateView);
        recording_on_off = findViewById(R.id.messageTest);

        //Set HeartRate sensor
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        //Set Message client
        messageClient = Wearable.getMessageClient(getApplicationContext());

        // Since we assume a smartwatch with heart rate sensor is used it shouldn't matter, but just in case
        if (heartRateSensor == null) {
            Log.d(TAG, "Warning: no heartRateSensor");
            heartRateView.setText(R.string.no_HeartRateSensor);
        }

        // Check if App has access on the heart rate sensor, if not request it
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( this, new String[] {  Manifest.permission.BODY_SENSORS  },2);
        }
    }

    private void activateSensor() {
        if (sensorManager != null) {
            sensorManager.registerListener((SensorEventListener) this, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
            heartRateValue = 0;
            heartRateView.setText(String.valueOf(heartRateValue));
        }
    }

    private void deactivateSensor() {
        if (sensorManager != null) {
            sensorManager.unregisterListener((SensorEventListener) this, heartRateSensor);
            heartRateValue = null;
            heartRateView.setText(R.string.heartRateSensor_off);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            heartRateValue = (int)sensorEvent.values[0];
            heartRateView.setText(String.format(""+heartRateValue));

            //Send a message back.
            String msgUpdatedHeartRateValue = heartRateValue.toString();
            executor = Executors.newFixedThreadPool(1);
            executor.submit(new SendThread(dataPath, msgUpdatedHeartRateValue));
        }
    }

    // auto override
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.d(TAG, "onAccuracyChanged - accuracy: " + i);
    }

    // auto override
    @Override
    protected void onResume() {
        super.onResume();
        Wearable.getMessageClient(this).addListener(this);
    }
    // auto override
    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getMessageClient(this).removeListener(this);
    }

    /**
     * Starts/Stops the transmission of heart rate sensor data
     * @param messageEvent : Message to start or stop the sensor and data recording
     */
    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        String message =new String(messageEvent.getData());
        if(message.equals(msgStart)) {
            //Activate sensor on receiving
            activateSensor();
            Log.v(TAG, "Wear activity received start message: " + message);

            heartRateView.setText(R.string.plain_zero);
            recording_on_off.setText(R.string.receivedStarted);
        }
        else if (message.equals(msgStop)) {
            deactivateSensor();
            //executor.shutdownNow();
            Log.v(TAG, "Wear activity received stop message: " + message);

            heartRateValue = null;
            heartRateView.setText(R.string.heartRateSensor_off);
            recording_on_off.setText(R.string.recordingStopped);
        }
        else {
            Log.e("ERROR: ",message);
        }
    }

    // Send the Data back to Mobile Phone
    // TODO restructure to function?
    class SendThread extends Thread {
        String path;
        String message;

        //constructor
        SendThread(String p, String msg) {
            path = p;
            message = msg;
        }

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