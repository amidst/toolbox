package eu.amidst.cim2015.examples;

/**
 * Created by ana@cs.aau.dk on 03/07/15.
 */
@FunctionalInterface
public interface DoubleArrayConsumer {
    void accept(double[] value);
}