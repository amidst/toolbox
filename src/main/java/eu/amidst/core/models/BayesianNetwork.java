/**
 ******************* ISSUE LIST **************************
 *
 * 1. (Andres) getListOfVariables should return a Set instead of a List.
 *
 * ********************************************************
 */

package eu.amidst.core.models;

import eu.amidst.core.distribution.*;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Created by afa on 02/07/14.
 */


public class BayesianNetwork {

    private ConditionalDistribution[] distributions;

    private DAG dag;

    public static BayesianNetwork newBayesianNetwork(DAG dag){
        return new BayesianNetwork(dag);
    }

    private BayesianNetwork(DAG dag) {
        this.dag = dag;
        initializeDistributions();
    }

    public <E extends ConditionalDistribution> E getDistribution(Variable var) {
        return (E)distributions[var.getVarID()];
    }

    public int getNumberOfVars() {
        return this.getDAG().getStaticVariables().getNumberOfVars();
    }

    public StaticVariables getStaticVariables() {
        return this.getDAG().getStaticVariables();
    }

    public DAG getDAG (){
        return dag;
    }

   // public List<Variable> getListOfVariables() {
   //     return this.getStaticVariables().getListOfVariables();
   // }

    private void initializeDistributions(){



        this.distributions = new ConditionalDistribution[this.getNumberOfVars()];


        /* Initialize the distribution for each variable depending on its distribution type
        as well as the distribution type of its parent set (if that variable has parents)
         */
        for (Variable var : getStaticVariables()) {
            ParentSet parentSet = this.getDAG().getParentSet(var);

            int varID = var.getVarID();
            this.distributions[varID]= DistributionBuilder.newDistribution(var, parentSet.getParents());
            parentSet.blockParents();
        }
    }

    public double getLogProbabiltyOfFullAssignment(Assignment assignment){
        double logProb = 0;
        for (Variable var: this.getStaticVariables()){
            if (assignment.getValue(var)== Utils.missingValue())
                throw new UnsupportedOperationException("This method can not compute the probabilty of a partial assignment.");

            logProb += this.distributions[var.getVarID()].getLogConditionalProbability(assignment);
        }
        return logProb;
    }


    public String toString(){
        String str = "Bayesian Network:\n";
        for (Variable var: this.getStaticVariables()){

            if (this.getDAG().getParentSet(var).getNumberOfParents()==0){
                str+="P(" + var.getName()+" [" +var.getDistributionType().toString()+ "]) follows a ";
                str+=this.getDistribution(var).label()+"\n";
            }else {

                str += "P(" + var.getName() + " [" + var.getDistributionType().toString() + "]" + " : ";

                for (Variable parent : this.getDAG().getParentSet(var)) {
                    str += parent.getName() + " [" + parent.getDistributionType().toString() + "], ";
                }
                if (this.getDAG().getParentSet(var).getNumberOfParents() > 0) str = str.substring(0, str.length() - 2);
                str += ") follows a ";
                str += this.getDistribution(var).label() + "\n";
            }
        }
        return str;
    }
}



