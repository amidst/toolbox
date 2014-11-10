
package eu.amidst.core.modelstructure.statics.impl;

import eu.amidst.core.header.Variable;
import eu.amidst.core.modelstructure.ParentSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by afa on 02/07/14.
 */

public class ParentSetImpl implements ParentSet {

    private ArrayList<Variable> vars;

    public void addParent(Variable var){
        vars.add(var);
    }

    public void removeParent(Variable var){
        vars.remove(var);
    }

    @Override
    public List<Variable> getParents(){
        return vars;
    }

    public int getNumberOfParents(){
        return vars.size();
    }
}
