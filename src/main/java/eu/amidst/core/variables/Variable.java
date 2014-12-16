package eu.amidst.core.variables;

import eu.amidst.core.database.Attribute;

/**
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

    default String toARFFString(){

        if (this.getDistributionType() == DistType.GAUSSIAN) {
            return "@attribute " + this.getName() + " real";
        }else{
            StringBuilder stringBuilder = new StringBuilder("@attribute " + this.getName() + " {");
            MultinomialStateSpace stateSpace = this.getStateSpace();
            stateSpace.getStatesNames().stream().limit(stateSpace.getNumberOfStates()-1).forEach(e -> stringBuilder.append(e+", "));
            stringBuilder.append(stateSpace.getStatesName(stateSpace.getNumberOfStates()-1)+"}");
            return stringBuilder.toString();
        }
    }

}
