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

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.Arrays;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import hust.cc.asynchronousacousticlocalization.R;
import hust.cc.asynchronousacousticlocalization.physical.AudioRecorder;
import hust.cc.asynchronousacousticlocalization.physical.PlayThread;
import hust.cc.asynchronousacousticlocalization.processing.DecodThread;
import hust.cc.asynchronousacousticlocalization.processing.DecodeScheduleMessage;
//import hust.cc.asynchronousacousticlocalization.processing.ScheduleListener;
import hust.cc.asynchronousacousticlocalization.processing.Decoder;
import hust.cc.asynchronousacousticlocalization.processing.HampelFilter;
import hust.cc.asynchronousacousticlocalization.utils.BioClient;
import hust.cc.asynchronousacousticlocalization.utils.FileUtils;
import hust.cc.asynchronousacousticlocalization.utils.FlagVar;
import hust.cc.asynchronousacousticlocalization.utils.JniUtils;
import hust.cc.asynchronousacousticlocalization.utils.RspHandler;
import hust.cc.asynchronousacousticlocalization.utils.TimeStamp;

public class MainActivity extends AppCompatActivity implements AudioRecorder.RecordingCallback,RspHandler.Callback{


    @BindView(R.id.omit_sound)
    Button omitButton;
    @BindView(R.id.recv_sound)
    Button recvButton;
    @BindView(R.id.text)
    TextView text;
    @BindView(R.id.text2)
    TextView text2;
    // by cc
    @BindView(R.id.button_test)
    Button testButton;
    @BindView(R.id.button_setting)
    Button settingButton;

    @BindView(R.id.graph)
    GraphView mGraphView;
    @BindView(R.id.graph2)
    GraphView mGraphView2;


    private boolean isFileWritten = false;

    private AudioRecorder audioRecorder = new AudioRecorder();
    //private PlayThread playThread = new PlayThread();
    private DecodThread decodThread;

    // by cc
    private DecodeScheduleMessage decodeScheduleMessage = null;
    private PlayThread playThread = null;
    private final String TAG = "MainActivity";
    public static int identity = 1;
    private String ipAddressStr = "IP_ADDRESS";
    private String addrPortStr = "ADDR_PORT";
    private String identityStr = "IDENTITY";
    private String settingStr = "SETTING";
    //NioClient client = null;
    //RspHandler rspHandler = null;
    private BioClient bioClient = null;


