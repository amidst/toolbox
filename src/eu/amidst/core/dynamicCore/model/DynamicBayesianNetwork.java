package eu.amidst.core.DynamicBayesianNetwork;

import eu.amidst.core.Estimators.Estimator;
import eu.amidst.core.StaticBayesianNetwork.ParentSet;
import eu.amidst.core.headers.DynamicModelHeader;
import eu.amidst.core.headers.StaticModelHeader;
import eu.amidst.core.headers.DynamicVariable;

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
