package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.header.Variable;

import java.util.List;


public interface EFConditionalDistribution {

    public Variable getMainVariable();

    public List<Variable> getConditioningVariables();

    public double getLogProbabilityConditionedTo(DataInstance instance);

    public SufficientStatistics getFullSufficientStatistics(DataInstance instance);

    public SufficientStatistics getSufficientStatisticsConditionedTo(DataInstance instance);

    public NaturalParameters getNaturalParametersConditionedTo(DataInstance instance);

    public MomentParameters getMomentParametersConditionedTo(DataInstance instance);

}
