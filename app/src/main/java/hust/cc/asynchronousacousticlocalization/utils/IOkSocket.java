package hust.cc.asynchronousacousticlocalization.utils;

public interface IOkSocket {
    // configure the socket like IP address or the port number
    public void init(String IP, int port);
    // establish connection with the remote server
    public void connect();
    // shutdown the connection
    public void close();
}
