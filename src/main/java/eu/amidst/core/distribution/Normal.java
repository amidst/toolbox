/**
 ******************* ISSUE LIST **************************
 *
 * 1. Do we need here the min and max of the variable, for instance, to check that the input value in computeProbabilityOf(value) is in the range [min,max]?
 *
 *
 * ********************************************************
 */

package eu.amidst.core.distribution;

import eu.amidst.core.exponentialfamily.EF_Normal;
import eu.amidst.core.exponentialfamily.EF_UnivariateDistribution;
import eu.amidst.core.exponentialfamily.MomentParameters;
import eu.amidst.core.variables.Variable;

import java.util.Random;

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
     * @param var1 The variable of the distribution.
     */
    public Normal(Variable var1) {
        this.var = var1;
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
     * @param mean1 A value for the mean.
     */
    public void setMean(double mean1) {
        this.mean = mean1;
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
     * @param sd1 A value for the standard deviation.
     */
    public void setSd(double sd1) {
        this.sd = sd1;
    }


    public void setVariance(double var){
        this.sd=Math.sqrt(var);
    }

    public double getVariance(){
        return this.sd*this.sd;
    }


    @Override
    public int getNumberOfFreeParameters() {
        return 2;
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

    @Override
    public double sample(Random rand) {
        return rand.nextGaussian()*this.getSd()+this.getMean();
    }

    public String label(){
        return "Normal";
    }

    @Override
    public void randomInitialization(Random random) {
        this.setMean(random.nextGaussian()*10);
        this.setVariance(random.nextDouble()*10+0.1);
    }

    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist.getClass().getName().equals("eu.amidst.core.distribution.Normal"))
            return this.equalDist((Normal)dist,threshold);
        return false;
    }

    @Override
    public String toString() {
        return "Normal ("+this.getVariable().getName()+") [ mu = " + this.getMean() + ", var = "+ this.getVariance() +" ]";
    }

    public boolean equalDist(Normal dist, double threshold){
        return Math.abs(this.getMean() - dist.getMean()) <= threshold && Math.abs(this.getSd() - dist.getSd()) <= threshold;
    }

    @Override
    public EF_Normal toEFUnivariateDistribution() {

        EF_Normal efNormal = new EF_Normal(this.getVariable());
        MomentParameters momentParameters = efNormal.createZeroedMomentParameters();
        momentParameters.set(EF_Normal.EXPECTED_MEAN, this.getMean());
        momentParameters.set(EF_Normal.EXPECTED_SQUARE, this.getMean() * this.getMean() + this.getSd() * this.getSd());
        efNormal.setMomentParameters(momentParameters);
        return efNormal;
    }
}
