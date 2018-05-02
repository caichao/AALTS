package hust.cc.asynchronousacousticlocalization.processing;

import org.jtransforms.fft.DoubleFFT_1D;
import org.jtransforms.fft.FloatFFT_1D;

/**
 * Created by e440 on 2018/4/27.
 */
public class TestFFT {


    /**
     * date1 is received signal ,data2 is the reference signal, the method is used to calculate the correlation.
     * @auther Ruinan Jin
     * @param data1
     * @param data2
     * @return correlation array
     */
    public static double[] corrDouble(double[] data1, double[] data2){;
        int len = (int)Math.ceil((float)(data1.length+data2.length)/2)*2;
        double[] hData1 = new double[len];
        double[] hData2 = new double[len];
        for(int i=0;i<len;i++){
            if(i<data1.length){
                hData1[i] = data1[i];
            }else{
                hData1[i] = 0;
            }
            if(i<data2.length){
                hData2[i] = data2[i];
            }else{
                hData2[i] = 0;
            }
        }
        double[] result = new double[len];
        double[] corr = new double[data1.length+data2.length-1];
        DoubleFFT_1D fft = new DoubleFFT_1D(len);
        fft.realForward(hData1);
        fft.realForward(hData2);

        result[0] = hData1[0] * hData2[0]; // value at f=0Hz is real-valued
        result[1] = hData1[1] * hData2[1]; // value at f=fs/2 is real-valued and packed at index 1
        for (int i = 1 ; i < result.length / 2 ; ++i) {
            double a = hData1[2*i];
            double b = hData1[2*i + 1];
            double c = hData2[2*i];
            double d = hData2[2*i + 1];

            result[2*i]     = a*c + b*d;
            result[2*i + 1] = b*c - a*d;
        }
        fft.realInverse(result, true);
        for(int i=0;i<corr.length;i++){
            corr[i] = Math.abs(result[i]);
        }

        return corr;
    }

    /**
     * date1 is received signal ,data2 is the reference signal, the method is used to calculate the correlation.
     * @auther Ruinan Jin
     * @param data1
     * @param data2
     * @return correlation array
     */
    public static float[] corrFloat(float[] data1, float[] data2 ){
        int len = (int)Math.ceil((float)(data1.length+data2.length)/2)*2;
        float[] hData1 = new float[len];
        float[] hData2 = new float[len];
        for(int i=0;i<len;i++){
            if(i<data1.length){
                hData1[i] = data1[i];
            }else{
                hData1[i] = 0;
            }
            if(i<data2.length){
                hData2[i] = data2[i];
            }else{
                hData2[i] = 0;
            }
        }
        float[] result = new float[len];
        float[] corr = new float[data1.length+data2.length-1];
        FloatFFT_1D fft = new FloatFFT_1D(len);
        fft.realForward(hData1);
        fft.realForward(hData2);

        result[0] = hData1[0] * hData2[0]; // value at f=0Hz is real-valued
        result[1] = hData1[1] * hData2[1]; // value at f=fs/2 is real-valued and packed at index 1
        for (int i = 1 ; i < result.length / 2 ; ++i) {
            float a = hData1[2*i];
            float b = hData1[2*i + 1];
            float c = hData2[2*i];
            float d = hData2[2*i + 1];

            result[2*i]     = a*c + b*d;
            result[2*i + 1] = b*c - a*d;
        }
        fft.realInverse(result, true);
        for(int i=0;i<corr.length;i++){
            corr[i] = Math.abs(result[i]);
        }

        return corr;

    }

    /** corr is the correlation array, chirpLength is the chirp signal's length. return the postion of the max correlation.
     * @auther Ruinan Jin
     * @param corr
     * @param chirpLength
     * @return
     */
    public int getMaxPosFromCorrFloat(float [] corr, int chirpLength){
        float max = 0;
        int index = 0;
        for(int i=0;i<corr.length;i++){
            if(corr[i]>max){
                max = corr[i];
                index = i;
            }
        }
        if(index+chirpLength>corr.length){
            index = index-corr.length;
        }
        return index;
    }
}
