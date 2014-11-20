/**
 ******************* ISSUE LIST **************************
 *
 * 1. Do we need here the min and max of the variable, for instance, to check that the input value in computeProbabilityOf(value) is in the range [min,max]?
 *
 *
 * ********************************************************
 */

package eu.amidst.core.distribution;

import eu.amidst.core.header.Variable;

/**
 * <h2>This class implements a univariate Normal distribution.</h2>
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-3
 */
public class Normal extends UnivariateDistribution {

    /**
     * The mean of the Normal distribution.
     */
    private double mean;

    /**
     * The standard deviation of the Normal distribution.
     */
    private double sd;

    /**
     * The class constructor.
     * @param var The variable of the distribution.
     */
    public Normal(Variable var) {
        this.var = var;
        this.mean = 0;
        this.sd = 1;
    }

    /**
     * Gets the mean of the distribution.
     * @return A <code>double</code> value with the mean.
     */
    public double getMean() {
        return mean;
    }

    /**
     * Sets the mean of the distribution.
     * @param mean A value for the mean.
     */
    public void setMean(double mean) {
        this.mean = mean;
    }

    /**
     * Gets the standard deviation of the distribution.
     * @return A <code>double</code> value with the standar deviation.
     */
    public double getSd() {
        return sd;
    }

    /**
     * Sets the standard deviation of the distribution.
     * @param sd A value for the standard deviation.
     */
    public void setSd(double sd) {
        this.sd = sd;
    }

    /**
     * Evaluates the density function in a given point.
     * @param value An value for the variable.
     * @return A <code>double</code> with the value of the density.
     */
    @Override
    public double getProbability(double value) {
        return (1 / (sd * Math.sqrt(2 * Math.PI)) * Math.exp(-0.5 * Math.pow(((value - mean) / sd), 2)));
    }

    /**
     * Computes the logarithm of the density function in a given point.
     * @param value An value for the variable.
     * @return A <code>double</code> with the logarithm of the density value.
     */
    @Override
    public double getLogProbability(double value) {
        return (-Math.log(sd) - 0.5 * Math.log(2 * Math.PI) - 0.5 * Math.pow(((value - mean) / sd), 2));
    }
}
