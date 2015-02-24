package eu.amidst.core.variables;

import eu.amidst.core.datastream.Attribute;

/**
 * TODO Implements toString method
 * Created by afa on 02/07/14.
 */
public interface Variable {

    String getName();

    int getVarID();

    boolean isObservable();

    <E extends StateSpace> E getStateSpace();

    int getNumberOfStates();

    DistType getDistributionType();

    boolean isTemporalClone();

    boolean isDynamicVariable();

    Attribute getAttribute();

    @Override
    int hashCode();

    @Override
    boolean equals(Object o);

    default boolean isGaussian(){
        return(this.getDistributionType().compareTo(DistType.GAUSSIAN)==0);
    }

    default boolean isMultinomial(){
        return(this.getDistributionType().compareTo(DistType.MULTINOMIAL)==0);
    }

    default boolean isMultinomialLogistic(){
        return(this.getDistributionType().compareTo(DistType.MULTINOMIAL_LOGISTIC)==0);
    }

    default boolean isIndicator(){
        return(this.getDistributionType().compareTo(DistType.INDICATOR)==0);
    }

    default String toARFFString(){

        if (this.isGaussian()) {
            return "@attribute " + this.getName() + " real";
        }else{
            StringBuilder stringBuilder = new StringBuilder("@attribute " + this.getName() + " {");
            FiniteStateSpace stateSpace = this.getStateSpace();
            stateSpace.getStatesNames().stream().limit(stateSpace.getNumberOfStates()-1).forEach(e -> stringBuilder.append(e+", "));
            stringBuilder.append(stateSpace.getStatesName(stateSpace.getNumberOfStates()-1)+"}");
            return stringBuilder.toString();
        }
    }

}
