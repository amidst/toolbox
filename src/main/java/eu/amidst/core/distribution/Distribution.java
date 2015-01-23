package eu.amidst.core.distribution;

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


    public abstract int getNumberOfFreeParameters();

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
}
