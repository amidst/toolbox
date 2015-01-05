package eu.amidst.core.utils;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public class ArrayVector implements Vector{

    private double[] array;

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

    public void copy(Vector vector){
        for (int i = 0; i < vector.size(); i++) {
            this.set(i,vector.get(i));
        }
    }

    public double[] getArray(){
        return this.array;
    }

    public void copy(ArrayVector vector){
        if (vector.size()!=vector.size())
            throw new IllegalArgumentException("Vectors differnt sizes");
        System.arraycopy(vector.getArray(),0,this.array,0,vector.getArray().length);
    }

    public void divideBy(double val){
        for (int i = 0; i < this.array.length ; i++) {
            this.array[i]/=val;
        }
    }

}
