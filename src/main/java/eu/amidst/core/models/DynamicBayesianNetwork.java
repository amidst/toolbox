package eu.amidst.core.models;


import eu.amidst.core.distribution.*;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.DynamicVariables;
import eu.amidst.core.variables.Variable;

import java.util.List;


/**
 * <h2>This class implements a dynamic Bayesian network.</h2>
 *
 * @author afalvarez@ual.es, andres@cs.aau.dk & ana@cs.aau.dk
 * @version 1.0
 * @since 2014-07-3
 *
 */
public class DynamicBayesianNetwork{


    /**
     * It contains the distributions for all variables at time 0.
     */
    private ConditionalDistribution[] distributionsTime0;

    /**
     * It contains the distributions for all variables at time T.
     */
    private ConditionalDistribution[] distributionsTimeT;

    private DynamicDAG dynamicDAG;

    private DynamicBayesianNetwork(DynamicDAG dynamicDAG_){
        dynamicDAG = dynamicDAG_;
        this.initializeDistributions();
    }

    public static DynamicBayesianNetwork newDynamicBayesianNetwork(DynamicDAG dynamicDAG_){
        return new DynamicBayesianNetwork(dynamicDAG_);
    }

    private void initializeDistributions() {
        //Parents should have been assigned before calling this method (from dynamicmodelling.models)

        this.distributionsTime0 = new ConditionalDistribution[this.getDynamicVariables().getNumberOfVars()];
        this.distributionsTimeT = new ConditionalDistribution[this.getDynamicVariables().getNumberOfVars()];

        for (Variable var : this.getDynamicVariables()) {
            int varID = var.getVarID();

            /* Distributions at time t */
            this.distributionsTimeT[varID] = DistributionBuilder.newDistribution(var, this.dynamicDAG.getParentSetTimeT(var).getParents());
            this.dynamicDAG.getParentSetTimeT(var).blockParents();

            /* Distributions at time 0 */
            this.distributionsTime0[varID] = DistributionBuilder.newDistribution(var, this.dynamicDAG.getListOfParentsTime0(var));
            //this.dynamicDAG.getParentSetTime0(var).blockParents();
        }
    }

    public int getNumberOfDynamicVars() {
        return this.getDynamicVariables().getNumberOfVars();
    }

    public DynamicVariables getDynamicVariables() {
        return this.dynamicDAG.getDynamicVariables();
    }

    public <E extends ConditionalDistribution> E getDistributionTimeT(Variable var) {
        return (E) this.distributionsTimeT[var.getVarID()];
    }

    public <E extends ConditionalDistribution> E getDistributionTime0(Variable var) {
        return (E) this.distributionsTime0[var.getVarID()];
    }

    public DynamicDAG getDynamicDAG (){
        return this.dynamicDAG;
    }

    public int getNumberOfVars() {
        return this.getDynamicVariables().getNumberOfVars();
    }

    //public List<Variable> getListOfDynamicVariables() {
    //    return this.getDynamicVariables().getListOfDynamicVariables();
    //}

    public double getLogProbabiltyOfFullAssignmentTimeT(Assignment assignment){
        double logProb = 0;
        for (Variable var: this.getDynamicVariables()){
            if (assignment.getValue(var)== Utils.missingValue())
                throw new UnsupportedOperationException("This method can not compute the probabilty of a partial assignment.");
            logProb += this.distributionsTimeT[var.getVarID()].getLogConditionalProbability(assignment);
        }
        return logProb;
    }

    public double getLogProbabiltyOfFullAssignmentTime0(Assignment assignment){
        double logProb = 0;
        for (Variable var: this.getDynamicVariables()){
            if (assignment.getValue(var) == Utils.missingValue())
                throw new UnsupportedOperationException("This method can not compute the probabilty of a partial assignment.");
            logProb += this.distributionsTime0[var.getVarID()].getLogConditionalProbability(assignment);
        }
        return logProb;
    }


    /*
    public Variable getVariableById(int varID) {
        return this.getListOfDynamicVariables().getVariableById(varID);
    }

    public Variable getTemporalCloneById(int varID) {
        return this.getListOfDynamicVariables().getTemporalCloneById(varID);
    }

    public Variable getTemporalClone(Variable variable) {
        return this.getListOfDynamicVariables().getTemporalClone(variable);
    }
    */


    public String toString(){
        String str = "Dynamic Bayesian Network Time 0:\n";
        for (Variable var: this.getDynamicVariables()){

            if (this.getDynamicDAG().getListOfParentsTime0(var).size()==0){
                str+="P(" + var.getName()+" [" +var.getDistributionType().toString()+ "]) follows a ";
                str+=this.getDistributionTime0(var).label()+"\n";
            }else {
                str += "P(" + var.getName() + " [" + var.getDistributionType().toString() + "]" + " : ";

                for (Variable parent : this.getDynamicDAG().getListOfParentsTime0(var)) {
                    str += parent.getName() + " [" + parent.getDistributionType().toString() + "], ";
                }
                if (this.getDynamicDAG().getListOfParentsTime0(var).size() > 0) str = str.substring(0, str.length() - 2);
                str += ") follows a ";
                str += this.getDistributionTime0(var).label() + "\n";
            }

        }

        str += "\nDynamic Bayesian Network Time T:\n";

        for (Variable var: this.getDynamicVariables()){

            if (this.getDynamicDAG().getParentSetTimeT(var).getNumberOfParents()==0){
                str+="P(" + var.getName()+" [" +var.getDistributionType().toString()+ "]) follows a ";
                str+=this.getDistributionTimeT(var).label()+"\n";
            }else {
                str += "P(" + var.getName() + " [" + var.getDistributionType().toString() + "]" + " : ";

                for (Variable parent : this.getDynamicDAG().getParentSetTimeT(var)) {
                    str += parent.getName() + " [" + parent.getDistributionType().toString() + "], ";
                }
                if (this.getDynamicDAG().getParentSetTimeT(var).getNumberOfParents() > 0) str = str.substring(0, str.length() - 2);
                str += ") follows a ";
                str += this.getDistributionTimeT(var).label() + "\n";
            }

        }
        return str;
    }

}
