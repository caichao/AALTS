package hust.cc.asynchronousacousticlocalization.physical;

import hust.cc.asynchronousacousticlocalization.utils.FlagVar;

public class SignalGenerator implements FlagVar {

    /**
     * generate up chirp signal
     * @param fs - sampling rate
     * @param t - douration of the chirp signal
     * @param b - bandwidth
     * @param f - fmin
     * @return audio samples in short format
     */
    public static short[] upChirpGenerator(int fs, float t, int b, int f){
        float[] x = chirpGenerator(fs, t, b, f, 0);
        waveformReshaping(x);
        short[] samples = new short[x.length];
        for(int i = 0; i < x.length; i++){
            samples[i] = (short)(32767 * x[i]);
        }
        return samples;
    }

    /**
     * generate down chirp signal
     * @param fs - sampliong rate
     * @param t - duration
     * @param b - bandwidth
     * @param f - fmax
     * @return audio samples in short format
     */
    public static short[] downChirpGenerator(int fs, float t, int b, int f){
        float[] x = chirpGenerator(fs, t, b, f, 1);
        waveformReshaping(x);
        short[] samples = new short[x.length];
        for(int i = 0; i < x.length; i++){
            samples[i] = (short)(32767 * x[i]);
        }
        return samples;
    }

    /**
     *  generate the chirp signal in short format
     * @param fs - sampling rate
     * @param t - duration of the chirp signal
     * @param b - bandwidth of the chirp signal
     * @param f - fmin for up chirp signal and fmax for the down chirp signal
     * @param type - 0 for up chirp signal and 1 for down chirp signal
     * @return chirp samples in float format
     */
    public static float[] chirpGenerator(int fs, float t, int b, int f, int type){
        int n = (int)(fs * t);
        float[] samples = new float[n];
        if( type == 0 ) {
            for (int i = 0; i < n; i++) {
                samples[i] = (float)Math.cos(2 * Math.PI * f * i / fs + Math.PI * b * i * i / t / fs / fs);
            }
        }else{
            for (int i = 0; i < n; i++) {
                samples[i] = (float)Math.cos(2 * Math.PI * f * i / fs - Math.PI * b * i * i / t / fs / fs);
            }
        }
        return samples;
    }

    /**
     * waveform reshaping to mitigate the audible noise
     * slowly ramping up the amplitude of the 100 samples and reversely perform it on the last 100 samples
     * @param samples
     */
    public static void waveformReshaping(float samples[]){
        int k = 100;
        float coefficients = 1.0f / k;

        for(int i = 0; i < k; i++){
            samples[i] = samples[i] * (i + 1) / k;
            samples[samples.length - 1 - i] = samples[samples.length - 1 - i] * (i + 1) / k;
        }
    }
}
