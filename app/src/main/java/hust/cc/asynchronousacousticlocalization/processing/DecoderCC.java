package hust.cc.asynchronousacousticlocalization.processing;

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
    public float[] upPreambleFFT;
    public float[] downPreambleFFT;
    public float[][] upSymbolFFTs;
    public float[][] downSymbolFFTs;

    private int preambleCorrLen;
    private int symbolCorrLen;
    private int recordBufferSize = 4096;
    private int processBufferSize = recordBufferSize + recordBufferSize / 2;

    public static float rThreshold = ratioThreshold;

    private void initParam(){
        preambleCorrLen = getCorrelationLength(processBufferSize, LPreamble);
        symbolCorrLen = getCorrelationLength(LSymbol, LSymbol);
        //compute the fft of the preambles and symbols to reduce the computation cost;
        upPreambleFFT = new float[preambleCorrLen];
        downPreambleFFT = new float[preambleCorrLen];
        upSymbolFFTs = new float[numberOfSymbols][symbolCorrLen];
        downSymbolFFTs = new float[numberOfSymbols][symbolCorrLen];
        for (int i = 1; i < numberOfSymbols; i++) {
            upSymbolFFTs[i] = new float[symbolCorrLen];
            downSymbolFFTs[i] = new float[symbolCorrLen];
        }
    }

    private int getCorrelationLength(int lengtha, int lengthb){
        int maxLength = lengtha + lengthb;
        return (int)(Math.log(maxLength) / Math.log(2) + 1);
    }
}
