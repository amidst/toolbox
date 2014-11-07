/*
package eu.amidst.core.modelstructure.statics.impl;

import eu.amidst.core.modelstructure.ParentSet;
import eu.amidst.core.modelstructure.statics.BayesianNetwork;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.header.Variable;
import eu.amidst.core.header.statics.StaticModelHeader;
import java.util.ArrayList;


/**
 * Created by afa on 02/07/14.
 */

/*

public class BayesianNetworkImpl implements BayesianNetwork {

    private ArrayList<Variable> vars;
    private Distribution[] estimators;
    private StaticModelHeader modelHeader;

    @Override
    public ParentSet getParentSet(Variable variable) {
        return parents[variable.getVarID()];
    }

    @Override
    public Distribution getDistribution(Variable var) {
        return estimators[var.getVarID()];
    }

    public void setDistribution(Variable var, Distribution distribution){
        this.estimators[var.getVarID()]=distribution;
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

    }

    @Override
    public boolean containCycles(){

    }
}

*/


