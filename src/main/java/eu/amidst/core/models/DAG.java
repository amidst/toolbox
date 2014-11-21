package eu.amidst.core.models;

import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

/**
 * Created by Hanen on 13/11/14.
 */
public class DAG {

    private StaticVariables variables;
    private ParentSet[] parents;

    public DAG(StaticVariables variables) {
        this.variables = variables;
        this.parents = new ParentSet[variables.getNumberOfVars()];

        for (int i=0;i<variables.getNumberOfVars();i++) {
            parents[i] = ParentSet.newParentSet();
        }
    }

    public StaticVariables getStaticVariables(){
        return this.variables;
    }

    public ParentSet getParentSet(Variable var) {
        return parents[var.getVarID()];
    }

    public boolean containCycles(){

        boolean[] bDone = new boolean[this.variables.getNumberOfVars()];

        for (Variable var: this.variables.getListOfVariables()){
            bDone[var.getVarID()] = false;
        }

        for (Variable var: this.variables.getListOfVariables()){

            // find a node for which all parents are 'done'
            boolean bFound = false;

            for (Variable variable2: this.variables.getListOfVariables()){
                if (!bDone[variable2.getVarID()]) {
                    boolean bHasNoParents = true;

                    for (Variable parent: this.getParentSet(variable2).getParents()){
                        if (!bDone[parent.getVarID()]) {
                            bHasNoParents = false;
                        }
                    }

                    if (bHasNoParents) {
                        bDone[variable2.getVarID()] = true;
                        bFound = true;
                        break;
                    }
                }
            }

            if (!bFound) {
                return true;
            }
        }

        return false;
    }
}
