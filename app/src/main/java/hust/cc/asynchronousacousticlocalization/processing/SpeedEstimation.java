package hust.cc.asynchronousacousticlocalization.processing;

import java.util.Arrays;

import hust.cc.asynchronousacousticlocalization.utils.FlagVar;

public class SpeedEstimation implements FlagVar, Runnable{

    private short[] data = null;
    private double[] normalizedData = null;
    private double[] speedFFT = null;
    private int checkFreqLow = 16900;
    private int checkFreqHigh = 18000;
    private volatile boolean speedEstimationRun = true;
    private volatile boolean isDataReady = false;

    private Spectrum spectrum = null;

    public SpeedEstimation(){
        init();
    }

    private void init(){
        data = new short[beconMessageLength];
        normalizedData = new double[beconMessageLength];
        spectrum = new Spectrum(FlagVar.Fs);
        speedFFT = new double[checkFreqHigh - checkFreqLow];
    }

    public void setSamples(short[] samples, int low, int high){
        Arrays.fill(data, (short) 0);
        System.arraycopy(samples, low, data, 0, high - low + 1);
        isDataReady = true;
    }

    private double getSpeed(){
        double speed = 0;
        Algorithm.signalNormalization(this.data, this.normalizedData);
        spectrum.performFFT(this.normalizedData);
        System.arraycopy(spectrum.getOutputMagnitude(), checkFreqLow, speedFFT, 0, checkFreqHigh - checkFreqLow + 1);

        return speed;
    }

    @Override
    public void run() {
        while (speedEstimationRun){
            if(isDataReady){
                isDataReady = false;


            }
        }
    }

    public void close(){
        speedEstimationRun = false;
    }
}
