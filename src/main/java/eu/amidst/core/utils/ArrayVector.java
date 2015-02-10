package eu.amidst.core.utils;

import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.exponentialfamily.NaturalParameters;
import eu.amidst.core.exponentialfamily.SufficientStatistics;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public class ArrayVector implements MomentParameters, NaturalParameters, SufficientStatistics{

    private double[] array;

    public ArrayVector(int size){
        this.array = new double[size];
    }

    public ArrayVector(double[] vec){
        this.array=vec;
    }

    @Override
    public double get(int i){
        return this.array[i];
    }

    @Override
    public void set(int i, double val){
        this.array[i]=val;
    }

    @Override
    public int size(){
        return this.array.length;
    }

    @Override
    public void sum(Vector vector) {
        for (int i = 0; i < vector.size(); i++) {
            this.array[i]+=vector.get(i);
        }
    }

    @Override
    public void substract(Vector vector) {
        for (int i = 0; i < vector.size(); i++) {
            this.array[i]-=vector.get(i);
        }
    }

    @Override
    public void copy(Vector vector){
        this.copy((ArrayVector)vector);
    }

    public double[] toArray(){
        return this.array;
    }

    public void copy(ArrayVector vector){
        if (vector.size()!=vector.size())
            throw new IllegalArgumentException("Vectors with different sizes");
        System.arraycopy(vector.toArray(),0,this.array,0,vector.toArray().length);
    }

    @Override
    public void divideBy(double val){
        for (int i = 0; i < this.array.length ; i++) {
            this.array[i]/=val;
        }
    }

    public void multiplyBy(double val){
        for (int i = 0; i < this.array.length ; i++) {
            this.array[i]*=val;
        }
    }
    @Override
    public double dotProduct(Vector vector) {
        double sum=0;
        for (int i = 0; i < vector.size(); i++) {
            sum+=this.array[i]*vector.get(i);
        }
        return sum;
    }

}
