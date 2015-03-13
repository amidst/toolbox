package eu.amidst.core.variables.distributionTypes;

import eu.amidst.core.distribution.*;
import eu.amidst.core.variables.DistributionType;
import eu.amidst.core.variables.DistributionTypeEnum;
import eu.amidst.core.variables.Variable;

import java.util.List;

/**
 * Created by andresmasegosa on 26/02/15.
 */
public class NormalType extends DistributionType{

    public NormalType(Variable variable){
        super(variable);
    }

    @Override
    public boolean isParentCompatible(Variable parent) {
        if (parent.getDistributionTypeEnum()==DistributionTypeEnum.MULTINOMIAL ||
                parent.getDistributionTypeEnum()==DistributionTypeEnum.MULTINOMIAL_LOGISTIC ||
                parent.getDistributionTypeEnum()==DistributionTypeEnum.NORMAL)
            return true;
        else
            return false;
    }

    @Override
    public Normal newUnivariateDistribution() {
        Normal normal = new Normal(variable);
        normal.setMean(0);
        normal.setSd(1);

        return normal;
    }

    @Override
    public <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents) {
        if (!this.areParentsCompatible(parents))
            throw new IllegalArgumentException("Parents are not compatible");

        boolean multinomialParents = false;
        boolean normalParents = false;
        for (Variable v : parents) {
            //TODO MultinomialLogist as parent
            if (v.isMultinomial() || (v.isMultinomialLogistic())) {
                multinomialParents = true;
            } else if (v.isNormal()) {
                normalParents = true;
            }
        }

        if (!multinomialParents && !normalParents){
            return (E) new BaseDistribution_MultinomialParents<Normal>(this.variable, parents);
        } else if (multinomialParents && !normalParents) {
            return (E)new BaseDistribution_MultinomialParents<Normal>(this.variable, parents);
        } else if (!multinomialParents && normalParents) {
            return (E)new Normal_NormalParents(this.variable, parents);
        } else if (multinomialParents && normalParents) {
            return (E)new BaseDistribution_MultinomialParents<Normal_NormalParents>(this.variable, parents);
        } else{
            return null;
        }
    }
}
