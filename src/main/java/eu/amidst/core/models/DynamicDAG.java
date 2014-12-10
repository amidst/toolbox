package eu.amidst.core.models;

import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private ParentSetImpl[] parentSetTime0;

    /**
     * It contains the ParentSets for all variables at time T.
     */
    private ParentSetImpl[] parentSetTimeT;



    public DynamicDAG(DynamicVariables dynamicVariables1) {
        this.dynamicVariables = dynamicVariables1;
        this.parentSetTime0 = new ParentSetImpl[dynamicVariables.getNumberOfVars()];
        this.parentSetTimeT = new ParentSetImpl[dynamicVariables.getNumberOfVars()];

        for (Variable var: dynamicVariables){
            parentSetTime0[var.getVarID()] = new ParentSetImpl(var);
            parentSetTimeT[var.getVarID()] = new ParentSetImpl(var);
        }
    }

    public DynamicVariables getDynamicVariables(){
        return this.dynamicVariables;
    }

    /* Methods accessing structure at time T*/
    public ParentSet getParentSetTimeT(Variable var) {
        if (var.isTemporalClone()) {
            throw new UnsupportedOperationException("Parents of clone variables can not be queried. Just query the parents" +
                    "of its dynamic counterpart.");
        }
        return this.parentSetTimeT[var.getVarID()];
    }

    /*public ParentSet getParentSetTime0(Variable var) {
        if (var.isTemporalClone())
            throw new UnsupportedOperationException("Parents of clone variables can not be queried. Just query the parents" +
                    "of its dynamic counterpart.");

        return this.parentSetTime0[var.getVarID()];
    }*/

    public List<Variable> getListOfParentsTime0(Variable var) {
        if (var.isTemporalClone()) {
            throw new UnsupportedOperationException("Parents of clone variables can not be queried. Just query the parents" +
                    "of its dynamic counterpart.");
        }
        return this.parentSetTime0[var.getVarID()].getParents();
    }

    public boolean containCycles(){

        boolean[] bDone = new boolean[this.dynamicVariables.getNumberOfVars()];


        for (Variable var: this.dynamicVariables){
            bDone[var.getVarID()] = false;
        }

        for (Variable var: this.dynamicVariables){

            // find a node for which all parents are 'done'
            boolean bFound = false;

            for (Variable variable2: this.dynamicVariables){
                if (!bDone[variable2.getVarID()]) {
                    boolean bHasNoParents = true;

                    for (Variable parent: this.getParentSetTimeT(variable2)){
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

    public String toString(){
        StringBuilder str = new StringBuilder();
        str.append("DAG Time 0\n");
        for (Variable var: this.getDynamicVariables()){
            str.append(var.getName() +" : "+this.parentSetTime0[var.getVarID()].toString() + "\n");
        }

        str.append("\nDAG Time T\n");
        for (Variable var: this.getDynamicVariables()){
            str.append(var.getName() +" : "+this.getParentSetTimeT(var).toString() + "\n");
        }
        return str.toString();
    }

    private final class ParentSetImpl implements ParentSet {

        private Variable mainVar;
        private List<Variable> vars;

        private ParentSetImpl(Variable mainVar1){
            mainVar = mainVar1;
            this.vars = new ArrayList<Variable>();
        }
        public void addParent(Variable var){
            if (!Utils.isLinkCLG(mainVar, var)) {
                throw new IllegalArgumentException("Adding a Gaussian variable as parent of a Multinomial variable");
            }

            if (this.contains(var)) {
                throw new IllegalArgumentException("Trying to add a duplicated parent");
            }

            vars.add(var);

            if (!var.isTemporalClone()) {
                parentSetTime0[mainVar.getVarID()].vars.add(var);
            }
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
            StringBuilder str = new StringBuilder();
            str.append("{ ");


            for(int i=0;i<numParents;i++){
                Variable parent = getParents().get(i);
                str.append(parent.getName());
                if (i<numParents-1) {
                    str.append(", ");
                }
            }



            str.append(" }");
            return str.toString();
        }

        /**
         * Is an ArrayList pointer to an ArrayList unmodifiable object still unmodifiable? I guess so right?
         */
        public void blockParents() {
            vars = Collections.unmodifiableList(vars);
        }

        public boolean contains(Variable var){
            return this.vars.contains(var);
        }
    }
}
