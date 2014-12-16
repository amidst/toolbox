
package eu.amidst.core.models;

import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by afa on 02/07/14.
 */

public interface ParentSet extends Iterable<Variable>{

    Variable getMainVar();

    void addParent(Variable var);

    void removeParent(Variable var);

    List<Variable> getParents();

    int getNumberOfParents();

    String toString();

    void blockParents();

    boolean contains(Variable var);

    @Override
    boolean equals(Object o);

    @Override
    default Iterator<Variable> iterator(){
        return this.getParents().iterator();
    }

}
