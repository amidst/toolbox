
package eu.amidst.core.models;

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

    public String toString() {

        int numParents = getNumberOfParents();
        String str = new String("{ ");


        for(int i=0;i<numParents;i++){
            Variable parent = getParents().get(i);
            str = str + parent.getName();
            if (i<numParents-1)
                str = str + ", ";
        }



        str = str + " }";
        return str;
    }

    /**
     * Is an ArrayList pointer to an ArrayList unmodifiable object still unmodifiable? I guess so right?
     */
    public void blockParents() {
        vars = Collections.unmodifiableList(vars);
    }
}
