package eu.amidst.core.modelstructure.statics.impl;


import eu.amidst.core.modelstructure.ParentSet;
import eu.amidst.core.modelstructure.statics.BayesianNetwork;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.header.statics.Variable;
import eu.amidst.core.header.statics.StaticModelHeader;

/**
 * Created by afa on 02/07/14.
 */
public class BayesianNetworkImpl implements BayesianNetwork {
    private ParentSet<Variable>[] parents;
    private Distribution[] estimators;
    private StaticModelHeader modelHeader;

    public BayesianNetworkImpl(StaticModelHeader modelHeader){

    }

    @Override
    public ParentSet<Variable> getParentSet(Variable variable) {
        return parents[variable.getVarID()];
    }

    @Override
    public Distribution getDistribution(Variable var) {
        return estimators[var.getVarID()];
    }

    @Override
    public void setDistribution(Variable var, Distribution distribution){
        this.estimators[var.getVarID()]=distribution;
    }

    @Override
    public int getNumberOfNodes() {
        return 0;
    }

    @Override
    public StaticModelHeader getStaticModelHeader() {
        return null;
    }
}
