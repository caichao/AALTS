package hust.cc.asynchronousacousticlocalization.utils;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import hust.cc.asynchronousacousticlocalization.physical.AudioRecorder;

public class OKSocket extends Thread implements IOkSocket {

    private Socket mSocket = null;
    private PrintStream out = null;
    BufferedReader br = null;
    private boolean isInitializationFinished = false;
    private boolean isSocketInitOk = false;
    private char[] receivedMessage = null;

    private static OKSocket instance = new OKSocket();
    private OKSocket(){};

    public static OKSocket getInstance() {
        return instance;
    }

    private OKSocket.Callback mSocketCallback;

    @Override
    public void run() {
        super.run();

        while (isSocketInitOk){
            try {
                int len = br.read(receivedMessage);
                JSONObject jsonObject = null;

                // TODO: transform the received char characters into json object

                mSocketCallback.onReceiveSocketMsg(jsonObject);
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
            }catch (Exception e){
                e.printStackTrace();
            }
        }
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
            try {
                //TODO here: implement how to transform the received message into json object

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
