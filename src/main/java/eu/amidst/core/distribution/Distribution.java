package eu.amidst.core.distribution;

import eu.amidst.core.header.Variable;

/**
 * Created by afa on 12/11/14.
 */
public interface Distribution {
    /**
     * Gets the variable of the distribution
     * @return A <code>Variable</code> object.
     */
    Variable getVariable();
}
