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
<<<<<<< HEAD
    public ParentSet getParentSet(Variable variable) {
        return null;
=======
    public ParentSet<Variable> getParentSet(Variable variable) {
        return parents[variable.getVarID()];
>>>>>>> b3b5ba83cfe63708302404628f2e9fecb1021023
    }

    @Override
    public Distribution getDistribution(Variable var) {
        return estimators[var.getVarID()];
    }

    @Override
<<<<<<< HEAD
    public Distribution getDistribution(int varId) {
        return null;
=======
    public void setDistribution(Variable var, Distribution distribution){
        this.estimators[var.getVarID()]=distribution;
>>>>>>> b3b5ba83cfe63708302404628f2e9fecb1021023
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
