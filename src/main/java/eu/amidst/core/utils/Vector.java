package eu.amidst.core.utils;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * Created by andresmasegosa on 12/11/14.
 */
public interface Vector {

    public double get(int i);

    public void set(int i, double val);

    public int size();

    public void sum(Vector vector);

    public default void substract(Vector vector){
            throw new NotImplementedException();
    }

    public void copy(Vector vector);

    public void divideBy(double val);

    public double dotProduct(Vector vec);

    public static Vector sumVector(Vector vec1, Vector vec2){
        vec2.sum(vec1);
        return vec2;
    }
}
