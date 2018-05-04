package hust.cc.asynchronousacousticlocalization.physical;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Process;
import android.util.Log;

import java.io.FileNotFoundException;

/**
 * Created by cc on 2016/10/12.
 */

public class AudioRecorder implements IAudioRecorder{

    public static final int RECORDER_SAMPLE_RATE = 48000;
    public static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_OUT_MONO;
    public static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;


    private static final int BUFFER_BYTES_ELEMENTS = 1024;
    private static final int BUFFER_BYTES_PER_ELEMENT = RECORDER_AUDIO_ENCODING;
    private static final int RECORDER_CHANNELS_IN = AudioFormat.CHANNEL_IN_MONO;


    public static final int RECORDER_STATE_FAILURE = -1;
    public static final int RECORDER_STATE_IDLE = 0;
    public static final int RECORDER_STATE_STARTING = 1;
    public static final int RECORDER_STATE_STOPPING = 2;
    public static final int RECORDER_STATE_BUSY = 3;

    private volatile int recorderState = RECORDER_STATE_IDLE;

    private final Object recorderStateMonitor = new Object();

    private RecordingCallback recordingCallback;

    public AudioRecorder recordingCallback(RecordingCallback recordingCallback) {
        this.recordingCallback = recordingCallback;
        return this;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void onRecordFailure() {
        recorderState = RECORDER_STATE_FAILURE;
        finishRecord();
    }

    @Override
    public void startRecord() {
        if (recorderState != RECORDER_STATE_IDLE) {
            return;
        }

        try {
            recorderState = RECORDER_STATE_STARTING;

            startRecordThread();
        } catch (FileNotFoundException e) {
            onRecordFailure();
            e.printStackTrace();
        }
    }

    private void startRecordThread() throws FileNotFoundException {

        new Thread(new PriorityRunnable(Process.THREAD_PRIORITY_AUDIO) {

            private void onExit() {
                synchronized (recorderStateMonitor) {
                    recorderState = RECORDER_STATE_IDLE;
                    recorderStateMonitor.notifyAll();
                }
            }


            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public void runImpl() {
                int bufferSize = getBufferSize();

                AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLE_RATE, RECORDER_CHANNELS_IN, RECORDER_AUDIO_ENCODING, bufferSize);
                if(recorder.getState() == AudioRecord.STATE_UNINITIALIZED){
                    Log.e(AudioRecorder.class.getSimpleName(),"*******************************Initialize audio recorder error");
                    return;
                }else{
                    Log.d(AudioRecorder.class.getSimpleName(),"-------------------------------Initialize AudioRecord ok");
                }
                try {
                    if (recorderState == RECORDER_STATE_STARTING) {
                        recorderState = RECORDER_STATE_BUSY;
                    }
                    recorder.startRecording();

                    short recordBuffer[] = new short[bufferSize];
                    do {
                        int len = recorder.read(recordBuffer, 0, bufferSize);

                        if (len > 0) {
                            recordingCallback.onDataReady(recordBuffer,len);
                        } else {
                            Log.e(AudioRecorder.class.getSimpleName(), "error: " + len);
                            onRecordFailure();
                        }
                    } while (recorderState == RECORDER_STATE_BUSY);
                } finally {
                    recorder.release();
                }
                onExit();
            }
        }).start();
    }

    @Override
    public void finishRecord() {
        int recorderStateLocal = recorderState;
        if (recorderStateLocal != RECORDER_STATE_IDLE) {
            synchronized (recorderStateMonitor) {
                recorderStateLocal = recorderState;
                if (recorderStateLocal == RECORDER_STATE_STARTING
                        || recorderStateLocal == RECORDER_STATE_BUSY) {

                    recorderStateLocal = recorderState = RECORDER_STATE_STOPPING;
                }

                do {
                    try {
                        if (recorderStateLocal != RECORDER_STATE_IDLE) {
                            recorderStateMonitor.wait();
                        }
                    } catch (InterruptedException ignore) {
                        /* Nothing to do */
                    }
                    recorderStateLocal = recorderState;
                } while (recorderStateLocal == RECORDER_STATE_STOPPING);
            }
        }
    }

    public static int getBufferSize(){
        int bufferSize = Math.max(BUFFER_BYTES_ELEMENTS * BUFFER_BYTES_PER_ELEMENT,
                AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE, RECORDER_CHANNELS_IN, RECORDER_AUDIO_ENCODING));

        int size = 1024;
        while(size < bufferSize){
            size = size * 2;
        }
        bufferSize = size;
        return bufferSize;
    }


    @Override
    public boolean isRecording() {
        return recorderState != RECORDER_STATE_IDLE;
    }

    public interface RecordingCallback {
        void onDataReady(short[] data, int bytelen);
    }


}
