package hust.cc.asynchronousacousticlocalization.processing;

public class CriticalPoint {
    double peak;
    double ratio;

    @Override
    public String toString() {
        return "CriticalPoint{" +
                "peak=" + peak +
                ", ratio=" + ratio +
                ", index=" + index +
                ", isReferenceSignalExist=" + isReferenceSignalExist +
                '}';
    }

    int index;
    boolean isReferenceSignalExist;

}
