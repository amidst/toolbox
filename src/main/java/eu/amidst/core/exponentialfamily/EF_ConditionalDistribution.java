package eu.amidst.core.exponentialfamily;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Map;


public abstract class EF_ConditionalDistribution extends EF_Distribution {


    protected List<Variable> parents;

    public List<Variable> getConditioningVariables() {
        return this.parents;
    }

    //public  abstract EF_UnivariateDistribution getEFUnivariateByInstantiatingTo(DataInstance instance);

    //public EFConditionalDistribution getEFConditionalByInstantiatingTo(DataInstance instance);

    public abstract double getExpectedLogNormalizer(Variable parent, Map<Variable,MomentParameters> momentChildCoParents);

    public abstract double getExpectedLogNormalizer(Map<Variable,MomentParameters> momentParents);

    public abstract NaturalParameters getExpectedNaturalFromParents(Map<Variable,MomentParameters> momentParents);

    public abstract NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable,MomentParameters> momentChildCoParents);

    public abstract EF_UnivariateDistribution getNewBaseEFUnivariateDistribution();

    public abstract EF_UnivariateDistribution getEFUnivariateDistribution(Assignment assignment);

    public abstract <E extends ConditionalDistribution> E toConditionalDistribution();
    //{
        //throw new UnsupportedOperationException("This EF distribution is not convertible to standard form");
    //}
}
