package com.example.skadi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;

public class MainActivity extends FragmentActivity implements SensorEventListener, MessageClient.OnMessageReceivedListener,
        AmbientModeSupport.AmbientCallbackProvider{

    private final static String TAG = "Wear MainActivity";
    String dataPath = "/message_path";

    MessageClient messageClient;

    private AmbientModeSupport.AmbientController ambientController;

    private SensorManager sensorManager;
    private Sensor heartRateSensor;
    private Sensor gyroscopeSensor;
    private Sensor acceleratorSensor;
    private Sensor lightSensor;

    private TextView heartRateValueView;
    private TextView gyroscopeValueView;
    private TextView acceleratorValueView;
    private TextView lightValueView;

    private String heartRateValue = null;
    private String gyroscopeValues = null;
    private String acceleratorValues = null;
    private String lightValue = null;

    private String oldGyroscopeValues = null;
    private String oldAcceleratorValues = null;

    private boolean heartRateSensorActivated = false;
    private boolean gyroscopeSensorActivated = false;
    private boolean acceleratorSensorActivated = false;
    private boolean lightSensorActivated = false;

    //ExecutorService executor = Executors.newFixedThreadPool(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        ambientController = AmbientModeSupport.attach(this);

        //Set sensors
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        acceleratorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (lightSensor == null) {
            Log.d(TAG, "Warning: no heartRateSensor");
            lightValueView.setText(R.string.no_light);
        }

        //Set sensor value views
        heartRateValueView = findViewById(R.id.heartRateSensorValueView);
        gyroscopeValueView = findViewById(R.id.gyroscopeSensorValueView);
        acceleratorValueView = findViewById(R.id.acceleratorSensorValueView);
        lightValueView = findViewById(R.id.lightSensorValueView);

        //Set Message client
        messageClient = Wearable.getMessageClient(getApplicationContext());

        // Check if App has access on the heart rate sensor, if not request it
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( this, new String[] {  Manifest.permission.BODY_SENSORS  },2);
        }
    }



    /**
     * Everytime a sensor value changes, send it via message and update the corresponding view
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            heartRateValue = ""+(int)sensorEvent.values[0];
            //Set first character in message differently to differentiate the sensor values
            new SendThread(dataPath, "h"+heartRateValue).start();

            heartRateValueView.setText(heartRateValue);
            //executor = Executors.newFixedThreadPool(1);
            //executor.submit(new SendThread(dataPath, msgUpdatedHeartRateValue));
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            /*String x = String.format(Locale.getDefault(),"%.2f", sensorEvent.values[0]);
            String y = String.format(Locale.getDefault(),"%.2f", sensorEvent.values[1]);
            String z = String.format(Locale.getDefault(),"%.2f", sensorEvent.values[2]);

            gyroscopeValues = x+","+y+","+z;*/
            gyroscopeValues = ""+sensorEvent.values[0];

            if(oldGyroscopeValues == null) {
                new SendThread(dataPath, "g"+gyroscopeValues).start();
                oldGyroscopeValues = gyroscopeValues;
            } else {
                // Only send new value on a more significant change to prevent massive flood of data
                Log.d("TEST: ", ""+gyroscopeValues);
                float oldgyro = Float.parseFloat(oldGyroscopeValues);
                Log.d("TEST2: ", "xdddsad");
                float gyro = Float.parseFloat(gyroscopeValues);
                float diff = oldgyro - gyro;
                Log.d("TEST3: ", ""+diff);
                if(Math.abs(diff) > 0.1) {
                    Log.d("TEST4: ", ""+diff);
                    new SendThread(dataPath, "g"+gyroscopeValues).start();
                    oldGyroscopeValues = gyroscopeValues;
                }
            }
            gyroscopeValueView.setText(gyroscopeValues);

            //executor = Executors.newFixedThreadPool(1);
            //executor.submit(new SendThread(dataPath, msgUpdatedHeartRateValue));
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            String x = String.format(Locale.getDefault(),"%.2f", sensorEvent.values[0]);
            /*String y = String.format(Locale.getDefault(),"%.2f", sensorEvent.values[1]);
            String z = String.format(Locale.getDefault(),"%.2f", sensorEvent.values[2]);
            acceleratorValues = x+","+y+","+z;*/
            acceleratorValues = x;

            if(oldAcceleratorValues == null) {
                new SendThread(dataPath, "a"+acceleratorValues).start();
                oldAcceleratorValues = acceleratorValues;
                acceleratorValueView.setText(acceleratorValues);
            } else {
                // Only send value on a more significant change to prevent massive flood of data
                if(Math.abs(Float.parseFloat(oldAcceleratorValues) - Float.parseFloat(acceleratorValues))
                        > 0.1) {
                    new SendThread(dataPath, "a"+acceleratorValues).start();
                    oldAcceleratorValues = acceleratorValues;
                    acceleratorValueView.setText(acceleratorValues);
                }
            }
            //executor = Executors.newFixedThreadPool(1);
            //executor.submit(new SendThread(dataPath, msgUpdatedHeartRateValue));
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT) {
            lightValue = ""+sensorEvent.values[0];
            new SendThread(dataPath, "l"+lightValue).start();

            lightValueView.setText(lightValue);
            //executor = Executors.newFixedThreadPool(1);
            //executor.submit(new SendThread(dataPath, msgUpdatedHeartRateValue));
        }
    }

    /**
     * Starts/Stops the transmission of heart rate sensor data
     * @param messageEvent : Message to start or stop the sensor and data recording
     */
    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        String message = new String(messageEvent.getData());
        switch(message) {
            case "heartRate":
                //revert boolean value
                this.heartRateSensorActivated = !this.heartRateSensorActivated;
                if(heartRateSensorActivated) {
                    sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    heartRateValueView.setText(R.string.initial_zero_value);
                } else {
                    sensorManager.unregisterListener(this, heartRateSensor);
                    heartRateValueView.setText(R.string.status_off);
                }
                break;
            case "gyroscope":
                //revert boolean value
                this.gyroscopeSensorActivated = !this.gyroscopeSensorActivated;
                if(gyroscopeSensorActivated) {
                    sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    gyroscopeValueView.setText(R.string.initial_zero_value);
                } else {
                    sensorManager.unregisterListener(this, gyroscopeSensor);
                    gyroscopeValueView.setText(R.string.status_off);
                }
                break;
            case "accelerator":
                //revert boolean value
                this.acceleratorSensorActivated = !this.acceleratorSensorActivated;
                if(acceleratorSensorActivated) {
                    sensorManager.registerListener(this, acceleratorSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    acceleratorValueView.setText(R.string.initial_zero_value);
                } else {
                    sensorManager.unregisterListener(this, acceleratorSensor);
                    acceleratorValueView.setText(R.string.status_off);
                }
                break;
            case "light":
                //revert boolean value
                this.lightSensorActivated = !this.lightSensorActivated;
                if(lightSensorActivated) {
                    sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    lightValueView.setText(R.string.initial_zero_value);
                } else {
                    sensorManager.unregisterListener(this, lightSensor);
                    lightValueView.setText(R.string.status_off);
                }
                break;
            default:
                Log.e(TAG, "Error occurred while receiving message: " + message);
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

    // TODO reset method
    public void resetToDefaultState() {
        Log.d(TAG, "RESET TO DEFAULT");
        sensorManager.unregisterListener(this, heartRateSensor);
        sensorManager.unregisterListener(this, gyroscopeSensor);
        sensorManager.unregisterListener(this, acceleratorSensor);
        sensorManager.unregisterListener(this, lightSensor);

        heartRateValueView.setText(R.string.status_off);
        gyroscopeValueView.setText(R.string.status_off);
        acceleratorValueView.setText(R.string.status_off);
        lightValueView.setText(R.string.status_off);

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
        resetToDefaultState();
    }
    // auto override
    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new AmbientCallback();
    }

    private static class AmbientCallback extends AmbientModeSupport.AmbientCallback {
        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            // Handle entering ambient mode
        }

        @Override
        public void onExitAmbient() {
            // Handle exiting ambient mode
        }

        @Override
        public void onUpdateAmbient() {
            // Update the content
        }
    }



}