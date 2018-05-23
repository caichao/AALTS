package hust.cc.asynchronousacousticlocalization.utils;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import hust.cc.asynchronousacousticlocalization.activity.MainActivity;

public class DataSendThread extends Thread{

    //private SocketChannel socketChannel = null;
    private Map rspHandlers = null;
    private Map pendingData = null;
    private Selector selector = null;
    private List pendingChanges = null;
    private InetAddress inetAddress = null;
    private int port = 0;
    private RspHandler handler;
    private byte[] data = null;
    private boolean isProcessingNow = false;

    private String TAG = "DataSendThread";

    public DataSendThread(Map rspHandlers,
                          Map pendingData,
                          Selector selector,
                          List pendingChanges,
                          InetAddress ip,
                          int port){
        //this.socketChannel = socketChannel;
        this.rspHandlers = rspHandlers;
        this.pendingData = pendingData;
        this.selector = selector;
        this.pendingChanges = pendingChanges;
        this.inetAddress = ip;
        this.port = port;

        data = new byte[1024];
    }

    public void fillData(byte[] msg, RspHandler handler){
        for(int i = 0; i < data.length; i++)
            data[i] = 0;
        System.arraycopy(msg, 0, this.data, 0, msg.length);
        this.handler = handler;
        synchronized (this){
            isProcessingNow = true;
        }
    }

    private SocketChannel initiateConnection() throws IOException {
        // Create a non-blocking socket channel
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);

        // Kick off connection establishment
        socketChannel.connect(new InetSocketAddress(this.inetAddress, this.port));

        // Queue a channel registration since the caller is not the
        // selecting thread. As part of the registration we'll register
        // an interest in connection events. These are raised when a channel
        // is ready to complete connection establishment.
        synchronized(this.pendingChanges) {
            this.pendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT));
        }

        return socketChannel;
    }
    @Override
    public void run() {
        super.run();

        while (true){
            try {
                if(isProcessingNow){
                    isProcessingNow = false;
                    SocketChannel socket = this.initiateConnection();
                    // Register the response handler
                    this.rspHandlers.put(socket, handler);

                    // And queue the data we want written
                    synchronized (this.pendingData) {
                        List queue = (List) this.pendingData.get(socket);
                        if (queue == null) {
                            queue = new ArrayList();
                            this.pendingData.put(socket, queue);
                        }
                        queue.add(ByteBuffer.wrap(data));
                    }

                    // Finally, wake up our selecting thread so it can make the required changes
                    this.selector.wakeup();
                    Log.e(TAG, "send ok");
                }


            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }
}
