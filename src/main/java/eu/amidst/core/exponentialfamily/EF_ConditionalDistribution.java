package eu.amidst.core.exponentialfamily;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.header.Variable;

import java.util.List;


public abstract class EF_ConditionalDistribution extends EF_Distribution {


    protected List<Variable> parents;


    public List<Variable> getConditioningVariables() {
        return this.parents;
    }

    public double getConditionalProbability(DataInstance dataInstance){
        return Math.exp(this.getLogConditionalProbability(dataInstance));
    }

    public abstract double getLogConditionalProbability(DataInstance instance);

    public  abstract SufficientStatistics getSufficientStatistics(DataInstance instance);

    public  abstract EF_UnivariateDistribution getEFUnivariateByInstantiatingTo(DataInstance instance);

    //public EFConditionalDistribution getEFConditionalByInstantiatingTo(DataInstance instance);
}
