package eu.amidst.core.modelstructure.statics;


import eu.amidst.core.modelstructure.ParentSet;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.header.statics.Variable;
import eu.amidst.core.header.statics.StaticModelHeader;

/**
 * Created by afa on 02/07/14.
 */
public interface BayesianNetwork {

    public ParentSet getParentSet(int varID);

    public Distribution getEstimator(int varId);

    public Variable getVariable(int varID);

    public void initEstimators();

    public int getNumberOfNodes();

    public StaticModelHeader getStaticModelHeader();
}
