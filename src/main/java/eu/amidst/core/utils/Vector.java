package eu.amidst.core.utils;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public interface Vector {

    public double get(int i);

    public void set(int i, double val);

    public int size();

    public default double sum() {
        double sum=0;
        for (int i = 0; i < size(); i++) {
            sum+=this.get(i);
        }
        return sum;
    }

    public default void sum(Vector vector){
        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors do not have same size.");

        for (int i = 0; i < vector.size(); i++) {
            this.set(i,this.get(i)+vector.get(i));
        }
    }


    public default void substract(Vector vector){
        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors do not have same size.");

        for (int i = 0; i < vector.size(); i++) {
            this.set(i,this.get(i)-vector.get(i));
        }
    }

    public default void copy(Vector vector){
        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors do not have same size.");

        for (int i = 0; i < vector.size(); i++) {
            this.set(i,vector.get(i));
        }
    }

    public default void divideBy(double val){
        for (int i = 0; i < this.size(); i++) {
            this.set(i,this.get(i)/val);
        }
    }

    public default void multiplyBy(double val){
        for (int i = 0; i < this.size(); i++) {
            this.set(i,this.get(i)*val);
        }
    }

    public default double dotProduct(Vector vector){
        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors do not have same size.");

        double sum=0;
        for (int i = 0; i < vector.size(); i++) {
            sum+=this.get(i)*vector.get(i);
        }

        return sum;
    }

    public default boolean equalsVector(Vector vector, double threshold){

        if (this.size()!=vector.size())
            throw new IllegalArgumentException("Vectors do not have same size.");

        for (int i = 0; i < vector.size(); i++) {
            if (Math.abs(this.get(i) - vector.get(i)) > threshold)
                return false;
        }

        return true;
    }

    public static Vector sumVector(Vector vec1, Vector vec2){
        vec2.sum(vec1);
        return vec2;
    }


}
