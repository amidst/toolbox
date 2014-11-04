package eu.amidst.core.distribution;


import eu.amidst.core.header.statics.Variable;
import java.util.List;

/**
 * Created by afa on 03/11/14.
 */
public interface ConditionalDistribution extends Distribution {

    //Is it necessary? Is the same as getParents from BN structure
    public List<Variable> getConditioningVariables();

    public double getProbability (double value, Assignment parentsAssignment);

    public double getLogProbability (double value, Assignment parentsAssignment);
}
