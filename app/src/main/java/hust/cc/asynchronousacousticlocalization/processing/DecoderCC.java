package hust.cc.asynchronousacousticlocalization.processing;

import java.util.Arrays;

import hust.cc.asynchronousacousticlocalization.physical.SignalGenerator;
import hust.cc.asynchronousacousticlocalization.utils.FlagVar;

public class DecoderCC implements FlagVar{

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
    private double[] upPreambleFFTComplex;
    private double[] downPreambleFFTComplex;
    private double[][] upSymbolFFTsComplex;
    private double[][] downSymbolFFTsComplex;

    private double[] preambleCorrResultsMagnitude;
    private double[] preambleCorrResultsComplex;
    private double[] symbolCorrResultsMagnitude;
    private double[] symbolCorrResultsComplex;
    private double[] preambleCalBufferComplex;
    private double[] symbolCalBufferComplex;

    private int preambleCorrLen;
    private int symbolCorrLen;

    private Spectrum forPreambleFFT = null;
    private Spectrum forSymbolFFT = null;

    public static float rThreshold = ratioThreshold;

    protected void initParam(int processBufferSize){
        preambleCorrLen = getCorrelationLength(processBufferSize, LPreamble);
        symbolCorrLen = getCorrelationLength(LSymbol, LSymbol);
        //compute the fft of the preambles and symbols to reduce the computation cost;
        upPreambleFFTComplex = new double[preambleCorrLen];
        downPreambleFFTComplex = new double[preambleCorrLen];
        upSymbolFFTsComplex = new double[numberOfSymbols][symbolCorrLen];
        downSymbolFFTsComplex = new double[numberOfSymbols][symbolCorrLen];

        preambleCorrResultsMagnitude = new double[preambleCorrLen];
        preambleCorrResultsComplex = new double[preambleCorrLen * 2];
        symbolCorrResultsMagnitude = new double[symbolCorrLen];
        symbolCorrResultsComplex = new double[symbolCorrLen * 2];
        preambleCalBufferComplex = new double[preambleCorrLen * 2];
        symbolCalBufferComplex = new double[symbolCorrLen * 2];

        forPreambleFFT = new Spectrum(preambleCorrLen);
        forSymbolFFT = new Spectrum(symbolCorrLen);

        signalNormalization(upPreamble, upPreambleFFTComplex);
        forPreambleFFT.performFFT(upPreambleFFTComplex);
        System.arraycopy(forPreambleFFT.getOutputComplex(),0, upPreambleFFTComplex, 0 ,  upPreambleFFTComplex.length);

        for(int i = 0; i < numberOfSymbols; i++){
            signalNormalization(downSymbolSamples[i], downSymbolFFTsComplex[i]);
            forSymbolFFT.performFFT(downSymbolFFTsComplex[i]);
            System.arraycopy(forSymbolFFT.getOutputComplex(), 0, downSymbolFFTsComplex[i], 0,  downSymbolFFTsComplex.length);
        }
    }

    private int getCorrelationLength(int lengtha, int lengthb){
        int maxLength = lengtha + lengthb;
        return (int)(Math.log(maxLength) / Math.log(2) + 1);
    }

    public void signalNormalization(short[] input, double[] normalizedSignal){
        for(int i = 0; i < input.length; i++){
            normalizedSignal[i] = input[i] / 32768.0;
        }
    }

    public void xcorr(double[] inputFFTComplex, double[] referenceFFTComplex, double[] results, int size){
        int length = results.length / 2;
        for(int i = 0; i < length; i++){
            results[2*i] = inputFFTComplex[2*i] * referenceFFTComplex[2*i] + inputFFTComplex[2*i+1] * referenceFFTComplex[2*i+1];
            results[2*i + 1] = inputFFTComplex[2*i+1]*referenceFFTComplex[2*i] - inputFFTComplex[2*i]*referenceFFTComplex[2*i+1];
        }
        if(size == preambleCorrLen){
            forPreambleFFT.performIFFT(results);
        }else if(size == symbolCorrLen){
            forSymbolFFT.performIFFT(results);
        }
    }

    public CriticalPoint preambleDetection(short[] inputSamples){
        CriticalPoint criticalPoint = null;
        Arrays.fill(preambleCorrResultsMagnitude, 0);
        signalNormalization(inputSamples, preambleCorrResultsMagnitude);
        forPreambleFFT.performFFT(preambleCorrResultsMagnitude);
        System.arraycopy(forPreambleFFT.getOutputComplex(), 0 ,preambleCalBufferComplex, 0, preambleCorrLen * 2);
        xcorr(preambleCalBufferComplex, upPreambleFFTComplex, preambleCorrResultsMagnitude, preambleCorrLen);
        criticalPoint = Algorithm.getCriticalPoint(preambleCorrResultsMagnitude, 0, preambleCorrResultsMagnitude.length);
        criticalPoint.isReferenceSignalExist = false;
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
        if(ratio > maxAvgRatioThreshold){
            criticalPoint.isReferenceSignalExist = true;
        }
        return criticalPoint;
    }

    public int symbolDecoding(short[] inputSamples){
        int decodedMessage = 0;
        Arrays.fill(symbolCorrResultsMagnitude, 0);
        signalNormalization(inputSamples, symbolCorrResultsMagnitude);
        forSymbolFFT.performFFT(symbolCorrResultsMagnitude);
        System.arraycopy(forSymbolFFT.getOutputComplex(), 0, symbolCalBufferComplex, 0,  symbolCorrLen);
        double average = 0;
        double max = 0;
        double[] ratioRecords = new double[numberOfSymbols];
        for(int i = 0; i < numberOfSymbols; i++){
            xcorr(symbolCalBufferComplex, downSymbolFFTsComplex[i], symbolCorrResultsMagnitude, symbolCorrLen);
            average = Algorithm.meanValue(symbolCorrResultsMagnitude, 0, symbolCorrResultsMagnitude.length);
            max = Algorithm.getMax(symbolCorrResultsMagnitude, 0, symbolCorrResultsMagnitude.length);
            ratioRecords[i] = max*max/average;
        }
        for(int i = 1; i < numberOfSymbols; i++){
            if(ratioRecords[decodedMessage] < ratioRecords[i]){
                decodedMessage = i;
            }
        }
        return decodedMessage;
    }

}
