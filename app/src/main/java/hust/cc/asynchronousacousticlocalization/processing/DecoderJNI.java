package hust.cc.asynchronousacousticlocalization.processing;

import android.util.Log;

import java.util.Arrays;

import hust.cc.asynchronousacousticlocalization.physical.SignalGenerator;
import hust.cc.asynchronousacousticlocalization.utils.FileUtils;
import hust.cc.asynchronousacousticlocalization.utils.FlagVar;
import hust.cc.asynchronousacousticlocalization.utils.JniUtils;

public class DecoderJNI implements FlagVar{

    public static short[] upPreamble = SignalGenerator.upChirpGenerator(FlagVar.Fs, FlagVar.TPreamble, FlagVar.BPreamble, FlagVar.Fmin);
    public static short[] downPreamble = SignalGenerator.downChirpGenerator(FlagVar.Fs, FlagVar.TPreamble, FlagVar.BPreamble, FlagVar.Fmax);

    public static short[][] upSymbolSamples = new short[][]{SignalGenerator.upChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FUpSymbol[0]),
            SignalGenerator.upChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FUpSymbol[1]),
            SignalGenerator.upChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FUpSymbol[2]),
            SignalGenerator.upChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FUpSymbol[3])
    };
    public static short[][] downSymbolSamples = new short[][]{SignalGenerator.downChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FDownSymbol[0]),
            SignalGenerator.downChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FDownSymbol[1]),
            SignalGenerator.downChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FDownSymbol[2]),
            SignalGenerator.downChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FDownSymbol[3])
    };

    //we calculate the fft before decoding in order to reduce computation time.
    private float[] upPreambleFFTComplex;
    private float[] downPreambleFFTComplex;
    private float[][] upSymbolFFTsComplex;
    private float[][] downSymbolFFTsComplex;

    private float[] preambleCorrResultsMagnitude;
    private float[] preambleCorrResultsComplex;
    private float[] symbolCorrResultsMagnitude;
    private float[] symbolCorrResultsComplex;
    private float[] preambleCalBufferComplex;
    private float[] symbolCalBufferComplex;

    private int preambleCorrLen;
    private int symbolCorrLen;

    private Spectrum forPreambleFFT = null;
    private Spectrum forSymbolFFT = null;

    private String TAG = "[DecoderCC]";

    public static float rThreshold = ratioThreshold;

    protected void initParam(int processBufferSize){
        preambleCorrLen = getCorrelationLength(processBufferSize, LPreamble);
        symbolCorrLen = getCorrelationLength(LSymbol, LSymbol);

        //compute the fft of the preambles and symbols to reduce the computation cost;
        upPreambleFFTComplex = new float[preambleCorrLen*2];
        downPreambleFFTComplex = new float[preambleCorrLen*2];
        upSymbolFFTsComplex = new float[numberOfSymbols][symbolCorrLen*2];
        downSymbolFFTsComplex = new float[numberOfSymbols][symbolCorrLen*2];

        preambleCorrResultsMagnitude = new float[preambleCorrLen];
        preambleCorrResultsComplex = new float[preambleCorrLen * 2];
        symbolCorrResultsMagnitude = new float[symbolCorrLen];
        symbolCorrResultsComplex = new float[symbolCorrLen * 2];
        preambleCalBufferComplex = new float[preambleCorrLen * 2];
        symbolCalBufferComplex = new float[symbolCorrLen * 2];

        forPreambleFFT = new Spectrum(preambleCorrLen);
        forSymbolFFT = new Spectrum(symbolCorrLen);

        signalNormalization(upPreamble, upPreambleFFTComplex);
        upPreambleFFTComplex = JniUtils.fft(upPreambleFFTComplex, preambleCorrLen);
//        forPreambleFFT.performFFT(upPreambleFFTComplex);
//        System.arraycopy(forPreambleFFT.getOutputComplex(),0, upPreambleFFTComplex, 0 ,  preambleCorrLen*2);
//        FileUtils.saveBytes(forPreambleFFT.getOutputComplex(), "FFT_complex");
//        FileUtils.saveBytes(forPreambleFFT.getOutputMagnitude(), "");
        for(int i = 0; i < numberOfSymbols; i++){
            Arrays.fill(symbolCalBufferComplex, 0);
            signalNormalization(downSymbolSamples[i], symbolCalBufferComplex);
//            forSymbolFFT.performFFT(downSymbolFFTsComplex[i]);
//            System.arraycopy(forSymbolFFT.getOutputComplex(), 0, downSymbolFFTsComplex[i], 0,  symbolCorrLen*2);
            downSymbolFFTsComplex[i] = JniUtils.fft(symbolCalBufferComplex, symbolCorrLen);
        }

    }

    public int getPreambleCorrLen(){
        return preambleCorrLen;
    }

    public int getSymbolCorrLen(){
        return symbolCorrLen;
    }

    private int getCorrelationLength(int lengtha, int lengthb){
        int maxLength = lengtha + lengthb;
        int N = (int)(Math.log(maxLength) / Math.log(2) + 1);
        return (int)(Math.pow(2, N));
    }

    public void signalNormalization(short[] input, float[] normalizedSignal){
        for(int i = 0; i < input.length; i++){
            normalizedSignal[i] = input[i] / 32768.0f;
        }
    }

    public CriticalPoint preambleDetection(short[] inputSamples){
        CriticalPoint criticalPoint = null;
        Arrays.fill(preambleCorrResultsMagnitude, 0);
        signalNormalization(inputSamples, preambleCorrResultsMagnitude);
        //Log.e(TAG, "Noramlized max = " + Algorithm.getMax(preambleCorrResultsMagnitude, 0, preambleCorrResultsMagnitude.length));
        preambleCalBufferComplex = JniUtils.fft(preambleCorrResultsMagnitude, preambleCorrLen);
        preambleCorrResultsMagnitude = JniUtils.xcorr(preambleCalBufferComplex, upPreambleFFTComplex);
//        forPreambleFFT.performFFT(preambleCorrResultsMagnitude);
//        System.arraycopy(forPreambleFFT.getOutputComplex(), 0 ,preambleCalBufferComplex, 0, preambleCorrLen * 2);
//        xcorr(preambleCalBufferComplex, upPreambleFFTComplex, preambleCorrResultsComplex, preambleCorrLen);
//        Algorithm.getMagnitude(preambleCorrResultsMagnitude, preambleCorrResultsComplex);
        //Log.e(TAG, "FFT max = " + Algorithm.getMax(forPreambleFFT.getOutputMagnitude(), 0, preambleCorrResultsMagnitude.length));
        criticalPoint = Algorithm.getCriticalPoint(preambleCorrResultsMagnitude, 0, preambleCorrResultsMagnitude.length);
        criticalPoint.isReferenceSignalExist = false;
//        if(criticalPoint.index > preambleCorrLen / 2){ // if this situation happens, the preamble must be detected at last time
            //criticalPoint.index = criticalPoint.index - preambleCorrLen / 2;
//            return criticalPoint;
//            Log.e(TAG, criticalPoint.toString());
//
//            if(criticalPoint.peak > 100){
//                FileUtils.saveStringMessage(criticalPoint.toString(), "abnormal_undetected_criticalPoint");
//                FileUtils.saveBytes(inputSamples, "abnormal_undetected");
//                Log.e(TAG, "over");
//                while(true);
//            }
//        }
        if(criticalPoint.peak < naiveThreshold){
            criticalPoint.isReferenceSignalExist = false;
            return criticalPoint;
        }
        int startIndex = criticalPoint.index - 200;
        if(startIndex < 0){
            startIndex = 0;
        }
        double mean = Algorithm.meanValue(preambleCorrResultsMagnitude, startIndex, criticalPoint.index);
        double ratio = criticalPoint.peak / mean;
        criticalPoint.ratio = ratio;

        // found the error, when the peak is very high and the ratio is very low, this means that multipath is very severe, we must handle it
//        if(criticalPoint.peak > 50 && ratio > 6 && ratio < maxAvgRatioThreshold){
//            FileUtils.saveBytes(inputSamples, "undetected");
//            FileUtils.saveStringMessage(criticalPoint.toString(), "criticalPoint");
//            while (true);
//        }
        // peak refinedment here
        IndexMaxVarInfo indexMaxVarInfo = Algorithm.peakRefinement(preambleCorrResultsMagnitude, criticalPoint.index);
        if(indexMaxVarInfo != null){ // refine the peak value by our normalization method
            criticalPoint.index = criticalPoint.index - indexMaxVarInfo.index;
            criticalPoint.ratio = indexMaxVarInfo.fitVal;
        }

        if(ratio > maxAvgRatioThreshold && ratio < upLimitThreshold){
            criticalPoint.isReferenceSignalExist = true;
        }
        return criticalPoint;
    }

    public int symbolDecoding(short[] samples){
        int decoded = 0;
        Arrays.fill(symbolCalBufferComplex, 0);
        signalNormalization(samples, symbolCalBufferComplex);
        symbolCalBufferComplex = JniUtils.fft(symbolCalBufferComplex, symbolCorrLen);
        float max = 0;
        float mean = 0;
        float []records = new float[numberOfSymbols];
        for(int i = 0; i < numberOfSymbols; i++){
            symbolCorrResultsMagnitude = JniUtils.xcorr(symbolCalBufferComplex, downSymbolFFTsComplex[i]);
            max = Algorithm.getMax(symbolCorrResultsMagnitude, 0, symbolCorrLen);
            mean = Algorithm.meanValue(symbolCorrResultsMagnitude, 0, symbolCorrLen);
            records[i] = max * max / mean;
        }
        max = records[0];
        for(int j = 1; j < numberOfSymbols; j++){
            if(records[j] > max){
                max = records[j];
                decoded = j;
            }
        }
        return decoded;
    }

}
