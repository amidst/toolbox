package eu.amidst.core.header;

import eu.amidst.core.database.statics.readers.DistType;
import eu.amidst.core.database.statics.readers.StateSpaceType;

/**
 * Created by andresmasegosa on 04/11/14.
 */
public final class VariableBuilder {
    private static String name;
    private static boolean observable;
    private static int numberOfStates;
    private static StateSpaceType stateSpaceStateSpaceType;
    private static DistType distributionType;
    private static int varID;

    public static String getName() {
        return name;
    }

    public static void setName(String name) {
        VariableBuilder.name = name;
    }

    public static boolean isObservable() {
        return observable;
    }

    public static void setIsObservable() {
        VariableBuilder.observable = observable;
    }

    public static int getNumberOfStates() {
        return numberOfStates;
    }

    public static void setNumberOfStates(int numberOfStates) {
        VariableBuilder.numberOfStates = numberOfStates;
    }

    public static StateSpaceType getStateSpaceType() {
        return stateSpaceStateSpaceType;
    }

    public static void setStateSpaceType(StateSpaceType stateSpaceStateSpaceType) {
        VariableBuilder.stateSpaceStateSpaceType = stateSpaceStateSpaceType;
    }

    public static DistType getDistributionType() {
        return distributionType;
    }

    public static void setDistributionType(DistType distributionType) {
        VariableBuilder.distributionType = distributionType;
    }

    public static void setVarID(int varID){
        VariableBuilder.varID = varID;
    }

    public static int getVarID(){
        return VariableBuilder.varID;
    }
}
