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
import hust.cc.asynchronousacousticlocalization.utils.JniUtils;

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
                    Date date1,dateS = new Date();
                    Date date2;
                    synchronized (samplesList) {
                        System.arraycopy(samplesList.get(0),processBufferSize-LPreamble-startBeforeMaxCorr,bufferedSamples,0,LPreamble+startBeforeMaxCorr);
                        System.arraycopy(samplesList.get(1),0,bufferedSamples,LPreamble+startBeforeMaxCorr,processBufferSize);
                        System.arraycopy(samplesList.get(2),0,bufferedSamples,processBufferSize+LPreamble+startBeforeMaxCorr,beconMessageLength);

                        samplesList.remove(0);

                    }
                    mLoopCounter++;
                    //compute the fft of the bufferedSamples, it will be used twice. It's computed here to reduce time cost.
                    float[] fft;
                    if(!useJni) {
                        fft = getData1HalfFFtFromSignals(bufferedSamples, 0, processBufferSize+LPreamble+startBeforeMaxCorr- 1, upPreamble.length);
                    }else {
                        float[] samplesF = normalization(bufferedSamples,0,processBufferSize+LPreamble+startBeforeMaxCorr-1);
                        fft = JniUtils.fft(samplesF,samplesF.length+ LPreamble);
                    }

                    //open this to see corr in graphs
//                    testGraph(fft);

                    // 1. the first step is to check the existence of preamble either up or down
                    mIndexMaxVarInfo.isReferenceSignalExist = false;
                    date1 = new Date();
                    mIndexMaxVarInfo = getIndexMaxVarInfoFromFDomain(fft,upPreambleFFT);
                    date2 = new Date();
                    soutDateDiff("getIndexMaxVarInfoFromFDomain",date1,date2);

                    // 2. if the preamble exist, then decode the anchor ID
                    if (mIndexMaxVarInfo.isReferenceSignalExist && isIndexAvailable(mIndexMaxVarInfo) ) {
                        mTDOACounter++;
                        date1 = new Date();
                        int anchorID = decodeAnchorID(bufferedSamples, true, mIndexMaxVarInfo);
                        date2 = new Date();
                        soutDateDiff("decodeAnchorID",date1,date2);
//                        System.out.println("anchorID 1:"+anchorID+"   ");
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
                    mIndexMaxVarInfo = getIndexMaxVarInfoFromFDomain(fft,downPreambleFFT);
                    date2 = new Date();
                    soutDateDiff("getIndexMaxVarInfoFromFDomain",date1,date2);
                    if (mIndexMaxVarInfo.isReferenceSignalExist && isIndexAvailable(mIndexMaxVarInfo) ) {
                        mTDOACounter++;
                        date1 = new Date();
                        int anchorID = decodeAnchorID(bufferedSamples, false, mIndexMaxVarInfo);
                        date2 = new Date();
                        soutDateDiff("decodeAnchorID",date1,date2);
//                        System.out.println("anchorID 2:"+anchorID+"   ");
                        TDOAUtils tdoaUtils = new TDOAUtils();
                        tdoaUtils.loopIndex = mLoopCounter;
                        tdoaUtils.preambleType = FlagVar.DOWN_PREAMBLE;
                        tdoaUtils.timeIndex = mIndexMaxVarInfo.index;
                        tdoaUtils.TDOACounter = mTDOACounter;
                        tdoaUtils.correspondingAnchorID = anchorID;

                        preambleInfoList.add(tdoaUtils);

                    }


                    date1 = new Date();
//                    processSpeedInformation();
                    date2 = new Date();
                    soutDateDiff("processSpeedInformation",date1,date2);

                    int tdoa = Integer.MIN_VALUE;
                    // 4. process the TDOA time information
                    if (mTDOACounter >= 2) {// receive two TDOA timming information
                        if (mTDOACounter == 3) {
                            preambleInfoList.pollFirst();
                        }
                        tdoa = processTDOAInformation();
                    }

                    System.out.println("size:"+samplesList.size()+"    mLoopCounter:"+mLoopCounter+"    tdoa:"+tdoa);
                    if(mLoopCounter > 100000){
                        return;
                    }
                    if((Math.abs(tdoa)>30 && Math.abs(tdoa) < 200000)) {
                        Log.v("","tdoa:"+tdoa);
                    }
                    Date dateE = new Date();
                    System.out.println("total time:"+(dateE.getTime()-dateS.getTime()));
                }


            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void processSpeedInformation(){
        float fshift = getFshift(normalization(bufferedSamples),sinSigF,speedDetectionSigLength,speedDetectionRangeF,Fs);
        float speed = (int)(fshift*soundSpeed/Fs);
        Message msg = new Message();
        msg.what = MESSAGE_SPEED;
        msg.arg1 = (int)speed;
        mHandler.sendMessage(msg);
    }

    public void testGraph(float[] fft){
        float[] data = xcorr(fft,normalization(upPreamble),true);
        int index1 = getFitPos(data);
        synchronized (graphBuffer) {
            System.arraycopy(data, 0, graphBuffer, 0, graphBuffer.length);
        }
        data = xcorr(fft,normalization(downPreamble),true);
        int index2 = getFitPos(data);
        synchronized (graphBuffer2) {
            System.arraycopy(data, 0, graphBuffer2, 0, graphBuffer2.length);
        }

        int tdoaTest = index2-index1;
        System.out.println("tdoa:"+tdoaTest);
        if(Math.abs(tdoaTest) > 30){
            System.out.println("out of range");
        }

        Message msg = new Message();
        msg.what = MESSAGE_GRAPH;
        mHandler.sendMessage(msg);
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

    public void soutDateDiff(String str, Date date1, Date date2){
        System.out.println(str+":"+(date2.getTime()-date1.getTime()));
    }
}
