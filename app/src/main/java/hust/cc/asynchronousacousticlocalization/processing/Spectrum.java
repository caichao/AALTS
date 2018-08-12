package hust.cc.asynchronousacousticlocalization.processing;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Arrays;

public class Spectrum {

    private double[] inputSignal;
    private double[] outputComplex;
    private double[] outputMagnitude;
    DoubleFFT_1D fft = null;
    private int N = 0;

    /**
     * initialize FFT with FFT size
     * @param N
     */
    public Spectrum(int N){
        this.N = N;
        fft = new DoubleFFT_1D(this.N);
        inputSignal = new double[this.N * 2];
        outputComplex = new double[this.N * 2];
        outputMagnitude = new double[this.N];
    }

    public void performFFT(double[] inputSignal){
        Arrays.fill(this.inputSignal, 0);
        System.arraycopy(inputSignal, 0, this.inputSignal, 0, inputSignal.length);
        fft.realForwardFull(this.inputSignal);
        System.arraycopy(this.inputSignal, 0, this.outputComplex, 0, this.N * 2);
        for(int i = 0; i < this.N; i++){
            outputMagnitude[i] = Math.sqrt(this.inputSignal[2*i] * this.inputSignal[2*i] + this.inputSignal[2*i+1] * this.inputSignal[2*i+1]);
        }
    }

    public double[] performIFFT(double[] inputSignal){
        Arrays.fill(this.inputSignal, 0);
        System.arraycopy(inputSignal, 0, this.inputSignal, 0, inputSignal.length);
        fft.complexInverse(this.inputSignal, true);
//        System.arraycopy(this.outputComplex, 0, this.inputSignal, 0, this.N * 2);
//        for(int i = 0; i < this.N; i++){
//            outputMagnitude[i] = Math.sqrt(this.inputSignal[2*i] * this.inputSignal[2*i] + this.inputSignal[2*i+1] * this.inputSignal[2*i+1]);
//        }
        return inputSignal;
    }

    public double[] getOutputComplex(){
        return outputComplex;
    }
    public double[] getOutputMagnitude(){
        return outputMagnitude;
    }
}
