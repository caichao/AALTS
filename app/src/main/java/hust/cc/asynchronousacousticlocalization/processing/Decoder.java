package hust.cc.asynchronousacousticlocalization.processing;

import android.util.Log;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.Date;

import hust.cc.asynchronousacousticlocalization.physical.AudioRecorder;
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

    public IndexMaxVarInfo getIndexMaxVarInfoFromFloats(float[] data1,float[] data2, boolean isData1FDomainSignal){
        IndexMaxVarInfo indexMaxVarInfo = new IndexMaxVarInfo();
        float[] corr = xcorrFast(data1,data2,isData1FDomainSignal);
        int index = getMaxPosFromCorrFloat(corr,data2.length);
        indexMaxVarInfo.index = index;
        indexMaxVarInfo.maxVar = corr[(index+corr.length)%corr.length];

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

    public static float[] xcorrFast(float []data1, float[] data2, boolean isData1FDomainSignal){

        int len = 1;
        if(!isData1FDomainSignal) {
            while (len < data1.length + data2.length) {
                len = len * 2;
            }
        }else{
            len = data1.length;
        }
        float[] hData1 = null;
        float[] hData2 = null;
        float[] result = new float[len];
        float[] corr = new float[len];
        FloatFFT_1D fft = new FloatFFT_1D(len);

        if(!isData1FDomainSignal) {
            hData1 = new float[len];
            hData2 = new float[len];
            for (int i = 0; i < len; i++) {
                if (i < data1.length) {
                    hData1[i] = data1[i];
                } else {
                    hData1[i] = 0;
                }
                if (i < data2.length) {
                    hData2[i] = data2[i];
                } else {
                    hData2[i] = 0;
                }
            }
            Date date1 = new Date();
            fft.realForward(hData1);
            fft.realForward(hData2);
            Date date2 = new Date();
            System.out.println("2 fft time:"+(date2.getTime()-date1.getTime()));
        }else{
            hData1 = data1;
            hData2 = new float[len];
            for (int i = 0; i < len; i++) {
                if (i < data2.length) {
                    hData2[i] = data2[i];
                } else {
                    hData2[i] = 0;
                }
            }
            Date date1 = new Date();
            fft.realForward(hData2);
            Date date2 = new Date();
            System.out.println("1 fft time:"+(date2.getTime()-date1.getTime()));
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
        Date date1 = new Date();
        fft.realInverse(result, true);
        Date date2 = new Date();
        System.out.println("1 ifft time:"+(date2.getTime()-date1.getTime()));
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

        IndexMaxVarInfo indexMaxVarInfo = getIndexMaxVarInfoFromFloats(data1,data2,false);

        return indexMaxVarInfo;
    }

    public IndexMaxVarInfo getIndexMaxVarInfoFromFAndTDomain(float[] sf, short[] reference){
        float[] data2 = normalization(reference);
        IndexMaxVarInfo indexMaxVarInfo = getIndexMaxVarInfoFromFloats(sf,data2,true);
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
        return xcorrFast(s0,reference,false);
    }


    public float[] xcorrSignal(short s[], int low, int high, short[] reference){
        float[] s0 = normalization(s);
        float[] r0 = normalization(reference);
        return xcorr(s0,low,high,r0);
    }


    public float[] getData1FFtFromSignals(short[] data1, int data2Len){
        int len = 1;
        while(len < data1.length+data2Len){
            len = len*2;
        }
        FloatFFT_1D fft = new FloatFFT_1D(len);
        float[] hData1 = new float[len];
        for(int i=0;i<len;i++){
            if(i<data1.length){
                hData1[i] = (float) data1[i]/32768;
            }else{
                hData1[i] = 0;
            }
        }
        Date date1 = new Date();
        fft.realForward(hData1);
        Date date2 = new Date();
        System.out.println("1 fft time:"+(date2.getTime()-date1.getTime()));
        return hData1;
    }


    public float[] getData1FFtFromSignals(short[] data1,int low, int high, int data2Len){
        short[] data0 = new short[high-low+1];
        for(int i=low;i<=high;i++){
            data0[i-low] = data1[i];
        }
        return getData1FFtFromSignals(data0,data2Len);
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
        int startIndex = p.index + FlagVar.LPreamble + FlagVar.guardIntervalLength;
        int endIndex = startIndex + FlagVar.LSymbol - 1;

        //debug
        if(endIndex>FlagVar.beconMessageLength+ AudioRecorder.getBufferSize()){
            System.out.println("endIndex:"+endIndex);
            StringBuilder sb = new StringBuilder();
            for(int i=0;i<s.length;i++){
                sb.append(s[i]).append(",");
                if(i%600 == 0){
                    sb.append("\n");
                }
            }
            System.out.println("data:"+sb.toString());
        }


        float[] maxRatios = new float[numberOfSymbols];
        float[] correlationResult = null;
        float[] fft = getData1FFtFromSignals(s,startIndex,endIndex,upSymbolSamples[0].length);;
        float max = 0;
        float mean = 0;
        // use the max/mean ratio as the indicator for symbol decoding
        if(isUpSymbol) {
            for (int i = 0; i < numberOfSymbols; i++) {
                correlationResult = xcorrFast(fft,normalization(upSymbolSamples[i]),true);
//                correlationResult = xcorrSignal(s, startIndex, endIndex, upSymbolSamples[i]);
                max = Algorithm.getMaxInfo(correlationResult, 0, correlationResult.length).maxVar;
                mean = Algorithm.meanValue(correlationResult, 0, correlationResult.length);
                maxRatios[i] = max / mean;
            }
        }else{
            for (int i = 0; i < numberOfSymbols; i++) {
                correlationResult = xcorrFast(fft,normalization(downSymbolSamples[i]),true);
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
