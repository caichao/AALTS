package hust.cc.asynchronousacousticlocalization.processing;

import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import hust.cc.asynchronousacousticlocalization.activity.MainActivity;
import hust.cc.asynchronousacousticlocalization.utils.CapturedBeaconMessage;
import hust.cc.asynchronousacousticlocalization.utils.FlagVar;
import hust.cc.asynchronousacousticlocalization.utils.JSONUtils;
import hust.cc.asynchronousacousticlocalization.utils.JniUtils;
import hust.cc.asynchronousacousticlocalization.utils.SpeedInfo;

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
    //list data to store samples from microphone.
    public List<short[]> unhandledSampleList;
    public List<short[]> samplesList;
    private List<String> ratioStrs;


    public short[] testData = new short[4096*4*100];
    public List<Short> testIndex = new LinkedList<>();
    private short testI = 0;
    public int[] testCounters = new int[100];

    //whether the chirp signal is detected in the last sample's processing.
    private boolean lastDetected = false;
    private boolean upPreambleRecv = false;
    private boolean downPreambleRecv = false;
    IndexMaxVarInfo infoUp;
    IndexMaxVarInfo infoDown;
    private int beconCnt = 0;
    private int errorCnt = 0;
    private float speed;
    private List<CapturedBeaconMessage> cbMsgs;
    private int[] ids;

    private IndexMaxVarInfo mIndexMaxVarInfo;

    public DecodThread(Handler mHandler){
        mIndexMaxVarInfo = new IndexMaxVarInfo();
        mTDOAUtils = new TDOAUtils();
        preambleInfoList = new ArrayDeque<>();
        samplesList = new LinkedList<short[]>();
        unhandledSampleList = new LinkedList<>();
        cbMsgs = new LinkedList<>();
        ratioStrs = new LinkedList<>();
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

    /**
     * data processing method
     * @auther ruinan jin
     */
    @Override
    public void run() {
        try {
            while (isThreadRunning) {
//                runByStepOnTightOrthotropic();
//                runByStepOnLooseOrthotropic();
                runByStepOnLoose();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * data processing method per sampling
     * @auther ruinan jin
     */
    private void runByStepOnLoose(){

//        pretreatmentData();
        if(samplesList.size() >= 3){
            Date date1 = new Date();
            mLoopCounter++;
            short[] buffer = new short[processBufferSize+LPreamble+startBeforeMaxCorr];
            //we use totally the last samples and partly this samples in case the misdetection while the chirp is cut off by the sampling.
            synchronized (samplesList){
                System.arraycopy(samplesList.get(0),0,buffer,0,processBufferSize);
                System.arraycopy(samplesList.get(1),0,buffer,processBufferSize,LPreamble+startBeforeMaxCorr);
            }

            //compute the fft of the buffer.
            float[] fft = JniUtils.fft(normalization(buffer),buffer.length+ LPreamble);
//            fft = frequencyFiltering(fft);
            //compute the corr of the buffer and the upPreamble.
            infoUp = getIndexMaxVarInfoFromFDomain(fft,upPreambleFFT);

            if(infoUp.isReferenceSignalExist){
                //we only handle the situations that we didn't got the chirp in the last sample's processing while we get it in this one.
                if(!upPreambleRecv) {
                    synchronized (samplesList) {
                        System.arraycopy(samplesList.get(0), 0, bufferUp, 0, processBufferSize);
                        System.arraycopy(samplesList.get(1), 0, bufferUp, processBufferSize, processBufferSize);
                        System.arraycopy(samplesList.get(2), 0, bufferUp, processBufferSize * 2, processBufferSize);
                    }
                    beconCnt++;
                    //decode the anchor and the sequence id.
                    ids = decodeAnchorSeqId(bufferUp, infoUp);
                    if(ids[0] != 0){
                        errorCnt++;
                    }
                    if(ids[1] != 2){
                        errorCnt++;
                    }

                    //compute the speed. the speed estimating information is emitting just the same time of the preamble, so we should set buffer like this.
                    short[] bufferSpeed = new short[beconMessageLength];
                    System.arraycopy(bufferUp,infoUp.index,bufferSpeed,0,beconMessageLength);
                    Date date3 = new Date();
                    processSpeedInformation(bufferSpeed);
                    Date date4 = new Date();
                    soutDateDiff("processSpeedInformation",date3,date4);
                    //send the info to the handler.
                    sendMsg();
                    //compute the sampling diff between becons.
                    calBeconDiff();
//                        if (testI < 100) {
//                            System.arraycopy(bufferUp, 0, testData, processBufferSize * (testI * 4+1), processBufferSize * 3);
//                            testCounters[testI] = mLoopCounter;
//
//                        }//
//                        testI++;
                }
                upPreambleRecv = true;
            }else{
                upPreambleRecv = false;
            }
            sendRatio();

            synchronized (samplesList){
                samplesList.remove(0);
            }

            Date date2 = new Date();
            soutDateDiff("process time",date1,date2);


//            System.out.println("size:"+samplesList.size()+"    mLoopCounter:"+mLoopCounter+"    tdoa:"+tdoa);
        }
    }

    private void sendRatio(){
        String str = "maRatio:"+maRatio+"  ratio:"+ratio;
        ratioStrs.add(str);
        if(ratioStrs.size()>10){
            ratioStrs.remove(0);
        }
        StringBuilder sb = new StringBuilder();
        for(String s:ratioStrs){
            sb.append(s).append("\n");
        }

        Message msg = new Message();
        msg.what = MESSAGE_RATIO;
        msg.obj = sb.toString();
        mHandler.sendMessage(msg);
    }

    private float[] frequencyFiltering(float [] data){
        int len = data.length;
        float[] res = new float[len];
        int startI = bandPassLow*len/2/Fs;
        int endI = bandPassHigh*len/2/Fs;
        for(int i=0;i<res.length/2;i++){
            if(i <= startI || i >= endI){
                res[2*i] = 0;
                res[2*i+1] = 0;
            }else{
                res[2*i] = data[2*i];
                res[2*i+1] = data[2*i+1];
            }
        }
        return res;
    }

    private void pretreatmentData(){
        while (unhandledSampleList.size() >0){
            short[] data = unhandledSampleList.get(0);
            short[] data1 = new short[data.length/2];
            short[] data2 = new short[data.length/2];
            for(int i=0;i<data.length/2;i++){
                data1[i] = data[2 * i];
                data2[i] = data[2 * i + 1];
            }
            samplesList.add(data2);
            synchronized (unhandledSampleList){
                unhandledSampleList.remove(0);
            }
        }
    }

    public void clear(){
        beconCnt = 0;
        errorCnt = 0;
    }

    public int getBeconCnt(){
        return beconCnt;
    }
    public int getErrorCnt() {
        return errorCnt;
    }

    private void sendMsg(){
        String jsonStr = encodeJsonMsg();
        Message msg = new Message();
        msg.what = MESSAGE_JSON;
        msg.obj = jsonStr;
        mHandler.sendMessage(msg);
    }

    /**
     * encode the capture beacon message to json.
     * @return
     */
    private String encodeJsonMsg(){
        CapturedBeaconMessage cbMsg = new CapturedBeaconMessage();
        cbMsg.capturedAnchorId = ids[0];
        cbMsg.capturedSequence = ids[1];
        cbMsg.looperCounter = mLoopCounter;
        cbMsg.preambleIndex = infoUp.index;
        cbMsg.selfAnchorId = MainActivity.identity;
        cbMsg.speed = speed;

        cbMsgs.add(cbMsg);
        if(cbMsgs.size() > 10){
            cbMsgs.remove(0);
        }
        String jsonStr = "";
        try {
            jsonStr = JSONUtils.toJson(cbMsg);
        }catch (Exception e){
            e.printStackTrace();
        }
        return jsonStr;
    }

    /**
     * compute the sampling diff between becons. it's a debug method.
     * @return diff
     */
    private long calBeconDiff(){
        if(cbMsgs.size() < 2){
            return 0;
        }
        CapturedBeaconMessage cbMsg1 = cbMsgs.get(cbMsgs.size()-1);
        CapturedBeaconMessage cbMsg2 = cbMsgs.get(cbMsgs.size()-2);
        long index1 = cbMsg1.looperCounter*processBufferSize+cbMsg1.preambleIndex;
        long index2 = cbMsg2.looperCounter*processBufferSize+cbMsg2.preambleIndex;
        long diff = index1-index2-19680;

        Message msg = new Message();
        msg.what = MESSAGE_DIFF;
        msg.obj = (Long)diff;
        mHandler.sendMessage(msg);
        return diff;
    }

    /**
     * old data processing method per sampling. it's abandoned.
     */
    private void runByStepOnLooseOrthotropic(){
        if(samplesList.size() >= 3){
            short[] buffer = new short[processBufferSize+LPreamble+startBeforeMaxCorr];
            synchronized (samplesList){
                System.arraycopy(samplesList.get(0),0,buffer,0,processBufferSize);
                System.arraycopy(samplesList.get(1),0,buffer,processBufferSize,LPreamble+startBeforeMaxCorr);
            }
            mLoopCounter++;
            float[] fft = JniUtils.fft(normalization(buffer),buffer.length+ LPreamble);

            infoUp = getIndexMaxVarInfoFromFDomain(fft,upPreambleFFT);
            infoDown = getIndexMaxVarInfoFromFDomain(fft,downPreambleFFT);

            if(infoUp.isReferenceSignalExist){
                if(!upPreambleRecv) {
                    mTDOACounter++;
                    synchronized (samplesList) {
                        System.arraycopy(samplesList.get(0), 0, bufferUp, 0, processBufferSize);
                        System.arraycopy(samplesList.get(1), 0, bufferUp, processBufferSize, processBufferSize);
                        System.arraycopy(samplesList.get(2), 0, bufferUp, processBufferSize * 2, processBufferSize);
                    }
                    ;
                    int anchorID = decodeAnchorIDOnOrthotropic(bufferUp, true, infoUp);
                    TDOAUtils tdoaUtils = new TDOAUtils();
                    // store the timming information
                    tdoaUtils.loopIndex = mLoopCounter;
                    tdoaUtils.preambleType = FlagVar.UP_PREAMBLE;
                    tdoaUtils.timeIndex = infoUp.index;
                    tdoaUtils.TDOACounter = mTDOACounter;
                    tdoaUtils.correspondingAnchorID = anchorID;
                    preambleInfoList.add(tdoaUtils);
                }
                upPreambleRecv = true;
            }else{
                upPreambleRecv = false;
            }

            if(infoDown.isReferenceSignalExist){
                if(!downPreambleRecv) {
                    mTDOACounter++;
                    synchronized (samplesList) {
                        System.arraycopy(samplesList.get(0), 0, bufferDown, 0, processBufferSize);
                        System.arraycopy(samplesList.get(1), 0, bufferDown, processBufferSize, processBufferSize);
                        System.arraycopy(samplesList.get(2), 0, bufferDown, processBufferSize * 2, processBufferSize);
                    }
                    int anchorID = decodeAnchorIDOnOrthotropic(bufferDown, false, infoDown);
                    TDOAUtils tdoaUtils = new TDOAUtils();
                    // store the timming information
                    tdoaUtils.loopIndex = mLoopCounter;
                    tdoaUtils.preambleType = FlagVar.DOWN_PREAMBLE;
                    tdoaUtils.timeIndex = infoDown.index;
                    tdoaUtils.TDOACounter = mTDOACounter;
                    tdoaUtils.correspondingAnchorID = anchorID;
                    preambleInfoList.add(tdoaUtils);
                }
                downPreambleRecv = true;
            }else{
                downPreambleRecv = false;
            }

            synchronized (samplesList){
                samplesList.remove(0);
            }

            int tdoa = Integer.MIN_VALUE;
            // 4. process the TDOA time information
            if (mTDOACounter >= 2) {// receive two TDOA timming information
                if (mTDOACounter == 3) {
                    preambleInfoList.pollFirst();
                }
                tdoa = processTDOAInformation();
            }
            //System.out.println("size:"+samplesList.size()+"    mLoopCounter:"+mLoopCounter+"    tdoa:"+tdoa);
        }
    }

    /**
     * old data processing method per sampling. it's abandoned.
     */
    private void runByStepOnTightOrthotropic(){
        if (samplesList.size() >= 3) {
            Date date1,dateS = new Date();
            Date date2;
            synchronized (samplesList) {
                System.arraycopy(samplesList.get(0),processBufferSize-LPreamble-startBeforeMaxCorr,bufferedSamples,0,LPreamble+startBeforeMaxCorr);
                System.arraycopy(samplesList.get(1),0,bufferedSamples,LPreamble+startBeforeMaxCorr,processBufferSize);
                System.arraycopy(samplesList.get(2),0,bufferedSamples,processBufferSize+LPreamble+startBeforeMaxCorr,beconMessageLength);
            }
            mLoopCounter++;
            //compute the fft of the bufferedSamples, it will be used twice. It's computed here to reduce time cost.
            float[] fft;
            float[] samplesF = normalization(bufferedSamples,0,processBufferSize+LPreamble+startBeforeMaxCorr-1);
            fft = JniUtils.fft(samplesF,samplesF.length+ LPreamble);

            // 1. the first step is to check the existence of preamble either up or down
            mIndexMaxVarInfo.isReferenceSignalExist = false;
            date1 = new Date();
            mIndexMaxVarInfo = getIndexMaxVarInfoFromFDomain(fft,upPreambleFFT);
            date2 = new Date();
//            soutDateDiff("getIndexMaxVarInfoFromFDomain",date1,date2);


            // 2. if the preamble exist, then decode the anchor ID
            if (mIndexMaxVarInfo.isReferenceSignalExist && isIndexAvailable(mIndexMaxVarInfo) ) {
                mTDOACounter++;
                date1 = new Date();
                int anchorID = decodeAnchorIDOnOrthotropic(bufferedSamples, true, mIndexMaxVarInfo);
                date2 = new Date();
//                soutDateDiff("decodeAnchorID",date1,date2);
//                System.out.println("anchorID 1:"+anchorID+"   ");
                TDOAUtils tdoaUtils = new TDOAUtils();
                // store the timming information
                tdoaUtils.loopIndex = mLoopCounter;
                tdoaUtils.preambleType = FlagVar.UP_PREAMBLE;
                tdoaUtils.timeIndex = mIndexMaxVarInfo.index;
                tdoaUtils.TDOACounter = mTDOACounter;
                tdoaUtils.correspondingAnchorID = anchorID;
                preambleInfoList.add(tdoaUtils);

                if(testI<100) {
                    System.arraycopy(samplesList.get(0), 0, testData, 4096 * testI, 4096);
                    if(!lastDetected){
                        testIndex.add(testI);
//                        System.out.println("testI:"+testI);
                    }
                }

                lastDetected = true;
                testI++;

            }else{
                lastDetected = false;
            }

            // 3. check the down preamble and do the above operation again
            mIndexMaxVarInfo.isReferenceSignalExist = false;
            date1 = new Date();
            mIndexMaxVarInfo = getIndexMaxVarInfoFromFDomain(fft,downPreambleFFT);
            date2 = new Date();
//            soutDateDiff("getIndexMaxVarInfoFromFDomain",date1,date2);
            if (mIndexMaxVarInfo.isReferenceSignalExist && isIndexAvailable(mIndexMaxVarInfo) ) {
                mTDOACounter++;
                date1 = new Date();
                int anchorID = decodeAnchorIDOnOrthotropic(bufferedSamples, false, mIndexMaxVarInfo);
                date2 = new Date();
//                soutDateDiff("decodeAnchorID",date1,date2);
//                System.out.println("anchorID 2:"+anchorID+"   ");
                TDOAUtils tdoaUtils = new TDOAUtils();
                tdoaUtils.loopIndex = mLoopCounter;
                tdoaUtils.preambleType = FlagVar.DOWN_PREAMBLE;
                tdoaUtils.timeIndex = mIndexMaxVarInfo.index;
                tdoaUtils.TDOACounter = mTDOACounter;
                tdoaUtils.correspondingAnchorID = anchorID;

                preambleInfoList.add(tdoaUtils);

            }

            synchronized (samplesList){
                samplesList.remove(0);
            }

            date1 = new Date();
            processSpeedInformation(bufferedSamples);
            date2 = new Date();
//            soutDateDiff("processSpeedInformation",date1,date2);

            int tdoa = Integer.MIN_VALUE;
            // 4. process the TDOA time information
            if (mTDOACounter >= 2) {// receive two TDOA timming information
                if (mTDOACounter == 3) {
                    preambleInfoList.pollFirst();
                }
                tdoa = processTDOAInformation();
            }

            //System.out.println("size:"+samplesList.size()+"    mLoopCounter:"+mLoopCounter+"    tdoa:"+tdoa);
            if(mLoopCounter > 100000){
                return;
            }
            Date dateE = new Date();
//            System.out.println("total time:"+(dateE.getTime()-dateS.getTime()));
        }
    }


    public void processSpeedInformation(short[] buffer){
        //calculate the speed based on doppler effect.
        SpeedInfo info = estimateSpeed(normalization(buffer),sinSigF,speedDetectionSigLength,speedDetectionRangeF,Fs,soundSpeed);
        speed = 0-info.speed;
        speed = speed-sOffset;

        Message msg = new Message();
        msg.what = MESSAGE_SPEED;
        msg.obj = info;
        mHandler.sendMessage(msg);


    }

    public float getSpeed(){
        return speed;
    }

    /**
     * obtain TDOA information from the preambleInfoList. it's abandoned.
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
