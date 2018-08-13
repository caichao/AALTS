package hust.cc.asynchronousacousticlocalization.processing;

import android.util.Log;

import java.io.File;
import java.util.TreeMap;

import hust.cc.asynchronousacousticlocalization.utils.FileUtils;

public class DecoderThreadCC extends DecoderJNI implements Runnable{

    private int recordBufferSize = 4096;
    private int lastBufferSize = recordBufferSize / 2;
    private int processBufferSize = recordBufferSize + recordBufferSize / 2; // = 6144
    private boolean isThreadAlive = true;
    private volatile boolean isDataReady = false; // synchronization
    private volatile boolean isWaited = false;
    private String TAG = "DecoderThreadCC";

    private short[] processBuffer = null;
    private short[] lastDataBuffer = null;
    private short[] tempBuffer = null;
    private short[] copyBuffer = null;
    private short[] symbolRegion = null;
    private short[] outBuffer = null;
    private FIFO fifo = null;

    public DecoderThreadCC(){
        initParam(this.processBufferSize);
        processBuffer = new short[processBufferSize];
        lastDataBuffer = new short[lastBufferSize];
        tempBuffer = new short[recordBufferSize];
        copyBuffer = new short[beconMessageLength];
        symbolRegion = new short[getSymbolCorrLen()];
        outBuffer = new short[recordBufferSize];
        fifo = new FIFO(recordBufferSize);
    }

    public void fillSamples(short[] samples){
//        System.arraycopy(samples, 0, tempBuffer, 0, tempBuffer.length);
        //isDataReady = true;
        for(int i = 0; i < recordBufferSize; i++){
            tempBuffer[i] = samples[2*i + 1];
        }
        fifo.push(tempBuffer);
    }

    private void extractSamples(){
        fifo.pop(outBuffer);
        System.arraycopy(lastDataBuffer, 0, processBuffer, 0, lastBufferSize);
        System.arraycopy(outBuffer, 0, processBuffer, lastBufferSize, recordBufferSize);
        System.arraycopy(processBuffer, recordBufferSize, lastDataBuffer, 0, lastBufferSize);
    }

    @Override
    public void run() {
        CriticalPoint criticalPoint = null;
        int start = 0;
        int end = 0;
        int length = 0;
        int id = 0;
        int sequence = 0;
        while (isThreadAlive){
//            if(isDataReady == true){
//                isDataReady = false;
                extractSamples();
                long startTime = System.currentTimeMillis();   //获取开始时间
//            Log.e(TAG, "program is running");
                if(isWaited == true){
                    isWaited = false;
                    length = beconMessageLength - (processBufferSize - criticalPoint.index);
                    if(length > recordBufferSize){ // seldom, this could happen, but should not impact decoding, this is because we use a very short buffer size
                        length = recordBufferSize;
                    }
                    System.arraycopy(processBuffer, recordBufferSize/2, copyBuffer, processBufferSize - criticalPoint.index, length);
                    start = LPreamble + guardIntervalLength;
                    end = start + LSymbol;
                    System.arraycopy(copyBuffer, start, symbolRegion,0, LSymbol);
                    id = symbolDecoding(symbolRegion);
                    start = end + guardIntervalLength;
                    end = start + LSymbol;
                    System.arraycopy(copyBuffer, start, symbolRegion, 0,LSymbol);
                    sequence = symbolDecoding(symbolRegion);

                    Log.e(TAG, "[waited] id = " + id + " sequence =" + sequence);
//                    FileUtils.saveBytes(copyBuffer, "copyBuffer");
//                    while(true);
                    if(id != 0 || sequence != 1) {
                        FileUtils.saveBytes(processBuffer, "abnormalBuffer[waited]");
                        while(true);
                    }
                }else {
                    criticalPoint = preambleDetection(processBuffer);
//                    Log.e(TAG, criticalPoint.toString());
                    if (criticalPoint.isReferenceSignalExist) {
                        if (criticalPoint.index + beconMessageLength > processBufferSize) {
                            // copy the array
                            System.arraycopy(processBuffer, criticalPoint.index, copyBuffer, 0, processBufferSize - criticalPoint.index);
                            isWaited = true;
                        } else { // decode directly
                            start = criticalPoint.index + LPreamble + guardIntervalLength;
                            end = start + LSymbol;
                            System.arraycopy(processBuffer, start, symbolRegion, 0, LSymbol);
                            id = symbolDecoding(symbolRegion);
                            start = end + guardIntervalLength;
                            end = start + LSymbol;
                            System.arraycopy(processBuffer, start, symbolRegion, 0, LSymbol);
                            sequence = symbolDecoding(symbolRegion);
                            Log.e(TAG, "id = " + id + " sequence = " + sequence);
//                            FileUtils.saveBytes(processBuffer, "normal_data");
//                            while(true);
//                            if(id != 0 || sequence != 1) {
//                                FileUtils.saveBytes(processBuffer, "abnormalBuffer");
//                                while(true);
//                            }
                        }
                    }
                }
//                Log.e(TAG, criticalPoint.toString());
//                if(criticalPoint.isReferenceSignalExist) {
//                    Log.e(TAG, "------" + criticalPoint.toString());
//                }
                long endTime = System.currentTimeMillis();
                Log.e(TAG, "Time = " + (endTime - startTime));
//                Log.e(TAG, "Max = "+Algorithm.getMax(processBuffer, 0, processBufferSize));
//            }
        }
    }

    public void close(){
        isThreadAlive = false;
    }
}
