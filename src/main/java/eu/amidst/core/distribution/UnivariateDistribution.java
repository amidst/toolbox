package eu.amidst.core.distribution;

/**
 * Created by afa on 03/11/14.
 */
public interface UnivariateDistribution extends Distribution {

    public double getLogProbability (double value);
    public double getProbability(double value);
    public void updateCounts(double value);
}
