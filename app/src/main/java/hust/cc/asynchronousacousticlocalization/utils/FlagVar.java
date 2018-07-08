package hust.cc.asynchronousacousticlocalization.utils;

import java.math.BigDecimal;

public interface FlagVar {

    //public int anchorID = 1;

    //sampling rate
    int Fs = 48000;

    // buffer length to store the samples for the audiotrack (for play thread)
    int bufferSize = 20480;
    // buffer length to process the samples for the audiorecorder (for the decoder thread)

    /*********** constant parameters ***************************/
    // the constant parameters for the signal representation
    int UP_PREAMBLE = 1;
    int DOWN_PREAMBLE = 2;
    int UP_SYMBOL = 3;
    int DOWN_SYMBOL = 4;

    // message type constant
    int MESSAGE_TDOA = 50;
    int MESSAGE_GRAPH = 51;
    int MESSAGE_SPEED = 52;
    int MESSAGE_JSON = 53;
    //public static final int

    /*****************************************************/

    /*****************************************************/
    // parameters for the preamble
    float TPreamble = 0.04f;
    int BPreamble = 4000;
    int Fmin = 18000;
    int Fmax = 22000;

    // parameter for the symbols
    float TSymbol = 0.03f;
    int BSymbol = 1000;
    int [] FUpSymbol = new int[]{18000, 19000, 20000, 21000};
    int [] FDownSymbol = new int[]{22000, 21000, 20000, 19000};
    int numberOfSymbols = 4;

    // guard interval
    float guardInterval = 0.005f;
    int guardIntervalLength = (int)(new BigDecimal(guardInterval * Fs).setScale(0, BigDecimal.ROUND_HALF_UP).floatValue());

    /*****************************************************/

    /***********************threshold parameters*********************/
    float preambleDetectionThreshold= 0.5f;
    int numberOfPreviousSamples = 100;
    float ratioThreshold = 5f;
    float ratioAvailableThreshold = 0.4f;
    /*****************************************************/

    //becon message
    float beconMessageTime = TPreamble+guardInterval+TSymbol;
    int LPreamble = (int)(new BigDecimal(TPreamble*Fs).setScale(0, BigDecimal.ROUND_HALF_UP).floatValue());
    int LSymbol = (int)(new BigDecimal(TSymbol*Fs).setScale(0, BigDecimal.ROUND_HALF_UP).floatValue());
    int beconMessageLength = (int)(new BigDecimal(beconMessageTime*Fs).setScale(0, BigDecimal.ROUND_HALF_UP).floatValue());
    int endBeforeMaxCorr = 0;
    int startBeforeMaxCorr = 200;

    int [] sinSigF = {17000,17200,17400,17600,17800};
    int speedDetectionSigLength = 65536;
    int speedDetectionRangeF = 40;
    int soundSpeed = 34000;



    /*************************String value for message exchanging*****************************/
    String upStr = "up";
    String downStr = "down";
    String tdoaStr = "tdoa";
    String anchorIdStr = "anchorId";
    String targetIdStr = "targetId";
    String xStr = "x";
    String yStr = "y";
    String identityStr = "identity";
}
