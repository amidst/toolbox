package eu.amidst.core.header.statics;

import eu.amidst.core.database.statics.readers.DistType;
import eu.amidst.core.database.statics.readers.StateSpaceType;

/**
 * Created by andresmasegosa on 04/11/14.
 */
public final class VariableBuilder {
    private static String name;
    private static boolean observable;
    private static int numberOfStates;
    private static boolean isLeave = false;
    private static StateSpaceType stateSpaceStateSpaceType;
    private static DistType distributionType;

    public static String getName() {
        return name;
    }

    public static void setName(String name) {
        VariableBuilder.name = name;
    }

    public static boolean isObservable() {
        return observable;
    }

    public static void setObservable(boolean observable) {
        VariableBuilder.observable = observable;
    }

    public static int getNumberOfStates() {
        return numberOfStates;
    }

    public static void setNumberOfStates(int numberOfStates) {
        VariableBuilder.numberOfStates = numberOfStates;
    }

    public static boolean isIsLeave() {
        return isLeave;
    }

    public static void setIsLeave(boolean isLeave) {
        VariableBuilder.isLeave = isLeave;
    }

    public static StateSpaceType getStateSpaceStateSpaceType() {
        return stateSpaceStateSpaceType;
    }

    public static void setStateSpaceStateSpaceType(StateSpaceType stateSpaceStateSpaceType) {
        VariableBuilder.stateSpaceStateSpaceType = stateSpaceStateSpaceType;
    }

    public static DistType getDistributionType() {
        return distributionType;
    }

    public static void setDistributionType(DistType distributionType) {
        VariableBuilder.distributionType = distributionType;
    }
}
