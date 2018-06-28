package hust.cc.asynchronousacousticlocalization.processing;

import java.util.LinkedList;
import java.util.List;

public class HampelFilter {

    private double standardFactor = 1.4826;

    private double hampelT;
    private int hampelK;
    private int len;
    private List<Double> sortedData;
    private List<Double> data;
    private Double[] diffData;
    private double diffMedian;
    private double hampelSk;

    public HampelFilter(double hampelT, int hampelK){
        this.hampelT = hampelT;
        this.hampelK = hampelK;
        len = 2*hampelK+1;
        sortedData = new LinkedList<>();
        data = new LinkedList<>();
        diffData = new Double[len];
    }

    public void addData(double d){
        Double D = d;
        if(sortedData.size() == 0){
            sortedData.add(D);
            data.add(D);
        }
        else if(sortedData.size() < len){
            addInOrder(D);
            data.add(D);
        }else{
            Double removedD = data.get(0);
            data.remove(0);
            data.add(D);
            sortedData.remove(removedD);
            addInOrder(D);
        }
        if(isReady()){
            for (int i=0;i<diffData.length;i++){
                diffData[i] = Math.abs(sortedData.get(i)-getMedian());
            }
            MedianFinder<Double> medianFinder = new MedianFinder<>();
            diffMedian = medianFinder.findMedium(diffData).get(0);
            hampelSk = standardFactor*diffMedian*hampelK;
        }

    }

    private void addInOrder(double d){
        int start = 0;
        int end = sortedData.size()-1;
        if(sortedData.get(start) >= d){
            sortedData.add(0,d);
        }else if(sortedData.get(end) <= d){
            sortedData.add(d);
        }else {
            addInOrder(d,start,end);
        }
    }

    private void addInOrder(double d, int start, int end){
        if(start >= 0 && start < end && end < sortedData.size()){
            if(start >= end-1){
                sortedData.add(start+1,d);
                return;
            }
            int med = (start+end)/2;
            if(sortedData.get(med) < d){
                addInOrder(d,med,end);
            }else if(sortedData.get(med) == d){
                sortedData.add(med,d);
            }else{
                addInOrder(d,start,med);
            }
        }
        else{
            throw new RuntimeException("out of range.");
        }
    }

    public double getMedian(){
        if(!isReady()){
            throw new RuntimeException("filter not ready.");
        }
        return sortedData.get(hampelK);
    }

    public double getHampelVal(){
        if(!isReady()){
            throw new RuntimeException("filter not ready.");
        }
        double diff = Math.abs(data.get(hampelK)-sortedData.get(hampelK));
        if(diff <= hampelSk){
            return data.get(hampelK);
        }else{
            return sortedData.get(hampelK);
        }
    }

    public boolean isReady(){
        return sortedData.size() == len;
    }


}
