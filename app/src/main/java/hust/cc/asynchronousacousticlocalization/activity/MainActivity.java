package hust.cc.asynchronousacousticlocalization.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.series.DataPoint;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
import hust.cc.asynchronousacousticlocalization.utils.CapturedBeaconMessage;
import hust.cc.asynchronousacousticlocalization.utils.FileUtils;
import hust.cc.asynchronousacousticlocalization.utils.FlagVar;
import hust.cc.asynchronousacousticlocalization.utils.JSONUtils;
import hust.cc.asynchronousacousticlocalization.utils.RspHandler;
import hust.cc.asynchronousacousticlocalization.utils.SpeedInfo;
import hust.cc.asynchronousacousticlocalization.utils.TimeStamp;

public class MainActivity extends AppCompatActivity implements AudioRecorder.RecordingCallback,RspHandler.Callback{


    @BindView(R.id.omit_sound)
    Button omitButton;
    @BindView(R.id.recv_sound)
    Button recvButton;
    @BindView(R.id.clear)
    Button clearButton;
    @BindView(R.id.text)
    TextView text;
    @BindView(R.id.text1)
    TextView text1;
    @BindView(R.id.text2)
    TextView text2;
    // by cc
    @BindView(R.id.button_test)
    Button testButton;
    @BindView(R.id.button_setting)
    Button settingButton;
    @BindView(R.id.threshold)
    EditText editThreshold;
    @BindView(R.id.out_linear)
    LinearLayout outLinear;
    @BindView(R.id.text3)
    TextView text3;
    @BindView(R.id.text4)
    TextView text4;
    @BindView(R.id.json_linear)
    LinearLayout jsonLinear;



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
    private String runningSettingsStr = "RUNNING_SETTINGS";
    //NioClient client = null;
    //RspHandler rspHandler = null;
    private BioClient bioClient = null;
    private List<SpeedInfo> speedInfos;
    private int text4ShowMode = 0;

    private String testCaptureBeaconMessage = "";

    private DecimalFormat decimalFormat = new DecimalFormat("##0.0");
    //public static int targetId =


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
//        testJni();
        initParams();
//        testHampel();
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

        speedInfos = new LinkedList<>();
        decodThread = new DecodThread(myHandler);
        decodThread.initialize(AudioRecorder.getBufferSize()/2);
        new Thread(decodThread).start();
        audioRecorder.recordingCallback(this);
        recvButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (!audioRecorder.isRecording()) {
                        audioRecorder.startRecord();
                        Toast.makeText(getApplicationContext(),"Start decoding sound signal.", Toast.LENGTH_SHORT).show();
                        recvButton.setTextColor(Color.RED);
                        recvButton.setText("stop receiving");
                    } else {
                        audioRecorder.finishRecord();
                        Toast.makeText(getApplicationContext(),"Stop decoding sound signal.", Toast.LENGTH_SHORT).show();
                        recvButton.setTextColor(Color.GREEN);
                        recvButton.setText("recv sound");
//                        writeIntoFiles();
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
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                decodThread.clear();
                Toast.makeText(getApplicationContext(), "Counter reset", Toast.LENGTH_SHORT).show();
            }
        });


        // thread that handle message from the server on schedule information
        decodeScheduleMessage = DecodeScheduleMessage.getInstance();
        decodeScheduleMessage.start();
        Log.e(TAG, "decodeSchedule thread started");
        editThreshold.setText(Float.toString(Decoder.rThreshold));
        editThreshold.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                float val = Float.parseFloat(editThreshold.getText().toString());
                if(hasFocus == false){
                    Decoder.rThreshold = val;
                }

            }
        });


        outLinear.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                outLinear.setFocusable(true);
                outLinear.setFocusableInTouchMode(true);
                outLinear.requestFocus();
                InputMethodManager imm = (InputMethodManager) MainActivity.this
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                return false;

            }
        });

        text4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (text4ShowMode){
                    case 0:{
                        jsonLinear.setVisibility(View.GONE);
                        text4ShowMode = 1;
                        text4.setText(SpeedInfos2DetailStr(speedInfos,false));
                        break;
                    }
                    case 1:{
                        text4ShowMode = 2;
                        text4.setText(SpeedInfos2DetailStr(speedInfos,true));
                        break;
                    }
                    case 2:{
                        jsonLinear.setVisibility(View.VISIBLE);
                        text4ShowMode = 0;
                        text4.setText(SpeedInfos2BriefStr(speedInfos));
                        break;
                    }
                }
            }
        });
        loadSettings();


