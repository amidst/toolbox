/**
 ******************* ISSUE LIST **************************
 *
 * 1. Think in using the parents of the BN to access the conditioning list.
 *
 * 2. Include exceptions in all the classes in this package
 * ********************************************************
 */


package eu.amidst.core.distribution;

import eu.amidst.core.header.Variable;

/**
 * <h2>This interface generalizes the set of possible distributions.</h2>
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-07-2
 */
public interface Distribution {

    /**
     * Gets the variable of the distribution
     * @return A <code>Variable</code> object.
     */
    Variable getVariable();

}
