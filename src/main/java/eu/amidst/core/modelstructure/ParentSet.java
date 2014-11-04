package eu.amidst.core.modelstructure;

import eu.amidst.core.header.statics.Variable;

<<<<<<< HEAD
/**
 * Created by afa on 02/07/14.
 */
public interface ParentSet {
    public void addParent(Variable variable);
=======
import java.util.List;
import java.util.Set;

/**
 * Created by afa on 02/07/14.
 */
public interface ParentSet<E extends Variable> {
    public void addParent(E variable);
    public List<E> getParents();
>>>>>>> b3b5ba83cfe63708302404628f2e9fecb1021023
    public int getNumberOfParents();
}
