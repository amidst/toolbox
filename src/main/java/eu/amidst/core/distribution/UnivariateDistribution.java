package eu.amidst.core.distribution;

import eu.amidst.core.variables.Assignment;

import java.util.Random;

/**
 * <h2>This interface generalizes the set of univariate distributions.</h2>
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-3
 */
public abstract class UnivariateDistribution extends Distribution {

    public UnivariateDistribution(){}
    /**
     * Evaluates the distribution in a given point.
     * @param value The point to be evaluated.
     * @return A <code>double</code> value with the evaluated distribution.
     */
    public double getProbability(double value){
        return Math.exp(this.getLogProbability(value));
    }

    @Override
    public double getLogProbability(Assignment assignment){
        return this.getLogProbability(assignment.getValue(this.var));
    }

    /**
     * Evaluates the distribution in a given point.
     * @param value The point to be evaluated.
     * @return A <code>double</code> value with the logarithm of the evaluated distribution.
     */
    public abstract double getLogProbability(double value);

    public abstract double sample(Random rand);
}
