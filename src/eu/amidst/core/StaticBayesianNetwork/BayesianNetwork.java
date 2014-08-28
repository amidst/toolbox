package eu.amidst.core.StaticBayesianNetwork;

import eu.amidst.core.Estimators.Estimator;
import eu.amidst.core.headers.StaticModelHeader;
import eu.amidst.core.headers.Variable;

/**
 * Created by afa on 02/07/14.
 */
public interface BayesianNetwork {
    public ParentSet getParentSet(int varID);

    public void initEstimators();

    public Estimator getEstimator(int varId);

    public int getNumberOfNodes();

    public Variable getVariable(int varID);

    public StaticModelHeader getStaticModelHeader();
}
