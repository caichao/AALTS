package hust.cc.asynchronousacousticlocalization.activity;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import butterknife.ButterKnife;
import hust.cc.asynchronousacousticlocalization.R;
import hust.cc.asynchronousacousticlocalization.physical.AudioRecorder;
import hust.cc.asynchronousacousticlocalization.utils.FlagVar;

public class MainActivity extends AppCompatActivity implements AudioRecorder.RecordingCallback {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // here we process the received audio samples
    @Override
    public void onDataReady(short[] data, int bytelen) {

    }

    public Handler myHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case FlagVar.MESSAGE_TDOA:
                // TODO here, post the tdoa information to the server
                    break;
            }
        }
    };
}
