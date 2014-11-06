package eu.amidst.core.modelstructure;

import eu.amidst.core.header.statics.Variable;
import java.util.List;



/**
 * Created by afa on 02/07/14.
 */
public interface ParentSet {
    public void addParent(Variable variable);
    public void removeParent(Variable variable);
    public List<Variable> getParents();
    public int getNumberOfParents();
}
