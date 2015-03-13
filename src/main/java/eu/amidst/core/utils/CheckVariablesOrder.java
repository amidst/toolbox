package eu.amidst.core.utils;

import eu.amidst.core.models.ParentSet;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ana@cs.aau.dk on 10/03/15.
 */
public final class CheckVariablesOrder {

    public static void ifVariablesNotOrderedThenExeption(List<Variable> parents){

        int previous = (int) parents.get(0).getVarID();
        for(Variable parent: parents){
            if(parent.getVarID() < previous)
                throw new IllegalStateException("The list of variables should be ordered");
        }
    }

    public static boolean isListOfVariablesOrdered(List<Variable> parents){

        int previous = (int) parents.get(0).getVarID();
        for(Variable parent: parents){
            if(parent.getVarID() < previous)
                return false;
        }
        return true;
    }

    public static List<Variable> orderListOfVariables(List<Variable> parents){
        return parents.stream().sorted((a, b) -> a.getVarID() - b.getVarID()).collect(Collectors.toList());
    }

    public static List<ParentSet> orderListOfParentSets(List<ParentSet> parents){
        return parents.stream().sorted((a, b) -> a.getMainVar().getVarID() - b.getMainVar().getVarID()).collect(Collectors.toList());
    }

}