    private short[] testArray1 = new short[409600*2];
    private short[] testArray2 = new short[409600*2];
    private int testI = 0;
    private LineGraphSeries<DataPoint> mSeries;
    private LineGraphSeries<DataPoint> mSeries2;
    //public static int targetId =


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
//        testJni();
//        initParams();
        testHampel();
    }

    private void testJni(){
        try {
            Decoder decoder = new Decoder();
            System.out.println(JniUtils.sayHello());
            float[] data0 = new float[8192];
            float[] data = new float[8192];
            float[] dataF = new float[8192];
            float[] dataFF = new float[8192];
            for (int i = 0; i < Decoder.upPreamble.length; i++) {
                data0[i] = Decoder.upPreamble[i];
                dataFF[i] = Decoder.upPreamble[i];
            }


            FloatFFT_1D floatFFT_1D = new FloatFFT_1D(8192);
            floatFFT_1D.realForward(dataFF);
            floatFFT_1D.realForward(dataFF);
            floatFFT_1D.realForward(dataFF);
            floatFFT_1D.realForward(dataFF);
            Date date1 = new Date();
            for(int i=0;i<100;i++) {
                System.arraycopy(data0,0,dataF,0,8192);
                floatFFT_1D.realForward(dataF);
            }
            Date date2 = new Date();
            System.out.println("java fft time:" + (date2.getTime() - date1.getTime()));
            System.out.println("java fft size:"+dataF.length);
            System.out.println("java fft:" + Arrays.toString(dataF));

            date1 = new Date();
            float[] corr = decoder.xcorr(dataF,dataF,true);
            date2 = new Date();
            System.out.println("java corr time:" + (date2.getTime() - date1.getTime()));
            System.out.println("java corr size:"+corr.length);
            System.out.println("java corr:" + Arrays.toString(corr));



            date1 = new Date();
            System.arraycopy(data0,0,data,0,8192);
            float[] fft;
            fft = JniUtils.fft(data,8192);
            for(int i=0;i<99;i++){
                fft = JniUtils.fft(data,8192);
            }
            date2 = new Date();
            System.out.println("c fft time:" + (date2.getTime() - date1.getTime()));
            System.out.println("c fft size:"+fft.length);
//            System.out.println("c fft:" + Arrays.toString(fft));

            date1 = new Date();
            float[] corr2 = JniUtils.xcorr(fft,fft);
            date2 = new Date();
            System.out.println("c corr time:" + (date2.getTime() - date1.getTime()));
            System.out.println("c corr size:"+corr2.length);
            System.out.println("c corr:" + Arrays.toString(corr2));

//            float[] corrDiff = new float[8192];
//            for(int i=0;i<corr.length;i++){
//                corrDiff[i] = Math.abs(corr[i]-corr2[i]);
//                System.out.println(corrDiff[i]+" "+i);
//            }
//            System.out.println("");
//        }catch (Exception e){
//            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void testHampel(){
        try {
            HampelFilter hf = new HampelFilter(4, 5);
            double[] data = new double[100];
            double[] medians = new double[100];
            double[] hampels = new double[100];
            for (int i = 0; i < data.length; i++) {
                data[i] = Math.random();
                if(i%10 == 0){
                    data[i] += 10;
                }
                hf.addData(data[i]);
                if (hf.isReady()) {
                    medians[i] = hf.getMedian();
                    hampels[i] = hf.getHampelVal();
                }else{
                    medians[i] = 0;
                    hampels[i] = 0;
                }

            }
            System.out.println(Arrays.toString(data));
            System.out.println(Arrays.toString(medians));
            System.out.println(Arrays.toString(hampels));
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void initParams(){

        mGraphView.setVisibility(View.GONE);
        mGraphView2.setVisibility(View.GONE);
        decodThread = new DecodThread(myHandler);
        decodThread.initialize(AudioRecorder.getBufferSize()/2,true);
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
        omitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                short[] testBuf = new short[1000];
                for(int i=0;i<testBuf.length;i++){
                    if(i%3 == 0){
                        testBuf[i] = 10000;
                    }
                }
//                playThread.fillBufferAndPlay(testBuf);
                playThread.fillBufferAndPlay(Decoder.upPreamble);
            }
        });

        mSeries = new LineGraphSeries<>();
        mGraphView.addSeries(mSeries);
        mSeries2 = new LineGraphSeries<>();
        mGraphView2.addSeries(mSeries2);

        // thread that handle message from the server on schedule information
        decodeScheduleMessage = DecodeScheduleMessage.getInstance();
        decodeScheduleMessage.start();
        Log.e(TAG, "decodeSchedule thread started");


        Log.e(TAG, "socket thread start");
        playThread = new PlayThread(decodeScheduleMessage);
        playThread.start();
        Log.e(TAG, "play thread is listening");
    }

    /**
     * generate data for test
     * @return
     */


    @Override
    protected void onDestroy() {
        super.onDestroy();

        decodeScheduleMessage.removeObserver(playThread);
        decodeScheduleMessage.close();
        decodThread.close();
        playThread.close();
        audioRecorder.finishRecord();
        //rspHandler.close();
}

    // ************************** UI events handling part***********************************************
    @OnClick(R.id.button_test)
    void onTestButtonClicked(){
        TimeStamp timeStamp = new TimeStamp(identity, 1234);
        //okSocket.sendTimeStamp(timeStamp);
        try {
            bioClient.send(timeStamp.formatMessage().toString());
            //client.sendMessage(timeStamp.formatMessage().toString().getBytes(), rspHandler);
            Log.e(TAG, "message to the server sent");
            Log.e(TAG, timeStamp.formatMessage().toString());
        }catch (Exception e){
            e.printStackTrace();
        }
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
                        if(bioClient == null){
                            //commSocket.setup(ipAddress,ipPort);
                            ///commSocket.start();
                            //okSocket.initSocket(ipAddress, ipPort);
                            //okSocket.start();
                            try {
                                /*client = new NioClient(InetAddress.getByName(ipAddress), ipPort);
                                Thread t = new Thread(client);
                                t.setDaemon(true);
                                t.start();
                                rspHandler = new RspHandler();
                                rspHandler.start();*/
                                bioClient = new BioClient(ipAddress, ipPort);
                                Thread t = new Thread(bioClient);
                                t.setDaemon(true);
                                t.start();

                                Log.e(TAG, "ipAddress = " + ipAddress + " port = "+ipPort);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
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
        short[] data1 = new short[len];
        short[] data2 = new short[len];
        for (int i = 0; i < len; i++) {
            data1[i] = data[2 * i];
            data2[i] = data[2 * i + 1];
        }

//        if(testI<200){
//            System.arraycopy(data1,0, testArray1,4096*testI,4096);
//            System.arraycopy(data2,0, testArray2,4096*testI,4096);
//            testI++;
//        }else{
//            if(!isFileWritten) {
//                isFileWritten = true;
//                try {
//                    System.out.println("file write start.");
//                    FileUtils.saveBytes(testArray1, "data1");
//                    FileUtils.saveBytes(testArray2, "data2");
//                    System.out.println("file write end.");
//                }catch (Exception e){
//                    e.printStackTrace();
//                }
//            }
//            return;
//        }
        if(decodThread.samplesList.size()<300) {
            decodThread.fillSamples(data2);
        }

    }

    // here we process the receiver message aboust schedule information


    public Handler myHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case FlagVar.MESSAGE_TDOA: {
                    StringBuilder sb = new StringBuilder();
                    sb.append("tdoa:").append(msg.arg1).append("    ").append("id num:").append(msg.arg2);
                    text.setText(sb.toString());

                    TimeStamp timeStamp = new TimeStamp(MainActivity.identity, msg.arg1);
                    //okSocket.sendTimeStamp(timeStamp);

                    break;
                }
                case FlagVar.MESSAGE_GRAPH: {
                    synchronized (decodThread.graphBuffer) {
                        DataPoint[] values = getPoints(decodThread.graphBuffer);
                        //Log.e(TAG, "sample length = " + s.length);
                        mSeries.resetData(values);
                    }
                    synchronized ((decodThread.graphBuffer2)){
                        DataPoint[] values = getPoints(decodThread.graphBuffer2);
                        mSeries2.resetData(values);
                    }
                    break;
                }
                case FlagVar.MESSAGE_SPEED:{
                    StringBuilder sb = new StringBuilder();
                    sb.append("speed:").append(msg.arg1);
                    text2.setText(sb.toString());
                    break;
                }
            }
        }

    };

    public DataPoint[] getPoints(float[] data){
        DataPoint[] values = new DataPoint[data.length];
        for (int i = 0; i < data.length; i++) {
            double xx = i;
            double yy = data[i];
            DataPoint vv = new DataPoint(xx, yy);
            values[i] = vv;
        }
        return values;
    }


    @Override
    public void onServerResponse(String msg) {

    }
}
