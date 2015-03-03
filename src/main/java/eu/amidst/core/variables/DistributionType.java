package eu.amidst.core.variables;

import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.variables.distributionTypes.MultinomialLogisticType;
import eu.amidst.core.variables.distributionTypes.MultinomialType;
import eu.amidst.core.variables.distributionTypes.NormalType;

import java.io.Serializable;
import java.util.List;

/**
 * Created by andresmasegosa on 26/02/15.
 */
public abstract class DistributionType  implements Serializable {

    private static final long serialVersionUID = 4158293895929418259L;

    protected Variable variable;

    public DistributionType(Variable var_){
        this.variable=var_;
    }

    public abstract boolean isParentCompatible(Variable parent);

    public abstract <E extends UnivariateDistribution> E newUnivariateDistribution();

    public abstract <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents);

    public boolean areParentsCompatible(List<Variable> parents){
        for(Variable parent: parents){
            if (!isParentCompatible(parent))
                return false;
        }
        return true;
    }

    public static boolean containsParentsThisDistributionType(List<Variable> parents, DistributionTypeEnum distributionTypeEnum){
        for (Variable var : parents){
            if (var.getDistributionTypeEnum()==distributionTypeEnum)
                return true;
        }

        return false;
    }

}

