package eu.amidst.cim2015.examples;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 03/07/15.
 */
public class Averager implements DoubleArrayConsumer
{
    private double[] total = new double[21];
    private int count = 0;

    public double[] average() {
        return count > 0? Arrays.stream(total).map(val -> val / count).toArray() : new double[0];
    }

    public void accept(double[] instance) {
        total = IntStream.rangeClosed(0, instance.length - 1).mapToDouble(i -> total[i] + instance[i]).toArray();
        count++;
    }

    public void combine(Averager other) {
        total = IntStream.rangeClosed(0,other.total.length-1).mapToDouble(i->total[i]+other.total[i]).toArray();
        count += other.count;
    }
}