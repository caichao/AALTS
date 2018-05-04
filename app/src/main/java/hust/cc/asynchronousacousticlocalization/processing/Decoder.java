package hust.cc.asynchronousacousticlocalization.processing;

import org.jtransforms.fft.FloatFFT_1D;

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

    public int processBufferSize = 9600;

    public void setProcessBufferSize(int processBufferSize){
        this.processBufferSize = processBufferSize;
        bufferedSamples = new short[processBufferSize];
        normalizedSamples = new float[processBufferSize];
    }

    public int getProcessBufferSize() {
        return processBufferSize;
    }

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
     * @param data1: audio samples
     * @param data2: reference signal
     * @return: return the max value and its index
     */

    public IndexMaxVarInfo getIndexMaxVarInfoFromFloats(float[] data1,float[] data2){
        IndexMaxVarInfo indexMaxVarInfo = new IndexMaxVarInfo();
        float[] corr = xcorr(data1,data2);
        int index = getMaxPosFromCorrFloat(corr,data2.length);
        indexMaxVarInfo.index = index;
        indexMaxVarInfo.maxVar = corr[index];

        IndexMaxVarInfo resultInfo = preambleDetection(corr,indexMaxVarInfo);
        return resultInfo;
    }




    /**
     * calculate correlation results,
     * @param data1: audio samples
     * @param data2: reference signal
     * @return: return the max value and its index
     */
    public float[] xcorr(float []data1, float[] data2){

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

    /**
     * correlation results, return both the max value and its index
     * @param s: audio samples
     * @param reference: reference signal
     * @return: return the max value and its index
     */
    public IndexMaxVarInfo getIndexMaxVarInfoFromSignals(short s[], short[] reference){
        float[] data1 = normalization(s);
        float[] data2 = normalization(reference);

        IndexMaxVarInfo indexMaxVarInfo = getIndexMaxVarInfoFromFloats(data1,data2);

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
    public IndexMaxVarInfo getIndexMaxVarInfoFromSignals(short s[], int low, int high, short[] reference){
        short[] s0 = new short[high-low+1];
        for(int i=low;i<=high;i++){
            s0[i-low] = s[i];
        }
        IndexMaxVarInfo indexMaxVarInfo = getIndexMaxVarInfoFromSignals(s0,reference);
        return indexMaxVarInfo;
    }


    public float[] xcorr(float s[], int low, int high, float[] reference){
        float[] s0 = new float[high-low+1];
        for(int i=low;i<=high;i++){
            s0[i-low] = s[i];
        }
        return xcorr(s0,reference);
    }

    public float[] xcorrSignal(short s[], int low, int high, short[] reference){
        float[] s0 = normalization(s);
        float[] r0 = normalization(reference);
        return xcorr(s0,low,high,r0);
    }

    /**
     * detect whether the preamble exist
     * @param corr - correlation array
     * @param indexMaxVarInfo - the info of the max corr index
     * @return true indicate the presence of the corresponding preamble and vise versa
     */
    public IndexMaxVarInfo preambleDetection(float[] corr, IndexMaxVarInfo indexMaxVarInfo){
        indexMaxVarInfo.isReferenceSignalExist = false;
//        if(indexMaxVarInfo.maxVar > FlagVar.preambleDetectionThreshold) {
            // use the ratio of peak value to the mean value of its previous 200 samples
        int startIndex = indexMaxVarInfo.index - numberOfPreviousSamples;
        float ratio = indexMaxVarInfo.maxVar / Algorithm.meanValue(corr, startIndex, indexMaxVarInfo.index);
        if(ratio > ratioThreshold) {
            indexMaxVarInfo.isReferenceSignalExist = true;
        }
//        }
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
        float[] correlationResult = null;
        float max = 0;
        float mean = 0;
        // use the max/mean ratio as the indicator for symbol decoding
        if(isUpSymbol) {
            for (int i = 0; i < numberOfSymbols; i++) {
                correlationResult = xcorrSignal(s, startIndex, endIndex, upSymbolSamples[i]);
                max = Algorithm.getMaxInfo(correlationResult, 0, correlationResult.length).maxVar;
                mean = Algorithm.meanValue(correlationResult, 0, correlationResult.length);
                maxRatios[i] = max / mean;
            }
        }else{
            for (int i = 0; i < numberOfSymbols; i++) {
                correlationResult = xcorrSignal(s, startIndex, endIndex, downSymbolSamples[i]);
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
