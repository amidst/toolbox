
package eu.amidst.core.models;

import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by afa on 02/07/14.
 */

public interface ParentSet {

    public void addParent(Variable var);

    public void removeParent(Variable var);

    public List<Variable> getParents();

    public int getNumberOfParents();

    public String toString();

    public void blockParents();

    public boolean contains(Variable var);

}
