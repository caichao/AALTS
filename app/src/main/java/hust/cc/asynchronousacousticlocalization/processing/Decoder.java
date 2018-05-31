package hust.cc.asynchronousacousticlocalization.processing;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.Date;

import hust.cc.asynchronousacousticlocalization.physical.AudioRecorder;
import hust.cc.asynchronousacousticlocalization.physical.SignalGenerator;
import hust.cc.asynchronousacousticlocalization.utils.FlagVar;

public class Decoder implements FlagVar{

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

    // create variables to store the samples in case frequent new and return

    public int processBufferSize = 9600;

    public int preambleCorrLen = getFFTLen(processBufferSize+LPreamble,LPreamble);
    public FloatFFT_1D preambleDetectionFFT;
    public int symbolCorrLen = getFFTLen(LSymbol,LSymbol);
    public FloatFFT_1D symbolDectectionFFT;
    public float[] preambleCorrResult;
    public float[] symbolCorrResult;
    public short[] preambleSamples;
    public short[] symbolSamples;
    public float[] preambleFSamples;
    public float[] symbolFSamples;
    public float[] graphBuffer;
    public float[] graphBuffer2;

    public float[] upPreambleFFT;
    public float[] downPreambleFFT;
    public float[][] upSymbolFFTs;
    public float[][] downSymbolFFTs;
    public float[] preambleCorrFitVals;
    public float[] symbolCorrFitVals;

    public void setProcessBufferSize(int processBufferSize){
        this.processBufferSize = processBufferSize;
        bufferedSamples = new short[processBufferSize+beconMessageLength];
        preambleCorrLen = getFFTLen(processBufferSize+LPreamble,LPreamble);
        symbolCorrLen = getFFTLen(LSymbol,LSymbol);
        preambleDetectionFFT = new FloatFFT_1D(preambleCorrLen);
        symbolDectectionFFT = new FloatFFT_1D(symbolCorrLen);
        preambleCorrResult = new float[preambleCorrLen];
        symbolCorrResult = new float[symbolCorrLen];
        preambleSamples = new short[processBufferSize+LPreamble];
        symbolSamples = new short[LSymbol];
        preambleFSamples = new float[preambleCorrLen];
        symbolFSamples = new float[symbolCorrLen];
        graphBuffer = new float[preambleCorrLen];
        graphBuffer2 = new float[preambleCorrLen];
        preambleCorrFitVals = new float[preambleCorrLen];
        symbolCorrFitVals = new float[symbolCorrLen];

        //compute the fft of the preambles and symbols to reduce the computation cost;
        upPreambleFFT = new float[preambleCorrLen];
        downPreambleFFT = new float[preambleCorrLen];
        upSymbolFFTs = new float[numberOfSymbols][symbolCorrLen];
        downSymbolFFTs = new float[numberOfSymbols][symbolCorrLen];
        for(int i=1;i<numberOfSymbols;i++){
            upSymbolFFTs[i] = new float[symbolCorrLen];
            downSymbolFFTs[i] = new float[symbolCorrLen];
        }
        System.arraycopy(normalization(upPreamble),0,upPreambleFFT,0,upPreamble.length);
        System.arraycopy(normalization(downPreamble),0,downPreambleFFT,0,downPreamble.length);
        for(int i=0;i<numberOfSymbols;i++){
            System.arraycopy(normalization(upSymbolSamples[i]),0,upSymbolFFTs[i],0,upSymbolSamples[i].length);
            System.arraycopy(normalization(downSymbolSamples[i]),0,downSymbolFFTs[i],0,downSymbolSamples[i].length);
        }
        getFFT(preambleCorrLen).realForward(upPreambleFFT);
        getFFT(preambleCorrLen).realForward(downPreambleFFT);
        for(int i=0;i<numberOfSymbols;i++) {
            getFFT(symbolCorrLen).realForward(upSymbolFFTs[i]);
            getFFT(symbolCorrLen).realForward(downSymbolFFTs[i]);
        }

    }



    public int getProcessBufferSize() {
        return processBufferSize;
    }

    public short[] bufferedSamples = new short[processBufferSize+beconMessageLength];


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

    public float[] normalization(short s[], int low, int high){
        float[] normalized = new float[high-low+1];
        for(int i=low;i<=high;i++){
            normalized[i-low] = (float)(s[i])/32768;
        }
        return normalized;
    }


    /**
     * correlation results, return both the max value and its index
     * @param data1: audio samples
     * @param data2: reference signal
     * @return: return the max value and its index
     */

