package eu.amidst.core.modelstructure.statics;


import eu.amidst.core.modelstructure.ParentSet;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.header.Variable;
import eu.amidst.core.header.statics.StaticModelHeader;

/**
 * Created by afa on 02/07/14.
 */
public interface BayesianNetwork {

    public ParentSet getParentSet(Variable variable);

    public Distribution getDistribution(Variable var);

    public void setDistribution(Variable var, Distribution distribution);

    public int getNumberOfNodes();

    public StaticModelHeader getStaticModelHeader();

    public void initializeDistributions();

    public boolean containCycles();
}
