package hust.cc.asynchronousacousticlocalization.utils;

public interface FlagVar {

    //sampling rate
    public int Fs = 48000;

    // buffer length to store the samples for the audiotrack (for play thread)
    public int bufferSize = 20480;
    // buffer length to process the samples for the audiorecorder (for the decoder thread)
    public int processBufferSize = 9600;

    /*********** constant parameters ***************************/
    // the constant parameters for the signal representation
    public static final int UP_PREAMBLE = 1;
    public static final int DOWN_PREAMBLE = 2;
    public static final int UP_SYMBOL = 3;
    public static final int DOWN_SYMBOL = 4;

    // message type constant
    public static final int MESSAGE_TDOA = 50;
    //public static final int

    /*****************************************************/

    /*****************************************************/
    // parameters for the preamble
    public double TPreamble = 0.04;
    public int BPreamble = 4000;
    public int Fmin = 17500;
    public int Fmax = 21500;
    public int preambleLength = (int)(Fs * TPreamble);

    // parameter for the symbols
    public double TSymbol = 0.03;
    public int BSymbol = 1000;
    public int [] FUpSymbol = new int[]{17500, 18500, 19500, 20500};
    public int [] FDownSymbol = new int[]{21500, 20500, 19500, 18500};
    public final int symbolLength = (int)(Fs * TSymbol);
    public int numberOfSymbols = 4;

    // guard interval
    public double guardInterval = 0.005;
    public int guardIntervalLength = (int)(guardInterval * Fs);

    /*****************************************************/

    /***********************threshold parameters*********************/
    public double preambleDetectionThreshold= 0.03;
    public int numberOfPreviousSamples = 100;
    public double ratioThreshold = 5;
    /*****************************************************/
}
