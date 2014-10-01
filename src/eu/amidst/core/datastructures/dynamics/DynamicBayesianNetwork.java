package eu.amidst.core.datastructures.dynamics;

import eu.amidst.core.datastructures.ParentSet;
import eu.amidst.core.estimator.Estimator;
import eu.amidst.core.header.dynamics.DynamicModelHeader;
import eu.amidst.core.header.dynamics.DynamicVariable;

/**
 * Created by afa on 03/07/14.
 */
public interface DynamicBayesianNetwork {

    public int getMarkovOrder();

    public void initEstimators();

    public int getNumberOfNodes();

    public DynamicModelHeader getDynamicModelHeader();

    public DynamicVariable getVariableById(int varID);

    public DynamicVariable getVariableByTimeId(int varTimeID);

    /* Methods accessing structure at time T*/

    public ParentSet getParentSetTimeT(int varID);

    public Estimator getEstimatorTimeT(int varId);


    /* Methods accessing structure at time 0*/

    public ParentSet getParentSetTime0(int varID);

    public Estimator getEstimatorTime0(int varId);

    /* Methods accessing structure initial times when markov order higher than 1*/

    public ParentSet getParentSetTime0(int varID, int initTime);

    public Estimator getEstimatorTime0(int varId, int initTime);

}
