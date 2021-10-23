package com.example.skadi;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Receives its own events using a listener API designed for foreground activities. Updates a data
 * item every second while it is open. Also allows user to take a photo and send that as an asset to
 * the paired wearable.
 */
public class MainActivity extends AppCompatActivity implements
        MessageClient.OnMessageReceivedListener,
        View.OnClickListener {

    String dataPath = "/message_path";

    Button startSensorButton;

    TextView currentHeartRate;
    TextView heartRateValue;
    TextView recordingStatus;

    protected Handler handler;
    String TAG = "Mobile MainActivity";
    boolean dataCollectionActive = false;
    private static final String msgStart = "START_RECORDING";
    private static final String msgStop = "STOP_RECORDING";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set text views
        currentHeartRate = findViewById(R.id.currentHeartRate); // Plain textView with static text
        heartRateValue = findViewById(R.id.heartRateValue);
        recordingStatus = findViewById(R.id.recordingStatus);

        // Set button
        startSensorButton = findViewById(R.id.start_data_collection);
        startSensorButton.setOnClickListener(this);

        //message handler for the send thread.
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Bundle stuff = msg.getData();
                return true;
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
     * This is a simple receiver add/removed in onResume/onPause
     * It receives the message from the wear device and displays to the screen.
     */
    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived() A message from watch was received:"
                + messageEvent.getRequestId() + " " + messageEvent.getPath());
        String message = new String(messageEvent.getData());
        Log.v(TAG, "Main activity received message: " + message);
        Log.d(TAG, "Received data: " + message);
        // Display message in UI
        heartRateValue.setText(message);
    }

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    public void onClick(View v) {
        this.dataCollectionActive = !this.dataCollectionActive;
        if(this.dataCollectionActive) {

            startSensorButton.setText(R.string.stop_data_collection);
            recordingStatus.setText(R.string.data_collection_on);

            //Requires a new thread to avoid blocking the UI
            new SendThread(dataPath, msgStart).start();
        }

        else {
            heartRateValue.setText(R.string.plain_zero);
            startSensorButton.setText(R.string.start_data_collection);
            recordingStatus.setText(R.string.data_collection_on);

            //Requires a new thread to avoid blocking the UI
            new SendThread(dataPath, msgStop).start();
        }

    }

    //method to create up a bundle to send to a handler via the thread below.
    /*public void sendMessage(String logthis) {
        Bundle b = new Bundle();
        b.putString("logthis", logthis);
        Message msg = handler.obtainMessage();
        msg.setData(b);
        msg.arg1 = 1;
        msg.what = 1; //so the empty message is not used!
        handler.sendMessage(msg);
    }*/

    //TODO restructure to normal method?
    //This actually sends the message to the wearable device.
    class SendThread extends Thread {
        String path;
        String message;

        //constructor
        SendThread(String p, String msg) {
            path = p;
            message = msg;
        }

        //sends the message via the thread.  this will send to all wearables connected, but
        //since there is (should only?) be one, no problem.
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