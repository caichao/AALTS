package hust.cc.asynchronousacousticlocalization.utils;

import android.util.Log;

public class RspHandler extends Thread{
    private byte[] rsp = null;
    private int timeOut = 2000;
    private boolean isTimeOut = false;
    private boolean isThreadAlive = true;
    private final String TAG = RspHandler.class.getSimpleName();

    private RspHandler.Callback myCallback;
    public synchronized boolean handleResponse(byte[] rsp) {
        this.rsp = rsp;
        this.notify();
        return true;
    }

    public void setCallback(RspHandler.Callback callback){
        this.myCallback = callback;
    }

    public synchronized void waitForResponse() {
        while(this.rsp == null || isTimeOut) {
            try {
                this.wait();
                Thread.sleep(timeOut);
                isTimeOut = true;
            } catch (InterruptedException e) {
            }
        }
        if(!isTimeOut) {
            //System.out.println(new String(this.rsp));
            Log.e(TAG, new String(this.rsp));
            myCallback.onServerResponse(new String(this.rsp));
        }
    }

    @Override
    public void run() {
        super.run();

        while (isThreadAlive){
            waitForResponse();
            Log.e(TAG, "one message send");
        }
    }

    public synchronized void  close(){
        isThreadAlive = false;
    }

    public interface Callback{
        void onServerResponse(String msg);
    }
}
