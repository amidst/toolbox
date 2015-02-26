package eu.amidst.core.variables;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.UnivariateDistribution;

import java.util.List;

/**
 * TODO Implements toString method
 * Created by afa on 02/07/14.
 */
public interface Variable {

    String getName();

    int getVarID();

    boolean isObservable();

    <E extends StateSpaceType> E getStateSpaceType();

    int getNumberOfStates();

    DistributionTypeEnum getDistributionTypeEnum();

    <E extends DistributionType> E getDistributionType();

    boolean isTemporalClone();

    boolean isDynamicVariable();

    Attribute getAttribute();

    default <E extends UnivariateDistribution> E newUnivariateDistribution(){
        return this.getDistributionType().newUnivariateDistribution();
    }

    default <E extends ConditionalDistribution> E newConditionalDistribution(List<Variable> parents){
        return this.getDistributionType().newConditionalDistribution(parents);
    }

    @Override
    int hashCode();

    @Override
    boolean equals(Object o);

    default boolean isNormal(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.NORMAL)==0);
    }

    default boolean isMultinomial(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.MULTINOMIAL)==0);
    }

    default boolean isMultinomialLogistic(){
        return(this.getDistributionTypeEnum().compareTo(DistributionTypeEnum.MULTINOMIAL_LOGISTIC)==0);
    }

    //default boolean isIndicator(){
    //    return(this.getDistributionTypeEnum().compareTo(DistType.INDICATOR)==0);
    //}

}
