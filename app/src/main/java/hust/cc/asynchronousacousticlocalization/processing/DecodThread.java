package hust.cc.asynchronousacousticlocalization.processing;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import hust.cc.asynchronousacousticlocalization.utils.FlagVar;

public class DecodThread extends Decoder implements Runnable{

    private static final String TAG = "DecodThread";
    private boolean isBufferReady = false;
    private boolean isThreadRunning = true;
    private boolean isTDOAObtained = false;
    private TDOAUtils mTDOAUtils;
    private int mLoopCounter = 0;
    private Deque<TDOAUtils> preambleInfoList;
    private int mTDOACounter = 0;
    private Handler mHandler;
    public List<Short[]> samplesList;

    private IndexMaxVarInfo mIndexMaxVarInfo;

    public DecodThread(Handler mHandler){
        mIndexMaxVarInfo = new IndexMaxVarInfo();
        mTDOAUtils = new TDOAUtils();
        preambleInfoList = new ArrayDeque<>();
        samplesList = new LinkedList<Short[]>();
        this.mHandler = mHandler;
    }

    /**
     * copy the samples from the audio buffer
     * @param s - input coming from the audio buffer
     */
    public void fillSamples(short[] s){

        Short[] samples = new Short[processBufferSize];
        for(int i=0;i<samples.length;i++){
            samples[i] = s[i];
        }
        synchronized (samplesList) {
            samplesList.add(samples);
        }

    }

    @Override
    public void run() {
        try {
            while (isThreadRunning) {
                if (samplesList.size() >= 3) {
                    short[] bufferedSamples = new short[processBufferSize + FlagVar.beconMessageLength];
                    synchronized (samplesList) {

                        for (int i = 0; i < LPreamble; i++) {
                            bufferedSamples[i] = samplesList.get(0)[processBufferSize-LPreamble+i];
                        }
                        for (int i = LPreamble; i < processBufferSize + LPreamble; i++) {
                            bufferedSamples[i] = samplesList.get(1)[i - LPreamble];
                        }
                        for (int i=processBufferSize+LPreamble;i<processBufferSize+beconMessageLength;i++){
                            bufferedSamples[i] = samplesList.get(2)[i-processBufferSize-LPreamble];
                        }
                        samplesList.remove(0);
                    }

                    mLoopCounter++;

                    // 1. the first step is to check the existence of preamble either up or down
                    mIndexMaxVarInfo.isReferenceSignalExist = false;
                    mIndexMaxVarInfo = getIndexMaxVarInfoFromSignals(bufferedSamples,0,processBufferSize+LPreamble-1, upPreamble);

                    // 2. if the preamble exist, then decode the anchor ID
                    if (mIndexMaxVarInfo.isReferenceSignalExist && !isSignalRepeatedDetected(mIndexMaxVarInfo,processBufferSize)) {
                        mTDOACounter++;

                        int anchorID = decodeAnchorID(bufferedSamples, true, mIndexMaxVarInfo);
                        TDOAUtils tdoaUtils = new TDOAUtils();
                        // store the timming information
                        tdoaUtils.loopIndex = mLoopCounter;
                        tdoaUtils.preambleType = FlagVar.UP_PREAMBLE;
                        tdoaUtils.timeIndex = mIndexMaxVarInfo.index;
                        tdoaUtils.TDOACounter = mTDOACounter;
                        tdoaUtils.correspondingAnchorID = anchorID;

                        preambleInfoList.add(tdoaUtils);

                    }

                    // 3. check the down preamble and do the above operation again
                    mIndexMaxVarInfo.isReferenceSignalExist = false;
                    mIndexMaxVarInfo = getIndexMaxVarInfoFromSignals(bufferedSamples, 0,processBufferSize+LPreamble-1,downPreamble);
                    if (mIndexMaxVarInfo.isReferenceSignalExist && !isSignalRepeatedDetected(mIndexMaxVarInfo,processBufferSize)) {
                        mTDOACounter++;

                        int anchorID = decodeAnchorID(bufferedSamples, false, mIndexMaxVarInfo);
                        TDOAUtils tdoaUtils = new TDOAUtils();

                        tdoaUtils.loopIndex = mLoopCounter;
                        tdoaUtils.preambleType = FlagVar.DOWN_PREAMBLE;
                        tdoaUtils.timeIndex = mIndexMaxVarInfo.index;
                        tdoaUtils.TDOACounter = mTDOACounter;
                        tdoaUtils.correspondingAnchorID = anchorID;

                        preambleInfoList.add(tdoaUtils);

                    }

                    int tdoa = Integer.MIN_VALUE;
                    // 4. process the TDOA time information
                    if (mTDOACounter >= 2) {// receive two TDOA timming information
                        if (mTDOACounter == 3) {
                            preambleInfoList.pollFirst();
                        }
                        tdoa = processTDOAInformation();
                    }

                    Log.v("","mLoopCounter:"+mLoopCounter);
                    System.out.println("mLoopCounter:"+mLoopCounter);
                    if(mLoopCounter > 300){
                        Log.v("","buffer samples size >= 300");
                        System.out.println("buffer samples size >= 300");
                        return;
                    }
                    if(Math.abs(tdoa)>1000 && Math.abs(tdoa) < 200000){
                        System.out.println(tdoa);Log.v("","tdoa:"+tdoa);
                        System.out.println("tdoa:"+tdoa);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /*
    obtain TDOA information from the preambleInfoList
     */
    private int processTDOAInformation(){
        // remove the first anchor information
        TDOAUtils mFirstAnchorInfo = preambleInfoList.pollFirst();
        // peek the last one
        TDOAUtils mSecondAnchorInfo = preambleInfoList.peekFirst();

        // if the preamble type of the two decode information is not the same, then we can get the tdoa information
        if(mFirstAnchorInfo.preambleType != mSecondAnchorInfo.preambleType){
            // post the information to the main thread
            int tdoa = (mFirstAnchorInfo.loopIndex - mSecondAnchorInfo.loopIndex) * processBufferSize + mFirstAnchorInfo.timeIndex - mSecondAnchorInfo.timeIndex;
            if(tdoa > beconMessageLength){
                mTDOACounter = 1;
                return tdoa;
            }
            mTDOACounter = 0;
            preambleInfoList.removeFirst();
            Message message = mHandler.obtainMessage();
            message.what = FlagVar.MESSAGE_TDOA;
            message.arg1 = tdoa;
            // the first four bits store the first anchor id, the last four bit store the second anchor ID
            message.arg2 = mFirstAnchorInfo.correspondingAnchorID << 4 | mSecondAnchorInfo.correspondingAnchorID;

            mHandler.sendMessage(message);
            return tdoa;

        } else{
            mTDOACounter = 1;
            return Integer.MAX_VALUE;
        }
    }

    /**
     * shutdown the thread
     */
    public void close() {
        synchronized (this){
            isThreadRunning = false;
            //Thread.currentThread().join();
        }
    }

    private class TDOAUtils{
        public int TDOACounter = 0;
        public int preambleType = FlagVar.UP_PREAMBLE;
        public int timeIndex = 0;
        public int loopIndex = 0;
        public int correspondingAnchorID = 0;
    }
}
