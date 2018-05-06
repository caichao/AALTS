package hust.cc.asynchronousacousticlocalization.processing;

import hust.cc.asynchronousacousticlocalization.physical.SignalGenerator;
import hust.cc.asynchronousacousticlocalization.utils.FlagVar;

public class BeaconMessage {

    /**
     * generate beacon messages: preamble + guard interval + anchorID
     * @param anchorID - the anchor ID, choosing from 0 - 3
     * @param type - up or down chirp modulation, 0 indicate up chirp, 1 indicate down chirp signal
     * @return
     */
    public short[] BeaconMessage(int anchorID, int type){
        short[] samples = new short[FlagVar.LPreamble + FlagVar.guardIntervalLength + FlagVar.LSymbol];
        if(type == 0){
            short[] preamble = SignalGenerator.upChirpGenerator(FlagVar.Fs, FlagVar.TPreamble, FlagVar.BPreamble, FlagVar.Fmin);
            short[] symbol = anchorIDEncoding(anchorID, 0);
            System.arraycopy(preamble, 0, samples, 0, preamble.length);
            System.arraycopy(symbol, 0, samples, (preamble.length + FlagVar.guardIntervalLength - 1), symbol.length);
        }else {
            short[] preamble = SignalGenerator.downChirpGenerator(FlagVar.Fs, FlagVar.TPreamble, FlagVar.BPreamble, FlagVar.Fmax);
            short[] symbol = anchorIDEncoding(anchorID, 1);
            System.arraycopy(preamble, 0, samples, 0, preamble.length);
            System.arraycopy(symbol, 0, samples, (preamble.length + FlagVar.guardIntervalLength - 1), symbol.length);

        }
        return samples;
    }

    /**
     *  encode the anchorID using chirp signal
     * @param anchorID - range: 0 - 3
     * @param type - 0 refers to up chirp signal while 1 refers to down chirp signal
     * @return : samples in short format
     */
    public short[] anchorIDEncoding(int anchorID, int type){
        short[] samples = null;
        if(type == 0){
            samples = SignalGenerator.upChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FUpSymbol[anchorID]);

        }else{
            samples = SignalGenerator.downChirpGenerator(FlagVar.Fs, FlagVar.TSymbol, FlagVar.BSymbol, FlagVar.FDownSymbol[anchorID]);
        }
        return samples;
    }

}
