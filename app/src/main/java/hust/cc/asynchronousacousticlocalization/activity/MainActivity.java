package hust.cc.asynchronousacousticlocalization.activity;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hust.cc.asynchronousacousticlocalization.R;
import hust.cc.asynchronousacousticlocalization.physical.AudioRecorder;
import hust.cc.asynchronousacousticlocalization.physical.PlayThread;
import hust.cc.asynchronousacousticlocalization.processing.DecodThread;
import hust.cc.asynchronousacousticlocalization.processing.DecodeScheduleMessage;
//import hust.cc.asynchronousacousticlocalization.processing.ScheduleListener;
import hust.cc.asynchronousacousticlocalization.utils.FlagVar;
import hust.cc.asynchronousacousticlocalization.utils.OKSocket;
import hust.cc.asynchronousacousticlocalization.utils.TimeStamp;

public class MainActivity extends AppCompatActivity implements AudioRecorder.RecordingCallback, OKSocket.Callback {


    @BindView(R.id.omit_sound)
    Button omitButton;
    @BindView(R.id.recv_sound)
    Button recvButton;
    @BindView(R.id.text)
    TextView text;
    @BindView(R.id.button_test)
    Button testButton;

    private AudioRecorder audioRecorder = new AudioRecorder();
    //private PlayThread playThread = new PlayThread();
    private DecodThread decodThread;

    // by cc
    private DecodeScheduleMessage decodeScheduleMessage = null;
    private OKSocket okSocket = null;
    private PlayThread playThread = null;
    private final String TAG = "MainActivity";
    public static int identity = 1;
    //public static int targetId =
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initParams();
    }

    private void initParams(){

        decodThread = new DecodThread(myHandler);
        decodThread.setProcessBufferSize(AudioRecorder.getBufferSize());
        new Thread(decodThread).start();
        audioRecorder.recordingCallback(this);
        recvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (!audioRecorder.isRecording()) {
                        audioRecorder.startRecord();
                    } else {
                        audioRecorder.finishRecord();
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        // thread that handle message from the server on schedule information
        decodeScheduleMessage = DecodeScheduleMessage.getInstance();
        decodeScheduleMessage.start();
        Log.e(TAG, "decodeSchedule thread started");

        okSocket = OKSocket.getInstance();
        okSocket.socketCallback(this);
        okSocket.initSocket("192.168.1.101", 22222);
        okSocket.start();

        Log.e(TAG, "socket thread start");
        playThread = new PlayThread(decodeScheduleMessage);
        playThread.start();
        Log.e(TAG, "play thread is listening");
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        okSocket.close();
        decodeScheduleMessage.close();
        decodThread.close();
        playThread.close();
        audioRecorder.finishRecord();
}

    // ************************** UI events handling part***********************************************
    @OnClick(R.id.button_test)
    void onTestButton(){
        TimeStamp timeStamp = new TimeStamp(identity, 1234);
        okSocket.sendTimeStamp(timeStamp);
        Log.e(TAG, "message to the server sent");
    }

    //************************ message handling part **************************************

    // here we process the received audio samples
    @Override
    public void onDataReady(short[] data, int len) {
        if (decodThread.samplesList.size() < 300) {
            decodThread.fillSamples(data);
        }

    }

    // here we process the received message from the server about tdoa information
    @Override
    public void onReceiveSocketMsg(JSONObject jsonObject) {
        Log.e(TAG, jsonObject.toString());
    }

    // here we process the receiver message aboust schedule information


    public Handler myHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case FlagVar.MESSAGE_TDOA:
                // TODO here, post the tdoa information to the server
                    StringBuilder sb = new StringBuilder();
                    sb.append("tdoa:").append(msg.arg1).append("\n").append("id num:").append(msg.arg2);
                    text.setText(sb.toString());
                    break;
            }
        }
    };


}
