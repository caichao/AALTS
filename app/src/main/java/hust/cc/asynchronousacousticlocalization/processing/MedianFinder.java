package hust.cc.asynchronousacousticlocalization.processing;

import java.util.ArrayList;
import java.util.List;


public class MedianFinder<T extends Comparable<T>> {
    private boolean bOdd;//is odd or not
    private int kv;
    private List<T> medium = new ArrayList<T>();//median array
    /**
     * one quick sort devide
     * @param a
     * @param low
     * @param high
     * @return
     */
    private int partition(T a[], int low, int high) {
        T tmp = a[low];
        int i = low, j = high;
        while (i < j) {
            while (i < j && a[j].compareTo(tmp) >= 0){
                j--;
            }
            while (i < j && a[i].compareTo(tmp) <= 0){
                i++;
            }
            swap(a, i, j);
        }
        a[low] = a[i];
        a[i] = tmp;
        return i;
    }

    /**
     * swap the value of 2 position
     * @param a
     * @param i
     * @param j
     */
    private void swap(T a[], int i, int j) {
        if(i == j){
            return;
        }
        T tmp = a[i];
        a[i] = a[j];
        a[j] = tmp;
    }

    /**
     * calculate the median
     * @param a
     * @return  when a.length is odd, return the medium value. when a.length is even, return two medium values.
     */
    public List<T> findMedium(T a[]){
        medium.clear();
        if(a.length == 1){
            medium.add(a[0]);
        }else if(a.length == 2){
            medium.add(a[0]);
            medium.add(a[1]);
        }else{
            bOdd = a.length % 2 == 0;
            kv = a.length / 2 + 1;
            findK(a, 0, a.length - 1, kv, -1);
        }
        return medium;
    }
    /**
     * find the k-th smallest value
     * @param a
     * @param low
     * @param high
     * @param k
     * @param prePart
     */
    private void findK(T a[], int low, int high, int k, int prePart){
        if(low > high){
            return;
        }
        int pos = partition(a, low, high);
        int left = pos - low + 1;
        if(k > left){
            findK(a, pos + 1, high, k - left, pos);
        }
        else if(k < left){
            findK(a, low, pos - 1, k, prePart);
        }
        else{
            if(bOdd){
                T v1 = a[pos];
                T v2 = null;
                if(low >= pos){
                    v2 = a[prePart];
                }else{
                    v2 = findMax(a, low, pos - 1);
                }
                medium.add(v1);
                medium.add(v2);
            }else{
                medium.add(a[pos]);
            }
        }
    }
    /**
     * find the max of the array in certain area.
     * @param a
     * @param low
     * @param high
     * @return
     */
    private T findMax(T a[], int low, int high){
        T max = a[low];
        for(int i = low + 1; i <= high; i ++){
            if(a[i].compareTo(max) > 0){
                max = a[i];
            }
        }
        return max;
    }
}
