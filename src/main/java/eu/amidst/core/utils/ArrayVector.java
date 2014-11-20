package eu.amidst.core.utils;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public class ArrayVector implements Vector{

    double[] array;

    public ArrayVector(int size){
        this.array = new double[size];
    }

    public ArrayVector(double[] vec){
        this.array=vec;
    }

    public double get(int i){
        return this.array[i];
    }

    public void set(int i, double val){
        this.array[i]=val;
    }

    public int size(){
        return this.array.length;
    }

}
