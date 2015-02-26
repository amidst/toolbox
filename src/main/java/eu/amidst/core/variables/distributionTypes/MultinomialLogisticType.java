package eu.amidst.core.variables.distributionTypes;

import eu.amidst.core.distribution.*;
import eu.amidst.core.variables.DistributionType;
import eu.amidst.core.variables.DistributionTypeEnum;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * Created by andresmasegosa on 26/02/15.
 */
public class MultinomialLogisticType extends DistributionType{

    public MultinomialLogisticType(Variable variable){
        super(variable);
    }

    @Override
    public boolean isParentCompatible(Variable parent) {
            return true;
    }

    @Override
    public Multinomial newUnivariateDistribution() {
        return new Multinomial(variable);
    }

    @Override
    public <E extends ConditionalDistribution> E  newConditionalDistribution(List<Variable> parents) {
        if (!this.areParentsCompatible(parents))
            throw new IllegalArgumentException("Parents are not compatible");

        return (E) new Multinomial_LogisticParents(this.variable,parents);
    }
}
