package hust.cc.asynchronousacousticlocalization.processing;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.Date;
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
    public List<short[]> samplesList;

    private IndexMaxVarInfo mIndexMaxVarInfo;

    public DecodThread(Handler mHandler){
        mIndexMaxVarInfo = new IndexMaxVarInfo();
        mTDOAUtils = new TDOAUtils();
        preambleInfoList = new ArrayDeque<>();
        samplesList = new LinkedList<short[]>();
        this.mHandler = mHandler;
    }

    /**
     * copy the samples from the audio buffer
     * @param s - input coming from the audio buffer
     */
    public void fillSamples(short[] s){

        synchronized (samplesList) {
            samplesList.add(s);
        }

    }

    @Override
    public void run() {
        try {
            while (isThreadRunning) {
                if (samplesList.size() >= 3) {
                    Date dateS = new Date();
                    synchronized (samplesList) {
                        Date date1 = new Date();
                        System.arraycopy(samplesList.get(0),processBufferSize-LPreamble,bufferedSamples,0,LPreamble);
                        System.arraycopy(samplesList.get(1),0,bufferedSamples,LPreamble,processBufferSize);
                        System.arraycopy(samplesList.get(2),0,bufferedSamples,processBufferSize+LPreamble,beconMessageLength-LPreamble);

                        Date date2 = new Date();
                        Log.v("","sample list time:"+(date2.getTime()-date1.getTime()));
                        samplesList.remove(0);

                    }
                    Date dateSS = new Date();
                    Log.v("","dateSS-dateS time:"+(dateSS.getTime()-dateS.getTime()));

                    mLoopCounter++;
                    //compute the fft of the bufferedSamples, it will be used twice. It's computed here to reduce time cost.
                    Date date1 = new Date();
                    float[] fft = getData1FFtFromSignals(bufferedSamples,0,processBufferSize+LPreamble-1, upPreamble.length);
                    Date date2 = new Date();
                    Log.v("","getData1FFtFromSignals time:"+(date2.getTime()-date1.getTime()));

                    // 1. the first step is to check the existence of preamble either up or down
                    mIndexMaxVarInfo.isReferenceSignalExist = false;
                    date1 = new Date();
                    mIndexMaxVarInfo = getIndexMaxVarInfoFromFAndTDomain(fft,upPreamble);
                    date2 = new Date();
                    Log.v("","getIndexMaxVarInfoFromFAndTDomain time:"+(date2.getTime()-date1.getTime()));
//                    mIndexMaxVarInfo = getIndexMaxVarInfoFromSignals(bufferedSamples,0,processBufferSize+LPreamble-1, upPreamble);

                    // 2. if the preamble exist, then decode the anchor ID
                    if (mIndexMaxVarInfo.isReferenceSignalExist && !isSignalRepeatedDetected(mIndexMaxVarInfo,processBufferSize)) {
                        mTDOACounter++;
                        date1 = new Date();
                        int anchorID = decodeAnchorID(bufferedSamples, true, mIndexMaxVarInfo);
                        date2 = new Date();
                        Log.v("","decodeAnchorID time:"+(date2.getTime()-date1.getTime()));
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
                    date1 = new Date();
                    mIndexMaxVarInfo = getIndexMaxVarInfoFromFAndTDomain(fft,downPreamble);
                    date2 = new Date();
                    Log.v("","getIndexMaxVarInfoFromFAndTDomain time:"+(date2.getTime()-date1.getTime()));
//                    mIndexMaxVarInfo = getIndexMaxVarInfoFromSignals(bufferedSamples, 0,processBufferSize+LPreamble-1,downPreamble);
                    if (mIndexMaxVarInfo.isReferenceSignalExist && !isSignalRepeatedDetected(mIndexMaxVarInfo,processBufferSize)) {
                        mTDOACounter++;
                        date1 = new Date();
                        int anchorID = decodeAnchorID(bufferedSamples, false, mIndexMaxVarInfo);
                        date2 = new Date();
                        Log.v("","decodeAnchorID time:"+(date2.getTime()-date1.getTime()));

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
                        date1 = new Date();
                        tdoa = processTDOAInformation();
                        date2 = new Date();
                        Log.v("","processTDOAInformation time:"+(date2.getTime()-date1.getTime()));
                    }

                    System.out.println("zize:"+samplesList.size()+"    mLoopCounter:"+mLoopCounter);
                    if(mLoopCounter > 100000){
                        Log.e("","buffer samples size >= 300");
                        return;
                    }
                    if(Math.abs(tdoa)>30 && Math.abs(tdoa) < 200000){
                        Log.v("","tdoa:"+tdoa);
                    }
                    Date dateE = new Date();
                    Log.v("","one loop time:"+(dateE.getTime()-dateS.getTime()));
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
            if(Math.abs(tdoa) > beconMessageLength){
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
