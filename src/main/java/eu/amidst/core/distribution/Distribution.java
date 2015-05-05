package eu.amidst.core.distribution;

import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.Random;

/**
 * Created by afa on 12/11/14.
 */
public abstract class Distribution implements Serializable {

    private static final long serialVersionUID = -3436599636425587512L;
    /**
     * The variable of the distribution
     */
    protected Variable var;


    public abstract double[] getParameters();

    public abstract int getNumberOfParameters();

    /**
     * Gets the variable of the distribution
     *
     * @return A <code>Variable</code> object.
     */
    public Variable getVariable() {
        return this.var;
    }

    public abstract String label();

    public abstract void randomInitialization(Random random);

    public abstract boolean equalDist(Distribution dist, double threshold);

    public abstract String toString();

    public abstract double getLogProbability(Assignment assignment);
}
