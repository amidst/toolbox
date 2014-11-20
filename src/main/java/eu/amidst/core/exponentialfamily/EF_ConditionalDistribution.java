package eu.amidst.core.exponentialfamily;

import eu.amidst.core.variables.Variable;

import java.util.List;


public abstract class EF_ConditionalDistribution extends EF_Distribution {


    protected List<Variable> parents;

    public List<Variable> getConditioningVariables() {
        return this.parents;
    }

    //public  abstract EF_UnivariateDistribution getEFUnivariateByInstantiatingTo(DataInstance instance);

    //public EFConditionalDistribution getEFConditionalByInstantiatingTo(DataInstance instance);
}
