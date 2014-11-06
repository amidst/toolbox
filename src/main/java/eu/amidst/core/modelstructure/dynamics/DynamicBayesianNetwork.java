package eu.amidst.core.modelstructure.dynamics;

import eu.amidst.core.modelstructure.ParentSet;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.header.dynamics.DynamicModelHeader;
import eu.amidst.core.header.statics.Variable;

/**
 * Created by afa on 03/07/14.
 */
public interface DynamicBayesianNetwork {


    public void initEstimators();

    public int getNumberOfNodes();

    public DynamicModelHeader getDynamicModelHeader();

    public Variable getVariableById(int varID);

    public Variable getVariableByTimeId(int varTimeID);

    /* Methods accessing structure at time T*/

    public ParentSet getParentSetTimeT(int varID);

    public Distribution getEstimatorTimeT(int varId);


    /* Methods accessing structure at time 0*/

    public ParentSet getParentSetTime0(int varID);

    public Distribution getEstimatorTime0(int varId);

    /* Methods accessing structure initial times when markov order higher than 1*/

    public ParentSet getParentSetTime0(int varID, int initTime);

    public Distribution getEstimatorTime0(int varId, int initTime);

}
