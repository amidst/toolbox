package eu.amidst.core.modelstructure.dynamics;

import eu.amidst.core.modelstructure.ParentSet;
import eu.amidst.core.distribution.Distribution;
import eu.amidst.core.header.dynamics.DynamicModelHeader;
import eu.amidst.core.header.Variable;

/**
 * Created by afa on 03/07/14.
 */
public interface DynamicBayesianNetwork {


    public void initDistributions();


    /* Methods accessing the variables in the modelHeader*/
    public int getNumberOfNodes();

    public DynamicModelHeader getDynamicModelHeader();

    public Variable getVariableById(int varID);

    public Variable getTemporalCloneById(int varID);

    public Variable getTemporalCloneFromVariable(Variable variable);


    /* Methods accessing structure at time T*/

    public ParentSet getParentSetTimeT(int varID);

    public Distribution getDistributionTimeT(int varId);


    /* Methods accessing structure at time 0*/

    public ParentSet getParentSetTime0(int varID);

    public Distribution getDistributionTime0(int varId);

}
