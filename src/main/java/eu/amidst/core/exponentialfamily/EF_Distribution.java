package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.header.Variable;

/**
 * Created by andresmasegosa on 13/11/14.
 */
public abstract class EF_Distribution {
    /**
     * The variable of the distribution
     */
    protected Variable var;

    /**
     * Gets the variable of the distribution
     * @return A <code>Variable</code> object.
     */
    public Variable getVariable() {
        return this.var;
    };

}
