package eu.amidst.cim2015.examples;

import eu.amidst.core.datastream.DataInstance;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 03/07/15.
 */
public class Averager //implements DoubleArrayConsumer
{
    private double[] total;
    private int count = 0;

    public Averager(int nAtts){
        total= new double[nAtts];
        count = 1;
    }

    public Averager(DataInstance dataInstance){
        total=dataInstance.toArray();
        count = 1;
    }

    public double[] average() {
        return count > 0? Arrays.stream(total).map(val -> val / count).toArray() : new double[0];
    }

    /*
    public void accept(double[] instance) {
        total = IntStream.rangeClosed(0, instance.length - 1).mapToDouble(i -> total[i] + instance[i]).toArray();
        count++;
    }*/

    public Averager combine(Averager other) {
        total = IntStream.rangeClosed(0,other.total.length-1).mapToDouble(i->total[i]+other.total[i]).toArray();
        count += other.count;
        return this;
    }
}