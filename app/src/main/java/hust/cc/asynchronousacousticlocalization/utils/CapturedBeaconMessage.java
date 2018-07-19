package hust.cc.asynchronousacousticlocalization.utils;

public class CapturedBeaconMessage implements Cloneable{
    public int selfAnchorId;
    public int capturedAnchorId;
    public int capturedSequence;
    public int preambleIndex;
    public long looperCounter;
    public float speed;


    @Override
    public String toString() {
        return "CapturedBeaconMessage{" +
                "selfAnchorId=" + selfAnchorId +
                ", capturedAnchorId=" + capturedAnchorId +
                ", capturedSequence=" + capturedSequence +
                ", preambleIndex=" + preambleIndex +
                ", looperCounter=" + looperCounter +
                ", speed=" + speed +
                '}';
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
