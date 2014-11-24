package eu.amidst.core.models;

import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;

/**
 * Created by Hanen on 13/11/14.
 */
public class DynamicDAG {


    /**
     * It contains a pointer to the variables (list of variables).
     */
    private DynamicVariables dynamicVariables;

    /**
     * It contains the ParentSets for all variables at time 0.
     */
    private ParentSet[] parentSetTime0;

    /**
     * It contains the ParentSets for all variables at time T.
     */
    private ParentSet[] parentSetTimeT;



    public DynamicDAG(DynamicVariables dynamicVariables_) {
        this.dynamicVariables = dynamicVariables_;
        this.parentSetTime0 = new ParentSet[dynamicVariables.getNumberOfVars()];
        this.parentSetTimeT = new ParentSet[dynamicVariables.getNumberOfVars()];
        for (int i=0;i<dynamicVariables.getNumberOfVars();i++) {
            parentSetTime0[i] = ParentSet.newParentSet();
            parentSetTimeT[i] = ParentSet.newParentSet();
        }
    }

    public DynamicVariables getDynamicVariables(){
        return this.dynamicVariables;
    }

    /* Methods accessing structure at time T*/
    public ParentSet getParentSetTimeT(Variable var) {
        return this.parentSetTimeT[var.getVarID()];
    }

    /* Methods accessing structure at time 0*/
    public ParentSet getParentSetTime0(Variable var) {
        return this.parentSetTime0[var.getVarID()];
    }

    public boolean containCycles(){

        boolean[] bDone = new boolean[this.dynamicVariables.getNumberOfVars()];


        for (Variable var: this.dynamicVariables.getListOfDynamicVariables()){
            bDone[var.getVarID()] = false;
        }

        for (Variable var: this.dynamicVariables.getListOfDynamicVariables()){

            // find a node for which all parents are 'done'
            boolean bFound = false;

            for (Variable variable2: this.dynamicVariables.getListOfDynamicVariables()){
                if (!bDone[variable2.getVarID()]) {
                    boolean bHasNoParents = true;

                    for (Variable parent: this.getParentSetTimeT(variable2).getParents()){
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
