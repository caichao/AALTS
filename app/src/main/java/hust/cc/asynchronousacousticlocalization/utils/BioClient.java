package hust.cc.asynchronousacousticlocalization.utils;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class BioClient implements Runnable{

    private String ip;
    private int port;
    private List messageQueue = null;
    private Socket socket = null;

    public BioClient(String ip, int port){
        this.ip = ip;
        this.port = port;
        messageQueue = new ArrayList();
    }

    public void send(String msg){
        synchronized (this){
            messageQueue.add(msg);
        }
    }

    @Override
    public void run() {
        String tmp = null;
        PrintStream printStream = null;
        try {
            socket = new Socket(this.ip, this.port);
            printStream = new PrintStream(socket.getOutputStream());
        }catch (Exception e){
            e.printStackTrace();
        }
        while (true){
            if(messageQueue.size() > 0){
                synchronized (this) {
                    tmp = (String) messageQueue.get(0);
                    messageQueue.remove(0);
                }
                try {
                    printStream.print(tmp);// can not use println here, or it will throw JSON malformat error
                    printStream.flush();
                }catch (Exception e){
                    e.printStackTrace();
                    printStream.close();
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    break;
                }
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
