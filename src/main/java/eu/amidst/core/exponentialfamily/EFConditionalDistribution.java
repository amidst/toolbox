package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.header.Variable;

import java.util.List;


public interface EFConditionalDistribution {

    public Variable getMainVariable();

    public List<Variable> getConditioningVariables();

    public double getLogProbabilityConditionedTo(DataInstance instance);

    public SufficientStatistics getSufficientStatistics(DataInstance instance);

    public EFUnivariateDistribution getEFUnivariateInstantiatedTo(DataInstance instance);

    public EFConditionalDistribution getEFConditionalInstantiatedTo(DataInstance instance);

}
