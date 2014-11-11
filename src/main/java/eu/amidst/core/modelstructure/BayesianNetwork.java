
package eu.amidst.core.modelstructure;

import eu.amidst.core.distribution.*;
import eu.amidst.core.header.Variable;
import eu.amidst.core.header.StaticModelHeader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Created by afa on 02/07/14.
 */


public class BayesianNetwork {

    private Distribution[] distributions;
    private StaticModelHeader modelHeader;
    private ParentSet[] parents;

    public static BayesianNetwork newBayesianNetwork(StaticModelHeader modelHeader){
        return new BayesianNetwork(modelHeader);
    }

    private BayesianNetwork(StaticModelHeader modelHeader) {
        this.modelHeader = modelHeader;
        this.parents = new ParentSet[modelHeader.getNumberOfVars()];

        for (int i=0;i<modelHeader.getNumberOfVars();i++) {
            parents[i] = ParentSet.newParentSet();
        }
    }

    public ParentSet getParentSet(Variable var) {
        return parents[var.getVarID()];
    }

    public Distribution getDistribution(Variable var) {
        return distributions[var.getVarID()];
    }

    public void setDistribution(Variable var, Distribution distribution){
        this.distributions[var.getVarID()] = distribution;
    }

    public int getNumberOfNodes() {
        return modelHeader.getNumberOfVars();
    }

    public StaticModelHeader getStaticModelHeader() {
        return modelHeader;
    }


    public void initializeDistributions(){

        List<Variable> vars = modelHeader.getVariables(); /* the list of all variables in the BN */

        this.distributions = new Distribution[vars.size()];


        /* Initialize the distribution for each variable depending on its distribution type
        as well as the distribution type of its parent set (if that variable has parents)
         */
        for (Variable var : modelHeader.getVariables()) {
            int varID = var.getVarID();
            this.distributions[varID]= DistributionBuilder.newDistribution(var, parents[varID].getParents());
            parents[varID].blockParents((ArrayList) Collections.unmodifiableList(parents[varID].getParents()));
        }
    }

    public boolean containCycles(){
        return false;
    }
}



