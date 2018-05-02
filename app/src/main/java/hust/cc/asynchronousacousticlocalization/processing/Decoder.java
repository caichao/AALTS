package hust.cc.asynchronousacousticlocalization.processing;

import hust.cc.asynchronousacousticlocalization.physical.SignalGenerator;
import hust.cc.asynchronousacousticlocalization.utils.FlagVar;

public class Decoder implements FlagVar{

    public short[] upPreamble = SignalGenerator.upChirpGenerator(FlagVar.Fs, FlagVar.TPreamble, FlagVar.BPreamble, FlagVar.Fmin);
    public short[] downPreamble = SignalGenerator.downChirpGenerator(FlagVar.Fs, FlagVar.TPreamble, FlagVar.BPreamble, FlagVar.Fmax);

    public short[][] upSymbolSamples = new short[][]{SignalGenerator.upChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FUpSymbol[0]),
            SignalGenerator.upChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FUpSymbol[1]),
            SignalGenerator.upChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FUpSymbol[2]),
            SignalGenerator.upChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FUpSymbol[3])
    };
    public short[][] downSymbolSamples = new short[][]{SignalGenerator.downChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FDownSymbol[0]),
            SignalGenerator.downChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FDownSymbol[1]),
            SignalGenerator.downChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FDownSymbol[2]),
            SignalGenerator.downChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FDownSymbol[3])
    };

    // create variables to store the samples in case frequent new and return

    public short[] bufferedSamples = new short[processBufferSize];
    public float[] normalizedSamples = new float[processBufferSize];

    /**
     * refresh the buffer area
     */
    public void normalizeBufferSamples(){
        for(int i = 0 ; i < processBufferSize ; i++){
            normalizedSamples[i] = bufferedSamples[i] / 32768.0f;
        }
    }

    /**
     * normalize the short data to float array
     * @param s : data stream of short samples
     * @return normalized data in float format
     */
    public float[] normalization(short s[]){
        float[] normalized = new float[s.length];
        for (int i = 0; i < s.length; i++) {
            normalized[i] = (float) (s[i]) / 32768;
        }
        return normalized;
    }

    /**
     * correlation results, return both the max value and its index
     * @param s: audio samples
     * @param reference: reference signal
     * @return: return the max value and its index
     */
    public IndexMaxVarInfo xcorr(float s[], float[] reference){
        IndexMaxVarInfo indexMaxVarInfo = new IndexMaxVarInfo();

        // TODO here, implementation of normaized xcorr
        // TODO : should call xccorrBasic
        return indexMaxVarInfo;
    }

    /**
     * correlation results, return both the max value and its index
     * @param s: audio samples
     * @param reference: reference signal
     * @return: return the max value and its index
     */
    public IndexMaxVarInfo xcorrFast(short s[], short[] reference){
        IndexMaxVarInfo indexMaxVarInfo = new IndexMaxVarInfo();

        // TODO here: implementation of xcorr details
        // TODO : should call xcorrFastBasic
        return indexMaxVarInfo;
    }

    /**
     *  correlation results with selective range
     * @param s - input samples
     * @param low - the low index corresponds to the samples
     * @param high - the high index corresponds to the samples
     * @param reference - reference signal
     * @return correlation results with maximum value and its index
     */
    public IndexMaxVarInfo xcorrFast(short s[], int low, int high, short[] reference){
        IndexMaxVarInfo indexMaxVarInfo = new IndexMaxVarInfo();

        // TODO here: implementation of xcorr details

        return indexMaxVarInfo;
    }


    public short[] xcorrBasic(float s[], int low, int high, float[] reference){

        short[] samples = new short[1];

        // TODO here: implementation of xcorr details

        return samples;
    }

    public short[] xcorrFastBasic(short s[], int low, int high, short[] reference){

        short[] samples = new short[1];

        // TODO here: implementation of xcorr details

        return samples;
    }

    /**
     * detect whether the preamble exist
     * @param s - input sample signal
     * @param reference - reference signal, can be either up and down preamble
     * @return true indicate the presence of the corresponding preamble and vise versa
     */
    public IndexMaxVarInfo preambleDetection(short[] s, short[] reference){
        IndexMaxVarInfo indexMaxVarInfo = xcorrFast(s, reference);
        indexMaxVarInfo.isReferenceSignalExist = false;
        if(indexMaxVarInfo.maxVar > FlagVar.preambleDetectionThreshold) {
            // use the ratio of peak value to the mean value of its previous 200 samples
            int startIndex = indexMaxVarInfo.index - numberOfPreviousSamples;
            if(startIndex < 0) startIndex = 0;  // in case the preamble exist in the head of the buffer
            float ratio = indexMaxVarInfo.maxVar / Algorithm.meanValue(s, startIndex, indexMaxVarInfo.index);
            if(ratio > ratioThreshold)
                indexMaxVarInfo.isReferenceSignalExist = true;
        }
        return indexMaxVarInfo;
    }

    /**
     * decode the anchor ID
     * @param s - the audio samples
     * @param p - the decoded preamble information
     * @param isUpSymbol - indicate whether we use up or down symbol to decode the anchor ID
     * @return the decoded anchor ID
     */
    public int decodeAnchorID(short[] s, boolean isUpSymbol, IndexMaxVarInfo p){
        int startIndex = p.index + FlagVar.preambleLength + FlagVar.guardIntervalLength;
        int endIndex = startIndex + FlagVar.symbolLength - 1;

        float[] maxRatios = new float[numberOfSymbols];
        short[] correlationResult = null;
        float max = 0;
        int mean = 0;
        // use the max/mean ratio as the indicator for symbol decoding
        if(isUpSymbol) {
            for (int i = 0; i < numberOfSymbols; i++) {
                correlationResult = xcorrFastBasic(s, startIndex, endIndex, upSymbolSamples[i]);
                max = Algorithm.getMaxInfo(correlationResult, 0, correlationResult.length).maxVar;
                mean = Algorithm.meanValue(correlationResult, 0, correlationResult.length);
                maxRatios[i] = max / mean;
            }
        }else{
            for (int i = 0; i < numberOfSymbols; i++) {
                correlationResult = xcorrFastBasic(s, startIndex, endIndex, downSymbolSamples[i]);
                max = Algorithm.getMaxInfo(correlationResult, 0, correlationResult.length).maxVar;
                mean = Algorithm.meanValue(correlationResult, 0, correlationResult.length);
                maxRatios[i] = max / mean;
            }
        }

        int decodeID = 0;
        max = maxRatios[0];
        for (int i = 1 ; i < numberOfSymbols ; i++){
            if(maxRatios[i] > max)
                decodeID = i;
        }
        return decodeID;
    }

}
