package hust.cc.asynchronousacousticlocalization.utils;

public interface IOkSocket {
    // configure the socket like IP address or the port number
    public void init(String IP, int port);

    // shutdown the connection
    public void close();
}