//        Log.e(TAG, "socket thread start");
//        playThread = new PlayThread(decodeScheduleMessage);
//        playThread.start();
//        Log.e(TAG, "play thread is listening");
    }


    private void writeIntoFiles(){
        System.out.println("write start");
        FileUtils.saveBytes(decodThread.testData, "testData");
        FileUtils.saveBytes(decodThread.testCounters,"testCounter");
//        FileUtils.saveBytes(DecodThread.downSymbolSamples[0],"down0");
//        FileUtils.saveBytes(DecodThread.downSymbolSamples[1],"down1");
//        FileUtils.saveBytes(DecodThread.downSymbolSamples[2],"down2");
//        FileUtils.saveBytes(DecodThread.downSymbolSamples[3],"down3");
//        System.out.println("write start2");
//        FileUtils.saveBytes(decodThread.testFFT, "testFFT");
//        System.out.println("write start3");
//        FileUtils.saveBytes(decodThread.testCorr, "testCorr");
//        System.out.println("write start4");
//        FileUtils.saveBytes(decodThread.testFitVals, "testFitVals");
        System.out.println("write end");
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

        //okSocket.sendTimeStamp(timeStamp);
        try {
            bioClient.send(testCaptureBeaconMessage);
            //client.sendMessage(timeStamp.formatMessage().toString().getBytes(), rspHandler);
            Log.e(TAG, "message to the server sent");
            Log.e(TAG, testCaptureBeaconMessage);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @OnClick(R.id.button_setting)
    void onSettingButtonClicked(){
        settingDialog();
        settingButton.setVisibility(View.GONE);

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
                        recvButton.setTextColor(Color.GREEN);
                        clearButton.setTextColor(Color.GREEN);
                        recvButton.setEnabled(true);
                        clearButton.setEnabled(true);
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

//        long time1 = System.nanoTime();

        short[] data1 = new short[len];
        short[] data2 = new short[len];
        for (int i = 0; i < len; i++) {
            data1[i] = data[2 * i];
            data2[i] = data[2 * i + 1];
        }

        if(decodThread.samplesList.size()<100) {
            if(Decoder.mUsed == FlagVar.MIC_UP) {
                decodThread.fillSamples(data2);
            }else if(Decoder.mUsed == FlagVar.MIC_DOWN){
                decodThread.fillSamples(data1);

            }
        }
        long time2 = System.nanoTime();
//        System.out.println("nano:"+(time2-time1));

    }

    // here we process the receiver message aboust schedule information


    public Handler myHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {


                switch (msg.what) {
                    case FlagVar.MESSAGE_TDOA: {
                        StringBuilder sb = new StringBuilder();
                        sb.append("tdoa:").append(msg.arg1).append("    ").append("id num:").append(msg.arg2);
                        text.setText(sb.toString());

                        TimeStamp timeStamp = new TimeStamp(MainActivity.identity, msg.arg1);
                        //okSocket.sendTimeStamp(timeStamp);

                        break;
                    }

                    case FlagVar.MESSAGE_DIFF: {
                        StringBuilder sb = new StringBuilder();
                        sb.append("diff:").append((Long)msg.obj);
                        text2.setText(sb.toString());
                        break;
                    }
                    case FlagVar.MESSAGE_JSON: {
                        String str = (String) (msg.obj);
                        testCaptureBeaconMessage = str;
                        try {
                            bioClient.send(str);
                            Log.e(TAG, str);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        CapturedBeaconMessage cbMsg = null;
                        try {
                            cbMsg = JSONUtils.decodeCapturedBeaconMessage(str);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        String showTxt = String.format(
                                        "selfAnchorId:    %n" +
                                        "capturedAnchorId:%n" +
                                        "capturedSequence:%n" +
                                        "preambleIndex:   %n" +
                                        "looperCounter:   %n" +
                                        "beconCnt:        %n" +
                                        "errCount:        %n" +
                                        "speed:           ");
                        String showTxt1 = String.format("%d%n" +
                                        "%d%n" +
                                        "%d%n" +
                                        "%d%n" +
                                        "%d%n" +
                                        "%d%n" +
                                        "%d%n" +
                                        "%d cm/s",
                                cbMsg.selfAnchorId, cbMsg.capturedAnchorId, cbMsg.capturedSequence, cbMsg.preambleIndex, cbMsg.looperCounter,
                                decodThread.getBeconCnt(),decodThread.getErrorCnt(),(int)cbMsg.speed);
                        text.setText(showTxt);
                        text1.setText(showTxt1);
                        break;
                    }
                    case FlagVar.MESSAGE_RATIO:{
                        String str = (String)msg.obj;
                        text3.setText(str);
                        break;
                    }
                    case FlagVar.MESSAGE_SPEED:{
                        SpeedInfo info = (SpeedInfo)msg.obj;
                        speedInfos.add(info);
                        while (speedInfos.size() > 13){
                            speedInfos.remove(0);
                        }
                        switch (text4ShowMode){
                            case 0:{
                                text4.setText(SpeedInfos2BriefStr(speedInfos));
                                break;
                            }
                            case 1:{
                                text4.setText(SpeedInfos2DetailStr(speedInfos,false));
                                break;
                            }
                            case 2:{
                                text4.setText(SpeedInfos2DetailStr(speedInfos,true));
                                break;
                            }
                        }

                        break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

    };

    private String SpeedInfos2BriefStr(List<SpeedInfo> infos){
        SpeedInfo info = infos.get(infos.size()-1);
        return SpeedInfo2Str(info,false);

    }

    private String SpeedInfos2DetailStr(List<SpeedInfo> infos, boolean isValid){
        StringBuilder sb = new StringBuilder();
        for(SpeedInfo info:infos){
            sb.append(SpeedInfo2Str(info,isValid)).append("\n");
        }
        return sb.toString();
    }


    private String SpeedInfo2Str(SpeedInfo info, boolean isValid){
        StringBuilder sb = new StringBuilder();
        if(isValid == false) {
            for (int i = 0; i < info.speeds.length; i++) {
                String str = decimalFormat.format(info.speeds[i]);
                sb.append("   \t").append(str);

                if (info.isValid[i] == true) {
                    sb.append(" (y)");
                } else {
                    sb.append(" (n)");
                }

            }
        }else{
            for (int i = 0; i < info.validSpeeds.length; i++) {
                String str = decimalFormat.format(info.validSpeeds[i]);
                sb.append("   \t").append(str);
            }
        }

        String str = decimalFormat.format(info.speed);
        sb.append("   \t:").append(str);
        return sb.toString();
    }


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

    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK ) {
            finish();
            System.exit(0);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu); //通过getMenuInflater()方法得到MenuInflater对象，再调用它的inflate()方法就可以给当前活动创建菜单了，第一个参数：用于指定我们通过哪一个资源文件来创建菜单；第二个参数：用于指定我们的菜单项将添加到哪一个Menu对象当中。
        return true; // true：允许创建的菜单显示出来，false：创建的菜单将无法显示。
    }

    /**
     *菜单的点击事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.id_settings:
                startActivityForResult(new Intent(MainActivity.this, SettingActivity.class), 1);
                break;
            default:
                break;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String result = data.getExtras().getString("result");//得到新Activity 关闭后返回的数据
        Decoder.rThreshold = data.getIntExtra("ratio",(int)FlagVar.ratioThreshold)*1.0f/10;
        Decoder.marThreshold = data.getIntExtra("maxAvgRatio",(int)FlagVar.maxAvgRatioThreshold)*1.0f/10;;
        Decoder.mUsed = data.getIntExtra("micUsed",FlagVar.micUsed);
        Decoder.pdType = data.getIntExtra("preambleDetectionType",FlagVar.micUsed);
        Decoder.sOffset = data.getIntExtra("speedOffset",FlagVar.speedOffset);
        System.out.println("rThreshold:"+Decoder.rThreshold+"  marThreshold:"+Decoder.marThreshold);
        System.out.println(result);
    }

    private void loadSettings(){
        SharedPreferences sharedPreferences = getSharedPreferences(runningSettingsStr, Context.MODE_PRIVATE);
        Decoder.mUsed = sharedPreferences.getInt("micUsed",FlagVar.micUsed);
        Decoder.pdType = sharedPreferences.getInt("preambleDetectionType",FlagVar.preambleDetectionType);
        Decoder.marThreshold = sharedPreferences.getInt("maxAvgRatio",(int)(FlagVar.maxAvgRatioThreshold*10))*1.0f/10;
        Decoder.rThreshold = sharedPreferences.getInt("ratio",(int)(FlagVar.ratioThreshold*10))*1.0f/10;
        Decoder.sOffset = sharedPreferences.getInt("speedOffset",FlagVar.speedOffset);
    }

}
