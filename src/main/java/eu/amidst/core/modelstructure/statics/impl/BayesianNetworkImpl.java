
package eu.amidst.core.modelstructure.statics.impl;

import eu.amidst.core.database.statics.readers.DistType;
import eu.amidst.core.distribution.*;
import eu.amidst.core.distribution.Normal_MultinomialParents;
import eu.amidst.core.modelstructure.ParentSet;
import eu.amidst.core.modelstructure.statics.BayesianNetwork;
import eu.amidst.core.header.Variable;
import eu.amidst.core.header.statics.StaticModelHeader;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by afa on 02/07/14.
 */


public class BayesianNetworkImpl implements BayesianNetwork {

    private Distribution[] estimators;
    private StaticModelHeader modelHeader;
    private ParentSet[] parents;

    public BayesianNetworkImpl(StaticModelHeader modelHeader) {
        this.modelHeader = modelHeader;
        this.parents = new ParentSet[modelHeader.getNumberOfVars()];

        for (int i=0;i<modelHeader.getNumberOfVars();i++) {
            parents[i] = getParentSet(modelHeader.getVariable(i));
        }
    }

    @Override
    public ParentSet getParentSet(Variable var) {
        return parents[var.getVarID()];
    }

    @Override
    public Distribution getDistribution(Variable var) {
        return estimators[var.getVarID()];
    }

    public void setDistribution(Variable var, Distribution distribution){
        this.estimators[var.getVarID()] = distribution;
    }

    @Override
    public int getNumberOfNodes() {
        return modelHeader.getNumberOfVars();
    }

    @Override
    public StaticModelHeader getStaticModelHeader() {
        return modelHeader;
    }


    @Override
    public void initializeDistributions(){

        List<Variable> vars = modelHeader.getVariables(); /* the list of all variables in the BN */
        List<Variable> multinomialParents = new ArrayList<Variable>();
        List<Variable> normalParents = new ArrayList<Variable>();

        /* Initialize the distribution for each variable depending on its distribution type
        as well as the distribution type of its parent set (if that variable has parents)
         */
        for(int i=0;i<vars.size();i++) {

            if (parents[i].getNumberOfParents() == 0) {
                switch (vars.get(i).getDistributionType()) {
                    case MULTINOMIAL:
                        this.estimators[vars.get(i).getVarID()] = new Multinomial(vars.get(i));
                    case GAUSSIAN:
                        this.estimators[vars.get(i).getVarID()] = new Normal(vars.get(i));
                    default:
                        throw new IllegalArgumentException("Error in variable distribution");
                }
            } else {
                switch (vars.get(i).getDistributionType()) {
                    case MULTINOMIAL:
                        /* The parents of a multinomial variable should always be multinomial */
                        this.estimators[vars.get(i).getVarID()] = new Multinomial_MultinomialParents(vars.get(i), parents[i].getParents());
                    case GAUSSIAN:
                        /* The parents of a gaussian variable are either multinomial and/or normal */
                        for (Variable v : parents[i].getParents()) {
                            if (v.getDistributionType().compareTo(DistType.MULTINOMIAL) == 0) {
                                multinomialParents.add(v);
                            } else {
                                normalParents.add(v);
                            }
                        }

                        if (normalParents.size() == 0) {
                            this.estimators[vars.get(i).getVarID()] = new Normal_MultinomialParents(vars.get(i), parents[i].getParents());
                        } else {
                            this.estimators[vars.get(i).getVarID()] = new Normal_MultinomialNormalParents(vars.get(i), parents[i].getParents());
                        }

                    default:
                        throw new IllegalArgumentException("Error in variable distribution");
                }
            }
        }
    }

    @Override
    public boolean containCycles(){
        return true;
    }
}



