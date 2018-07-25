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

    public int processBufferSize = 9600;

    public int preambleCorrLen = getFFTLen(processBufferSize+LPreamble,LPreamble);
    public int symbolCorrLen = getFFTLen(LSymbol,LSymbol);

    //we calculate the fft before decoding in order to reduce computation time.
    public float[] upPreambleFFT;
    public float[] downPreambleFFT;
    public float[][] upSymbolFFTs;
    public float[][] downSymbolFFTs;

    protected short[] bufferUp;
    protected short[] bufferDown;

    public static float rThreshold;
    public static float marThreshold;
    public static int mUsed;
    public static int pdType;


    public void initialize(int processBufferSize){
        this.processBufferSize = processBufferSize;
        //this value is abandoned
        bufferedSamples = new short[processBufferSize+beconMessageLength+startBeforeMaxCorr+LPreamble];
        bufferUp = new short[processBufferSize*3];
        bufferDown = new short[processBufferSize*3];
        /*
            the fft we use must have data length to be the power of 2. And we use the fft to compute the correlation, so
        the correlation data length should be the min value larger than the sum of the 2 buffers size that is the power of 2.
        the fft data length should also be this. we compute them here.
        */
        preambleCorrLen = getFFTLen(processBufferSize+LPreamble+startBeforeMaxCorr,LPreamble);
        symbolCorrLen = getFFTLen(LSymbol,LSymbol);

        //compute the fft of the preambles and symbols to reduce the computation cost;
        upPreambleFFT = new float[preambleCorrLen];
        downPreambleFFT = new float[preambleCorrLen];
        upSymbolFFTs = new float[numberOfSymbols][symbolCorrLen];
        downSymbolFFTs = new float[numberOfSymbols][symbolCorrLen];
        for (int i = 1; i < numberOfSymbols; i++) {
            upSymbolFFTs[i] = new float[symbolCorrLen];
            downSymbolFFTs[i] = new float[symbolCorrLen];
        }

        upPreambleFFT = JniUtils.fft(normalization(upPreamble),preambleCorrLen);
        downPreambleFFT = JniUtils.fft(normalization(downPreamble),preambleCorrLen);
        for (int i = 0; i < numberOfSymbols; i++) {
            upSymbolFFTs[i] = JniUtils.fft(normalization(upSymbolSamples[i]),symbolCorrLen);
            downSymbolFFTs[i] = JniUtils.fft(normalization(downSymbolSamples[i]),symbolCorrLen);
        }



    }



    //this value is abandoned
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
     * compute the best fit position of the chirp signal (data2) from data1.
     * @auther ruinan jin
     * @param data1: audio samples
     * @param data2: reference signal
     * @param isFdomain: whether the data is frequency domain data.
     * @return: return the fit value and its index
     */

    public IndexMaxVarInfo getIndexMaxVarInfoFromFloats(float[] data1,float[] data2, boolean isFdomain){

        if(!isFdomain){
            int len = getFFTLen(data1.length,data2.length);
            data1 = JniUtils.fft(data1,len);
            data2 = JniUtils.fft(data2,len);
        }
        //compute the corr
        float[] corr = JniUtils.xcorr(data1,data2);

        if(Decoder.pdType >= DETECT_TYPE1 && Decoder.pdType <= DETECT_TYPE3) {
            IndexMaxVarInfo indexMaxVarInfo = new IndexMaxVarInfo();
            //compute the fit vals
            float[] fitVals = getFitValsFromCorr(corr);
            //get the fit index
            int index = getFitPos(fitVals, corr);
            indexMaxVarInfo.index = index;
            indexMaxVarInfo.fitVal = fitVals[index];

            //detect whether the chirp signal exists.
            IndexMaxVarInfo resultInfo = preambleDetection(corr, indexMaxVarInfo);
            return resultInfo;
        }else {
            IndexMaxVarInfo info = Algorithm.getMaxInfo(corr,0,corr.length-1);
            float mean = Algorithm.meanValue(corr,info.index-startBeforeMaxCorr,info.index-endBeforeMaxCorr-1);
            float fitVal = info.fitVal/mean;
            System.out.println("index:"+info.index+"   ratio:"+fitVal+"   maxCorr:"+corr[info.index]);
            if(isIndexAvailable(info)){

                if((fitVal>marThreshold)) {
                    info.isReferenceSignalExist = true;
                }
            }
            return info;
        }
    }





    /** compute the position of the max value of the fitVals, while the corr of this position should be larger than a threshold.
     * @auther Ruinan Jin
     * @param fitVals fit value arrays.
     * @param corr correlation arrays.
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

    /**
     * get the fitVals from corr. fitVals is the value by which the corr is devided the average of the previous 200(startBeforeMaxCorr) corrs.
     * The first 200 fitVals is zero, while this is why the buffer length should be added by startBeforeMaxCorr.
     * @auther ruinan jin
     * @param corr
     * @return fitVals
     */
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
            if(Decoder.pdType == FlagVar.DETECT_TYPE2){
                fitVals[i] = fitVals[i]*corr[i];
            }else if(Decoder.pdType == FlagVar.DETECT_TYPE3){
                fitVals[i] = fitVals[i]*corr[i]*corr[i];
            }
        }
        return fitVals;
    }

    /**
     * getIndexMaxVarInfoFromFloats method for only frequency domain.
     * @auther ruinan jin
     * @param sf
     * @param rf
     * @return
     */
    public IndexMaxVarInfo getIndexMaxVarInfoFromFDomain(float[] sf, float[] rf){
        IndexMaxVarInfo indexMaxVarInfo = getIndexMaxVarInfoFromFloats(sf,rf,true);
        return indexMaxVarInfo;
    }

    /**
     * compute the min value larger than the sum of len1 and len2 which is the power of 2.
     * @auther ruinan jin
     * @param len1
     * @param len2
     * @return
     */
    public int getFFTLen(int len1, int len2){
        int len = 1;
        while(len < len1+len2){
            len = len*2;
        }
        return len;
    }





    /**
     * detect whether the preamble exist
     * @auther ruinan jin
     * @param corr - correlation array
     * @param indexMaxVarInfo - the info of the max corr index
     * @return true indicate the presence of the corresponding preamble and vise versa
     */
    public IndexMaxVarInfo preambleDetection(float[] corr, IndexMaxVarInfo indexMaxVarInfo){
        indexMaxVarInfo.isReferenceSignalExist = false;
//        float ratio = indexMaxVarInfo.fitVal*corr[indexMaxVarInfo.index];
//        float ratio = indexMaxVarInfo.fitVal;
        /*
            we mainly detect the preamble by thersholding the value of the fitVals*log(corr+1) in the fit position. it's tested to be the
        best method considering the fitVals and the corr.
         */
        float fitVal = indexMaxVarInfo.fitVal;
        if(Decoder.pdType == FlagVar.DETECT_TYPE2){
            fitVal = fitVal/corr[indexMaxVarInfo.index];
        }else if(Decoder.pdType == FlagVar.DETECT_TYPE3){
            fitVal = fitVal/corr[indexMaxVarInfo.index]/corr[indexMaxVarInfo.index];
        }
        float ratio = (float) (fitVal*Math.log(corr[indexMaxVarInfo.index]+1));
        if(fitVal > marThreshold && ratio > rThreshold && isIndexAvailable(indexMaxVarInfo)) {
            indexMaxVarInfo.isReferenceSignalExist = true;
        }
        //System.out.println("index:"+indexMaxVarInfo.index+"   ratio:"+ratio+"   maxCorr:"+corr[indexMaxVarInfo.index]);
        return indexMaxVarInfo;
    }

    /**
     * decode the anchor and the sequence id from the buffer.
     * @auther ruinan jin
     * @param s buffer
     * @param info fit val information
     * @return
     */

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


    /**
     * decode the id from certain samples. it's the max id of corr*corr/avg(corr).
     * @auther ruinan jin
     * @param s
     * @return
     */
    public int decodeId(float[] s){
        float[] fft = JniUtils.fft(s,2*LSymbol);
        float[] corr = null;
        float[] maxRatios = new float[numberOfSymbols];

        for(int i=0;i<numberOfSymbols;i++){
            corr = JniUtils.xcorr(fft,downSymbolFFTs[i]);
            float max = Algorithm.getMaxInfo(corr, 0, corr.length-1).fitVal;
            float mean = Algorithm.meanValue(corr, 0, corr.length-1);
            maxRatios[i] = max*max/mean;
        }

        int id = Algorithm.getMaxInfo(maxRatios,0,maxRatios.length-1).index;
        return id;

    }

    /**
     * decode the anchor ID. the method is abandoned.
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
        float[] sampleF = normalization(s,startIndex,endIndex);
        fft = JniUtils.fft(sampleF,2*LSymbol);
        System.out.println("");
        float max = 0;
        float mean = 0;
        // use the max/mean ratio as the indicator for symbol decoding
        if(isUpSymbol) {
            for (int i = 0; i < numberOfSymbols; i++) {
                correlationResult = JniUtils.xcorr(fft,upSymbolFFTs[i]);
                max = Algorithm.getMaxInfo(correlationResult, 0, correlationResult.length-1).fitVal;
                mean = Algorithm.meanValue(correlationResult, 0, correlationResult.length-1);
                maxRatios[i] = max / mean;
            }
        }else{
            for (int i = 0; i < numberOfSymbols; i++) {
                correlationResult = JniUtils.xcorr(fft,downSymbolFFTs[i]);
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
        if(index >= processBufferSize+startBeforeMaxCorr || index < startBeforeMaxCorr){
            return false;
        }else{
            return true;
        }
    }

    /**
     * calculate the frequency shift of the sin wav.
     * @auther ruinan jin
     * @param data samples
     * @param sinSigFs sin wav frequency
     * @param len fft length
     * @param rangeF detect range for each frequency. it should be smaller than the half of the frquency diff.
     * @param fs sound sampling rate.
     * @return
     */

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
