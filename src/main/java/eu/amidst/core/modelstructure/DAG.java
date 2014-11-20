package eu.amidst.core.modelstructure;

import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

/**
 * Created by Hanen on 13/11/14.
 */
public class DAG {

    private StaticVariables modelHeader;
    private ParentSet[] parents;

    public DAG(StaticVariables modelHeader) {
        this.modelHeader = modelHeader;
        this.parents = new ParentSet[modelHeader.getNumberOfVars()];

        for (int i=0;i<modelHeader.getNumberOfVars();i++) {
            parents[i] = ParentSet.newParentSet();
        }
    }

    public StaticVariables getModelHeader(){
        return this.modelHeader;
    }

    public ParentSet getParentSet(Variable var) {
        return parents[var.getVarID()];
    }

    public boolean containCycles(){

        boolean[] bDone = new boolean[this.modelHeader.getNumberOfVars()];

        for (Variable var: this.modelHeader.getVariables()){
            bDone[var.getVarID()] = false;
        }

        for (Variable var: this.modelHeader.getVariables()){

            // find a node for which all parents are 'done'
            boolean bFound = false;

            for (Variable variable2: this.modelHeader.getVariables()){
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
