package eu.amidst.core.modelstructure;

import eu.amidst.core.header.statics.Variable;

import java.util.List;
import java.util.Set;

/**
 * Created by afa on 02/07/14.
 */
public interface ParentSet<E extends Variable> {
    public void addParent(E variable);
    public List<E> getParents();
    public int getNumberOfParents();
}
