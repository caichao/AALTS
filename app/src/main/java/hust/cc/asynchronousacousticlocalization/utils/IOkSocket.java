package hust.cc.asynchronousacousticlocalization.utils;

public interface IOkSocket {
    // indicate whether the send command is completed
    public void onSendComplete();
    // receive command from the server
    public void onRecieve();
    // send command to Server
    public void send();
}
