package hust.cc.asynchronousacousticlocalization.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    // by cc
    @BindView(R.id.button_test)
    Button testButton;
    @BindView(R.id.button_setting)
    Button settingButton;

    private AudioRecorder audioRecorder = new AudioRecorder();
    //private PlayThread playThread = new PlayThread();
    private DecodThread decodThread;

    // by cc
    private DecodeScheduleMessage decodeScheduleMessage = null;
    private OKSocket okSocket = null;
    private PlayThread playThread = null;
    private final String TAG = "MainActivity";
    public static int identity = 1;
    private String ipAddressStr = "IP_ADDRESS";
    private String addrPortStr = "ADDR_PORT";
    private String identityStr = "IDENTITY";
    private String settingStr = "SETTING";
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
        //okSocket.initSocket("192.168.1.101", 22222);
        //okSocket.start();

        Log.e(TAG, "socket thread start");
        playThread = new PlayThread(decodeScheduleMessage);
        playThread.start();
        Log.e(TAG, "play thread is listening");
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        okSocket.close();
        decodeScheduleMessage.removeObserver(playThread);
        decodeScheduleMessage.close();
        decodThread.close();
        playThread.close();
        audioRecorder.finishRecord();
}

    // ************************** UI events handling part***********************************************
    @OnClick(R.id.button_test)
    void onTestButtonClicked(){
        TimeStamp timeStamp = new TimeStamp(identity, 1234);
        okSocket.sendTimeStamp(timeStamp);
        Log.e(TAG, "message to the server sent");
        Log.e(TAG, timeStamp.formatMessage().toString());
    }

    @OnClick(R.id.button_setting)
    void onSettingButtonClicked(){
        settingDialog();
        settingButton.setVisibility(View.INVISIBLE);
    }

    private void settingDialog(){
        LayoutInflater layoutInflater = getLayoutInflater();
        View layout = layoutInflater.inflate(R.layout.layout_setting,null);
        final EditText ip = (EditText)layout.findViewById(R.id.ip_address);
        final EditText port = (EditText)layout.findViewById(R.id.ip_port);
        final EditText identity = (EditText)layout.findViewById(R.id.identity);

        SharedPreferences setting = getSharedPreferences(settingStr, Context.MODE_PRIVATE);
        String ipAddr = setting.getString(ipAddressStr," ").trim();
        int ipPort = setting.getInt(addrPortStr,-1);

        MainActivity.identity = setting.getInt(identityStr, 1);

        ip.setText(ipAddr);
        port.setText(String.valueOf(ipPort));
        identity.setText(String.valueOf(MainActivity.identity));
        new AlertDialog.Builder(this)
                .setTitle("Parameter settings")
                .setView(layout)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String ipAddress = ip.getText().toString().trim();
                        int ipPort = Integer.parseInt(port.getText().toString());
                        MainActivity.identity = Integer.parseInt(identity.getText().toString());
                        if(okSocket != null){
                            //commSocket.setup(ipAddress,ipPort);
                            ///commSocket.start();
                            okSocket.initSocket(ipAddress, ipPort);
                            okSocket.start();
                            Toast.makeText(getApplicationContext(),"Init Socket ok",Toast.LENGTH_SHORT).show();
                        }

                        SharedPreferences setting = getSharedPreferences(settingStr,Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = setting.edit();
                        editor.putString(ipAddressStr,ipAddress);
                        editor.putInt(addrPortStr,ipPort);
                        editor.putInt(identityStr, MainActivity.identity);
                        editor.apply();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();

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

                    TimeStamp timeStamp = new TimeStamp(MainActivity.identity, msg.arg1);
                    okSocket.sendTimeStamp(timeStamp);

                    break;
            }
        }
    };


}
