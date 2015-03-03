package eu.amidst.core.variables;

import eu.amidst.core.distribution.*;
import eu.amidst.core.variables.distributionTypes.MultinomialLogisticType;
import eu.amidst.core.variables.distributionTypes.MultinomialType;
import eu.amidst.core.variables.distributionTypes.NormalType;

/**
 * Created by Hanen on 05/11/14.
 */

public enum DistributionTypeEnum {
    MULTINOMIAL, NORMAL, MULTINOMIAL_LOGISTIC, INV_GAMMA, DIRICHLET;// INDICATOR;

    public <E extends DistributionType> E newDistributionType(Variable var) {
        switch (this) {
            case MULTINOMIAL:
                return (E) new MultinomialType(var);
            case NORMAL:
                return (E) new NormalType(var);
            case MULTINOMIAL_LOGISTIC:
                return (E) new MultinomialLogisticType(var);
            default:
                throw new IllegalArgumentException("Unknown Distribution Type");
        }
    }


    public static <E extends Distribution> E conditionalDistributionToDistribution(ConditionalDistribution dist) {

        if (dist instanceof BaseDistribution_MultinomialParents){
            BaseDistribution_MultinomialParents base = (BaseDistribution_MultinomialParents)dist;
            if (base.getBaseDistribution(0) instanceof Multinomial && base.getConditioningVariables().size()==0) {
                return (E) ((BaseDistribution_MultinomialParents<Multinomial>)base).getBaseDistribution(0);
            }else if (base.getBaseDistribution(0) instanceof Multinomial && base.getConditioningVariables().size()>0) {
                return (E)new Multinomial_MultinomialParents((BaseDistribution_MultinomialParents<Multinomial>)base);
            }else  if (base.getBaseDistribution(0) instanceof Normal && base.getConditioningVariables().size()==0){
                return (E) ((BaseDistribution_MultinomialParents<Normal>)base).getBaseDistribution(0);
            }else  if (base.getBaseDistribution(0) instanceof Normal && base.getConditioningVariables().size()>0){
                return (E)new Normal_MultinomialParents((BaseDistribution_MultinomialParents<Normal>)base);
            }else  if (base.getBaseDistribution(0) instanceof Normal_NormalParents) {
                return (E)new Normal_MultinomialNormalParents((BaseDistribution_MultinomialParents<Normal_NormalParents>)base);
            }else{
                return (E) base;
            }
        }else {
            return (E) dist;
        }

    }

}