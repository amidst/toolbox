
package eu.amidst.core.modelstructure;

import eu.amidst.core.distribution.*;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

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

    public void setDistribution(Variable var, ConditionalDistribution distribution){
        this.distributions[var.getVarID()] = distribution;
    }

    public int getNumberOfVars() {
        return this.getDAG().getVariables().getNumberOfVars();
    }

    public StaticVariables getStaticVariables() {
        return this.getDAG().getVariables();
    }

    public DAG getDAG (){
        return dag;
    }

    public List<Variable> getVariables() {
        return this.getStaticVariables().getVariables();
    }

    private void initializeDistributions(){

        List<Variable> vars = getStaticVariables().getVariables(); /* the list of all variables in the BN */

        this.distributions = new ConditionalDistribution[vars.size()];


        /* Initialize the distribution for each variable depending on its distribution type
        as well as the distribution type of its parent set (if that variable has parents)
         */
        for (Variable var : getStaticVariables().getVariables()) {
            ParentSet parentSet = this.getDAG().getParentSet(var);

            int varID = var.getVarID();
            this.distributions[varID]= DistributionBuilder.newDistribution(var, parentSet.getParents());
            parentSet.blockParents();
        }
    }


}



