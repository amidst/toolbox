package eu.amidst.core.datastructures.statics;


import eu.amidst.core.datastructures.ParentSet;
import eu.amidst.core.estimator.Estimator;
import eu.amidst.core.header.statics.Variable;
import eu.amidst.core.header.statics.StaticModelHeader;

/**
 * Created by afa on 02/07/14.
 */
public interface BayesianNetwork {

    public ParentSet getParentSet(int varID);

    public Estimator getEstimator(int varId);

    public Variable getVariable(int varID);

    public void initEstimators();

    public int getNumberOfNodes();

    public StaticModelHeader getStaticModelHeader();
}
