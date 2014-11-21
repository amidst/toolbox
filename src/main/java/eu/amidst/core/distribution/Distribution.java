package eu.amidst.core.distribution;

import eu.amidst.core.variables.Variable;

/**
 * Created by afa on 12/11/14.
 */
public abstract class Distribution {

    /**
     * The variable of the distribution
     */
    protected Variable var;

    /**
     * Gets the variable of the distribution
     *
     * @return A <code>Variable</code> object.
     */
    public Variable getVariable() {
        return this.var;
    }


}
