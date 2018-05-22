package hust.cc.asynchronousacousticlocalization.utils;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import hust.cc.asynchronousacousticlocalization.physical.AudioRecorder;

import static android.content.ContentValues.TAG;

public class OKSocket extends Thread implements IOkSocket {

    private Socket mSocket = null;
    private PrintStream out = null;
    BufferedReader br = null;
    private boolean isInitializationFinished = false;
    private boolean isSocketInitOk = false;
    private char[] receivedMessage = null;
    private boolean openSendLoop = false;
    private TimeStamp timeStamp = null;
    private static final String TAG = "Socket channel";
    private String ip;
    private int port;

    private static OKSocket instance = new OKSocket();
    private OKSocket(){};

    public static OKSocket getInstance() {
        return instance;
    }

    private OKSocket.Callback mSocketCallback;

    @Override
    public void run() {
        super.run();
        init(this.ip, this.port);
        while (isSocketInitOk){
            try {
                if(openSendLoop){
                    //Log.e(TAG, "send a timestamp to server");
                    openSendLoop = false;
                    send(this.timeStamp.formatMessage());
                    mSocketCallback.onReceiveSocketMsg(receive());
                }

            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    public OKSocket socketCallback(OKSocket.Callback socketCallback) {
        this.mSocketCallback = socketCallback;
        return this;
    }

    @Override
    public void init(String ip, int port) {
        if(!isInitializationFinished){
            isInitializationFinished = true;

            try {
                mSocket = new Socket(ip, port); // connect to the server
                mSocket.setSoTimeout(5000);
                out = new PrintStream(mSocket.getOutputStream());
                br = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                isSocketInitOk = true;
                receivedMessage = new char[20480];
                Log.e(TAG, "socket init ok");
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        timeStamp = new TimeStamp();
    }

    public void initSocket(String ip, int port){
        this.ip = ip;
        this.port = port;
    }

    @Override
    public void close() {
        if(mSocket != null) {
            try {
                br.close();
                out.close();
                mSocket.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void sendTimeStamp(TimeStamp timeStamp){
        //
        this.timeStamp = timeStamp;
        synchronized (this){
            openSendLoop = true;
        }
    }

    /**
     * send json object to the server
     * @param jsonObject
     */
    public void send(JSONObject jsonObject){
        if(mSocket != null){
            try{
                out.print(jsonObject.toString());
                out.flush();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public JSONObject receive(){
        JSONObject jsonObject = null;
        if(mSocket != null){
            //Log.e(TAG, "trying to parse server information");
            try {
                //TODO here: implement how to transform the received message into json object
                br.read(receivedMessage);
                //Log.e(TAG, "message from server" + len);
                jsonObject = new JSONObject(new String(receivedMessage));
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return jsonObject;
    }

    /**
     * when the client send information to the server, the server will respond with ack information
     */
    public interface Callback{
        // TODO: receive ack from the server
        public void onReceiveSocketMsg(JSONObject jsonObject);
    }

}
