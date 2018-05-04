package hust.cc.asynchronousacousticlocalization.activity;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;
import hust.cc.asynchronousacousticlocalization.R;
import hust.cc.asynchronousacousticlocalization.physical.AudioRecorder;
import hust.cc.asynchronousacousticlocalization.physical.PlayThread;
import hust.cc.asynchronousacousticlocalization.processing.DecodThread;
import hust.cc.asynchronousacousticlocalization.utils.FlagVar;
import hust.cc.asynchronousacousticlocalization.utils.OKSocket;

public class MainActivity extends AppCompatActivity implements AudioRecorder.RecordingCallback, OKSocket.Callback {


    @BindView(R.id.omit_sound)
    Button omitButton;
    @BindView(R.id.recv_sound)
    Button recvButton;
    @BindView(R.id.text)
    TextView text;

    private AudioRecorder audioRecorder = new AudioRecorder();
    private PlayThread playThread = new PlayThread();
    private DecodThread decodThread;
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
                if(!audioRecorder.isRecording()) {
                    audioRecorder.startRecord();
                }else{
                    audioRecorder.finishRecord();
                }
            }
        });

//        playThread.fillBufferAndPlay();
        omitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                playThread.run();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // here we process the received audio samples
    @Override
    public void onDataReady(short[] data, int len) {
        decodThread.fillSamples(data);
    }

    // here we process the received message from the server
    @Override
    public void onReceiveSocketMsg(JSONObject jsonObject) {

    }

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
