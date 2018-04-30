package hust.cc.asynchronousacousticlocalization.processing;

import android.os.Handler;
import android.os.Message;

import java.util.ArrayDeque;
import java.util.Deque;

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

    private IndexMaxVarInfo mIndexMaxVarInfo;

    public DecodThread(Handler mHandler){
        mIndexMaxVarInfo = new IndexMaxVarInfo();
        mTDOAUtils = new TDOAUtils();
        preambleInfoList = new ArrayDeque<>();
        this.mHandler = mHandler;
    }

    /**
     * copy the samples from the audio buffer
     * @param s - input coming from the audio buffer
     */
    public void fillSamples(short[] s){
        synchronized (this){
            isBufferReady = true;
        }
        System.arraycopy(s, s.length, this.bufferedSamples, 0, s.length);
    }

    @Override
    public void run() {

        while (isThreadRunning){
            if(isBufferReady){
                mLoopCounter ++;
                // 1. the first step is to check the existence of preamble either up or down
                mIndexMaxVarInfo.isReferenceSignalExist = false;
                mIndexMaxVarInfo = xcorrFast(bufferedSamples, upPreamble);

                // 2. if the preamble exist, then decode the anchor ID
                if(mIndexMaxVarInfo.isReferenceSignalExist){
                    mTDOACounter ++;

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
                mIndexMaxVarInfo = xcorrFast(bufferedSamples, downPreamble);
                if(mIndexMaxVarInfo.isReferenceSignalExist){
                    mTDOACounter ++;

                    int anchorID = decodeAnchorID(bufferedSamples, false, mIndexMaxVarInfo);
                    TDOAUtils tdoaUtils = new TDOAUtils();

                    tdoaUtils.loopIndex = mLoopCounter;
                    tdoaUtils.preambleType = FlagVar.DOWN_PREAMBLE;
                    tdoaUtils.timeIndex = mIndexMaxVarInfo.index;
                    tdoaUtils.TDOACounter = mTDOACounter;
                    tdoaUtils.correspondingAnchorID = anchorID;

                    preambleInfoList.add(tdoaUtils);

                }

                // 4. process the TDOA time information
                if(mTDOACounter == 2) {// receive two TDOA timming information
                    processTDOAInformation();
                }
            }
        }

    }

    /*
    obtain TDOA information from the preambleInfoList
     */
    private void processTDOAInformation(){
        // remove the first anchor information
        TDOAUtils mFirstAnchorInfo = preambleInfoList.pollFirst();
        // peek the last one
        TDOAUtils mSecondAnchorInfo = preambleInfoList.peekFirst();

        // if the preamble type of the two decode information is not the same, then we can get the tdoa information
        if(mFirstAnchorInfo.preambleType != mSecondAnchorInfo.preambleType){
            // post the information to the main thread
            Message message = mHandler.obtainMessage();
            message.what = FlagVar.MESSAGE_TDOA;


            int tdoa = (mFirstAnchorInfo.loopIndex - mSecondAnchorInfo.loopIndex) * processBufferSize + mFirstAnchorInfo.timeIndex - mSecondAnchorInfo.timeIndex;
            message.arg1 = tdoa;
            // the first four bits store the first anchor id, the last four bit store the second anchor ID
            message.arg2 = mFirstAnchorInfo.correspondingAnchorID << 4 | mSecondAnchorInfo.correspondingAnchorID;

            mHandler.sendMessage(message);

        } else{
            mTDOACounter = 1;
            preambleInfoList.pollFirst(); // remove the first preamble
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
