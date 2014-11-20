
package eu.amidst.core.modelstructure;

import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by afa on 02/07/14.
 */

public class ParentSet {

    private List<Variable> vars;

    private ParentSet(){
        this.vars = new ArrayList<Variable>();
    }

    public static ParentSet newParentSet(){
        return new ParentSet();
    }

    public void addParent(Variable var){
        vars.add(var);
    }

    public void removeParent(Variable var){
        vars.remove(var);
    }

    public List<Variable> getParents(){
        return vars;
    }

    public int getNumberOfParents(){
        return vars.size();
    }

    /**
     * Is an ArrayList pointer to an ArrayList unmodifiable object still unmodifiable? I guess so right?
     */
    public void blockParents() {
        vars = Collections.unmodifiableList(vars);
    }
}
