package eu.amidst.core.distribution;


import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * <h2>This interface generalizes the set of possible conditional distributions.</h2>
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-3
 */
public abstract class ConditionalDistribution extends Distribution {

    /**
     * The list of parents of the variable
     */
    protected List<Variable> parents;

    /**
     * Gets the set of conditioning variables
     * @return An <code>unmodifiable List</code> object with the set of conditioning variables.
     */
    public List<Variable> getConditioningVariables() {
        return this.parents;
    }

    /**
     * Evaluates the conditional distribution given a value of the variable and an assignment of the parents.
     * @param assignment An <code>Assignment</code> for the parents.
     * @return A <code>double</code> value with the evaluated distribution.
     */
    public double getConditionalProbability(Assignment assignment){
        return Math.exp(this.getLogConditionalProbability(assignment));
    }

    /**
     * Evaluates the conditional distribution given a value of the variable and an assignment of the parents.
     * @param assignment An <code>Assignment</code> for the parents.
     * @return A <code>double</code> value with the logarithm of the evaluated distribution.
     */
    public abstract double getLogConditionalProbability(Assignment assignment);

    public abstract UnivariateDistribution getUnivariateDistribution(Assignment assignment);
}
