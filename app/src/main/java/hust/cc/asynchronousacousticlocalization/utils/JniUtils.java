package hust.cc.asynchronousacousticlocalization.utils;

public class JniUtils {
    static {
        System.loadLibrary("JniUtils");

    }
    public static native String sayHello();
}
