package eu.amidst.core.modelstructure;

import eu.amidst.core.header.statics.Variable;

/**
 * Created by afa on 02/07/14.
 */
public interface ParentSet {
    public void addParent(Variable variable);
    public int getNumberOfParents();
}
