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

}
