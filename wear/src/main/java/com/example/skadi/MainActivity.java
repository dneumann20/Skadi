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
import java.util.concurrent.ExecutionException;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientModeSupport;

public class MainActivity extends FragmentActivity implements SensorEventListener, MessageClient.OnMessageReceivedListener,
        AmbientModeSupport.AmbientCallbackProvider{

    private final static String TAG = "Wear MainActivity";
    String dataPath = "/message_path";

    MessageClient messageClient;

    private SensorManager sensorManager;
    private Sensor heartRateSensor;
    private Sensor gyroscopeSensor;
    private Sensor acceleratorSensor;
    private Sensor lightSensor;

    private TextView heartRateValueView;
    private TextView gyroscopeValueView;
    private TextView acceleratorValueView;
    private TextView lightValueView;

    private String heartRateValue = 0+"";
    private String gyroscopeValue_x = 0+"";
    private String acceleratorValue_x = 0+"";
    private String lightValue = 0+"";

    private String oldGyroscopeValue_x = 0+"";
    private String oldAcceleratorValue_x = 0+"";

    private boolean heartRateSensorActivated = false;
    private boolean gyroscopeSensorActivated = false;
    private boolean acceleratorSensorActivated = false;
    private boolean lightSensorActivated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        //Make the app be permanently in the foreground
        AmbientModeSupport.AmbientController ambientController = AmbientModeSupport.attach(this);

        //Set sensors
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        acceleratorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        //Set sensor value views
        heartRateValueView = findViewById(R.id.heartRateSensorValueView);
        gyroscopeValueView = findViewById(R.id.gyroscopeSensorValueView);
        acceleratorValueView = findViewById(R.id.acceleratorSensorValueView);
        lightValueView = findViewById(R.id.lightSensorValueView);

        //Set Message client
        messageClient = Wearable.getMessageClient(getApplicationContext());

        // Check if App has access on the sensors, if not request it
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions( this, new String[] {  Manifest.permission.BODY_SENSORS  },2);
        }
    }



    /**
     * Everytime a sensor value changes, send it via message and update the corresponding view
     * Gyroscope and Accelerator "constantly" change their values, so the difference between new and old values are
     * calculated and will be only updated on a significant change. Min difference was set on 1 for
     * gyroscope and 2 for accelerator for testing reasons.
     *
     * To differentiate the data of according sensors, the message is sent with a prefix character that is
     * removed in the mobile phone app again
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            heartRateValue = ""+(int)sensorEvent.values[0];
            new SendThread(dataPath, "h"+heartRateValue).start();

            heartRateValueView.setText(heartRateValue);
            //executor.submit(new SendThread(dataPath, msgUpdatedHeartRateValue));
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroscopeValue_x = ""+sensorEvent.values[0];

            if(Math.abs(Float.parseFloat(oldGyroscopeValue_x) - Float.parseFloat(gyroscopeValue_x)) > 1) {
                new SendThread(dataPath, "g"+ gyroscopeValue_x).start();
                oldGyroscopeValue_x = gyroscopeValue_x;
                gyroscopeValueView.setText(gyroscopeValue_x);
            }
            //executor.submit(new SendThread(dataPath, msgUpdatedHeartRateValue));
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            acceleratorValue_x = ""+sensorEvent.values[0];

            if(Math.abs(Float.parseFloat(oldAcceleratorValue_x) - Float.parseFloat(acceleratorValue_x))
                    > 2) {
                new SendThread(dataPath, "a"+ acceleratorValue_x).start();
                oldAcceleratorValue_x = acceleratorValue_x;
                acceleratorValueView.setText(acceleratorValue_x);
            }
            //executor.submit(new SendThread(dataPath, msgUpdatedHeartRateValue));
        }
        else if (sensorEvent.sensor.getType() == Sensor.TYPE_LIGHT) {
            lightValue = ""+sensorEvent.values[0];
            new SendThread(dataPath, "l"+lightValue).start();

            lightValueView.setText(lightValue);
            //executor.submit(new SendThread(dataPath, msgUpdatedHeartRateValue));
        }
    }

    /**
     * Starts/Stops the transmission of the sensor data
     * @param messageEvent : Message to start or stop the according sensor
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
                    heartRateValueView.setText(heartRateValue);
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
                    gyroscopeValueView.setText(gyroscopeValue_x);
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
                    acceleratorValueView.setText(acceleratorValue_x);
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
                    lightValueView.setText(lightValue);
                } else {
                    sensorManager.unregisterListener(this, lightSensor);
                    lightValueView.setText(R.string.status_off);
                }
                break;
            case "reset":
                resetToDefaultState();
                break;
            default:
                Log.e(TAG, "Error occurred while receiving message: " + message);
        }
    }

    // Send the Data to Mobile Phone
    class SendThread extends Thread {
        String path;
        String message;

        //constructor
        SendThread(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            //first get all the nodes, in this case assuming there is only one paired mobile phone
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
                        Log.v(TAG, "SendThread "+result+": message "+message+" sent to " + node.getDisplayName());

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

    private void resetToDefaultState() {
        Log.d(TAG, "RESET TO DEFAULT");
        sensorManager.unregisterListener(this, heartRateSensor);
        sensorManager.unregisterListener(this, gyroscopeSensor);
        sensorManager.unregisterListener(this, acceleratorSensor);
        sensorManager.unregisterListener(this, lightSensor);

        heartRateValueView.setText(R.string.status_off);
        gyroscopeValueView.setText(R.string.status_off);
        acceleratorValueView.setText(R.string.status_off);
        lightValueView.setText(R.string.status_off);

        this.heartRateSensorActivated = false;
        this.gyroscopeSensorActivated = false;
        this.acceleratorSensorActivated = false;
        this.lightSensorActivated = false;

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
    // Reset when app is inactive
    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getMessageClient(this).removeListener(this);
        resetToDefaultState();
    }

    // Reset when app is closed or goes in the background
    @Override
    protected void onStop() {
        super.onStop();
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