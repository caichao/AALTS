package hust.cc.asynchronousacousticlocalization.processing;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.LinkedList;
import java.util.List;

import hust.cc.asynchronousacousticlocalization.physical.SignalGenerator;
import hust.cc.asynchronousacousticlocalization.utils.FlagVar;
import hust.cc.asynchronousacousticlocalization.utils.JniUtils;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

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

    protected boolean useJni = false;
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

    protected short[] bufferUp;
    protected short[] bufferDown;

    public void initialize(int processBufferSize, boolean useJni){
        this.processBufferSize = processBufferSize;
        this.useJni = useJni;
        bufferedSamples = new short[processBufferSize+beconMessageLength+startBeforeMaxCorr+LPreamble];
        bufferUp = new short[processBufferSize*3];
        bufferDown = new short[processBufferSize*3];
        preambleCorrLen = getFFTLen(processBufferSize+LPreamble+startBeforeMaxCorr,LPreamble);
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
        for (int i = 1; i < numberOfSymbols; i++) {
            upSymbolFFTs[i] = new float[symbolCorrLen];
            downSymbolFFTs[i] = new float[symbolCorrLen];
        }

        if(!useJni) {
            System.arraycopy(normalization(upPreamble), 0, upPreambleFFT, 0, upPreamble.length);
            System.arraycopy(normalization(downPreamble), 0, downPreambleFFT, 0, downPreamble.length);
            for (int i = 0; i < numberOfSymbols; i++) {
                System.arraycopy(normalization(upSymbolSamples[i]), 0, upSymbolFFTs[i], 0, upSymbolSamples[i].length);
                System.arraycopy(normalization(downSymbolSamples[i]), 0, downSymbolFFTs[i], 0, downSymbolSamples[i].length);
            }
            getFFT(preambleCorrLen).realForward(upPreambleFFT);
            getFFT(preambleCorrLen).realForward(downPreambleFFT);
            for (int i = 0; i < numberOfSymbols; i++) {
                getFFT(symbolCorrLen).realForward(upSymbolFFTs[i]);
                getFFT(symbolCorrLen).realForward(downSymbolFFTs[i]);
            }
        }else{
            upPreambleFFT = JniUtils.fft(normalization(upPreamble),preambleCorrLen);
            downPreambleFFT = JniUtils.fft(normalization(downPreamble),preambleCorrLen);
            for (int i = 0; i < numberOfSymbols; i++) {
                upSymbolFFTs[i] = JniUtils.fft(normalization(upSymbolSamples[i]),symbolCorrLen);
                downSymbolFFTs[i] = JniUtils.fft(normalization(downSymbolSamples[i]),symbolCorrLen);
            }

        }

    }



    public int getProcessBufferSize() {
        return processBufferSize;
    }

    public short[] bufferedSamples = new short[processBufferSize+beconMessageLength+startBeforeMaxCorr+LPreamble];


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

        float[] corr;
        if(!useJni) {
            corr = xcorr(data1, data2, isFdomain);
        }else{
            corr = JniUtils.xcorr(data1,data2);
        }


        float[] fitVals = getFitValsFromCorr(corr);
        int index = getFitPos(fitVals, corr);
        indexMaxVarInfo.index = index;
        indexMaxVarInfo.fitVal = fitVals[index];

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
     * @param fitVals
     * @return
     */
    public int getFitPos(float [] fitVals, float[] corr){
        float max = 0;
        float maxCorr = 0;
        int index = 0;
        for(int i=0;i<fitVals.length;i++){
            maxCorr = maxCorr<corr[i]?corr[i]:maxCorr;
        }
        float threshold = ratioAvailableThreshold*maxCorr;
        for(int i=0;i<fitVals.length;i++){
            if(fitVals[i]>max && corr[i]>threshold){
                max = fitVals[i];
                index = i;
            }
        }

        return index;
    }

    public float[] getFitValsFromCorr(float [] corr){
        float[] fitVals = new float[processBufferSize+LPreamble+startBeforeMaxCorr];
        float val = 0;
        for(int i=0;i<startBeforeMaxCorr-endBeforeMaxCorr;i++){
            val += corr[i];
        }
        for(int i=startBeforeMaxCorr;i<fitVals.length;i++){
            fitVals[i] = val;
            val -= corr[i-startBeforeMaxCorr];
            val += corr[i-endBeforeMaxCorr];

        }
        for(int i=startBeforeMaxCorr;i<fitVals.length;i++){
            fitVals[i] = corr[i]*(startBeforeMaxCorr-endBeforeMaxCorr)/fitVals[i];
        }
        return fitVals;
    }




    public IndexMaxVarInfo getIndexMaxVarInfoFromFDomain(float[] sf, float[] rf){
        IndexMaxVarInfo indexMaxVarInfo = getIndexMaxVarInfoFromFloats(sf,rf,true);
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
        float ratio = indexMaxVarInfo.fitVal*corr[indexMaxVarInfo.index];
        if(corr[indexMaxVarInfo.index] > FlagVar.preambleDetectionThreshold && ratio > ratioThreshold) {
            indexMaxVarInfo.isReferenceSignalExist = true;
        }
        System.out.println("index:"+indexMaxVarInfo.index+"   ratio:"+ratio+"   maxCorr:"+corr[indexMaxVarInfo.index]);
        return indexMaxVarInfo;
    }


    public int[] decodeAnchorSeqId(short[] s ,IndexMaxVarInfo info){
        int[] ids = new int[2];
        int startIndex = info.index+LPreamble+guardIntervalLength;
        int endIndex = startIndex+LSymbol-1;
        float[] anchorSamples = normalization(s,startIndex,endIndex);
        ids[0] = decodeId(anchorSamples);
        startIndex = startIndex+LSymbol+guardIntervalLength;
        endIndex = endIndex+LSymbol+guardIntervalLength;
        float[] seqSamples = normalization(s,startIndex,endIndex);
        ids[1] = decodeId(seqSamples);
        return ids;
    }



    public int decodeId(float[] s){
        float[] fft = JniUtils.fft(s,2*LSymbol);
        float[] corr = null;
        float[] maxRatios = new float[numberOfSymbols];

        for(int i=0;i<numberOfSymbols;i++){
            corr = JniUtils.xcorr(fft,downSymbolFFTs[i]);
            float max = Algorithm.getMaxInfo(corr, 0, corr.length-1).fitVal;
            float mean = Algorithm.meanValue(corr, 0, corr.length-1);
            maxRatios[i] = max/mean;
        }

        int id = Algorithm.getMaxInfo(maxRatios,0,maxRatios.length-1).index;
        return id;

    }

    /**
     * decode the anchor ID
     * @param s - the audio samples
     * @param p - the decoded preamble information
     * @param isUpSymbol - indicate whether we use up or down symbol to decode the anchor ID
     * @return the decoded anchor ID
     */
    public int decodeAnchorIDOnOrthotropic(short[] s, boolean isUpSymbol, IndexMaxVarInfo p){
        int startIndex = p.index + FlagVar.LPreamble + FlagVar.guardIntervalLength;
        int endIndex = startIndex + FlagVar.LSymbol - 1;

        float[] maxRatios = new float[numberOfSymbols];
        float[] correlationResult = null;

        float[] fft;
        if(!useJni) {
            fft = getData1HalfFFtFromSignals(s, startIndex, endIndex, upSymbolSamples[0].length);
        }
        else{
            float[] sampleF = normalization(s,startIndex,endIndex);
            fft = JniUtils.fft(sampleF,2*LSymbol);
            System.out.println("");
        }
        float max = 0;
        float mean = 0;
        // use the max/mean ratio as the indicator for symbol decoding
        if(isUpSymbol) {
            for (int i = 0; i < numberOfSymbols; i++) {
                correlationResult = useJni?JniUtils.xcorr(fft,upSymbolFFTs[i]):xcorr(fft,upSymbolFFTs[i],true);
                max = Algorithm.getMaxInfo(correlationResult, 0, correlationResult.length-1).fitVal;
                mean = Algorithm.meanValue(correlationResult, 0, correlationResult.length-1);
                maxRatios[i] = max / mean;
            }
        }else{
            for (int i = 0; i < numberOfSymbols; i++) {
                correlationResult = useJni?JniUtils.xcorr(fft,downSymbolFFTs[i]):xcorr(fft,downSymbolFFTs[i],true);
                max = Algorithm.getMaxInfo(correlationResult, 0, correlationResult.length-1).fitVal;
                mean = Algorithm.meanValue(correlationResult, 0, correlationResult.length-1);
                maxRatios[i] = max / mean;
            }
        }

        int decodeID = 0;
        max = maxRatios[0];
        for (int i = 1 ; i < numberOfSymbols ; i++){
            if(maxRatios[i] > max)
                max = maxRatios[i];
                decodeID = i;
        }
        return decodeID;
    }

    public boolean isIndexAvailable(IndexMaxVarInfo indexMaxVarInfo){
        int index = indexMaxVarInfo.index;
        if(index >= processBufferSize+LPreamble+startBeforeMaxCorr || index < startBeforeMaxCorr){
            return false;
        }else{
            return true;
        }
    }

    public float getFshift(float[] data, int[] sinSigFs, int len, int rangeF, int fs){
        int a = 1;
        while (a < len){
            a = a*2;
        }
        if(a != len){
            throw new RuntimeException("len should be power of 2.");
        }
        if(sinSigFs.length >= 2){
            for(int i=1;i<sinSigFs.length;i++){
                if(sinSigFs[i] <= sinSigFs[i-1]){
                    throw new RuntimeException("sinSigFs should be increasing.");
                }
            }
        }
        float[] fft = JniUtils.fft(data,len);

        float[] absfft = new float[fft.length/2];
        for(int i=0;i<absfft.length;i++){
            absfft[i] = Math.abs(fft[2*i]*fft[2*i]+fft[2*i+1]*fft[2*i+1]);
        }
        List<int[]> detectIntervals = new LinkedList<int[]>();
        for(int i=0;i<sinSigFs.length;i++){
            detectIntervals.add(getDetectInterval(len,fs,rangeF,sinSigFs[i]));
            if(i >=1){
                if(detectIntervals.get(i)[0] <= detectIntervals.get(i-1)[1]){
                    throw new RuntimeException("the start of the detectInterval shold be bigger than the end of the last interval.");
                }
            }
        }
        float[] detectFs = new float[sinSigFs.length];
        for(int i=0;i<detectFs.length;i++){
            IndexMaxVarInfo info = Algorithm.getMaxInfo(absfft,detectIntervals.get(i)[0],detectIntervals.get(i)[1]);
            detectFs[i] = 1.0f*info.index*fs/len;
        }
        return fShiftCalculate(sinSigFs,detectFs);
    }

    public float fShiftCalculate(int[] sinSigFs, float[] detectFs){
        if(sinSigFs.length != detectFs.length){
            throw new RuntimeException("sinSigFs should have the same size of detectFs");
        }
        float fShift = 0;
        for(int i=0;i<sinSigFs.length;i++){
            fShift += sinSigFs[i]-detectFs[i];
        }
        fShift /= sinSigFs.length;
        return  fShift;

    }



    public int[] getDetectInterval(int len, int fs,int detectRangeF,int sinSigF){
        int[] interval = new int[2];
        interval[0] = (sinSigF-detectRangeF)*len/fs;
        interval[1] = (sinSigF+detectRangeF)*len/fs;
        return interval;
    }






}
