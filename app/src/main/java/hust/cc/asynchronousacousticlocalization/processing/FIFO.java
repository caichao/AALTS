package hust.cc.asynchronousacousticlocalization.processing;

public class FIFO {
    private int capacity = 10;
    private int segmentLength = 0;
    private short[] buffer = null;
    private volatile int storageIndex = 0;
    private volatile int readIndex = 0;

    public FIFO(int bufferLength){
        this.segmentLength = bufferLength;
        init();
    }

    private void init() {
        buffer = new short[this.segmentLength * capacity];
        storageIndex = 0;
        readIndex = 0;
    }

    public void push(short[] data){
        System.arraycopy(data, 0, buffer, (storageIndex % capacity) * this.segmentLength, this.segmentLength);
        storageIndex++;
    }

    public void pop(short[] data){
        while(storageIndex <= readIndex);
        System.arraycopy(buffer, (readIndex % capacity) * this.segmentLength, data, 0, this.segmentLength);
        readIndex++;
    }
}
