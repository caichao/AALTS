package hust.cc.asynchronousacousticlocalization.physical;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.Arrays;

import hust.cc.asynchronousacousticlocalization.utils.FlagVar;

public class PlayThread extends Thread implements FlagVar{

    /*
    This thread is used to play audio samples in PCM format
     */

    private boolean isRunning = true;
    private boolean isBufferReady = false;
    private int validBufferLenght = 0;
    private int minBufferSize = 0;
    private short[] buffer;
    public PlayThread(){
    // init the data
        // create a large buffer to store the waveform samples
        buffer = new short[FlagVar.bufferSize];
    }

    /**
     * fill the buffer to the audiotrack and play
     * @param data
     */
    public void fillBufferAndPlay(short[] data){
        // firt clear the buffer
        Arrays.fill(buffer, (short)(0));
        // copy short samples to the buffer
        System.arraycopy(data, 0, buffer, 0, data.length);

        // determine the write() function length
        if(data.length > minBufferSize){
            validBufferLenght = data.length;
        }else {
            validBufferLenght = minBufferSize;
        }
        synchronized (this){
            isBufferReady = true;
        }
    }

    /**
     * run process, always stay alive until the end of the program
     */
    public void run(){

        AudioTrack audiotrack;
        // get the minimum buffer size
        minBufferSize = AudioTrack.getMinBufferSize(
                FlagVar.Fs,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        // initialize the audiotrack
        audiotrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                FlagVar.Fs,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize,
                AudioTrack.MODE_STREAM);

        while (isRunning){
            if(isBufferReady){
                audiotrack.play();
                audiotrack.write(buffer,0,validBufferLenght);
            }
        }
    }

    /*
    shut down the thread
     */
    public void close(){
        isRunning = false;
    }
}
