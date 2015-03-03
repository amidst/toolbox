package eu.amidst.core.variables.distributionTypes;

import eu.amidst.core.distribution.*;
import eu.amidst.core.variables.DistributionType;
import eu.amidst.core.variables.DistributionTypeEnum;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * Created by andresmasegosa on 26/02/15.
 */
public class MultinomialType extends DistributionType{

    public MultinomialType(Variable variable){
        super(variable);
    }

    @Override
    public boolean isParentCompatible(Variable parent) {
        if (parent.getDistributionTypeEnum()==DistributionTypeEnum.MULTINOMIAL || parent.getDistributionTypeEnum()==DistributionTypeEnum.MULTINOMIAL_LOGISTIC )
            return true;
        else if (parent.getDistributionTypeEnum()==DistributionTypeEnum.DIRICHLET && this.variable.getNumberOfStates()==parent.getNumberOfStates())
            return true;
        else
            return false;
    }


    @Override
    public Multinomial newUnivariateDistribution() {
        return new Multinomial(variable);
    }

    @Override
    public <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents) {
        if (!this.areParentsCompatible(parents))
            throw new IllegalArgumentException("Parents are not compatible");

        if (containsParentsThisDistributionType(parents,DistributionTypeEnum.DIRICHLET)) {
            throw new IllegalArgumentException("To create conditional with Dirichlet parents use the \"toLearningDistribution\" in the exp. family package");
        }

        return (E) new BaseDistribution_MultinomialParents<Multinomial>(this.variable, parents);

    }
}
