package hust.cc.asynchronousacousticlocalization.processing;

import android.util.Log;

public class DecoderThreadCC extends DecoderCC implements Runnable{

    private int recordBufferSize = 4096;
    private int lastBufferSize = recordBufferSize / 2;
    private int processBufferSize = recordBufferSize + recordBufferSize / 2; // = 6144
    private boolean isThreadAlive = true;
    private volatile boolean isDataReady = false; // synchronization
    private String TAG = "DecoderThreadCC";

    private short[] processBuffer = null;
    private short[] lastDataBuffer = null;
    private short[] tempBuffer = null;

    public DecoderThreadCC(){
        initParam(this.processBufferSize);
        processBuffer = new short[processBufferSize];
        lastDataBuffer = new short[lastBufferSize];
        tempBuffer = new short[recordBufferSize * 2];
    }

    public void fillSamples(short[] samples){
        System.arraycopy(samples, 0, tempBuffer, 0, tempBuffer.length);
        isDataReady = true;
    }

    private void extractSamples(){
        System.arraycopy(lastDataBuffer, 0, processBuffer, 0, lastDataBuffer.length);
        for(int i = 0; i < recordBufferSize; i++){
            processBuffer[i] = tempBuffer[2*i+1];
        }
    }

    @Override
    public void run() {
        CriticalPoint criticalPoint = null;
        while (isThreadAlive){
            if(isDataReady == true){
                isDataReady = false;
                extractSamples();
                long startTime = System.currentTimeMillis();   //获取开始时间
                criticalPoint = preambleDetection(processBuffer);
                if(criticalPoint.isReferenceSignalExist) {
//                    Log.e(TAG, "------" + criticalPoint.toString());
                }
                long endTime = System.currentTimeMillis();
                Log.e(TAG, "Time = " + (endTime - startTime));
//                Log.e(TAG, "Max = "+Algorithm.getMax(processBuffer, 0, processBufferSize));
            }
        }
    }

    public void close(){
        isThreadAlive = false;
    }
}
