/**
 ******************* ISSUE LIST **************************
 *
 * 1. In general, should we clone attributes in the constructor to avoid bad uses of input variables later on?
 *
 * 2. How are we going to update the probabilities? Value by value? Or directly with the whole set of probabilities? or both?
 * Two methods are included: setProbabilities(double[] probabilities) and setProbabilityAt(int index, double value)
 *
 * 3. Is needed the method setProbabilityAt ?
 * ********************************************************
 */
package eu.amidst.core.distribution;

import eu.amidst.core.header.Variable;


/**
 * <h2>This class implements a univariate multinomial distribution.</h2>
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-3
 */
public class Multinomial implements UnivariateDistribution {

    /**
     * The variable of the distribution
     */
    private Variable var;

    /**
     * A set of probabilities, one for each state of the variable
     */
    private double[] probabilities;

    /**
     * The class constructor.
     * @param var The variable of the distribution.
     */
    public Multinomial(Variable var) {

        this.var = var;
        this.probabilities = new double[var.getNumberOfStates()];

        for (int i = 0; i < var.getNumberOfStates(); i++) {
            this.probabilities[i] = 1.0 / var.getNumberOfStates();
        }
    }

    /**
     * Sets the probability values to the distribution.
     * @param probabilities An array of probabilities in the same order as the variable states.
     */
    public void setProbabilities(double[] probabilities) {
        this.probabilities = probabilities;
    }

    /**
     * Set a probability value in a given position in the array of probabilities.
     * @param index The position in which the probability is set.
     * @param value A probability value.
     */
    public void setProbabilityAt(int index, double value) {
        this.probabilities[index] = value;
    }

    /**
     * Computes the probability of the variable for a given state.
     * @param value The position of the variable state in the array of probabilities (represented as a
     *              <code>double</code> for generality reasons).
     * @return A <code>double</code> value with the probability.
     */
    @Override
    public double getProbability(double value) {
        return (probabilities[(int) value]);
    }

    /**
     * Computes the logarithm of the probability for a given variable state.
     * @param value The position of the variable state in the array of probabilities (represented as a
     *              <code>double</code> for generality reasons).
     * @return A <code>double</code> value with the logarithm of the probability.
     */
    @Override
    public double getLogProbability(double value) {
        return (Math.log(this.getProbability(value)));
    }

    /**
     * Gets the variable of the distribution.
     * @return A <code>Variable</code> object.
     */
    @Override
    public Variable getVariable() {
        return var;
    }

}