    public IndexMaxVarInfo getIndexMaxVarInfoFromFloats(float[] data1,float[] data2, boolean isFdomain){
        IndexMaxVarInfo indexMaxVarInfo = new IndexMaxVarInfo();
        float[] corr = xcorr(data1,data2,isFdomain);
        int index = getMaxPosFromCorrFloat(corr);
        indexMaxVarInfo.index = index;
        indexMaxVarInfo.maxVar = corr[(index+corr.length)%corr.length];

        IndexMaxVarInfo resultInfo = preambleDetection(corr,indexMaxVarInfo);
        return resultInfo;
    }



    public float[] xcorr(float []data1, float[] data2, boolean isFDomain){

        int len = 1;
        if(!isFDomain) {
            len = getFFTLen(data1.length,data2.length);
        }else{
            len = data1.length;
        }
        float[] hData1 = null;
        float[] hData2 = null;
        float[] result = getCorrArray(len);
        FloatFFT_1D fft = getFFT(len);

        if(!isFDomain) {
            hData1 = new float[len];
            hData2 = new float[len];
            System.arraycopy(data1,0,hData1,0,data1.length);
            System.arraycopy(data2,0,hData2,0,data2.length);
            fft.realForward(hData1);
            fft.realForward(hData2);
//            Log.v("","2 fft time:"+(date2.getTime()-date1.getTime()));
        }else{
            hData1 = data1;
            hData2 = data2;
        }

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
        for(int i=0;i<result.length;i++){
            result[i] = Math.abs(result[i]);
        }
        return result;
    }

    public float[] getCorrArray(int len){
        float[] result;
        if(len == preambleCorrLen) {
            result = preambleCorrResult;
        }else if(len == symbolCorrLen){
            result = symbolCorrResult;
        }else{
            result = new float[len];
        }
        return result;
    }

    /** corr is the correlation array, chirpLength is the chirp signal's length. return the postion of the max correlation.
     * @auther Ruinan Jin
     * @param corr
     * @return
     */
    public int getMaxPosFromCorrFloat(float [] corr){
        float max = 0;
        int end = 0;
        int index = 0;
        Date date1 = new Date();
//        float[] fitVals = getFitPosFromCorrFloat(corr,200);
        Date date2 = new Date();
        System.out.println("getFitPosFromCorrFloat:"+(date2.getTime()-date1.getTime()));
        for(int i=0;i<corr.length;i++){
            if(corr[i]>max){
                max = corr[i];
                end = i;
                index = i;
            }
        }
        int start = end-400>0?end-400:0;
        for(int i=start;i<=end;i++){
            if(corr[i] >= 0.6*max){
                index = i;
                break;
            }
        }
        return end;
    }

    public float[] getFitPosFromCorrFloat(float [] corr, int halfBlockLength){
        float[] fitVals = getFitVals(corr.length);
        float val = 0;
        for(int i=0;i<halfBlockLength*2+1;i++){
            val += corr[i];
        }
        for(int i=halfBlockLength;i<corr.length-halfBlockLength-1;i++){
            fitVals[i] = val;
            val -= corr[i-halfBlockLength];
            val += corr[i+halfBlockLength+1];
        }
        fitVals[corr.length-halfBlockLength-1] = val;
        for(int i=0;i<halfBlockLength;i++){
            fitVals[i] = fitVals[halfBlockLength];
        }
        for(int i=corr.length-halfBlockLength;i<corr.length;i++){
            fitVals[i] = fitVals[corr.length-halfBlockLength-1];
        }
        float blockLengh = 2*halfBlockLength+1;
        for(int i=0;i<corr.length;i++){
            fitVals[i] = corr[i]*blockLengh/fitVals[i];
        }
        return fitVals;
    }

    public float[] getFitVals(int len){
        float[] fitVals;
        if(len == preambleCorrLen) {
            fitVals = preambleCorrFitVals;
        }else if(len == symbolCorrLen){
            fitVals = symbolCorrFitVals;
        }else{
            fitVals = new float[len];
        }
        return fitVals;
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

        IndexMaxVarInfo indexMaxVarInfo = getIndexMaxVarInfoFromFloats(data1,data2,false);

        return indexMaxVarInfo;
    }

    public IndexMaxVarInfo getIndexMaxVarInfoFromFDomain(float[] sf, float[] rf){
        IndexMaxVarInfo indexMaxVarInfo = getIndexMaxVarInfoFromFloats(sf,rf,true);
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
        return xcorr(s0,reference,false);
    }


    public float[] xcorrSignal(short s[], int low, int high, short[] reference){
        float[] s0 = normalization(s);
        float[] r0 = normalization(reference);
        return xcorr(s0,low,high,r0);
    }


