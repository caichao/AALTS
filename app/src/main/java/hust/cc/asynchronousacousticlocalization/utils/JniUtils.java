package hust.cc.asynchronousacousticlocalization.utils;

public class JniUtils {
    static {
        System.loadLibrary("JniUtils");

    }
    public static native String sayHello();

    public static native void realForward(float[] data, int len, float[] output);

    public static native void xcorr(float[] data1,float[] data2, float[] output);

//    public static native void realForward(double[] data);
}
