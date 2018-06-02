package hust.cc.asynchronousacousticlocalization.utils;

public class JniUtils {
    static {
        System.loadLibrary("JniUtils");

    }
    public static native String sayHello();

    public static native float[] realForward(float[] data, int len);

    public static native float[] xcorr(float[] data1,float[] data2);

//    public static native void realForward(double[] data);
}
