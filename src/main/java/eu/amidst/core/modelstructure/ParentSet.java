package eu.amidst.core.modelstructure;

import eu.amidst.core.header.Variable;
import java.util.List;



/**
 * Created by afa on 02/07/14.
 */

public interface ParentSet {
    public void addParent(Variable var);
    public void removeParent(Variable var);
    public List<Variable> getParents();
    public int getNumberOfParents();
}
