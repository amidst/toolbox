
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

    public void addParent(Variable variable){
        vars.add(variable);
    }

    public void removeParent(Variable variable){
        vars.remove(variable);
    }

    @Override
    public List<Variable> getParents(){
        ArrayList<Variable> parents;
        parents = new ArrayList<Variable>();
        for (int i=0;i<vars.size();i++) parents.add(vars.get(i));
        return parents;
    }

    public int getNumberOfParents(){
        return vars.size();
    }
}
