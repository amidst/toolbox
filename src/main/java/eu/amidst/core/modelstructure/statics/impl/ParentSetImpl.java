package eu.amidst.core.modelstructure.statics.impl;

import eu.amidst.core.header.statics.Variable;
import eu.amidst.core.modelstructure.ParentSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by afa on 02/07/14.
 */
public class ParentSetImpl implements ParentSet<Variable>{
    private ArrayList<Variable> vars;

    public void addParent(Variable variable){
<<<<<<< HEAD
=======
        vars.add(variable);
    }
>>>>>>> b3b5ba83cfe63708302404628f2e9fecb1021023

    @Override
    public List<Variable> getParents() {
        return vars;
    }

    @Override
    public int getNumberOfParents() {
        return 0;
    }
}
