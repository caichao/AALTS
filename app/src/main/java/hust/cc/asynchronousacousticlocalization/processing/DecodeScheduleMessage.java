package hust.cc.asynchronousacousticlocalization.processing;

import android.util.Log;

import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

import hust.cc.asynchronousacousticlocalization.activity.MainActivity;
import hust.cc.asynchronousacousticlocalization.utils.FlagVar;

public class DecodeScheduleMessage extends Thread implements Subject{

    // declare single instance
    private static final DecodeScheduleMessage instance = new DecodeScheduleMessage();

    private DecodeScheduleMessage(){}

    public static DecodeScheduleMessage getInstance(){
        return instance;
    }

    private final int portNum = 12000;
    private byte[] buffer = null;
    private int bufferLength = 1024;
    private String recvMsg = null;
    private JSONObject jsonObject = null;
    private boolean isThreadAlive = true;
    private final String TAG = DecodeScheduleMessage.currentThread().getName();
    @Override
    public void run() {
        super.run();

        try{
            DatagramSocket datagramSocket = new DatagramSocket(portNum);
            buffer = new byte[bufferLength];
            DatagramPacket packet = new DatagramPacket(buffer, bufferLength);

            while (isThreadAlive){
                for(int i = 0; i < bufferLength; i++){
                    buffer[i] = 0;
                }
                datagramSocket.receive(packet);
                recvMsg = new String(packet.getData()).trim();
                decodeScheduleMessage(recvMsg);
                Log.e(TAG, recvMsg);
            }

            datagramSocket.close();
            datagramSocket = null;
            packet = null;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * decode the message based on the received json string
     * @param s
     * @throws Exception
     */
    public void decodeScheduleMessage(String s) throws Exception{
        jsonObject = new JSONObject(s);
        int parity = jsonObject.getInt("parity");
        if(parity == MainActivity.identity % 2){
            // obtain if we use up or down chirp signal
            String cmd = (String)jsonObject.get(MainActivity.identity + "");
            if(cmd.equals("up")){
                Log.e(TAG, "transmit up chirp");
                setNotifyMessage("up");
            }else if(cmd.equals("down")){
                Log.e(TAG, "transmit down chirp");
                setNotifyMessage("down");
            }
            // notify the user with message

        }
    }

    public void close(){
        isThreadAlive = false;
    }

    /**
     * the following part are passively parts to notify users
     */

    private List<Observer> observerList = new ArrayList<>();
    private String notifyMessage = "";
    @Override
    public void addObserver(Observer observer) {
        observerList.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        int index = observerList.indexOf(observer);
        if(index >= 0)
            observerList.remove(index);
    }

    @Override
    public void notifyObserver() {
        for(Observer observer : observerList){
            observer.updata(notifyMessage);
        }
    }

    public void setNotifyMessage(String msg){
        this.notifyMessage = msg;
        notifyObserver();
    }
}