    public float[] getData1HalfFFtFromSignals(short[] data1, int data2Len){
        int len = getFFTLen(data1.length,data2Len);

        FloatFFT_1D fft = getFFT(len);

        float[] hData1 = getFSamples(len);
        for(int i=0;i<len;i++){
            if(i<data1.length){
                hData1[i] = (float) data1[i]/32768;
            }else{
                hData1[i] = 0;
            }
        }

        fft.realForward(hData1);
        return hData1;
    }

    public float[] getFSamples(int len){
        float[] fSamples;
        if(len == preambleCorrLen) {
            fSamples = preambleFSamples;
        }else if(len == symbolCorrLen){
            fSamples = symbolFSamples;
        }else{
            fSamples = new float[len];
        }
        return fSamples;
    }



    public FloatFFT_1D getFFT(int len) {
        FloatFFT_1D fft;
        if(len == preambleCorrLen) {
            fft = preambleDetectionFFT;
        }else if(len == symbolCorrLen){
            fft = symbolDectectionFFT;
        }else{
            fft = new FloatFFT_1D(len);
        }
        return fft;
    }

    public int getFFTLen(int len1, int len2){
        int len = 1;
        while(len < len1+len2){
            len = len*2;
        }
        return len;
    }


    public float[] getData1HalfFFtFromSignals(short[] data1, int low, int high, int data2Len){

        short[] data0 = getPreSamples(high-low+1);
        System.arraycopy(data1,low,data0,0,high-low+1);
        return getData1HalfFFtFromSignals(data0,data2Len);
    }

    public short[] getPreSamples(int len){
        short[] samples;
        if(len == processBufferSize+LPreamble) {
            samples = preambleSamples;
        }else if(len == LSymbol){
            samples = symbolSamples;
        }else{
            samples = new short[len];
        }
        return samples;
    }


    /**
     * detect whether the preamble exist
     * @param corr - correlation array
     * @param indexMaxVarInfo - the info of the max corr index
     * @return true indicate the presence of the corresponding preamble and vise versa
     */
    public IndexMaxVarInfo preambleDetection(float[] corr, IndexMaxVarInfo indexMaxVarInfo){
        indexMaxVarInfo.isReferenceSignalExist = false;
        if(indexMaxVarInfo.maxVar > FlagVar.preambleDetectionThreshold) {
            // use the ratio of peak value to the mean value of its previous 200 samples
            int startIndex = indexMaxVarInfo.index - numberOfPreviousSamples;
            float ratio = indexMaxVarInfo.maxVar / Algorithm.meanValue(corr, startIndex, indexMaxVarInfo.index);
            if(ratio > ratioThreshold) {
                indexMaxVarInfo.isReferenceSignalExist = true;
            }
            System.out.println("index:"+indexMaxVarInfo.index+"   ratio:"+ratio+"   maxCorr:"+indexMaxVarInfo.maxVar);
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
        int startIndex = p.index + FlagVar.LPreamble + FlagVar.guardIntervalLength;
        int endIndex = startIndex + FlagVar.LSymbol - 1;

        //debug
        if(endIndex>FlagVar.beconMessageLength+ AudioRecorder.getBufferSize()){
//            Log.v("","endIndex:"+endIndex);
            StringBuilder sb = new StringBuilder();
            for(int i=0;i<s.length;i++){
                sb.append(s[i]).append(",");
                if(i%600 == 0){
                    sb.append("\n");
                }
            }
//            Log.v("","data:"+sb.toString());
        }


        float[] maxRatios = new float[numberOfSymbols];
        float[] correlationResult = null;
        float[] fft = getData1HalfFFtFromSignals(s,startIndex,endIndex,upSymbolSamples[0].length);;
        float max = 0;
        float mean = 0;
        // use the max/mean ratio as the indicator for symbol decoding
        if(isUpSymbol) {
            for (int i = 0; i < numberOfSymbols; i++) {
                correlationResult = xcorr(fft,upSymbolFFTs[i],true);
//                correlationResult = xcorrSignal(s, startIndex, endIndex, upSymbolSamples[i]);
                max = Algorithm.getMaxInfo(correlationResult, 0, correlationResult.length).maxVar;
                mean = Algorithm.meanValue(correlationResult, 0, correlationResult.length);
                maxRatios[i] = max / mean;
            }
        }else{
            for (int i = 0; i < numberOfSymbols; i++) {
                correlationResult = xcorr(fft,downSymbolFFTs[i],true);
//                correlationResult = xcorrSignal(s, startIndex, endIndex, downSymbolSamples[i]);
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

    public boolean isSignalRepeatedDetected(IndexMaxVarInfo indexMaxVarInfo, int bufferLen){
        int index = indexMaxVarInfo.index;
        if(index >= bufferLen){
            return true;
        }else{
            return false;
        }
    }


}
