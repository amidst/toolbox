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

    public void sum(Vector vector);

    public void copy(Vector vector);

    public void divideBy(double val);

    public double dotProduct(Vector vec);
}
